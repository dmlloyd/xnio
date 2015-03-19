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

package org.xnio.streams;

import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;

import org.wildfly.common.Assert;
import org.xnio.ByteBufferPool;
import org.xnio._private.Messages;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractInputStream extends InputStream implements DataInput {

    // JDK 1.9 API
    public long transferTo(OutputStream output) throws IOException {
        return transferTo(output, Long.MAX_VALUE);
    }

    public long transferTo(OutputStream output, long count) throws IOException {
        final ByteBuffer buffer = ByteBufferPool.NORMAL_HEAP.allocate();
        try {
            byte[] array = buffer.array();
            final int offs = buffer.arrayOffset();
            final int len = buffer.capacity();
            Assert.assertNotNull(array);
            int res;
            long t = 0L;
            for (;;) {
                res = read(array, offs, (int) Math.min((long) len, count - t));
                if (res == -1) {
                    return t;
                }
                output.write(array, offs, res);
                t += res;
                if (t == count) {
                    return t;
                }
            }
        } finally {
            ByteBufferPool.free(buffer);
        }
    }

    public void readFully(final byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        Assert.checkArrayBounds(b, off, len);
        int res, cnt = 0;
        while (cnt < len) {
            res = read(b, off + cnt, len - cnt);
            if (res == -1) {
                throw Messages.msg.unexpectedEof();
            }
            cnt += res;
        }
    }

    public int skipBytes(final int n) throws IOException {
        return (int) super.skip(n);
    }

    public boolean readBoolean() throws IOException {
        return readByte() != 0;
    }

    public byte readByte() throws IOException {
        return (byte) readUnsignedByte();
    }

    public int readUnsignedByte() throws IOException {
        int res = read();
        if (res == -1) throw Messages.msg.unexpectedEof();
        return res;
    }

    public short readShort() throws IOException {
        return (short) (readUnsignedShort());
    }

    public int readUnsignedShort() throws IOException {
        return readUnsignedByte() << 8 | readUnsignedByte();
    }

    public char readChar() throws IOException {
        return (char) (readUnsignedShort());
    }

    public int readInt() throws IOException {
        return readUnsignedShort() << 16 | readUnsignedShort();
    }

    public long readLong() throws IOException {
        return (readInt() & 0xFFFF_FFFFL) << 32L | readInt() & 0xFFFF_FFFFL;
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public String readLine() throws IOException {
        final StringBuilder b = new StringBuilder();
        int res;
        for (;;) {
            res = read();
            if (res == -1) {
                return b.toString();
            }
            if (res == 13) {
                // skip by spec
            } else if (res == 10) {
                // skip and return by spec
                return b.toString();
            }
            b.appendCodePoint(res);
        }
    }

    public String readUTF() throws IOException {
        final int length = readUnsignedShort();
        // use maximum possible length
        final StringBuilder builder = new StringBuilder(length);
        int a, b, c;
        for (int i = 0; i < length; i ++) {
            a = readUnsignedByte();
            if (a < 0b1_0000000) {
                builder.append((char) a);
            } else if (a <= 0b10_111111) {
                throw new UTFDataFormatException();
            } else if (a <= 0b110_11111) {
                b = readUnsignedByte();
                if (b < 0b10_000000) {
                    throw new UTFDataFormatException();
                }
                builder.append((char) ((a & 0b000_11111) << 6 | b & 0b00_111111));
            } else if (a <= 0b1110_1111) {
                b = readUnsignedByte();
                if (b < 0b10_000000) {
                    throw new UTFDataFormatException();
                }
                c = readUnsignedByte();
                if (c < 0b10_000000) {
                    throw new UTFDataFormatException();
                }
                builder.append((char) ((a & 0b0000_1111) << 12 | (b & 0b00_111111) << 6 | c & 0b00_111111));
            } else {
                throw new UTFDataFormatException();
            }
        }
        return builder.toString();
    }
}
