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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.embulk.config.ConfigDiff;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.guess.SchemaGuess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestSchemaGuess {
    @Test
    public void testGuess() {
        final LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        record.put("int", "1");
        record.put("str", "a");
        final ArrayList<LinkedHashMap<String, Object>> records = new ArrayList<>();
        records.add(record);

        final List<ConfigDiff> guessed = fromLinkedHashMap(records);
        assertEquals(2, guessed.size());
        assertEquals("0", guessed.get(0).get(String.class, "index"));
        assertEquals(0, guessed.get(0).get(int.class, "index"));
        assertEquals("int", guessed.get(0).get(String.class, "name"));
        assertEquals("long", guessed.get(0).get(String.class, "type"));
        assertFalse(guessed.get(0).has("format"));
        assertEquals("1", guessed.get(1).get(String.class, "index"));
        assertEquals(1, guessed.get(1).get(int.class, "index"));
        assertEquals("str", guessed.get(1).get(String.class, "name"));
        assertEquals("string", guessed.get(1).get(String.class, "type"));
        assertFalse(guessed.get(1).has("format"));
    }

    @Test
    public void testCoalesce1() {
        final LinkedHashMap<String, Object> record1 = new LinkedHashMap<>();
        record1.put("a", "20160101");
        final LinkedHashMap<String, Object> record2 = new LinkedHashMap<>();
        record2.put("a", "20160101");
        final ArrayList<LinkedHashMap<String, Object>> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);

        final List<ConfigDiff> guessed = fromLinkedHashMap(records);
        assertEquals(1, guessed.size());
        assertEquals("0", guessed.get(0).get(String.class, "index"));
        assertEquals(0, guessed.get(0).get(int.class, "index"));
        assertEquals("a", guessed.get(0).get(String.class, "name"));
        assertEquals("timestamp", guessed.get(0).get(String.class, "type"));
        assertEquals("%Y%m%d", guessed.get(0).get(String.class, "format"));
    }

    @Test
    public void testCoalesce2() {
        final LinkedHashMap<String, Object> record1 = new LinkedHashMap<>();
        record1.put("a", "20160101");
        final LinkedHashMap<String, Object> record2 = new LinkedHashMap<>();
        record2.put("a", "20160101");
        final LinkedHashMap<String, Object> record3 = new LinkedHashMap<>();
        record3.put("a", "12345678");
        final ArrayList<LinkedHashMap<String, Object>> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);
        records.add(record3);

        final List<ConfigDiff> guessed = fromLinkedHashMap(records);
        assertEquals(1, guessed.size());
        assertEquals("0", guessed.get(0).get(String.class, "index"));
        assertEquals(0, guessed.get(0).get(int.class, "index"));
        assertEquals("a", guessed.get(0).get(String.class, "name"));
        assertEquals("long", guessed.get(0).get(String.class, "type"));
    }

    @Test
    public void testCoalesce3TimestampFormat() {
        final LinkedHashMap<String, Object> record1 = new LinkedHashMap<>();
        record1.put("a", "2016-01-01T12:34:56");
        final LinkedHashMap<String, Object> record2 = new LinkedHashMap<>();
        record2.put("a", "2016/01/01 12:34:56");
        final ArrayList<LinkedHashMap<String, Object>> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);

        final List<ConfigDiff> guessed = fromLinkedHashMap(records);
        assertEquals(1, guessed.size());
        assertEquals("0", guessed.get(0).get(String.class, "index"));
        assertEquals(0, guessed.get(0).get(int.class, "index"));
        assertEquals("a", guessed.get(0).get(String.class, "name"));
        assertEquals("timestamp", guessed.get(0).get(String.class, "type"));
        // Not testing timestamp format.
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false",
            "t",
            "f",
            "yes",
            "no",
            "y",
            "n",
            "on",
            "off",
    })
    public void testBoolean(final String str) {
        // If at least one of three kinds of boolean strings (i.e., downcase, upcase, capitalize) is
        // mistakenly detected as "string," the guesser concludes the column type is "string."
        final LinkedHashMap<String, Object> record1 = new LinkedHashMap<>();
        record1.put("a", str.toLowerCase());
        final LinkedHashMap<String, Object> record2 = new LinkedHashMap<>();
        record2.put("a", str.toUpperCase());
        final LinkedHashMap<String, Object> record3 = new LinkedHashMap<>();
        record3.put("a", str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase());
        final ArrayList<LinkedHashMap<String, Object>> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);
        records.add(record3);

        final List<ConfigDiff> guessed = fromLinkedHashMap(records);
        assertEquals(1, guessed.size());
        assertEquals("0", guessed.get(0).get(String.class, "index"));
        assertEquals(0, guessed.get(0).get(int.class, "index"));
        assertEquals("a", guessed.get(0).get(String.class, "name"));
        assertEquals("boolean", guessed.get(0).get(String.class, "type"));
    }

    private static List<ConfigDiff> fromLinkedHashMap(final List<LinkedHashMap<String, Object>> listOfMap) {
        return SchemaGuess.of(CONFIG_MAPPER_FACTORY).fromLinkedHashMapRecords(listOfMap);
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.withDefault();
}
