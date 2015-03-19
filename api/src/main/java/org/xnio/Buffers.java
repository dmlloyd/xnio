/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2008 Red Hat, Inc. and/or its affiliates.
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.BufferOverflowException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Arrays;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.xnio._private.Messages.msg;

import org.wildfly.common.Assert;

/**
 * Buffer utility methods.
 *
 * @apiviz.exclude
 */
public final class Buffers {
    private Buffers() {}

    /**
     * Drain a buffer.  This is the same as setting the position to be equal to the limit.  Returns the number of bytes
     * skipped.
     *
     * @param buffer the buffer to drain
     * @return the number of bytes drained from this buffer
     */
    public static int drain(Buffer buffer) {
        final int oldPos = buffer.position();
        final int oldLim = buffer.limit();
        buffer.position(oldLim);
        return oldLim - oldPos;
    }

    /**
     * Flip a buffer.
     *
     * @see Buffer#flip()
     * @param <T> the buffer type
     * @param buffer the buffer to flip
     * @return the buffer instance
     */
    public static <T extends Buffer> T flip(T buffer) {
        buffer.flip();
        return buffer;
    }

    /**
     * Clear a buffer.
     *
     * @see Buffer#clear()
     * @param <T> the buffer type
     * @param buffer the buffer to clear
     * @return the buffer instance
     */
    public static <T extends Buffer> T clear(T buffer) {
        buffer.clear();
        return buffer;
    }

    /**
     * Set the buffer limit.
     *
     * @see Buffer#limit(int)
     * @param <T> the buffer type
     * @param buffer the buffer to set
     * @param limit the new limit
     * @return the buffer instance
     */
    public static <T extends Buffer> T limit(T buffer, int limit) {
        buffer.limit(limit);
        return buffer;
    }

    /**
     * Set the buffer mark.
     *
     * @see Buffer#mark()
     * @param <T> the buffer type
     * @param buffer the buffer to mark
     * @return the buffer instance
     */
    public static <T extends Buffer> T mark(T buffer) {
        buffer.mark();
        return buffer;
    }

    /**
     * Set the buffer position.
     *
     * @see Buffer#position(int)
     * @param <T> the buffer type
     * @param buffer the buffer to set
     * @param position the new position
     * @return the buffer instance
     */
    public static <T extends Buffer> T position(T buffer, int position) {
        buffer.position(position);
        return buffer;
    }

    /**
     * Reset the buffer.
     *
     * @see Buffer#reset()
     * @param <T> the buffer type
     * @param buffer the buffer to reset
     * @return the buffer instance
     */
    public static <T extends Buffer> T reset(T buffer) {
        buffer.reset();
        return buffer;
    }

    /**
     * Rewind the buffer.
     *
     * @see Buffer#rewind()
     * @param <T> the buffer type
     * @param buffer the buffer to rewind
     * @return the buffer instance
     */
    public static <T extends Buffer> T rewind(T buffer) {
        buffer.rewind();
        return buffer;
    }

    /**
     * Flip all the buffers in the array, freeing buffers that have no data in them, and returning the new length.
     *
     * @param buffers the buffers to flip
     * @return the number of buffers in the array after flipping
     */
    public static int flipAndFree(ByteBuffer[] buffers) {
        return flipAndFree(buffers, 0, buffers.length);
    }

    /**
     * Flip all the buffers in the array, freeing buffers that have no data in them, and returning the new length.
     *
     * @param buffers the buffers to flip
     * @param offs the offset into the buffers array
     * @return the number of buffers in the array after flipping
     */
    public static int flipAndFree(ByteBuffer[] buffers, int offs) {
        return flipAndFree(buffers, offs, buffers.length - offs);
    }

    /**
     * Flip all the buffers in the array, freeing buffers that have no data in them, and returning the new length.
     *
     * @param buffers the buffers to flip
     * @param offs the offset into the buffers array
     * @param len the number of buffers to flip
     * @return the number of buffers in the array after flipping
     */
    public static int flipAndFree(ByteBuffer[] buffers, int offs, int len) {
        Assert.checkArrayBounds(buffers, offs, len);
        ByteBuffer buffer;
        int i = 0, j = 0;
        while (i < len) {
            buffer = buffers[offs + i];
            if (buffer.position() == 0) {
                ByteBufferPool.free(buffer);
            } else {
                buffer.flip();
                buffers[offs + j++] = buffer;
            }
        }
        int res = j;
        while (j < len) {
            buffers[offs + j++] = null;
        }
        return res;
    }

    /**
     * Compact all the buffers in the array, freeing buffers that have been fully read, and returning the new length.
     *
     * @param buffers the buffers to compact
     * @return the number of buffers in the array after compacting
     */
    public static int compactAndFree(ByteBuffer[] buffers) {
        return compactAndFree(buffers, 0, buffers.length);
    }

    /**
     * Compact all the buffers in the array, freeing buffers that have been fully read, and returning the new length.
     *
     * @param buffers the buffers to compact
     * @param offs the offset into the buffers array
     * @return the number of buffers in the array after compacting
     */
    public static int compactAndFree(ByteBuffer[] buffers, int offs) {
        return compactAndFree(buffers, offs, buffers.length - offs);
    }

    /**
     * Compact all the buffers in the array, freeing buffers that have been fully read, and returning the new length.
     *
     * @param buffers the buffers to compact
     * @param offs the offset into the buffers array
     * @param len the number of buffers to flip
     * @return the number of buffers in the array after compacting
     */
    public static int compactAndFree(ByteBuffer[] buffers, int offs, int len) {
        Assert.checkArrayBounds(buffers, offs, len);
        ByteBuffer buffer;
        int i = 0, j = 0;
        while (i < len) {
            buffer = buffers[offs + i];
            if (buffer.hasRemaining()) {
                buffer.compact();
                buffers[offs + j++] = buffer;
            } else {
                ByteBufferPool.free(buffer);
            }
        }
        int res = j;
        while (j < len) {
            buffers[offs + j++] = null;
        }
        return res;
    }

    /**
     * Slice the buffer.  The original buffer's position will be moved up past the slice that was taken.
     *
     * @see ByteBuffer#slice()
     * @param buffer the buffer to slice
     * @param sliceSize the size of the slice
     * @return the buffer slice
     */
    public static ByteBuffer slice(ByteBuffer buffer, int sliceSize) {
        final int oldRem = buffer.remaining();
        if (sliceSize > oldRem || sliceSize < -oldRem) {
            throw msg.bufferUnderflow();
        }
        final int oldPos = buffer.position();
        final int oldLim = buffer.limit();
        if (sliceSize < 0) {
            // count from end (sliceSize is NEGATIVE)
            buffer.limit(oldLim + sliceSize);
            try {
                return buffer.slice();
            } finally {
                buffer.limit(oldLim);
                buffer.position(oldLim + sliceSize);
            }
        } else {
            // count from start
            buffer.limit(oldPos + sliceSize);
            try {
                return buffer.slice();
            } finally {
                buffer.limit(oldLim);
                buffer.position(oldPos + sliceSize);
            }
        }
    }

