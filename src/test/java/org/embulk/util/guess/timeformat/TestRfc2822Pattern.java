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

public class TestRfc2822Pattern {
    @SuppressWarnings("checkstyle:ParenPad")
    @Test
    public void test() {
        // This test is disabled because of https://github.com/jruby/jruby/issues/3702
        // assertRfc2822("%a, %d %b %Y %H:%M:%S %Z", "Fri, 20 Feb 2015 14:02:34 PST");
        assertRfc2822("%a, %d %b %Y %H:%M:%S %z", "Fri, 20 Feb 2015 22:02:34 UT");
        assertRfc2822("%a, %d %b %Y %H:%M:%S %z", "Fri, 20 Feb 2015 22:02:34 GMT");
        assertRfc2822(    "%d %b %Y %H:%M:%S %z",      "20 Feb 2015 22:02:34 GMT");
        assertRfc2822(    "%d %b %Y %H:%M %z",         "20 Feb 2015 22:02 GMT");
        assertRfc2822("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 GMT");
        assertRfc2822(    "%d %b %Y",                  "20 Feb 2015");
        assertRfc2822("%a, %d %b %Y",             "Fri, 20 Feb 2015");
        assertRfc2822("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 +0000");
        assertRfc2822("%a, %d %b %Y %H:%M %:z",   "Fri, 20 Feb 2015 22:02 +00:00");
        assertRfc2822("%a, %d %b %Y %H:%M %z",    "Fri, 20 Feb 2015 22:02 +00");
    }

    private static void assertRfc2822(final String expected, final String example) {
        assertEquals(expected, new Rfc2822Pattern().match(example).getFormat());
    }
}
