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

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.jupiter.api.Test;

public class TestNewlineGuess {
    @Test
    public void test() {
        assertNewline("CRLF", "UTF-8", ARRAY1);
        assertNewline("CR", "UTF-8", ARRAY2);
        assertNewline("CR", "UTF-8", ARRAY3);
    }

    @Test
    public void testCount() {
        assertEquals(2, NewlineGuess.countForTesting(ARRAY1, CRLF));
        assertEquals(3, NewlineGuess.countForTesting(ARRAY1, CR));
        assertEquals(3, NewlineGuess.countForTesting(ARRAY1, LF));
    }

    private static void assertNewline(final String expectedNewline, final String charset, final byte[] sample) {
        final ConfigSource parserConfig = configMapperFactory.newConfigSource();
        parserConfig.set("charset", charset);
        final ConfigSource config = configMapperFactory.newConfigSource();
        config.setNested("parser", parserConfig);
        final ConfigDiff configDiff = NewlineGuess.of(configMapperFactory).guess(config, new FakeBufferImpl(sample));
        assertEquals(expectedNewline, configDiff.getNested("parser").get(String.class, "newline"));
    }

    private static final byte[] ARRAY1 = {
        (byte) 'a',
        (byte) '\r',
        (byte) '\r',
        (byte) '\n',
        (byte) 'a',
        (byte) '\r',
        (byte) '\n',
        (byte) '\n',
    };

    private static final byte[] ARRAY2 = {
        (byte) 'a',
        (byte) '\r',
        (byte) '\n',
        (byte) 'a',
        (byte) '\r',
        (byte) '\n',
        (byte) '\n',
        (byte) 'a',
        (byte) '\r',
        (byte) '\r',
    };

    private static final byte[] ARRAY3 = {
        (byte) 'a',
        (byte) '\n',
        (byte) '\r',
        (byte) 'a',
        (byte) '\n',
        (byte) '\r',
        (byte) 'a',
        (byte) '\n',
        (byte) '\r',
    };

    private static final byte[] CRLF = { (byte) '\r', (byte) '\n' };
    private static final byte[] CR = { (byte) '\r' };
    private static final byte[] LF = { (byte) '\n' };

    private static final ConfigMapperFactory configMapperFactory = ConfigMapperFactory.withDefault();
}
