/*
 * Copyright 2020 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.util.guess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.embulk.config.ConfigDiff;
import org.embulk.util.config.ConfigMapperFactory;

/**
 * Guesses a schema from sample objects.
 *
 * <p>It reimplements {@code SchemaGuess} in {@code /embulk/guess/schema_guess.rb}.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/schema_guess.rb">schema_guess.rb</a>
 */
public final class SchemaGuess {
    private SchemaGuess(final ConfigMapperFactory configMapperFactory, final TimeFormatGuess timeFormatGuess) {
        this.configMapperFactory = configMapperFactory;
        this.timeFormatGuess = timeFormatGuess;
    }

    public static SchemaGuess of(final ConfigMapperFactory configMapperFactory) {
        return new SchemaGuess(configMapperFactory, TimeFormatGuess.of());
    }

    /**
     * Guesses a schema from a list of sample records in {@link java.util.LinkedHashMap}s.
     *
     * <p>It returns a list of {@link org.embulk.config.ConfigDiff} in contrast to the original Ruby method returning
     * {@link org.embulk.spi.Schema},
     *
     * <p>Note that it assumes {@link java.util.LinkedHashMap} because an order matters in schema.
     *
     * @param listOfMap  a list of {@link java.util.LinkedHashMap}s which represent a record for each
     * @return a list of {@link org.embulk.config.ConfigDiff}s of the schema guessed
     */
    public List<ConfigDiff> fromLinkedHashMapRecords(final List<LinkedHashMap<String, Object>> listOfMap) {
        if (listOfMap.isEmpty()) {
            throw new RuntimeException("SchemaGuess cannot guess Schema from no records.");
        }
        final List<String> columnNames = Collections.unmodifiableList(new ArrayList<String>(listOfMap.get(0).keySet()));

        final List<List<Object>> samples = new ArrayList<>();
        for (final LinkedHashMap<String, Object> map : listOfMap) {
            final ArrayList<Object> record = new ArrayList<>();
            for (final String name : columnNames) {
                record.add(map.get(name));
            }
            samples.add(Collections.unmodifiableList(record));
        }
        return this.fromListRecords(columnNames, Collections.unmodifiableList(samples));
    }

    /**
     * Guesses a schema from a list of sample records in {@link java.util.List}s.
     *
     * <p>It returns a list of {@link org.embulk.config.ConfigDiff} in contrast to the original Ruby method returning
     * {@link org.embulk.spi.Schema},
     *
     * @param columnNames  a list of column names in order
     * @param samples  a list of sample data
     * @return a list of {@link org.embulk.config.ConfigDiff}s of the schema guessed
     */
    public List<ConfigDiff> fromListRecords(final List<String> columnNames, final List<List<Object>> samples) {
        final List<GuesstimatedType> columnTypes = typesFromListRecords(samples);
        if (columnNames.size() != columnTypes.size()) {
            throw new IllegalArgumentException("The number of column names are different from actual sample data.");
        }

        final List<ConfigDiff> columns = new ArrayList<>();

        final int size = columnNames.size();
        for (int i = 0; i < size; ++i) {
            final GuesstimatedType type = columnTypes.get(i);
            final String name = columnNames.get(i);

            final ConfigDiff column = this.configMapperFactory.newConfigDiff();
            column.set("index", i);
            column.set("name", name);
            column.set("type", type.toString());
            if (type.isTimestamp()) {
                column.set("format", type.getFormatOrTimeValue());
            }
            columns.add(column);
        }

        return Collections.unmodifiableList(columns);
    }

    /**
     * Guesses types from a list of sample records in {@link java.util.List}s.
     *
     * <p>It returns a list of {@link GuesstimatedType}.
     *
     * @param samples  a list of sample data
     * @return a list of {@link GuesstimatedType}s
     */
    public List<GuesstimatedType> typesFromListRecords(final List<List<Object>> samples) {
        final int maxRecords = samples.stream().mapToInt(List::size).max().orElse(0);
        if (maxRecords <= 0) {
            return Collections.emptyList();
        }
        final ArrayList<ArrayList<GuesstimatedType>> columnarTypes = new ArrayList<>(maxRecords);
        for (int i = 0; i < maxRecords; ++i) {
            columnarTypes.add(new ArrayList<>());
        }

        for (final List<Object> record : samples) {
            for (int i = 0; i < record.size(); ++i) {
                final Object value = record.get(i);
                columnarTypes.get(i).add(this.guessType(value));
            }
        }

        return columnarTypes.stream().map(types -> this.mergeTypes(types)).collect(Collectors.toList());
    }

    private GuesstimatedType guessType(final Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map || value instanceof List) {
            return GuesstimatedType.JSON;
        }
        final String str = value.toString();

        if (TRUE_STRINGS.contains(str) || FALSE_STRINGS.contains(str)) {
            return GuesstimatedType.BOOLEAN;
        }

        if (this.timeFormatGuess.guess(Arrays.asList(str)) != null) {
            return GuesstimatedType.timestamp(str);
        }

        try {
            if (Long.valueOf(str).toString().equals(str)) {
                return GuesstimatedType.LONG;
            }
        } catch (final RuntimeException ex) {
            // Pass-through.
        }

