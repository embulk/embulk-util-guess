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

import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.util.config.ConfigMapperFactory;

/**
 * Guesses a newline from {@link org.embulk.spi.Buffer}.
 *
 * <p>It reimplements {@code NewlineGuessPlugin} in {@code /embulk/guess/newline.rb}.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/newline.rb">newline.rb</a>
 */
public final class NewlineGuess {
    private NewlineGuess(final ConfigMapperFactory configMapperFactory) {
        this.configMapperFactory = configMapperFactory;
        this.charsetGuess = CharsetGuess.of(configMapperFactory);
    }

    public static NewlineGuess of(final ConfigMapperFactory configMapperFactory) {
        return new NewlineGuess(configMapperFactory);
    }

    /**
     * Guesses a newline from {@link org.embulk.spi.Buffer}.
     *
     * @param config  a partial config which it starts guessing from
     * @param sample  the byte sequence to be guessed
     * @return {@link org.embulk.config.ConfigDiff} guessed
     */
    public ConfigDiff guess(final ConfigSource config, final Buffer sample) {
        if (!config.getNestedOrGetEmpty("parser").has("charset")) {
            return this.charsetGuess.guess(sample);
        }

        final int sampleLength = sample.limit();
        final byte[] sampleArray = new byte[sampleLength];
        sample.getBytes(0, sampleArray, 0, sampleLength);

        final int crCount = count(sampleArray, CR);
        final int lfCount = count(sampleArray, LF);
        final int crlfCount = count(sampleArray, CRLF);

        final ConfigDiff newlineConfig = configMapperFactory.newConfigDiff();
        if (crlfCount > crCount / 2 && crlfCount > lfCount / 2) {
            newlineConfig.set("newline", "CRLF");
        } else if (crCount > lfCount / 2) {
            newlineConfig.set("newline", "CR");
        } else {
            newlineConfig.set("newline", "LF");
        }

        final ConfigDiff parserConfig = configMapperFactory.newConfigDiff();
        parserConfig.setNested("parser", newlineConfig);
        return parserConfig;
    }

    static int countForTesting(final byte[] array, final byte[] target) {
        return count(array, target);
    }

    private static int count(final byte[] array, final byte[] target) {
        if (target.length == 0) {
            return 0;
        }

        int count = 0;
        outer: for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            count++;
        }
        return count;
    }

    private static final byte[] CRLF = { (byte) '\r', (byte) '\n' };
    private static final byte[] CR = { (byte) '\r' };
    private static final byte[] LF = { (byte) '\n' };

    private final ConfigMapperFactory configMapperFactory;
    private final CharsetGuess charsetGuess;
}
