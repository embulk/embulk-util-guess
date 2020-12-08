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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.jupiter.api.Test;

public class TestLineGuessHelper {
    @Test
    public void testOneLine() {
        final int size = 65536;

        final byte[] longByteArray = new byte[size];
        Arrays.fill(longByteArray, (byte) '*');
        assertLineGuess(Arrays.asList(), longByteArray);

        longByteArray[size - 1] = (byte) '\n';
        assertLineGuess(Arrays.asList(), longByteArray);
        assertLineGuess(Arrays.asList(repeatedString('*', size - 1)), longByteArray, "LF");

        longByteArray[size - 2] = (byte) '\r';
        longByteArray[size - 1] = (byte) '\n';
        assertLineGuess(Arrays.asList(repeatedString('*', size - 2)), longByteArray);
        assertLineGuess(Arrays.asList(repeatedString('*', size - 2)), longByteArray, "LF");
    }

    @Test
    public void testSimpleMultilines() {
        assertLineGuess(Arrays.asList("abc", "def"), "abc\r\ndef\r\nghi".getBytes(StandardCharsets.UTF_8));
        assertLineGuess(Arrays.asList("abc", "def", "ghi"), "abc\r\ndef\r\nghi\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static void assertLineGuess(final List<String> expectedStrings, final byte[] sample, final String newline) {
        final ConfigSource parserConfig = CONFIG_MAPPER_FACTORY.newConfigSource();
        if (newline != null) {
            parserConfig.set("newline", newline);
        }
        final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
        config.setNested("parser", parserConfig);

        assertEquals(expectedStrings,
                     LineGuessHelper.of(CONFIG_MAPPER_FACTORY).toLines(config, new FakeBufferImpl(sample)));
    }

    private static void assertLineGuess(final List<String> expectedStrings, final byte[] sample) {
        assertLineGuess(expectedStrings, sample, null);
    }

    private static String repeatedString(final char ch, final int times) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(ch);
        }
        return builder.toString();
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.withDefault();
}
