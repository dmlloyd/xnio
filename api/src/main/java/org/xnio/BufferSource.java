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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.util.function.Supplier;

/**
 * A supplier of populated {@link ByteBuffer} instances which are {@linkplain ByteBuffer#flip() flipped} for emptying.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface BufferSource extends Supplier<ByteBuffer> {

    /**
     * Get the next byte buffer.  Return {@code null} if there are no available buffers.  The same buffer will be
     * returned until the contents of that buffer have been emptied; all leading empty buffers are automatically
     * freed when this method is called.
     *
     * @return the next non-empty byte buffer, or {@code null} if there are no more buffers available
     */
    ByteBuffer get();

    /**
     * Take the next byte buffer off of this source.  Return {@code null} if there are a no available buffers.  After
     * this call, the caller owns the taken buffer and is responsible for freeing it.
     *
     * @return the next non-empty byte buffer, or {@code null} if there are no more buffers available
     */
    ByteBuffer take();

    /**
     * Get the number of bytes currently available from this supplier.
     *
     * @return the number of bytes available from this supplier
     */
    long bytesAvailable();

    /**
     * Pre-fill the current buffer so that the number of available bytes is equal to or greater than {@code size}.  The
     * given {@code size} minus the remaining byte count of the current buffer may not exceed {@link #bytesAvailable()}.
     * The position of the buffer or the buffer instance itself may change if it has to be compacted to satisfy the size.
     * If the buffer cannot accommodate the size, an exception is thrown.  All implementations are required to support
     * up to 64 bytes or the current buffer's current capacity, whichever is larger.
     *
     * @param size the amount of data required in the current buffer
     * @throws IllegalArgumentException if {@code size} is less than zero or there is not enough data available to
     *      prefill the first buffer
     * @throws BufferOverflowException if the first buffer is not large enough to support {@code size}
     */
    void prefill(int size) throws IllegalArgumentException, BufferOverflowException;

    void prefillDirect(int size) throws IllegalArgumentException, BufferOverflowException;

    void prefillHeap(int size) throws IllegalArgumentException, BufferOverflowException;

    /**
     * Attempt to write all the data in this buffer source to the given channel.  This method may or may not block,
     * depending on the target channel's behavior.
     *
     * @param channel the channel to write to
     * @return the number of bytes written (possibly 0)
     * @throws IOException if an error occurred
     */
    default long writeTo(GatheringByteChannel channel) throws IOException {
        return writeTo(channel, Long.MAX_VALUE);
    }

    /**
     * Attempt to write some of the data in this buffer source to the given channel.  This method may or may not block,
     * depending on the target channel's behavior.
     *
     * @param channel the channel to write to
     * @param maxLength the maximum number of bytes to write
     * @return the number of bytes written (possibly 0)
     * @throws IOException if an error occurred
     */
    long writeTo(GatheringByteChannel channel, long maxLength) throws IOException;

    /**
     * Construct an exception indicating that there is not enough data to prefill the first buffer.
     *
     * @param expected the expected amount of data
     * @param available the actual amount of available data
     * @return the exception
     */
    static IllegalArgumentException notEnoughData(int expected, int available) {
        // TODO: i18n
        return new IllegalArgumentException("Not enough data to prefill the first buffer");
    }

    /**
     * An empty buffer source.
     */
    BufferSource EMPTY = new BufferSource() {
        public ByteBuffer get() {
            return null;
        }

        public ByteBuffer take() {
            return null;
        }

        public long bytesAvailable() {
            return 0;
        }

        public void prefill(final int size) throws IllegalArgumentException, BufferOverflowException {
            throw notEnoughData(size, 0);
        }

        public void prefillDirect(final int size) throws IllegalArgumentException, BufferOverflowException {
            throw notEnoughData(size, 0);
        }

        public void prefillHeap(final int size) throws IllegalArgumentException, BufferOverflowException {
            throw notEnoughData(size, 0);
        }

        public long writeTo(final GatheringByteChannel channel, final long maxLength) throws IOException {
            return 0L;
        }
    };
}
