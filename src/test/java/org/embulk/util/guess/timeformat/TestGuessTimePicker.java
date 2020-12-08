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

package org.embulk.util.guess.timeformat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestGuessTimePicker {
    @ParameterizedTest
    @CsvSource({
            "T,:,12,34,56,,T12:34:56",
            "T,-,12,34,56,,T12-34-56",
            "_,:,12,34,56,,_12:34:56",
            ". ,:,12,34,56,,. 12:34:56",
            ",,12,34,56,,123456",
            "T,,12,,,@34@56,T12@34@56",  // Only ":", and "-" are expected as a time delimiter.
    })
    public void testTimePicking(
            final String dateTimeDelim,
            final String timeDelim,
            final String hour,
            final String minute,
            final String second,
            final String rest,
            final String timeString) {
        final GuessTimePicker picker = GuessTimePicker.from(timeString, "");
        // Trimmed in this test as of now. Wanted: https://github.com/junit-team/junit5/issues/2420
        assertEquals(dateTimeDelim == null ? "" : dateTimeDelim.trim(), picker.getDateTimeDelim().trim());

        assertEquals(timeDelim == null ? "" : timeDelim, picker.getTimeDelim());
        assertEquals(hour == null ? "" : hour, picker.getHour());
        assertEquals(minute == null ? "" : minute, picker.getMinute());
        assertEquals(second == null ? "" : second, picker.getSecond());

        // Trimmed in this test as of now. Wanted: https://github.com/junit-team/junit5/issues/2420
        assertEquals(rest == null ? "" : rest.trim(), picker.getRest().trim());
    }

    /**
     * Tests an expectedly unintentional case.
     *
     * <p>"T123456" is expected to be split to: hour="12", minute="34", second="56", rest="".
     *
     * <p>But as "T123456" matches the first regular expression (expectedly unintentionally),
     * it has been split to: hour="12", minute=nil, second=nil, rest="3456".
     *
     * <p>This Java reimplementation just follow the original behavior as of now. It would be fixed later.
     *
     * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/time_format_guess.rb#L248">time_format_guess.rb</a>
     */
    @Test
    public void testUnintentionalCase() {
        final GuessTimePicker picker = GuessTimePicker.from("T123456", "");
        assertEquals("", picker.getTimeDelim());
        assertEquals("12", picker.getHour());
        assertEquals("", picker.getMinute());
        assertEquals("", picker.getSecond());
        assertEquals("3456", picker.getRest().trim());
    }
}