    /**
     * Copy a portion of the buffer into a newly allocated buffer.  The original buffer's position will be moved up past the copy that was taken.
     *
     * @param buffer the buffer to slice
     * @param count the size of the copy
     * @param allocator the buffer allocator to use
     * @return the buffer slice
     */
    @Deprecated
    public static ByteBuffer copy(ByteBuffer buffer, int count, BufferAllocator<ByteBuffer> allocator) {
        final int oldRem = buffer.remaining();
        if (count > oldRem || count < -oldRem) {
            throw msg.bufferUnderflow();
        }
        final int oldPos = buffer.position();
        final int oldLim = buffer.limit();
        if (count < 0) {
            // count from end (sliceSize is NEGATIVE)
            final ByteBuffer target = allocator.allocate(-count);
            buffer.position(oldLim + count);
            try {
                target.put(buffer);
                return target;
            } finally {
                buffer.limit(oldLim);
                buffer.position(oldLim + count);
            }
        } else {
            // count from start
            final ByteBuffer target = allocator.allocate(count);
            buffer.limit(oldPos + count);
            try {
                target.put(buffer);
                return target;
            } finally {
                buffer.limit(oldLim);
                buffer.position(oldPos + count);
            }
        }
    }

    /**
     * Copy as many bytes as possible from {@code source} into {@code destination}.
     *
     * @param destination the destination buffer
     * @param source the source buffer
     * @return the number of bytes put into the destination buffer
     */
    public static int copy(final ByteBuffer destination, final ByteBuffer source) {
        final int sl = source.limit();
        final int sp = source.position();
        final int sr = sl - sp;
        final int dr = destination.remaining();
        if (sr == 0 || dr == 0) {
            return 0;
        } else if (dr >= sr) {
            destination.put(source);
            return sr;
        } else {
            source.limit(sp + dr);
            try {
                destination.put(source);
            } finally {
                source.limit(sl);
            }
            return dr;
        }
    }

    /**
     * Copy as many bytes as possible from {@code sources} into {@code destinations} in a "scatter" fashion.
     *
     * @param destinations the destination buffers
     * @param offset the offset into the destination buffers array
     * @param length the number of buffers to update
     * @param source the source buffer
     * @return the number of bytes put into the destination buffers
     */
    public static int copy(final ByteBuffer[] destinations, final int offset, final int length, final ByteBuffer source) {
        int t = 0;
        for (int i = 0; i < length && source.hasRemaining(); i ++) {
            t += copy(destinations[i + offset], source);
        }
        return t;
    }

    /**
     * Copy as many bytes as possible from {@code sources} into {@code destination} in a "gather" fashion.
     *
     * @param destination the destination buffer
     * @param sources the source buffers
     * @param offset the offset into the source buffers array
     * @param length the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static int copy(final ByteBuffer destination, final ByteBuffer[] sources, final int offset, final int length) {
        int t = 0;
        for (int i = 0; i < length && destination.hasRemaining(); i ++) {
            t += copy(destination, sources[i + offset]);
        }
        return t;
    }

    /**
     * Copy as many bytes as possible from {@code sources} into {@code destinations} by a combined "scatter"/"gather" operation.
     *
     * @param destinations the destination buffers
     * @param destOffset the offset into the destination buffers array
     * @param destLength the number of buffers to write to
     * @param sources the source buffers
     * @param srcOffset the offset into the source buffers array
     * @param srcLength the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static long copy(final ByteBuffer[] destinations, final int destOffset, final int destLength, final ByteBuffer[] sources, final int srcOffset, final int srcLength) {
        long t = 0L;
        int s = 0, d = 0;
        if (destLength == 0 || srcLength == 0) {
            return 0L;
        }
        ByteBuffer source;
        ByteBuffer dest;
        while (s < srcLength && d < destLength) {
            source = sources[srcOffset + s];
            dest = destinations[destOffset + d];
            t += copy(dest, source);
            if (! source.hasRemaining()) s++;
            if (! dest.hasRemaining()) d++;
        }
        return t;
    }

    /**
     * Copy at most {@code count} bytes from {@code source} into {@code destination}.
     *
     * @param count the maximum number of bytes to copy
     * @param destination the destination buffer
     * @param source the source buffer
     * @return the number of bytes put into the destination buffer
     */
    public static int copy(int count, final ByteBuffer destination, final ByteBuffer source) {
        Assert.checkMinimumParameter("count", 0, count);
        if (count == 0) return 0;
        final int sl = source.limit();
        final int sp = source.position();
        final int sr = sl - sp;
        final int dr = destination.remaining();
        if (sr < count || sr < dr || dr < count) {
            return copy(destination, source);
        }
        source.limit(sp + count);
        try {
            return copy(destination, source);
        } finally {
            source.limit(sl);
        }
    }

    /**
     * Copy at most {@code count} bytes from {@code sources} into {@code destinations} in a "scatter" fashion.
     *
     * @param count the maximum number of bytes to copy
     * @param destinations the destination buffers
     * @param offset the offset into the destination buffers array
     * @param length the number of buffers to update
     * @param source the source buffer
     * @return the number of bytes put into the destination buffers
     */
    public static int copy(int count, final ByteBuffer[] destinations, final int offset, final int length, final ByteBuffer source) {
        Assert.checkMinimumParameter("count", 0, count);
        if (count == 0 || length == 0) return 0;
        final int sl = source.limit();
        final int sp = source.position();
        final int sr = sl - sp;
        if (sr > count) {
            source.limit(sp + count);
            try {
                return copy(destinations, offset, length, source);
            } finally {
                source.limit(sl);
            }
        } else {
            return copy(destinations, offset, length, source);
        }
    }

    /**
     * Copy at most {@code count} bytes from {@code sources} into {@code destination} in a "gather" fashion.
     *
     * @param count the maximum number of bytes to copy
     * @param destination the destination buffer
     * @param sources the source buffers
     * @param offset the offset into the source buffers array
     * @param length the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static int copy(int count, final ByteBuffer destination, final ByteBuffer[] sources, final int offset, final int length) {
        Assert.checkMinimumParameter("count", 0, count);
        if (count == 0 || length == 0) return 0;
        final int dp = destination.position();
        final int dl = destination.limit();
        final int dr = dl - dp;
        if (dr > count) {
            destination.limit(dp + count);
            try {
                return copy(destination, sources, offset, length);
            } finally {
                destination.limit(dl);
            }
        } else {
            return copy(destination, sources, offset, length);
        }
    }

    /**
     * Copy at most {@code count} bytes from {@code sources} into {@code destinations} by a combined "scatter"/"gather" operation.
     *
     * @param count the maximum number of bytes to copy
     * @param destinations the destination buffers
     * @param destOffset the offset into the destination buffers array
     * @param destLength the number of buffers to write to
     * @param sources the source buffers
     * @param srcOffset the offset into the source buffers array
     * @param srcLength the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static long copy(long count, final ByteBuffer[] destinations, final int destOffset, final int destLength, final ByteBuffer[] sources, final int srcOffset, final int srcLength) {
        Assert.checkMinimumParameter("count", 0L, count);
        if (count == 0L || destLength == 0L || srcLength == 0L) return 0L;
        long t = 0L, c;
        int s = 0, d = 0;
        ByteBuffer source;
        ByteBuffer dest;
        while (s < srcLength && d < destLength) {
            source = sources[srcOffset + s];
            dest = destinations[destOffset + d];
            c = count > (long) Integer.MAX_VALUE ? copy(dest, source) : copy((int) count, dest, source);
            t += c;
            count -= c;
            if (count == 0) break;
            if (! source.hasRemaining()) s++;
            if (! dest.hasRemaining()) d++;
        }
        return t;
    }


    /**
     * Copy as many bytes as possible from {@code source} into {@code destination}, freeing the source buffer if it was
     * emptied.
     *
     * @param destination the destination buffer
     * @param source the source buffer
     * @return the number of bytes put into the destination buffer
     */
    public static int copyAndFree(final ByteBuffer destination, final ByteBuffer source) {
        final int sl = source.limit();
        final int sp = source.position();
        final int sr = sl - sp;
        final int dr = destination.remaining();
        if (sr == 0 || dr == 0) {
            return 0;
        } else if (dr >= sr) {
            destination.put(source);
            ByteBufferPool.free(source);
            return sr;
        } else {
            source.limit(sp + dr);
            try {
                destination.put(source);
            } finally {
                source.limit(sl);
            }
            return dr;
        }
    }

