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

package org.xnio;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.Arrays;

/**
 * A buffer source which is backed by a queue of flipped buffers.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class QueueBufferSource implements BufferSource {
    // drain position
    private int pos;
    // fill position
    private int lim;
    // buffers
    private ByteBuffer[] buffers;

    public QueueBufferSource(final int capacity) {
        final int finalSize = min(Integer.highestOneBit(capacity - 1), 0x40000000) << 1;
        this.buffers = new ByteBuffer[finalSize];
        lim = finalSize;
    }

    public ByteBuffer take() {
        final int pos = this.pos;
        final int lim = this.lim;
        if (pos == lim) {
            throw new BufferUnderflowException();
        }
        if (pos + 1 == lim) {
            this.lim = 0;
            this.pos = 0;
        } else {
            this.pos = pos + 1;
        }
        ByteBuffer[] buffers = this.buffers;
        try {
            return buffers[pos];
        } finally {
            buffers[pos] = null;
        }
    }

    public ByteBuffer get() {
        final int pos = this.pos;
        final int lim = this.lim;
        if (pos >= lim) {
            throw new BufferUnderflowException();
        }
        return buffers[pos];
    }

    public void prefill(final int size) throws IllegalArgumentException, BufferOverflowException {
        final int pos = this.pos;
        final int lim = this.lim;
        final ByteBuffer[] buffers = this.buffers;
        ByteBuffer targetBuffer = buffers[pos];
        if (pos >= lim || size > bytesAvailable()) {
            throw BufferSource.notEnoughData(size, 0);
        }
        if (max(ByteBufferPool.NORMAL_SIZE, targetBuffer.capacity()) < size) {
            throw new BufferOverflowException();
        }
        if (targetBuffer.capacity() < size) {
            ByteBuffer cloneBuffer = targetBuffer.isDirect() ? ByteBufferPool.NORMAL_DIRECT.allocate() : ByteBufferPool.NORMAL_HEAP.allocate();
            cloneBuffer.put(targetBuffer);
            ByteBufferPool.free(targetBuffer);
            targetBuffer = buffers[pos] = cloneBuffer;
        }
        doCopy(size, buffers, targetBuffer, pos);
    }

    private void prefillChange(final int size, final boolean direct) {
        final int pos = this.pos;
        final int lim = this.lim;
        final ByteBuffer[] buffers = this.buffers;
        ByteBuffer targetBuffer = buffers[pos];
        if (pos >= lim || size > bytesAvailable()) {
            throw BufferSource.notEnoughData(size, 0);
        }
        if (ByteBufferPool.NORMAL_SIZE < size) {
            throw new BufferOverflowException();
        }
        if (targetBuffer.isDirect() != direct || targetBuffer.capacity() < size) {
            ByteBuffer cloneBuffer = direct ? ByteBufferPool.NORMAL_DIRECT.allocate() : ByteBufferPool.NORMAL_HEAP.allocate();
            cloneBuffer.put(targetBuffer);
            ByteBufferPool.free(targetBuffer);
            targetBuffer = buffers[pos] = cloneBuffer;
        }
        doCopy(size, buffers, targetBuffer, pos);
    }

    private void doCopy(final int size, final ByteBuffer[] buffers, final ByteBuffer targetBuffer, int i) {
        if (targetBuffer.remaining() < size) {
            // fill up the buffer as far as we can
            targetBuffer.compact();
            try {
                do {
                    Buffers.copy(size - targetBuffer.position(), targetBuffer, buffers[i + 1]);
                    if (! buffers[i + 1].hasRemaining()) {
                        ByteBufferPool.free(buffers[i + 1]);
                        buffers[i] = null;
                        buffers[i + 1] = targetBuffer;
                        this.pos = ++i;
                    }
                } while (targetBuffer.position() < size);
            } finally {
                targetBuffer.flip();
            }
        }
    }

    public void prefillDirect(final int size) throws IllegalArgumentException, BufferOverflowException {
        prefillChange(size, true);
    }

    public void prefillHeap(final int size) throws IllegalArgumentException, BufferOverflowException {
        prefillChange(size, false);
    }

    public long bytesAvailable() {
        long t = 0L;
        final ByteBuffer[] buffers = this.buffers;
        final int lim = this.lim;
        for (int i = pos; i < lim; i ++) {
            t += buffers[i].remaining();
        }
        return t;
    }

    void compact() {
        final int pos = this.pos;
        final int lim = this.lim;
        if (pos == lim || pos == 0) {
            return;
        }
        final ByteBuffer[] buffers = this.buffers;
        final int rem = lim - pos;
        System.arraycopy(buffers, pos, buffers, 0, rem);
        Arrays.fill(buffers, max(rem, pos), lim, null);
    }

    public long writeTo(final GatheringByteChannel channel, final long maxLength) throws IOException {
        final ByteBuffer[] buffers = this.buffers;
        final int pos = this.pos;
        final int lim = this.lim;
        final int rem = lim - pos;
        long t = 0L, c;
        int br;
        int freed = 0;
        ByteBuffer buffer;
        for (int i = 0; i < rem; i ++) {
            buffer = buffers[pos + i];
            br = buffer.remaining();
            if (br == 0) {
                this.pos ++;
                this.lim --;
                ByteBufferPool.free(buffer);
                freed ++;
                buffers[pos + i] = null;
            } else {
                c = t + br;
                if (c > maxLength) {
                    // commence partial write
                    int bl = buffer.limit();
                    buffer.limit(bl - (int) (maxLength - c));
                    try {
                        long res = channel.write(buffers, pos + freed, lim - pos - freed);
                        // free any emptied buffers
                        get();
                        return res;
                    } finally {
                        buffer.limit(bl);
                    }
                } else {
                    t = c;
                }
            }
        }
        // write all data
        long res = t == 0L ? 0L : channel.write(buffers, pos + freed, lim - pos - freed);
        // free any emptied buffers
        get();
        return res;
    }

    public void add(final ByteBuffer buffer) {
        final int pos = this.pos;
        final int lim = this.lim;
        final ByteBuffer[] buffers = this.buffers;
        final int cap = buffers.length;
        if (lim == cap) {
            if (pos == 0) throw new BufferOverflowException();
            compact();
            add(buffer);
            return;
        }
        buffers[lim] = buffer;
        this.lim = lim + 1;
    }

    public long fillFrom(final BufferSource bufferSource, final long maxLength) {
        if (maxLength <= 0L) return 0L;
        compact();
        long t = 0L;
        int rem;
        final ByteBuffer[] buffers = this.buffers;
        final int cap = buffers.length;
        ByteBuffer buffer;
        int lim = this.lim;
        try {
            while (lim < cap) {
                buffer = bufferSource.get();
                if (buffer == null) {
                    return t;
                }
                rem = buffer.remaining();
                if (t + rem > maxLength) {
                    return t;
                }
                buffers[lim ++] = bufferSource.take();
                t += rem;
            }
            return t;
        } finally {
            this.lim = lim;
        }
    }

    public long fillFrom(final ScatteringByteChannel channel, final long maxLength, final ByteBufferPool byteBufferPool) throws IOException {
        if (maxLength <= 0L) return 0L;
        compact();
        final int pos = this.pos;
        final int lim = this.lim;
        final ByteBuffer[] buffers = this.buffers;
        final int cap = buffers.length;
        if (lim > pos) {
            // there is data and we can fill more in the last one
            final ByteBuffer lastBuffer = buffers[lim - 1];
            if (lastBuffer.remaining() < lastBuffer.capacity()) {
                // it still has room in it
                lastBuffer.compact();
                try {
                    byteBufferPool.allocate(buffers, lim);
                    try {
                        return channel.read(buffers, lim - 1, cap - lim + 1);
                    } finally {
                        for (int i = lim; i < cap; i ++) {
                            if (buffers[i].position() == 0) {
                                ByteBufferPool.free(buffers[i]);
                                buffers[i] = null;
                            } else {
                                buffers[i].flip();
                            }
                        }
                    }
                } finally {
                    lastBuffer.flip();
                }
            }
        }
        // just fill in the empty slots and continue from there
        if (lim == cap) {
            // can't read any more
            return 0L;
        }
        byteBufferPool.allocate(buffers, lim);
        try {
            return channel.read(buffers, lim, cap - lim);
        } finally {
            for (int i = lim; i < cap; i ++) {
                if (buffers[i].position() == 0) {
                    ByteBufferPool.free(buffers[i]);
                    buffers[i] = null;
                } else {
                    buffers[i].flip();
                }
            }
        }
    }
}
