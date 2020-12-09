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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.embulk.config.ConfigSource;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.jupiter.api.Test;

public class TestTextGuessHelper {
    @Test
    public void testCharsetShiftJis() {
        assertTextGuess("いろはにほへとちりぬるを", "いろはにほへとちりぬるを".getBytes(Charset.forName("Shift_JIS")));
    }

    @Test
    public void testCharsetEucJp() {
        assertTextGuess("わかよたれそつねらなむ", "わかよたれそつねらなむ".getBytes(Charset.forName("EUC-JP")));
    }

    @Test
    public void testNewlineDefault() {
        assertTextGuess("The quick brown fox\r\njumps over\r\nthe lazy dog.",
                        "The quick brown fox\njumps over\nthe lazy dog.".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testNewlineCr() {
        assertTextGuess("The quick brown fox\rjumps over\rthe lazy dog.",
                        "The quick brown fox\njumps over\nthe lazy dog.".getBytes(StandardCharsets.UTF_8),
                        "CR");
    }

    private static void assertTextGuess(final String expectedString, final byte[] sample, final String newline) {
        final ConfigSource parserConfig = CONFIG_MAPPER_FACTORY.newConfigSource();
        if (newline != null) {
            parserConfig.set("newline", newline);
        }
        final ConfigSource config = CONFIG_MAPPER_FACTORY.newConfigSource();
        config.setNested("parser", parserConfig);

        assertEquals(expectedString, TextGuessHelper.of(CONFIG_MAPPER_FACTORY).toText(config, new FakeBufferImpl(sample)));
    }

    private static void assertTextGuess(final String expectedString, final byte[] sample) {
        assertTextGuess(expectedString, sample, null);
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.withDefault();
}
