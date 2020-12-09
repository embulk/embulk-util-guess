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

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import org.embulk.util.guess.timeformat.TimeFormatMatch;
import org.embulk.util.rubytime.RubyDateTimeFormatter;
import org.embulk.util.rubytime.RubyDateTimeParseException;
import org.junit.jupiter.api.Test;

/**
 * Tests imported from embulk-core's.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/test/ruby/vanilla/guess/test_time_format_guess.rb">test_time_format_guess.rb</a>
 */
public class TestTimeFormatGuess {
    @Test
    public void testMultiple() {
        assertGuess(
                "%a, %d %b %Y %H:%M %z",
                "Fri, 20 Feb 2015 22:02 +00",
                "Fri, 20 Feb 2015 22:03 +00:00",
                "Fri, 20 Feb 2015 22:04 Z",
                "Fri, 20 Feb 2015 22:05 +0000");
        assertGuess(
                "%a, %d %b %Y %H:%M %:z",
                "Fri, 20 Feb 2016 22:00 Z",
                "Fri, 20 Feb 2016 22:01 -05:00",
                "Fri, 20 Feb 2016 22:02 +01:00",
                "Fri, 20 Feb 2016 22:03 +01:00",
                "Fri, 20 Feb 2016 22:03:03 +00:00",
                "Fri, 20 Feb 2016 22:04:04 +03:00",
                "Fri, 20 Feb 2016 22:04 +12:00",
                "Fri, 20 Feb 2016 22:05 +02:00",
                "Fri, 20 Feb 2016 22:06 +10",
                "Fri, 20 Feb 2016 22:06 Z",
                "Fri, 20 Feb 2016 22:07 +1230");
    }

    @Test
    public void testFormatDelims() {
        // date-delim "-"  date-time-delim " "  time-delim ":"  frac delim "."
        assertGuess("%Y-%m-%d %H:%M:%S.%N",    "2014-01-01 01:01:01.000000001");
        assertGuess("%Y-%m-%d %H:%M:%S.%N",    "2014-01-01 01:01:01.000001");
        assertGuess("%Y-%m-%d %H:%M:%S.%L",    "2014-01-01 01:01:01.001");
        assertGuess("%Y-%m-%d %H:%M:%S",       "2014-01-01 01:01:01");
        assertGuess("%Y-%m-%d %H:%M",          "2014-01-01 01:01");
        assertGuess("%Y-%m-%d",                "2014-01-01");

        // date-delim "/"  date-time-delim " "  time-delim "-"  frac delim ","
        assertGuess("%Y/%m/%d %H-%M-%S,%N",    "2014/01/01 01-01-01,000000001");
        assertGuess("%Y/%m/%d %H-%M-%S,%N",    "2014/01/01 01-01-01,000001");
        assertGuess("%Y/%m/%d %H-%M-%S,%L",    "2014/01/01 01-01-01,001");
        assertGuess("%Y/%m/%d %H-%M-%S",       "2014/01/01 01-01-01");
        assertGuess("%Y/%m/%d %H-%M",          "2014/01/01 01-01");
        assertGuess("%Y/%m/%d",                "2014/01/01");

        // date-delim "."  date-time-delim "."  time-delim ":"  frac delim "."
        assertGuess("%Y.%m.%d.%H:%M:%S.%N",    "2014.01.01.01:01:01.000000001");
        assertGuess("%Y.%m.%d.%H:%M:%S.%N",    "2014.01.01.01:01:01.000001");
        assertGuess("%Y.%m.%d.%H:%M:%S.%L",    "2014.01.01.01:01:01.001");
        assertGuess("%Y.%m.%d.%H:%M:%S",       "2014.01.01.01:01:01");
        assertGuess("%Y.%m.%d.%H:%M",          "2014.01.01.01:01");
        assertGuess("%Y.%m.%d",                "2014.01.01");

        // date-delim "."  date-time-delim ". "  time-delim ":"  frac delim ","
        assertGuess("%Y.%m.%d. %H:%M:%S,%N",    "2014.01.01. 01:01:01,000000001");
        assertGuess("%Y.%m.%d. %H:%M:%S,%N",    "2014.01.01. 01:01:01,000001");
        assertGuess("%Y.%m.%d. %H:%M:%S,%L",    "2014.01.01. 01:01:01,001");
        assertGuess("%Y.%m.%d. %H:%M:%S",       "2014.01.01. 01:01:01");
        assertGuess("%Y.%m.%d. %H:%M",          "2014.01.01. 01:01");
        assertGuess("%Y.%m.%d",                 "2014.01.01");
    }

