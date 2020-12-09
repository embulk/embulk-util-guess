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

import org.embulk.spi.Buffer;

public class FakeBufferImpl extends Buffer {
    public FakeBufferImpl(final byte[] array) {
        this.array = array;
        this.offset = 0;
        this.filled = array.length;
    }

    @SuppressWarnings("deprecation")
    @Override
    public byte[] array() {
        return this.array;
    }

    @Override
    public int offset() {
        return this.offset;
    }

    @Override
    public Buffer offset(final int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public int limit() {
        return this.filled - this.offset;
    }

    @Override
    public Buffer limit(final int limit) {
        if (this.array.length < limit) {
            throw new IllegalStateException();
        }
        this.filled = this.offset + limit;
        return this;
    }

    @Override
    public int capacity() {
        return this.array.length;
    }

    @Override
    public void setBytes(final int index, final byte[] source, final int sourceIndex, final int length) {
        System.arraycopy(source, sourceIndex, this.array, this.offset + index, length);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBytes(final int index, final Buffer source, final int sourceIndex, final int length) {
        this.setBytes(index, source.array(), source.offset() + sourceIndex, length);
    }

    @Override
    public void getBytes(final int index, final byte[] dest, final int destIndex, final int length) {
        System.arraycopy(this.array, this.offset + index, dest, destIndex, length);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void getBytes(final int index, final Buffer dest, final int destIndex, final int length) {
        this.getBytes(index, dest.array(), dest.offset() + destIndex, length);
    }

    @Override
    public void release() {
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean equals(final Object otherObject) {
        if (!(otherObject instanceof FakeBufferImpl)) {
            return false;
        }
        final FakeBufferImpl other = (FakeBufferImpl) otherObject;

        if (this.limit() != other.limit()) {
            return false;
        }

        int i = this.offset;
        int io = other.offset();
        while (i < this.filled) {
            if (this.array[i] != other.array()[io]) {
                return false;
            }
            i++;
            io++;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int i = this.offset; i < this.filled; i++) {
            result = 31 * result + this.array[i];
        }
        return result;
    }

    private final byte[] array;

    private int offset;
    private int filled;
}