    /**
     * Copy as many bytes as possible from {@code sources} into {@code destinations} in a "scatter" fashion, freeing the
     * source buffer if it was emptied.
     *
     * @param destinations the destination buffers
     * @param offset the offset into the destination buffers array
     * @param length the number of buffers to update
     * @param source the source buffer
     * @return the number of bytes put into the destination buffers
     */
    public static int copyAndFree(final ByteBuffer[] destinations, final int offset, final int length, final ByteBuffer source) {
        int t = 0;
        for (int i = 0; i < length && source.hasRemaining(); i ++) {
            t += copyAndFree(destinations[i + offset], source);
        }
        return t;
    }

    /**
     * Copy as many bytes as possible from {@code sources} into {@code destination} in a "gather" fashion, freeing any
     * source buffers that were emptied.
     *
     * @param destination the destination buffer
     * @param sources the source buffers
     * @param offset the offset into the source buffers array
     * @param length the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static int copyAndFree(final ByteBuffer destination, final ByteBuffer[] sources, final int offset, final int length) {
        int t = 0;
        for (int i = 0; i < length && destination.hasRemaining(); i++) {
            t += copyAndFree(destination, sources[i + offset]);
        }
        return t;
    }

    /**
     * Copy as many bytes as possible from {@code sources} into {@code destinations} by a combined "scatter"/"gather" operation, freeing any
     * source buffers that were emptied.
     *
     * @param destinations the destination buffers
     * @param destOffset the offset into the destination buffers array
     * @param destLength the number of buffers to write to
     * @param sources the source buffers
     * @param srcOffset the offset into the source buffers array
     * @param srcLength the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static long copyAndFree(final ByteBuffer[] destinations, final int destOffset, final int destLength, final ByteBuffer[] sources, final int srcOffset, final int srcLength) {
        long t = 0L;
        int s = 0, d = 0;
        if (destLength == 0 || srcLength == 0) {
            return 0L;
        }
        ByteBuffer source;
        ByteBuffer dest;
        while (s < srcLength && d < destLength) {
            source = sources[srcOffset + s];
            dest = destinations[destOffset + d];
            t += copy(dest, source);
            if (! source.hasRemaining()) {
                ByteBufferPool.free(source);
                sources[srcOffset + s++] = null;
            }
            if (! dest.hasRemaining()) d++;
        }
        return t;
    }

    /**
     * Copy at most {@code count} bytes from {@code source} into {@code destination}, freeing the source buffer if it
     * was emptied.
     *
     * @param count the maximum number of bytes to copy
     * @param destination the destination buffer
     * @param source the source buffer
     * @return the number of bytes put into the destination buffer
     */
    public static int copyAndFree(int count, final ByteBuffer destination, final ByteBuffer source) {
        Assert.checkMinimumParameter("count", 0, count);
        if (count == 0) return 0;
        final int sl = source.limit();
        final int sp = source.position();
        final int sr = sl - sp;
        final int dr = destination.remaining();
        if (sr < count || sr < dr) {
            return copyAndFree(destination, source);
        } else if (dr < count) {
            return copy(destination, source);
        }
        source.limit(sp + count);
        try {
            return copy(destination, source);
        } finally {
            source.limit(sl);
        }
    }

    /**
     * Copy at most {@code count} bytes from {@code sources} into {@code destinations} in a "scatter" fashion, freeing
     * the source buffer if it was emptied.
     *
     * @param count the maximum number of bytes to copy
     * @param destinations the destination buffers
     * @param offset the offset into the destination buffers array
     * @param length the number of buffers to update
     * @param source the source buffer
     * @return the number of bytes put into the destination buffers
     */
    public static int copyAndFree(int count, final ByteBuffer[] destinations, final int offset, final int length, final ByteBuffer source) {
        Assert.checkMinimumParameter("count", 0, count);
        if (count == 0 || length == 0) return 0;
        final int sl = source.limit();
        final int sp = source.position();
        final int sr = sl - sp;
        if (sr > count) {
            source.limit(sp + count);
            try {
                return copy(destinations, offset, length, source);
            } finally {
                source.limit(sl);
            }
        } else {
            return copyAndFree(destinations, offset, length, source);
        }
    }

    /**
     * Copy at most {@code count} bytes from {@code sources} into {@code destination} in a "gather" fashion, freeing any
     * source buffers that were emptied.
     *
     * @param count the maximum number of bytes to copy
     * @param destination the destination buffer
     * @param sources the source buffers
     * @param offset the offset into the source buffers array
     * @param length the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static int copyAndFree(int count, final ByteBuffer destination, final ByteBuffer[] sources, final int offset, final int length) {
        Assert.checkMinimumParameter("count", 0, count);
        if (count == 0 || length == 0) return 0;
        final int dp = destination.position();
        final int dl = destination.limit();
        final int dr = dl - dp;
        if (dr > count) {
            destination.limit(dp + count);
            try {
                return copyAndFree(destination, sources, offset, length);
            } finally {
                destination.limit(dl);
            }
        } else {
            return copyAndFree(destination, sources, offset, length);
        }
    }

    /**
     * Copy at most {@code count} bytes from {@code sources} into {@code destinations} by a combined "scatter"/"gather"
     * operation, freeing any source buffers that were emptied.
     *
     * @param count the maximum number of bytes to copy
     * @param destinations the destination buffers
     * @param destOffset the offset into the destination buffers array
     * @param destLength the number of buffers to write to
     * @param sources the source buffers
     * @param srcOffset the offset into the source buffers array
     * @param srcLength the number of buffers to read from
     * @return the number of bytes put into the destination buffers
     */
    public static long copyAndFree(long count, final ByteBuffer[] destinations, final int destOffset, final int destLength, final ByteBuffer[] sources, final int srcOffset, final int srcLength) {
        Assert.checkMinimumParameter("count", 0L, count);
        if (count == 0L || destLength == 0L || srcLength == 0L) return 0L;
        long t = 0L, c;
        int s = 0, d = 0;
        ByteBuffer source;
        ByteBuffer dest;
        while (s < srcLength && d < destLength) {
            source = sources[srcOffset + s];
            dest = destinations[destOffset + d];
            c = count > (long) Integer.MAX_VALUE ? copy(dest, source) : copy((int) count, dest, source);
            t += c;
            count -= c;
            if (! source.hasRemaining()) {
                ByteBufferPool.free(source);
                sources[srcOffset + s++] = null;
            }
            if (count == 0) break;
            if (! dest.hasRemaining()) d++;
        }
        return t;
    }