    @Test
    public void testFormatYmdOrders() {
        assertGuess("%Y-%m-%d", "2014-01-01");
        assertGuess("%Y/%m/%d", "2014/01/01");
        assertGuess("%Y.%m.%d", "2014.01.01");
        assertGuess("%m/%d/%Y", "01/01/2014");
        assertGuess("%m.%d.%Y", "01.01.2014");
        assertGuess("%d/%m/%Y", "13/01/2014");
        assertGuess("%d/%m/%Y", "21/01/2014");

        assertGuess("%d/%m/%Y %H-%M-%S,%N",    "21/01/2014 01-01-01,000000001");
        assertGuess("%d/%m/%Y %H-%M-%S,%N",    "21/01/2014 01-01-01,000001");
        assertGuess("%d/%m/%Y %H-%M-%S,%L",    "21/01/2014 01-01-01,001");
        assertGuess("%d/%m/%Y %H-%M-%S",       "21/01/2014 01-01-01");
        assertGuess("%d/%m/%Y %H-%M",          "21/01/2014 01-01");
        assertGuess("%d/%m/%Y",                "21/01/2014");
    }

    @Test
    public void testFormatBorders() {
        assertGuess("%Y-%m-%d %H:%M:%S.%N",    "2014-12-31 23:59:59.999999999");
    }

    @Test
    public void testFormatIso8601() {
        assertGuess("%Y-%m-%d", "1981-04-05");
        assertGuess("%Y-%m-%dT%H", "2007-04-06T13");
        assertGuess("%Y-%m-%dT%H:%M", "2007-04-06T00:00");
        assertGuess("%Y-%m-%dT%H:%M", "2007-04-05T24:00");
        assertGuess("%Y-%m-%dT%H:%M:%S", "2007-04-06T13:47:30");
        assertGuess("%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30Z");
        assertGuess("%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30+00");
        assertGuess("%Y-%m-%dT%H:%M:%S%:z", "2007-04-06T13:47:30+00:00");
        assertGuess("%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30+0000");
        assertGuess("%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30-01");
        assertGuess("%Y-%m-%dT%H:%M:%S%:z", "2007-04-06T13:47:30-01:30");
        assertGuess("%Y-%m-%dT%H:%M:%S%z", "2007-04-06T13:47:30-0130");
    }

    @SuppressWarnings("checkstyle:ParenPad")
    @Test
    public void testFormatRfc2822() {
        // This test is disabled because of https://github.com/jruby/jruby/issues/3702
        // assertGuess("%a, %d %b %Y %H:%M:%S %Z", "Fri, 20 Feb 2015 14:02:34 PST");
        assertGuess("%a, %d %b %Y %H:%M:%S %z", "Fri, 20 Feb 2015 22:02:34 UT");
        assertGuess("%a, %d %b %Y %H:%M:%S %z", "Fri, 20 Feb 2015 22:02:34 GMT");
        assertGuess(    "%d %b %Y %H:%M:%S %z",      "20 Feb 2015 22:02:34 GMT");
        assertGuess(    "%d %b %Y %H:%M %z",         "20 Feb 2015 22:02 GMT");
        assertGuess("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 GMT");
        assertGuess(    "%d %b %Y",                  "20 Feb 2015");
        assertGuess("%a, %d %b %Y",             "Fri, 20 Feb 2015");
        assertGuess("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 +0000");
        assertGuess("%a, %d %b %Y %H:%M %:z",   "Fri, 20 Feb 2015 22:02 +00:00");
        assertGuess("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 +00");
    }

