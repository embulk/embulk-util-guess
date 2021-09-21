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
import java.util.Objects;
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
     * Guesses a schema from a list of sample records in {@link java.util.LinkedHashMap}s.
     *
     * <p>It returns a list of {@link org.embulk.config.ConfigDiff} in contrast to the original Ruby method returning
     * {@link org.embulk.spi.Schema},
     *
     * @param columnNames  a list of column names in order
     * @param samples  a list of sample data
     * @return a list of {@link org.embulk.config.ConfigDiff}s of the schema guessed
     */
    public List<ConfigDiff> fromListRecords(final List<String> columnNames, final List<List<Object>> samples) {
        final List<GuessedType> columnTypes = typesFromListRecords(samples);
        if (columnNames.size() != columnTypes.size()) {
            throw new IllegalArgumentException("The number of column names are different from actual sample data.");
        }

        final List<ConfigDiff> columns = new ArrayList<>();

        final int size = columnNames.size();
        for (int i = 0; i < size; ++i) {
            final GuessedType type = columnTypes.get(i);
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

    private static class GuessedType implements Comparable<GuessedType> {
        private GuessedType(final String string, final String formatOrTimeValue) {
            this.string = string;
            this.formatOrTimeValue = formatOrTimeValue;
        }

        private GuessedType(final String string) {
            this(string, null);
        }

        static GuessedType timestamp(final String formatOrTimeValue) {
            return new GuessedType("timestamp", formatOrTimeValue);
        }

        boolean isTimestamp() {
            return this.string.equals("timestamp");
        }

        String getFormatOrTimeValue() {
            return this.formatOrTimeValue;
        }

        /**
         * Returns {@code true} if just its type is the same with another object's.
         *
         * <p>Note that it does not take care of {@code formatOrTimeValue}. It returns {@code true} if
         * both are {@code "timestamp"}, even if their {@code formatOrTimeValue}s are different.
         *
         * <p>It is expected to be called only from {@code mergeType} which should merge {@code "timestamp"}
         * and {@code "timestamp"} into {@code "timestamp"}, even if their {@code formatOrTimeValue}s are
         * different. Those {@code formatOrTimeValue}s are considered in {@code mergeTypes} later.
         */
        boolean typeEquals(final Object otherObject) {
            if (!(otherObject instanceof GuessedType)) {
                return false;
            }
            final GuessedType other = (GuessedType) otherObject;
            return Objects.equals(this.string, other.string);
        }

        /**
         * Returns {@code true} if its type and {@code formatOrTimeValue} are the same with another object's.
         *
         * <p>Note that it takes care of {@code formatOrTimeValue}. This equality is used out of {@code SchemaGuess},
         * in {@code CSVGuessPlugin} to compare lists of {@code GuessedType}s.
         */
        @Override
        public boolean equals(final Object otherObject) {
            if (!(otherObject instanceof GuessedType)) {
                return false;
            }
            final GuessedType other = (GuessedType) otherObject;
            return Objects.equals(this.string, other.string) && Objects.equals(this.formatOrTimeValue, other.formatOrTimeValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.string, this.formatOrTimeValue);
        }

        @Override
        public int compareTo(final GuessedType other) {
            if (this.formatOrTimeValue != null && other.formatOrTimeValue != null) {
                return this.formatOrTimeValue.compareTo(other.formatOrTimeValue);
            }
            return this.string.compareTo(other.string);
        }

        @Override
        public String toString() {
            return this.string;
        }

        static final GuessedType BOOLEAN = new GuessedType("boolean");
        static final GuessedType DOUBLE = new GuessedType("double");
        static final GuessedType JSON = new GuessedType("json");
        static final GuessedType LONG = new GuessedType("long");
        static final GuessedType STRING = new GuessedType("string");

        private final String string;
        private final String formatOrTimeValue;
    }

    private List<GuessedType> typesFromListRecords(final List<List<Object>> samples) {
        final int maxRecords = samples.stream().mapToInt(List::size).max().orElse(0);
        if (maxRecords <= 0) {
            return Collections.emptyList();
        }
        final ArrayList<ArrayList<GuessedType>> columnarTypes = new ArrayList<>(maxRecords);
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

    private GuessedType guessType(final Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map || value instanceof List) {
            return GuessedType.JSON;
        }
        final String str = value.toString();

        if (TRUE_STRINGS.contains(str) || FALSE_STRINGS.contains(str)) {
            return GuessedType.BOOLEAN;
        }

        if (this.timeFormatGuess.guess(Arrays.asList(str)) != null) {
            return GuessedType.timestamp(str);
        }

        try {
            if (Long.valueOf(str).toString().equals(str)) {
                return GuessedType.LONG;
            }
        } catch (final RuntimeException ex) {
            // Pass-through.
        }

        // Introduce a regular expression to make better suggestion to double type. It refers to Guava 21.0's regular
        // expression in Doubles#fpPattern() but, there're difference as following:
        // * It intentionaly rejects float values when they start with "0" like "001.0", "010.01". "0.1" is ok.
        // * It doesn't support hexadecimal representation. It could be improved more later.
        if (DOUBLE_PATTERN.matcher(str).matches()) {
            return GuessedType.DOUBLE;
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
                return GuessedType.JSON;
            }
        } catch (final Exception ex) {
            // Pass-through.
        }

        return GuessedType.STRING;
    }

    private GuessedType mergeTypes(final List<GuessedType> types) {
        final GuessedType t = Optional.ofNullable(types.stream().reduce(null, (final GuessedType merged, final GuessedType type) -> {
            return mergeType(merged, type);
        })).orElse(GuessedType.STRING);
        if (t.isTimestamp()) {
            final String format = this.timeFormatGuess.guess(
                    types.stream()
                            .map(type -> type.isTimestamp() ? type.getFormatOrTimeValue() : null)
                            .filter(type -> type != null)
                            .collect(Collectors.toList()));
            return GuessedType.timestamp(format);
        }
        return t;
    }

    private static GuessedType mergeType(final GuessedType type1, final GuessedType type2) {
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

    private static GuessedType coalesceType(final GuessedType type1, final GuessedType type2) {
        final GuessedType[] types = { type1, type2 };
        Arrays.sort(types);

        if (types[0] == GuessedType.DOUBLE && types[1] == GuessedType.LONG) {
            return GuessedType.DOUBLE;
        } else if (types[0] == GuessedType.BOOLEAN && types[1] == GuessedType.LONG) {
            return GuessedType.LONG;
        } else if (types[0] == GuessedType.LONG && types[1].isTimestamp()) {
            return GuessedType.LONG;
        }
        return GuessedType.STRING;
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
