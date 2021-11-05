/*
 * Copyright 2021 The Embulk project
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

import java.util.Objects;

/**
 * Represents (Embulk's) data type estimated by guess.
 *
 * <p>It reimplements {@code SchemaGuess.GuessedType} in {@code /embulk/guess/schema_guess.rb}.
 *
 * @see <a href="https://github.com/embulk/embulk/blob/v0.10.19/embulk-core/src/main/ruby/embulk/guess/schema_guess.rb">schema_guess.rb</a>
 */
public class GuesstimatedType implements Comparable<GuesstimatedType> {
    private GuesstimatedType(final String string, final String formatOrTimeValue) {
        this.string = string;
        this.formatOrTimeValue = formatOrTimeValue;
    }

    private GuesstimatedType(final String string) {
        this(string, null);
    }

    static GuesstimatedType timestamp(final String formatOrTimeValue) {
        return new GuesstimatedType("timestamp", formatOrTimeValue);
    }

    /**
     * Checks if the type is {@code TIMESTAMP}.
     *
     * @return {@code true} if the type is {@code TIMESTAMP}
     */
    public boolean isTimestamp() {
        return this.string.equals("timestamp");
    }

    /**
     * Gets its timestamp format, or a corresponding timestamp data value, from a {@code TIMESTAMP} type.
     *
     * @return a timestamp format, or a corresponding timestamp data value formatted as a string
     */
    public String getFormatOrTimeValue() {
        return this.formatOrTimeValue;
    }

    /**
     * Returns {@code true} if just its type is the same with another object's.
     *
     * <p>Note that it does not take care of {@code formatOrTimeValue}. It returns {@code true} if
     * both are {@code "timestamp"}, even if their {@code formatOrTimeValue}s are different.
     *
     * <p>It is expected to be called only from {@code mergeType} which should merge {@code "timestamp"}
     * and {@code "timestamp"} into {@code "timestamp"}, even if their {@code formatOrTimeValue}s are
     * different. Those {@code formatOrTimeValue}s are considered in {@code mergeTypes} later.
     */
    public boolean typeEquals(final Object otherObject) {
        if (!(otherObject instanceof GuesstimatedType)) {
            return false;
        }
        final GuesstimatedType other = (GuesstimatedType) otherObject;
        return Objects.equals(this.string, other.string);
    }

    /**
     * Returns {@code true} if its type and {@code formatOrTimeValue} are the same with another object's.
     *
     * <p>Note that it takes care of {@code formatOrTimeValue}. This equality is used out of {@code SchemaGuess},
     * in {@code CSVGuessPlugin} to compare lists of {@code GuesstimatedType}s.
     */
    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof GuesstimatedType)) {
            return false;
        }
        final GuesstimatedType other = (GuesstimatedType) otherObject;
        return Objects.equals(this.string, other.string) && Objects.equals(this.formatOrTimeValue, other.formatOrTimeValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.string, this.formatOrTimeValue);
    }

    @Override
    public int compareTo(final GuesstimatedType other) {
        if (this.formatOrTimeValue != null && other.formatOrTimeValue != null) {
            return this.formatOrTimeValue.compareTo(other.formatOrTimeValue);
        }
        return this.string.compareTo(other.string);
    }

    @Override
    public String toString() {
        return this.string;
    }

    public static final GuesstimatedType BOOLEAN = new GuesstimatedType("boolean");
    public static final GuesstimatedType DOUBLE = new GuesstimatedType("double");
    public static final GuesstimatedType JSON = new GuesstimatedType("json");
    public static final GuesstimatedType LONG = new GuesstimatedType("long");
    public static final GuesstimatedType STRING = new GuesstimatedType("string");

    private final String string;
    private final String formatOrTimeValue;
}
