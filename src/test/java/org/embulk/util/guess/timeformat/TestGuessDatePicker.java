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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestGuessDatePicker {
    @ParameterizedTest
    @CsvSource({
            "YMD,/,2020,12,31,,2020/12/31",
            "MDY,/,2020,12,31,,12/31/2020",
            "DMY,/,2020,12,31,,31/12/2020",
            "YMD,/,2020,12,12,,2020/12/12",  // No YDM.
            "MDY,/,2020,12,12,,12/12/2020",  // MDY is prioritized over DMY.
            "DMY,.,2020,12,13,,13.12.2020",
            "YMD,-,2020,12,12,,2020-12-12",
            "YMD,/,2020,12,31,12:34:56,2020/12/31 12:34:56",
            "YMD,/,2020,12,31,12:34:56 GMT,2020/12/31 12:34:56 GMT",
    })
    public void testValidDatePicking(
            final String order,
            final String delimiter,
            final String year,
            final String month,
            final String day,
            final String rest,
            final String dateString) {
        final GuessDatePicker picker = GuessDatePicker.from(dateString);
        assertEquals(GuessDateOrder.valueOf(order), picker.getOrder());
        assertEquals(delimiter, picker.getDateDelim());
        assertEquals(year, picker.getYear());
        assertEquals(month, picker.getMonth());
        assertEquals(day, picker.getDay());

        // Trimmed in this test as of now. Wanted: https://github.com/junit-team/junit5/issues/2420
        assertEquals(rest == null ? "" : rest.trim(), picker.getRest().trim());
    }

    @ParameterizedTest
    @CsvSource({
            "2020@12@12",  // Only "/", "-", and "." are expected as a dete delimiter.
    })
    public void testInvalidDate(final String dateString) {
        assertEquals(null, GuessDatePicker.from(dateString));
    }
}
