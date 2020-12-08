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

import java.util.Arrays;
import org.embulk.util.guess.timeformat.TimeFormatMatch;
import org.junit.jupiter.api.Test;

public class TestTimeFormatGuess {
    @Test
    public void testMultiple() {
        assertTimeFormat(
                "%a, %d %b %Y %H:%M %z",
                "Fri, 20 Feb 2015 22:02 +00",
                "Fri, 20 Feb 2015 22:03 +00:00",
                "Fri, 20 Feb 2015 22:04 Z",
                "Fri, 20 Feb 2015 22:05 +0000");
        assertTimeFormat(
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

    @SuppressWarnings("checkstyle:ParenPad")
    @Test
    public void testFormatRfc2822() {
        // This test is disabled because of https://github.com/jruby/jruby/issues/3702
        // assertTimeFormat("%a, %d %b %Y %H:%M:%S %Z", "Fri, 20 Feb 2015 14:02:34 PST");
        assertTimeFormat("%a, %d %b %Y %H:%M:%S %z", "Fri, 20 Feb 2015 22:02:34 UT");
        assertTimeFormat("%a, %d %b %Y %H:%M:%S %z", "Fri, 20 Feb 2015 22:02:34 GMT");
        assertTimeFormat(    "%d %b %Y %H:%M:%S %z",      "20 Feb 2015 22:02:34 GMT");
        assertTimeFormat(    "%d %b %Y %H:%M %z",         "20 Feb 2015 22:02 GMT");
        assertTimeFormat("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 GMT");
        assertTimeFormat(    "%d %b %Y",                  "20 Feb 2015");
        assertTimeFormat("%a, %d %b %Y",             "Fri, 20 Feb 2015");
        assertTimeFormat("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 +0000");
        assertTimeFormat("%a, %d %b %Y %H:%M %:z",   "Fri, 20 Feb 2015 22:02 +00:00");
        assertTimeFormat("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 +00");
    }

    @Test
    public void testFormatApacheClf() {
        assertTimeFormat("%d/%b/%Y:%H:%M:%S %z", "07/Mar/2004:16:05:50 -0800");
    }

    @Test
    public void testFormatAnsiCAsctime() {
        assertTimeFormat("%a %b %e %H:%M:%S %Y", "Fri May 11 21:44:53 2001");
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

    private static void assertTimeFormat(final String expected, final Object... examples) {
        assertEquals(expected, TimeFormatGuess.of().guess(Arrays.asList(examples)));
    }
}
