/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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

package org.xnio.channels.async;

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.wildfly.common.Assert;
import org.xnio.Buffers;
import org.xnio.ByteBufferPool;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractOutputStream extends OutputStream implements DataOutput {

    public void write(ByteBuffer buffer) throws IOException {
        if (buffer.hasArray()) {
            write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
        } else while (buffer.hasRemaining()) {
            final ByteBuffer heapBuffer;
            if (buffer.remaining() <= ByteBufferPool.SMALL_SIZE) {
                heapBuffer = ByteBufferPool.SMALL_HEAP.allocate();
            } else {
                // 8k chunks
                heapBuffer = ByteBufferPool.NORMAL_HEAP.allocate();
            }
            try {
                Buffers.copy(heapBuffer, buffer);
                heapBuffer.flip();
                write(heapBuffer);
            } finally {
                ByteBufferPool.free(heapBuffer);
            }
        }
    }

    public void writeBoolean(final boolean v) throws IOException {
        write(v ? 1 : 0);
    }

    public void writeByte(final int v) throws IOException {
        write(v);
    }

    public void writeShort(final int v) throws IOException {
        write(v >> 8);
        write(v);
    }

    public void writeChar(final int v) throws IOException {
        write(v >> 8);
        write(v);
    }

    public void writeInt(final int v) throws IOException {
        write(v >> 24);
        write(v >> 16);
        write(v >> 8);
        write(v);
    }

    public void writeLong(final long v) throws IOException {
        writeInt((int) (v >> 32));
        writeInt((int) v);
    }

    public void writeFloat(final float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    public void writeDouble(final double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    public void writeBytes(final String string) throws IOException {
        Assert.checkNotNullParam("string", string);
        final int length = string.length();
        for (int i = 0; i < length; i ++) {
            // todo - optimize this maybe
            write(string.charAt(i));
        }
    }

    public void writeChars(final String string) throws IOException {
        Assert.checkNotNullParam("string", string);
        final int length = string.length();
        for (int i = 0; i < length; i ++) {
            // todo - optimize this maybe
            writeChar(string.charAt(i));
        }
    }

    public void writeUTF(final String string) throws IOException {
        Assert.checkNotNullParam("string", string);
        // todo: add later
        throw new UnsupportedOperationException();
    }
}