    /**
     * Fill a buffer with a repeated value.
     *
     * @param buffer the buffer to fill
     * @param value the value to fill
     * @param count the number of bytes to fill
     * @return the buffer instance
     */
    public static ByteBuffer fill(ByteBuffer buffer, int value, int count) {
        if (count > buffer.remaining()) {
            throw msg.bufferUnderflow();
        }
        if (buffer.hasArray()) {
            final int offs = buffer.arrayOffset();
            Arrays.fill(buffer.array(), offs + buffer.position(), offs + buffer.limit(), (byte) value);
            skip(buffer, count);
        } else {
            for (int i = count; i > 0; i--) {
                buffer.put((byte)value);
            }
        }
        return buffer;
    }

    /**
     * Advance a buffer's position relative to its current position.
     *
     * @see Buffer#position(int)
     * @param <T> the buffer type
     * @param buffer the buffer to set
     * @param cnt the distance to skip
     * @return the buffer instance
     * @throws BufferUnderflowException if there are fewer than {@code cnt} bytes remaining
     */
    public static <T extends Buffer> T skip(T buffer, int cnt) throws BufferUnderflowException {
        Assert.checkMinimumParameter("cnt", 0, cnt);
        if (cnt > buffer.remaining()) {
            throw msg.bufferUnderflow();
        }
        if (cnt != 0) buffer.position(buffer.position() + cnt);
        return buffer;
    }

    /**
     * Attempt to advance a buffer's position relative to its current position.
     *
     * @see Buffer#position(int)
     * @param buffer the buffer to set
     * @param cnt the distance to skip
     * @return the actual number of bytes skipped
     */
    public static int trySkip(Buffer buffer, int cnt) {
        Assert.checkMinimumParameter("cnt", 0, cnt);
        final int rem = buffer.remaining();
        if (cnt > rem) {
            cnt = rem;
        }
        if (cnt != 0) buffer.position(buffer.position() + cnt);
        return cnt;
    }

    /**
     * Attempt to advance a series of buffers' overall position relative to its current position.
     *
     * @see Buffer#position(int)
     * @param buffers the buffers to set
     * @param offs the offset into the buffers array
     * @param len the number of buffers to consider
     * @param cnt the distance to skip
     * @return the actual number of bytes skipped
     */
    public static long trySkip(Buffer[] buffers, int offs, int len, long cnt) {
        Assert.checkArrayBounds(buffers, offs, len);
        long c = 0L;
        for (int i = 0; i < len; i ++) {
            final Buffer buffer = buffers[offs + i];
            final int rem = buffer.remaining();
            if (rem < cnt) {
                buffer.position(buffer.position() + rem);
                cnt -= (long) rem;
                c += (long) rem;
            } else {
                buffer.position(buffer.position() + (int) cnt);
                return c + cnt;
            }
        }
        return c;
    }

    /**
     * Rewind a buffer's position relative to its current position.
     *
     * @see Buffer#position(int)
     * @param <T> the buffer type
     * @param buffer the buffer to set
     * @param cnt the distance to skip backwards
     * @return the buffer instance
     */
    public static <T extends Buffer> T unget(T buffer, int cnt) {
        Assert.checkMinimumParameter("cnt", 0, cnt);
        if (cnt > buffer.position()) {
            throw msg.bufferUnderflow();
        }
        buffer.position(buffer.position() - cnt);
        return buffer;
    }

    /**
     * Take a certain number of bytes from the buffer and return them in an array.
     *
     * @param buffer the buffer to read
     * @param cnt the number of bytes to take
     * @return the bytes
     */
    public static byte[] take(ByteBuffer buffer, int cnt) {
        Assert.checkMinimumParameter("cnt", 0, cnt);
        if (buffer.hasArray()) {
            final int pos = buffer.position();
            final int lim = buffer.limit();
            if (lim - pos < cnt) {
                throw new BufferUnderflowException();
            }
            final byte[] array = buffer.array();
            final int offset = buffer.arrayOffset();
            buffer.position(pos + cnt);
            final int start = offset + pos;
            return Arrays.copyOfRange(array, start, start + cnt);
        }
        final byte[] bytes = new byte[cnt];
        buffer.get(bytes);
        return bytes;
    }

    private static final byte[] NO_BYTES = new byte[0];

