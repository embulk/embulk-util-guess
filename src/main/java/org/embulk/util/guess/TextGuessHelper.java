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
import java.util.ArrayList;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.file.ListFileInput;
import org.embulk.util.text.LineDecoder;
import org.embulk.util.text.LineDelimiter;
import org.embulk.util.text.Newline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper to convert {@link org.embulk.spi.Buffer} to {@link java.lang.String} for guess.
 *
 * <p>It reimplements {@code TextGuessPlugin} in {@code /embulk/guess_plugin.rb}.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess_plugin.rb">guess_plugin.rb</a>
 */
public final class TextGuessHelper {
    private TextGuessHelper(final ConfigMapperFactory configMapperFactory) {
        this.configMapperFactory = configMapperFactory;
    }

    public static TextGuessHelper of(final ConfigMapperFactory configMapperFactory) {
        return new TextGuessHelper(configMapperFactory);
    }

    public String toText(final ConfigSource config, final Buffer sample) {
        final ConfigSource parserConfig = config.getNestedOrGetEmpty("parser");

        final Charset charset;
        try {
            charset = GuessUtil.getCharset(parserConfig, this.configMapperFactory, sample);
        } catch (final IllegalArgumentException ex) {
            logger.warn(ex.getMessage(), ex);
            return null;
        }

        final LineDelimiter lineDelimiter;
        try {
            lineDelimiter = GuessUtil.getLineDelimiter(parserConfig);
        } catch (final IllegalArgumentException ex) {
            logger.warn(ex.getMessage(), ex);
            return null;
        }

        final Newline newline;
        try {
            newline = GuessUtil.getNewline(parserConfig);
        } catch (final IllegalArgumentException ex) {
            logger.warn(ex.getMessage(), ex);
            return null;
        }

        final ArrayList<Buffer> listBuffer = new ArrayList<>();
        listBuffer.add(sample);
        final ArrayList<ArrayList<Buffer>> listListBuffer = new ArrayList<>();
        listListBuffer.add(listBuffer);
        final LineDecoder decoder = LineDecoder.of(new ListFileInput(listListBuffer), charset, lineDelimiter);

        final StringBuilder sampleText = new StringBuilder();

        while (decoder.nextFile()) {  // TODO: Confirm decoder contains only one, and stop looping.
            boolean first = true;
            while (true) {
                final String line = decoder.poll();
                if (line == null) {
                    break;
                }

                if (first) {
                    first = false;
                } else {
                    sampleText.append(newline.getString());
                }
                sampleText.append(line);
            }
        }

        return sampleText.toString();
    }

    private static final Logger logger = LoggerFactory.getLogger(TextGuessHelper.class);

    private final ConfigMapperFactory configMapperFactory;
}
