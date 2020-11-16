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
import org.embulk.config.ConfigDiff;
import org.embulk.util.config.ConfigMapperFactory;
import org.junit.jupiter.api.Test;

public class TestCharsetGuess {
    @Test
    public void test() {
        assertCharset("UTF-8", "abc".getBytes(StandardCharsets.UTF_8));
        assertCharset("MS932", "いろはにほへとちりぬるを".getBytes(Charset.forName("Shift_JIS")));
        assertCharset("EUC-JP", "わかよたれそつねらなむ".getBytes(Charset.forName("EUC-JP")));
    }

    private static void assertCharset(final String expectedCharset, final byte[] sample) {
        final ConfigDiff configDiff = CharsetGuess.of(CONFIG_MAPPER_FACTORY).guess(new FakeBufferImpl(sample));
        assertEquals(expectedCharset, configDiff.getNested("parser").get(String.class, "charset"));
    }

    private static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.withDefault();
}