    /**
     * Take all of the remaining bytes from the buffer and return them in an array.
     *
     * @param buffer the buffer to read
     * @return the bytes
     */
    public static byte[] take(ByteBuffer buffer) {
        final int remaining = buffer.remaining();
        if (remaining == 0) return NO_BYTES;
        if (buffer.hasArray()) {
            final int pos = buffer.position();
            final int lim = buffer.limit();
            final byte[] array = buffer.array();
            final int offset = buffer.arrayOffset();
            buffer.position(lim);
            return Arrays.copyOfRange(array, offset + pos, offset + lim);
        }
        final byte[] bytes = new byte[remaining];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * Take all of the remaining bytes from the buffers and return them in an array.
     *
     * @param buffers the buffer to read
     * @param offs the offset into the array
     * @param len the number of buffers
     * @return the bytes
     */
    public static byte[] take(final ByteBuffer[] buffers, final int offs, final int len) {
        if (len == 1) return take(buffers[offs]);
        final long remaining = Buffers.remaining(buffers, offs, len);
        if (remaining == 0L) return NO_BYTES;
        if (remaining > Integer.MAX_VALUE) throw new OutOfMemoryError("Array too large");
        final byte[] bytes = new byte[(int) remaining];
        int o = 0;
        int rem;
        ByteBuffer buffer;
        for (int i = 0; i < len; i ++) {
            buffer = buffers[i + offs];
            rem = buffer.remaining();
            buffer.get(bytes, o, rem);
            o += rem;
        }
        return bytes;
    }

    /**
     * Create an object that returns the dumped form of the given byte buffer when its {@code toString()} method is called.
     * Useful for logging byte buffers; if the {@code toString()} method is never called, the process of dumping the
     * buffer is never performed.
     *
     * @param buffer the buffer
     * @param indent the indentation to use
     * @param columns the number of 8-byte columns
     * @return a stringable object
     */
    public static Object createDumper(final ByteBuffer buffer, final int indent, final int columns) {
        Assert.checkMinimumParameter("columns", 1, columns);
        Assert.checkMinimumParameter("indent", 0, indent);
        return new Object() {
            public String toString() {
                StringBuilder b = new StringBuilder();
                try {
                    dump(buffer, b, indent, columns);
                } catch (IOException e) {
                    // ignore, not possible!
                }
                return b.toString();
            }
        };
    }

    /**
     * Dump a byte buffer to the given target.
     *
     * @param buffer the buffer
     * @param dest the target
     * @param indent the indentation to use
     * @param columns the number of 8-byte columns
     * @throws IOException if an error occurs during append
     */
    public static void dump(final ByteBuffer buffer, final Appendable dest, final int indent, final int columns) throws IOException {
        Assert.checkMinimumParameter("columns", 1, columns);
        Assert.checkMinimumParameter("indent", 0, indent);
        final int pos = buffer.position();
        final int remaining = buffer.remaining();
        final int rowLength = (8 << (columns - 1));
        final int n = Math.max(Integer.toString(buffer.remaining(), 16).length(), 4);
        for (int idx = 0; idx < remaining; idx += rowLength) {
            // state: start of line
            for (int i = 0; i < indent; i ++) {
                dest.append(' ');
            }
            final String s = Integer.toString(idx, 16);
            for (int i = n - s.length(); i > 0; i --) {
                dest.append('0');
            }
            dest.append(s);
            dest.append(" - ");
            appendHexRow(buffer, dest, pos + idx, columns);
            appendTextRow(buffer, dest, pos + idx, columns);
            dest.append('\n');
        }
    }

    private static void appendHexRow(final ByteBuffer buffer, final Appendable dest, final int startPos, final int columns) throws IOException {
        final int limit = buffer.limit();
        int pos = startPos;
        for (int c = 0; c < columns; c ++) {
            for (int i = 0; i < 8; i ++) {
                if (pos >= limit) {
                    dest.append("  ");
                } else {
                    final int v = buffer.get(pos++) & 0xff;
                    final String hexVal = Integer.toString(v, 16);
                    if (v < 16) {
                        dest.append('0');
                    }
                    dest.append(hexVal);
                }
                dest.append(' ');
            }
            dest.append(' ');
            dest.append(' ');
        }
    }

    private static void appendTextRow(final ByteBuffer buffer, final Appendable dest, final int startPos, final int columns) throws IOException {
        final int limit = buffer.limit();
        int pos = startPos;
        dest.append('[');
        dest.append(' ');
        for (int c = 0; c < columns; c ++) {
            for (int i = 0; i < 8; i ++) {
                if (pos >= limit) {
                    dest.append(' ');
                } else {
                    final char v = (char) (buffer.get(pos++) & 0xff);
                    if (Character.isISOControl(v)) {
                        dest.append('.');
                    } else {
                        dest.append(v);
                    }
                }
            }
            dest.append(' ');
        }
        dest.append(']');
    }

    /**
     * The empty byte buffer.
     */
    public static final ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocate(0);

    /**
     * The empty pooled byte buffer.  Freeing or discarding this buffer has no effect.
     */
    @Deprecated
    public static final Pooled<ByteBuffer> EMPTY_POOLED_BYTE_BUFFER = emptyPooledByteBuffer();

    /**
     * Determine whether any of the buffers has remaining data.
     *
     * @param buffers the buffers
     * @param offs the offset into the buffers array
     * @param len the number of buffers to check
     * @return {@code true} if any of the selected buffers has remaining data
     */
    public static boolean hasRemaining(final Buffer[] buffers, final int offs, final int len) {
        for (int i = 0; i < len; i ++) {
            if (buffers[i + offs].hasRemaining()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether any of the buffers has remaining data.
     *
     * @param buffers the buffers
     * @return {@code true} if any of the selected buffers has remaining data
     */
    public static boolean hasRemaining(final Buffer[] buffers) {
        return hasRemaining(buffers, 0, buffers.length);
    }

    /**
     * Get the total remaining size of all the given buffers.
     *
     * @param buffers the buffers
     * @param offs the offset into the buffers array
     * @param len the number of buffers to check
     * @return the number of remaining elements
     */
    public static long remaining(final Buffer[] buffers, final int offs, final int len) {
        long t = 0L;
        for (int i = 0; i < len; i ++) {
            t += buffers[i + offs].remaining();
        }
        return t;
    }

    /**
     * Get the total remaining size of all the given buffers.
     *
     * @param buffers the buffers
     * @return the number of remaining elements
     */
    public static long remaining(final Buffer[] buffers) {
        return remaining(buffers, 0, buffers.length);
    }

    /**
     * Put the string into the byte buffer, encoding it using "modified UTF-8" encoding.
     *
     * @param dest the byte buffer
     * @param orig the source bytes
     * @return the byte buffer
     * @throws BufferOverflowException if there is not enough space in the buffer for the complete string
     * @see DataOutput#writeUTF(String)
     */
    public static ByteBuffer putModifiedUtf8(ByteBuffer dest, String orig) throws BufferOverflowException {
        final char[] chars = orig.toCharArray();
        for (char c : chars) {
            if (c > 0 && c <= 0x7f) {
                dest.put((byte) c);
            } else if (c <= 0x07ff) {
                dest.put((byte)(0xc0 | 0x1f & c >> 6));
                dest.put((byte)(0x80 | 0x3f & c));
            } else {
                dest.put((byte)(0xe0 | 0x0f & c >> 12));
                dest.put((byte)(0x80 | 0x3f & c >> 6));
                dest.put((byte)(0x80 | 0x3f & c));
            }
        }
        return dest;
    }

    /**
     * Get a 0-terminated string from the byte buffer, decoding it using "modified UTF-8" encoding.
     *
     * @param src the source buffer
     * @return the string
     * @throws BufferUnderflowException if the end of the buffer was reached before encountering a {@code 0}
     */
    public static String getModifiedUtf8Z(ByteBuffer src) throws BufferUnderflowException {
        final StringBuilder builder = new StringBuilder();
        for (;;) {
            final int ch = readUTFChar(src);
            if (ch == -1) {
                return builder.toString();
            }
            builder.append((char) ch);
        }
    }

    /**
     * Get a modified UTF-8 string from the remainder of the buffer.
     *
     * @param src the buffer
     * @return the modified UTF-8 string
     * @throws BufferUnderflowException if the buffer ends abruptly in the midst of a single character
     */
    public static String getModifiedUtf8(ByteBuffer src) throws BufferUnderflowException {
        final StringBuilder builder = new StringBuilder();
        while (src.hasRemaining()) {
            final int ch = readUTFChar(src);
            if (ch == -1) {
                builder.append('\0');
            } else {
                builder.append((char) ch);
            }
        }
        return builder.toString();
    }

    private static int readUTFChar(final ByteBuffer src) throws BufferUnderflowException {
        final int a = src.get() & 0xff;
        if (a == 0) {
            return -1;
        } else if (a < 0x80) {
            return (char)a;
        } else if (a < 0xc0) {
            return '?';
        } else if (a < 0xe0) {
            final int b = src.get() & 0xff;
            if ((b & 0xc0) != 0x80) {
                return '?';
            }
            return (a & 0x1f) << 6 | b & 0x3f;
        } else if (a < 0xf0) {
            final int b = src.get() & 0xff;
            if ((b & 0xc0) != 0x80) {
                return '?';
            }
            final int c = src.get() & 0xff;
            if ((c & 0xc0) != 0x80) {
                return '?';
            }
            return (a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f;
        }
        return '?';
    }

    /**
     * Read an ASCIIZ ({@code NUL}-terminated) string from a byte buffer, appending the results to the given string
     * builder.  If no {@code NUL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character {@code '?'} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readAsciiZ(final ByteBuffer src, final StringBuilder builder) {
        return readAsciiZ(src, builder, '?');
    }

    /**
     * Read an ASCIIZ ({@code NUL}-terminated) string from a byte buffer, appending the results to the given string
     * builder.  If no {@code NUL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character for invalid bytes
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readAsciiZ(final ByteBuffer src, final StringBuilder builder, final char replacement) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final byte b = src.get();
            if (b == 0) {
                return true;
            }
            builder.append(b < 0 ? replacement : (char) b);
        }
    }

    /**
     * Read a single line of ASCII text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character {@code '?'} is written
     * to the string builder in its place.  The {@code EOL} character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readAsciiLine(final ByteBuffer src, final StringBuilder builder) {
        return readAsciiLine(src, builder, '?', '\n');
    }

    /**
     * Read a single line of ASCII text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.  The {@code EOL} character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character for invalid bytes
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readAsciiLine(final ByteBuffer src, final StringBuilder builder, final char replacement) {
        return readAsciiLine(src, builder, replacement, '\n');
    }

    /**
     * Read a single line of ASCII text from a byte buffer, appending the results to the given string
     * builder, using the given delimiter character instead of {@code EOL}.  If no delimiter character is encountered,
     * {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.  The delimiter character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character for invalid bytes
     * @param delimiter the character which marks the end of the line
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readAsciiLine(final ByteBuffer src, final StringBuilder builder, final char replacement, final char delimiter) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final byte b = src.get();
            builder.append(b < 0 ? replacement : (char) b);
            if (b == delimiter) {
                return true;
            }
        }
    }

    /**
     * Read the remainder of a buffer as ASCII text, appending the results to the given string
     * builder.  If an invalid byte is read, the character {@code '?'} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     */
    public static void readAscii(final ByteBuffer src, final StringBuilder builder) {
        readAscii(src, builder, '?');
    }

    /**
     * Read the remainder of a buffer as ASCII text, appending the results to the given string
     * builder.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character for invalid bytes
     */
    public static void readAscii(final ByteBuffer src, final StringBuilder builder, final char replacement) {
        for (;;) {
            if (! src.hasRemaining()) {
                return;
            }
            final byte b = src.get();
            builder.append(b < 0 ? replacement : (char) b);
        }
    }

    /**
     * Read the remainder of a buffer as ASCII text, up to a certain limit, appending the results to the given string
     * builder.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param limit the maximum number of characters to write
     * @param replacement the replacement character for invalid bytes
     */
    public static void readAscii(final ByteBuffer src, final StringBuilder builder, int limit, final char replacement) {
        while (limit > 0) {
            if (! src.hasRemaining()) {
                return;
            }
            final byte b = src.get();
            builder.append(b < 0 ? replacement : (char) b);
            limit--;
        }
    }

    /**
     * Read a {@code NUL}-terminated Latin-1 string from a byte buffer, appending the results to the given string
     * builder.  If no {@code NUL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readLatin1Z(final ByteBuffer src, final StringBuilder builder) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final byte b = src.get();
            if (b == 0) {
                return true;
            }
            builder.append((char) (b & 0xff));
        }
    }

    /**
     * Read a single line of Latin-1 text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  The {@code EOL} character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readLatin1Line(final ByteBuffer src, final StringBuilder builder) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final byte b = src.get();
            builder.append((char) (b & 0xff));
            if (b == '\n') {
                return true;
            }
        }
    }

    /**
     * Read a single line of Latin-1 text from a byte buffer, appending the results to the given string
     * builder.  If no delimiter character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  The delimiter character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param delimiter the character which marks the end of the line
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readLatin1Line(final ByteBuffer src, final StringBuilder builder, final char delimiter) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final byte b = src.get();
            builder.append((char) (b & 0xff));
            if (b == delimiter) {
                return true;
            }
        }
    }

    /**
     * Read the remainder of a buffer as Latin-1 text, appending the results to the given string
     * builder.
     *
     * @param src the source buffer
     * @param builder the destination builder
     */
    public static void readLatin1(final ByteBuffer src, final StringBuilder builder) {
        for (;;) {
            if (! src.hasRemaining()) {
                return;
            }
            final byte b = src.get();
            builder.append((char) (b & 0xff));
        }
    }

    /**
     * Read a {@code NUL}-terminated {@link DataInput modified UTF-8} string from a byte buffer, appending the results to the given string
     * builder.  If no {@code NUL} byte is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte sequence is read, the character {@code '?'} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readModifiedUtf8Z(final ByteBuffer src, final StringBuilder builder) {
        return readModifiedUtf8Z(src, builder, '?');
    }

    /**
     * Read a {@code NUL}-terminated {@link DataInput modified UTF-8} string from a byte buffer, appending the results to the given string
     * builder.  If no {@code NUL} byte is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte sequence is read, the character designated by {@code replacement} is written
     * to the string builder in its place.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character to use
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readModifiedUtf8Z(final ByteBuffer src, final StringBuilder builder, final char replacement) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final int a = src.get() & 0xff;
            if (a == 0) {
                return true;
            } else if (a < 0x80) {
                builder.append((char)a);
            } else if (a < 0xc0) {
                builder.append(replacement);
            } else if (a < 0xe0) {
                if (src.hasRemaining()) {
                    final int b = src.get() & 0xff;
                    if ((b & 0xc0) != 0x80) {
                        builder.append(replacement);
                    } else {
                        builder.append((char) ((a & 0x1f) << 6 | b & 0x3f));
                    }
                } else {
                    unget(src, 1);
                    return false;
                }
            } else if (a < 0xf0) {
                if (src.hasRemaining()) {
                    final int b = src.get() & 0xff;
                    if ((b & 0xc0) != 0x80) {
                        builder.append(replacement);
                    } else {
                        if (src.hasRemaining()) {
                            final int c = src.get() & 0xff;
                            if ((c & 0xc0) != 0x80) {
                                builder.append(replacement);
                            } else {
                                builder.append((char) ((a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f));
                            }
                        } else {
                            unget(src, 2);
                            return false;
                        }
                    }
                } else {
                    unget(src, 1);
                    return false;
                }
            } else {
                builder.append(replacement);
            }
        }
    }

    /**
     * Read a single line of {@link DataInput modified UTF-8} text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character {@code '?'} is written
     * to the string builder in its place.  The {@code EOL} character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readModifiedUtf8Line(final ByteBuffer src, final StringBuilder builder) {
        return readModifiedUtf8Line(src, builder, '?');
    }

    /**
     * Read a single line of {@link DataInput modified UTF-8} text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.  The {@code EOL} character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character for invalid bytes
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readModifiedUtf8Line(final ByteBuffer src, final StringBuilder builder, final char replacement) {
        return readModifiedUtf8Line(src, builder, replacement, '\n');
    }

    /**
     * Read a single line of {@link DataInput modified UTF-8} text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  If an invalid byte is read, the character designated by {@code replacement} is written
     * to the string builder in its place.  The delimiter character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param replacement the replacement character for invalid bytes
     * @param delimiter the character which marks the end of the line
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readModifiedUtf8Line(final ByteBuffer src, final StringBuilder builder, final char replacement, final char delimiter) {
        for (;;) {
            if (! src.hasRemaining()) {
                return false;
            }
            final int a = src.get() & 0xff;
            if (a < 0x80) {
                builder.append((char)a);
                if (a == delimiter) {
                    return true;
                }
            } else if (a < 0xc0) {
                builder.append(replacement);
            } else if (a < 0xe0) {
                if (src.hasRemaining()) {
                    final int b = src.get() & 0xff;
                    if ((b & 0xc0) != 0x80) {
                        builder.append(replacement);
                    } else {
                        final char ch = (char) ((a & 0x1f) << 6 | b & 0x3f);
                        builder.append(ch);
                        if (ch == delimiter) {
                            return true;
                        }
                    }
                } else {
                    unget(src, 1);
                    return false;
                }
            } else if (a < 0xf0) {
                if (src.hasRemaining()) {
                    final int b = src.get() & 0xff;
                    if ((b & 0xc0) != 0x80) {
                        builder.append(replacement);
                    } else {
                        if (src.hasRemaining()) {
                            final int c = src.get() & 0xff;
                            if ((c & 0xc0) != 0x80) {
                                builder.append(replacement);
                            } else {
                                final char ch = (char) ((a & 0x0f) << 12 | (b & 0x3f) << 6 | c & 0x3f);
                                builder.append(ch);
                                if (ch == delimiter) {
                                    return true;
                                }
                            }
                        } else {
                            unget(src, 2);
                            return false;
                        }
                    }
                } else {
                    unget(src, 1);
                    return false;
                }
            } else {
                builder.append(replacement);
            }
        }
    }

    /**
     * Read a single line of text from a byte buffer, appending the results to the given string
     * builder.  If no {@code EOL} character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  Invalid bytes are handled according to the policy specified by the {@code decoder} instance.
     * Since this method decodes only one character at a time, it should not be expected to have the same performance
     * as the other optimized, character set-specific methods specified in this class.
     * The {@code EOL} character will be included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param decoder the decoder to use
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readLine(final ByteBuffer src, final StringBuilder builder, final CharsetDecoder decoder) {
        return readLine(src, builder, decoder, '\n');
    }

    /**
     * Read a single line of text from a byte buffer, appending the results to the given string
     * builder.  If no delimiter character is encountered, {@code false} is returned, indicating that more data needs
     * to be acquired before the operation can be complete.  On return, there may be data remaining
     * in the source buffer.  Invalid bytes are handled according to the policy specified by the {@code decoder} instance.
     * Since this method decodes only one character at a time, it should not be expected to have the same performance
     * as the other optimized, character set-specific methods specified in this class.  The delimiter character will be
     * included in the resultant string.
     *
     * @param src the source buffer
     * @param builder the destination builder
     * @param decoder the decoder to use
     * @param delimiter the character which marks the end of the line
     * @return {@code true} if the entire string was read, {@code false} if more data is needed
     */
    public static boolean readLine(final ByteBuffer src, final StringBuilder builder, final CharsetDecoder decoder, final char delimiter) {
        final CharBuffer oneChar = CharBuffer.allocate(1);
        for (;;) {
            final CoderResult coderResult = decoder.decode(src, oneChar, false);
            if (coderResult.isUnderflow()) {
                if (oneChar.hasRemaining()) {
                    return false;
                }
            } else if (oneChar.hasRemaining()) {
                throw new IllegalStateException();
            }
            final char ch = oneChar.get(0);
            builder.append(ch);
            if (ch == delimiter) {
                return true;
            }
            oneChar.clear();
        }
    }

    /**
     * Create a pooled wrapper around a buffer.  The buffer is unreferenced for garbage collection when
     * freed or discarded.
     *
     * @param buffer the buffer to wrap
     * @param <B> the buffer type
     * @return the pooled wrapper
     */
    @Deprecated
    public static <B extends Buffer> Pooled<B> pooledWrapper(final B buffer) {
        return new Pooled<B>() {
            private volatile B buf = buffer;

            public void discard() {
                buf = null;
            }

            public void free() {
                buf = null;
            }

            public B getResource() throws IllegalStateException {
                final B buffer = buf;
                if (buffer == null) {
                    throw new IllegalStateException();
                }
                return buffer;
            }

            public void close() {
                free();
            }

            public String toString() {
                return "Pooled wrapper around " + buffer;
            }
        };
    }

    /**
     * Create a "pooled" empty buffer.  Discarding or freeing the buffer has no effect; the returned buffer is
     * always empty.
     *
     * @return a new pooled empty buffer
     */
    @Deprecated
    public static Pooled<ByteBuffer> emptyPooledByteBuffer() {
        return new Pooled<ByteBuffer>() {
            public void discard() {
            }

            public void free() {
            }

            public ByteBuffer getResource() throws IllegalStateException {
                return EMPTY_BYTE_BUFFER;
            }

            public void close() {
            }
        };
    }

    /**
     * A buffer allocator which allocates slices off of the given buffer.  Once the buffer is exhausted, further
     * attempts to allocate buffers will result in {@link BufferUnderflowException}.
     *
     * @param buffer the source buffer
     * @return the slice allocator
     */
    @Deprecated
    public static BufferAllocator<ByteBuffer> sliceAllocator(final ByteBuffer buffer) {
        return new BufferAllocator<ByteBuffer>() {
            public ByteBuffer allocate(final int size) throws IllegalArgumentException {
                return Buffers.slice(buffer, size);
            }
        };
    }

    /**
     * A buffer pool which allocates a new buffer on every allocate request, and discards buffers on free.
     *
     * @param allocator the buffer allocator
     * @param size the buffer size
     * @param <B> the buffer type
     * @return the buffer pool
     */
    @Deprecated
    public static <B extends Buffer> Pool<B> allocatedBufferPool(final BufferAllocator<B> allocator, final int size) {
        return new Pool<B>() {
            public Pooled<B> allocate() {
                return pooledWrapper(allocator.allocate(size));
            }
        };
    }

    /**
     * A byte buffer pool which zeroes the content of the buffer before re-pooling it.
     *
     * @param delegate the delegate pool
     * @return the wrapper pool
     */
    @Deprecated
    public static Pool<ByteBuffer> secureBufferPool(final Pool<ByteBuffer> delegate) {
        return new SecureByteBufferPool(delegate);
    }

    /**
     * Determine whether the given pool is a secure pool.  Note that this test will fail if used on a pool
     * which wraps a secure pool.
     *
     * @param pool the pool to test
     * @return {@code true} if it is a secure pool instance
     */
    @Deprecated
    public static boolean isSecureBufferPool(Pool<?> pool) {
        return pool instanceof SecureByteBufferPool;
    }

    private static final byte[] ZEROS = new byte[1024];

    /**
     * Zero a buffer.  Ensures that any potentially sensitive information in the buffer is
     * overwritten.
     *
     * @param buffer the buffer
     */
    public static void zero(ByteBuffer buffer) {
        buffer.clear();
        while (buffer.remaining() >= 1024) {
            buffer.put(ZEROS);
        }
        buffer.put(ZEROS, 0, buffer.remaining());
        buffer.position(0);
    }

    /**
     * Determine whether the given buffers list is comprised solely of direct buffers or solely of heap buffers.
     *
     * @param buffers the buffers
     * @return {@code true} if all the buffers are direct, {@code false} if they are all heap buffers
     * @throws IllegalArgumentException if both direct and heap buffers were found, or if a buffer is {@code null}
     */
    public static boolean isDirect(Buffer... buffers) throws IllegalArgumentException {
        return isDirect(buffers, 0, buffers.length);
    }

    /**
     * Determine whether the given buffers list is comprised solely of direct buffers or solely of heap buffers.
     *
     * @param buffers the buffers
     * @return {@code true} if all the buffers are direct, {@code false} if they are all heap buffers
     * @throws IllegalArgumentException if both direct and heap buffers were found, or if a buffer is {@code null}
     */
    public static boolean isDirect(final Buffer[] buffers, final int offset, final int length) {
        boolean foundDirect = false;
        boolean foundHeap = false;
        for (int i = 0; i < length; i ++) {
            final Buffer buffer = Assert.checkNotNullArrayParam("buffers", i + offset, buffers[i + offset]);
            if (buffer.isDirect()) {
                if (foundHeap) {
                    throw msg.mixedDirectAndHeap();
                }
                foundDirect = true;
            } else {
                if (foundDirect) {
                    throw msg.mixedDirectAndHeap();
                }
                foundHeap = true;
            }
        }
        return foundDirect;
    }

    /**
     * Assert the writability of the given buffers.
     *
     * @param buffers the buffers array
     * @param offs the offset in the array to start searching
     * @param len the number of buffers to check
     * @throws ReadOnlyBufferException if any of the buffers are read-only
     */
    public static void assertWritable(Buffer[] buffers, int offs, int len) throws ReadOnlyBufferException {
        for (int i = 0; i < len; i ++) {
            if (buffers[i + offs].isReadOnly()) {
                throw msg.readOnlyBuffer();
            }
        }
    }

    /**
     * Assert the writability of the given buffers.
     *
     * @param buffers the buffers array
     * @throws ReadOnlyBufferException if any of the buffers are read-only
     */
    public static void assertWritable(Buffer... buffers) throws ReadOnlyBufferException {
        assertWritable(buffers, 0, buffers.length);
    }

    /**
     * Add {@code count} bytes of random data to the target buffer.
     *
     * @param target the target buffer
     * @param random the RNG
     * @param count the number of bytes to add
     */
    public static void addRandom(ByteBuffer target, Random random, int count) {
        final byte[] bytes = new byte[count];
        random.nextBytes(bytes);
        target.put(bytes);
    }

    /**
     * Add {@code count} bytes of random data to the target buffer using the thread-local RNG.
     *
     * @param target the target buffer
     * @param count the number of bytes to add
     */
    public static void addRandom(ByteBuffer target, int count) {
        addRandom(target, ThreadLocalRandom.current(), count);
    }

    /**
     * Add a random amount of random data to the target buffer.
     *
     * @param target the target buffer
     * @param random the RNG
     */
    public static void addRandom(ByteBuffer target, Random random) {
        if (target.remaining() == 0) {
            return;
        }
        addRandom(target, random, random.nextInt(target.remaining()));
    }

    /**
     * Add a random amount of random data to the target buffer using the thread-local RNG.
     *
     * @param target the target buffer
     */
    public static void addRandom(ByteBuffer target) {
        addRandom(target, ThreadLocalRandom.current());
    }

    /**
     * Fill a buffer from an input stream.  Specially optimized for heap buffers.  If a partial transfer occurs
     * due to interruption, the buffer's position is updated accordingly.
     *
     * @param target the target buffer
     * @param source the source stream
     * @return the number of bytes transferred, or {@code -1} if no bytes were moved due to end-of-stream
     * @throws IOException if the stream read fails
     */
    public static int fillFromStream(ByteBuffer target, InputStream source) throws IOException {
        final int remaining = target.remaining();
        if (remaining == 0) {
            return 0;
        } else {
            final int p = target.position();
            if (target.hasArray()) {
                // fast path
                final int res;
                try {
                    res = source.read(target.array(), p + target.arrayOffset(), remaining);
                } catch (InterruptedIOException e) {
                    target.position(p + e.bytesTransferred);
                    throw e;
                }
                if (res > 0) {
                    target.position(p + res);
                }
                return res;
            } else {
                byte[] tmp = new byte[remaining];
                final int res;
                try {
                    res = source.read(tmp);
                } catch (InterruptedIOException e) {
                    final int n = e.bytesTransferred;
                    target.put(tmp, 0, n);
                    target.position(p + n);
                    throw e;
                }
                if (res > 0) {
                    target.put(tmp, 0, res);
                }
                return res;
            }
        }
    }

    /**
     * Get a debug-friendly description of the buffer.
     *
     * @param buffer the buffer to describe
     * @return the string
     */
    public static String debugString(ByteBuffer buffer) {
        StringBuilder b = new StringBuilder();
        b.append("1 buffer of ").append(buffer.remaining()).append(" bytes");
        return b.toString();
    }

    /**
     * Get a debug-friendly description of the buffer.
     *
     * @param buffers the buffers to describe
     * @param offs the offset into the array
     * @param len the number of buffers
     * @return the string
     */
    public static String debugString(ByteBuffer[] buffers, int offs, int len) {
        StringBuilder b = new StringBuilder();
        b.append(len).append(" buffer(s)");
        if (len > 0) {
            b.append(" of ").append(Buffers.remaining(buffers, offs, len)).append(" bytes");
        }
        return b.toString();
    }

    /**
     * Empty a buffer to an output stream.  Specially optimized for heap buffers.  If a partial transfer occurs
     * due to interruption, the buffer's position is updated accordingly.
     *
     * @param target the target stream
     * @param source the source buffer
     * @throws IOException if the stream write fails
     */
    public static void emptyToStream(OutputStream target, ByteBuffer source) throws IOException {
        final int remaining = source.remaining();
        if (remaining == 0) {
            return;
        } else {
            final int p = source.position();
            if (source.hasArray()) {
                // fast path
                try {
                    target.write(source.array(), p + source.arrayOffset(), remaining);
                } catch (InterruptedIOException e) {
                    source.position(p + e.bytesTransferred);
                    throw e;
                }
                source.position(source.limit());
                return;
            } else {
                byte[] tmp = take(source);
                try {
                    target.write(tmp);
                } catch (InterruptedIOException e) {
                    source.position(p + e.bytesTransferred);
                    throw e;
                } catch (IOException e) {
                    source.position(p);
                    throw e;
                }
            }
        }
    }

    @Deprecated
    private static class SecureByteBufferPool implements Pool<ByteBuffer> {

        private final Pool<ByteBuffer> delegate;

        SecureByteBufferPool(final Pool<ByteBuffer> delegate) {
            this.delegate = delegate;
        }

        public Pooled<ByteBuffer> allocate() {
            return new SecurePooledByteBuffer(delegate.allocate());
        }
    }

    @Deprecated
    private static class SecurePooledByteBuffer implements Pooled<ByteBuffer> {

        private static final AtomicIntegerFieldUpdater<SecurePooledByteBuffer> freedUpdater = AtomicIntegerFieldUpdater.newUpdater(SecurePooledByteBuffer.class, "freed");

        private final Pooled<ByteBuffer> allocated;
        @SuppressWarnings("unused")
        private volatile int freed;

        SecurePooledByteBuffer(final Pooled<ByteBuffer> allocated) {
            this.allocated = allocated;
        }

        public void discard() {
            if (freedUpdater.compareAndSet(this, 0, 1)) {
                zero(allocated.getResource());
                allocated.discard();
            }
        }

        public void free() {
            if (freedUpdater.compareAndSet(this, 0, 1)) {
                zero(allocated.getResource());
                allocated.free();
            }
        }

        public ByteBuffer getResource() throws IllegalStateException {
            // trust the delegate to handle illegal state since we can't do it securely by ourselves
            return allocated.getResource();
        }

        public void close() {
            free();
        }

        public String toString() {
            return "Secure wrapper around " + allocated;
        }
    }
}