        // Introduce a regular expression to make better suggestion to double type. It refers to Guava 21.0's regular
        // expression in Doubles#fpPattern() but, there're difference as following:
        // * It intentionaly rejects float values when they start with "0" like "001.0", "010.01". "0.1" is ok.
        // * It doesn't support hexadecimal representation. It could be improved more later.
        if (DOUBLE_PATTERN.matcher(str).matches()) {
            return GuesstimatedType.DOUBLE;
        }

        if (str.isEmpty()) {
            return null;
        }

        // It was implemented as below when SchemaGuess was implemented with Ruby.
        //
        // begin
        //   JSON.parse(str)
        //   return "json"
        // rescue
        // end
        //
        // The 'json' gem 1.8.X raised JSON::ParserError by default because an older JSON RFC 4627
        // accepted only an object an an array as its top-level value.
        // https://datatracker.ietf.org/doc/html/rfc4627#section-2
        //
        // The 'json' gem 2.0+ started to accept any JSON value because a newer JSON RFC 7159
        // changed the constraint that it be an object or array.
        // https://datatracker.ietf.org/doc/html/rfc7159#section-2
        // https://bugs.ruby-lang.org/issues/13070
        // https://bugs.ruby-lang.org/issues/14054
        //
        // Embulk till v0.10.21 had expected (embedded) JRuby 9.1.15.0, which bundled 'json' 1.8.X.
        // JSON.parse(str) here did not accept a quoted string such as '"example_string"'.
        //
        // (JFYI, JRuby 9.2+ bundles 'json' 2.0+. If a user used JRuby 9.2+ with Embulk v0.10.22+,
        // the schema guess should have behaved a little bit different against a quoted string.)
        //
        // We replaced JSON.parse(str) to Jackson ObjectMapper#readTree(str) when reimplementing
        // the guess in Java. On the other hand, Jackson's ObjectMapper followed the new RFC.
        //
        // Therefore, we introduced an explicit check to accept only an object or an array so that:
        // 1) The guess keeps compatible with older versions.
        // 2) The guess behaves more natural -- just a quoted string is naturally parsed as STRING.
        try {
            final JsonNode node = new ObjectMapper().readTree(str);
            if (node.isContainerNode()) {
                return GuesstimatedType.JSON;
            }
        } catch (final Exception ex) {
            // Pass-through.
        }

        return GuesstimatedType.STRING;
    }

    private GuesstimatedType mergeTypes(final List<GuesstimatedType> types) {
        final GuesstimatedType t = Optional.ofNullable(types.stream().reduce(null, (final GuesstimatedType merged, final GuesstimatedType type) -> {
            return mergeType(merged, type);
        })).orElse(GuesstimatedType.STRING);
        if (t.isTimestamp()) {
            final String format = this.timeFormatGuess.guess(
                    types.stream()
                            .map(type -> (type != null && type.isTimestamp()) ? type.getFormatOrTimeValue() : null)
                            .filter(type -> type != null)
                            .collect(Collectors.toList()));
            return GuesstimatedType.timestamp(format);
        }
        return t;
    }

    private static GuesstimatedType mergeType(final GuesstimatedType type1, final GuesstimatedType type2) {
        if (type1 == null) {
            return type2;
        } else if (type2 == null) {
            return type1;
        } else if (type1.typeEquals(type2)) {
            return type1;
        } else {
            return coalesceType(type1, type2);
        }
    }

    private static GuesstimatedType coalesceType(final GuesstimatedType type1, final GuesstimatedType type2) {
        final GuesstimatedType[] types = { type1, type2 };
        Arrays.sort(types);

        if (types[0] == GuesstimatedType.DOUBLE && types[1] == GuesstimatedType.LONG) {
            return GuesstimatedType.DOUBLE;
        } else if (types[0] == GuesstimatedType.BOOLEAN && types[1] == GuesstimatedType.LONG) {
            return GuesstimatedType.LONG;
        } else if (types[0] == GuesstimatedType.LONG && types[1].isTimestamp()) {
            return GuesstimatedType.LONG;
        }
        return GuesstimatedType.STRING;
    }

    private static final Pattern DOUBLE_PATTERN = Pattern.compile(
            "^[+-]?(NaN|Infinity|([1-9]\\d*|0)(\\.\\d+)([eE][+-]?\\d+)?[fFdD]?)$");

    // taken from CsvParserPlugin.TRUE_STRINGS
    private static final String[] TRUE_STRINGS_ARRAY = {
        "true", "True", "TRUE",
        "yes", "Yes", "YES",
        "t", "T", "y", "Y",
        "on", "On", "ON",
    };

    private static final String[] FALSE_STRINGS_ARRAY = {
        "false", "False", "FALSE",
        "no", "No", "NO",
        "f", "F", "n", "N",
        "off", "Off", "OFF",
    };

    private static final Set<String> TRUE_STRINGS;
    private static final Set<String> FALSE_STRINGS;

    static {
        TRUE_STRINGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TRUE_STRINGS_ARRAY)));
        FALSE_STRINGS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(FALSE_STRINGS_ARRAY)));
    }

    private final ConfigMapperFactory configMapperFactory;

    private final TimeFormatGuess timeFormatGuess;
}