    @Test
    public void testFormatApacheClf() {
        assertGuess("%d/%b/%Y:%H:%M:%S %z", "07/Mar/2004:16:05:50 -0800");
    }

    @Test
    public void testFormatAnsiCAsctime() {
        assertGuess("%a %b %e %H:%M:%S %Y", "Fri May 11 21:44:53 2001");
    }

    @Test
    public void testFormatMergeFrequency() {
        assertGuessPartial(2, "%Y-%m-%d %H:%M:%S", "2014-01-01", "2014-01-01 00:00:00", "2014-01-01 00:00:00");
        assertGuessPartial(3, "%Y-%m-%d %H:%M:%S %z", "2014-01-01 00:00:00 +0000", "2014-01-01 00:00:00 +0000", "2014-01-01 00:00:00 +00:00");
    }

    @Test
    public void testFormatMergeDmy() {
        // DMY has higher priority than MDY
        assertGuess("%m/%d/%Y", "01/01/2014");
        assertGuess("%d/%m/%Y", "01/01/2014", "01/01/2014", "13/01/2014");
        assertGuess("%d.%m.%Y", "01.01.2014", "01.01.2014", "13.01.2014");
        // but frequency is more important if delimiter is different
        assertGuessPartial(2, "%m/%d/%Y", "01/01/2014", "01/01/2014", "13.01.2014");
    }

    @Test
    public void testMergeMostFrequentMatches() {
        final TimeFormatMatch[] matches1 = {
            new FakeMatch("foo"),
            new FakeMatch("bar"),
            new FakeMatch("bar"),
            new FakeMatch("foo"),
            new FakeMatch("foo"),
            new FakeMatch("bar"),
            new FakeMatch("foo"),
        };
        assertEquals("foo", TimeFormatGuess.mergeMostFrequentMatches(Arrays.asList(matches1)).getFormat());

        final TimeFormatMatch[] matches2 = {
            new FakeMatch("bar"),
            new FakeMatch("foo"),
            new FakeMatch("bar"),
            new FakeMatch("bar"),
            new FakeMatch("foo"),
            new FakeMatch("bar"),
            new FakeMatch("foo"),
            new FakeMatch("foo"),
            new FakeMatch("bar"),
        };
        assertEquals("bar", TimeFormatGuess.mergeMostFrequentMatches(Arrays.asList(matches2)).getFormat());
    }

    private static class FakeMatch implements TimeFormatMatch {
        FakeMatch(final String format) {
            this.format = format;
        }

        @Override
        public String getFormat() {
            return this.format;
        }

        @Override
        public String getIdentifier() {
            return this.format;
        }

        @Override
        public void mergeFrom(final TimeFormatMatch anotherInGroup) {
            return;
        }

        @Override
        public String toString() {
            return this.format + "(" + super.toString() + ")";
        }

        private final String format;
    }

    private static void assertGuess(final String expected, final Object... examples) {
        assertEquals(expected, TimeFormatGuess.of().guess(Arrays.asList(examples)));
    }

    private static void assertGuessPartial(final int expectedCount, final String expectedPattern, final Object... examples) {
        assertEquals(expectedPattern, TimeFormatGuess.of().guess(Arrays.asList(examples)));
        final RubyDateTimeFormatter formatter = RubyDateTimeFormatter.ofPattern(expectedPattern);

        final ArrayList<TemporalAccessor> times = new ArrayList<>();
        for (final Object example : examples) {
            final TemporalAccessor parsed;
            try {
                parsed = formatter.parse(example.toString());
            } catch (final RubyDateTimeParseException ex) {
                continue;
            }
            times.add(parsed);
        }
        assertEquals(expectedCount, times.size());

        for (final TemporalAccessor time : times) {
            assertEquals(Instant.from(time), Instant.from(formatter.parse(formatter.format(time))));
        }
    }
}
