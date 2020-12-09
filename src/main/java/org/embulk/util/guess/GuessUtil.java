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

import java.nio.charset.Charset;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.text.LineDelimiter;
import org.embulk.util.text.Newline;

final class GuessUtil {
    private GuessUtil() {
        // No instantiation.
    }

    static Charset getCharset(final ConfigSource parserConfig, final ConfigMapperFactory configMapperFactory, final Buffer sample) {
        final String charsetString;
        if (parserConfig.has("charset")) {
            charsetString = parserConfig.get(String.class, "charset", "utf-8");
        } else {
            charsetString = CharsetGuess.of(configMapperFactory)
                .guess(sample)
                .getNestedOrGetEmpty("parser")
                .get(String.class, "charset", "utf-8");
        }

        return Charset.forName(charsetString);
    }

    static LineDelimiter getLineDelimiter(final ConfigSource parserConfig) {
        final String lineDelimiterString = parserConfig.get(String.class, "line_delimiter_recognized", null);
        if (lineDelimiterString == null) {
            return null;
        }
        return LineDelimiter.valueOf(LineDelimiter.class, lineDelimiterString);
    }

    static Newline getNewline(final ConfigSource parserConfig) {
        final String newlineString = parserConfig.get(String.class, "newline", "CRLF");
        if (newlineString == null) {
            throw new IllegalArgumentException(new NullPointerException("newline is unexpectedly null."));
        }
        return Newline.valueOf(Newline.class, newlineString);
    }

    static boolean endsWith(final Buffer buffer, final Newline target) {
        switch (target) {
            case CR:
            case LF:
                if (buffer.offset() + buffer.limit() - 1 >= 0) {
                    final byte[] last = new byte[1];
                    buffer.getBytes(buffer.limit() - 1, last, 0, 1);
                    return ((char) last[0]) == target.getFirstCharCode();
                }
                return false;

            case CRLF:
                if (buffer.offset() + buffer.limit() - 2 >= 0) {
                    final byte[] last = new byte[2];
                    buffer.getBytes(buffer.limit() - 2, last, 0, 2);
                    return ((char) last[0]) == target.getFirstCharCode() && ((char) last[1]) == target.getSecondCharCode();
                }
                return false;

            default:
                return false;
        }
    }
}
