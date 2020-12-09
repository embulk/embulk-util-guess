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
 *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.util.guess;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class TestFakeBufferImpl {
    @Test
    public void testAscii() {
        final FakeBufferImpl buffer = new FakeBufferImpl("abc".getBytes(StandardCharsets.UTF_8));

        assertEquals(3, buffer.capacity());
        assertEquals(0, buffer.offset());
        assertEquals(3, buffer.limit());

        final int bufferLength = buffer.limit();
        final byte[] bufferArray = new byte[bufferLength];
        buffer.getBytes(0, bufferArray, 0, bufferLength);

        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), bufferArray);
    }
}
