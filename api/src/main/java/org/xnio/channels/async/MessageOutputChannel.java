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

import java.io.IOException;
import java.nio.ByteBuffer;

import org.xnio.IoCallback;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface MessageOutputChannel extends AsynchronousChannel {

    /**
     * Enqueue a buffer into the next message.  If the channel was shut down or failed due to error, the buffer will be re-pooled.
     *
     * @param buffer the buffer to enqueue
     * @throws IllegalArgumentException if the given buffer does not match the channel buffer pool, or if the message
     *      exceeds a preconfigured message size limit
     */
    void enqueueBuffer(ByteBuffer buffer) throws IllegalArgumentException;

    /**
     * Finish the message being built.  The current message is then enqueued for sending.
     */
    void finishMessage();

    /**
     * Enqueue a channel shutdown into this channel.  No more buffers may be enqueued, and any current message is
     * discarded and the buffers re-pooled.  Already finished messages will be flushed before the channel is shut down.
     */
    void shutdown();

    /**
     * Get the number of pending unflushed bytes on this channel.  Even if this returns 0, the channel may still
     * need to be flushed.
     *
     * @return the number of pending unflushed bytes
     */
    long pending();

    /**
     * Flush some amount of data.
     *
     * @param minLength the minimum amount to flush, or 0 to flush all data enqueued at the time of this method call
     * @param callback the callback to call when the flush operation completes
     * @param attachment the attachment to pass to the callback
     * @param <T> the attachment type
     * @throws IllegalArgumentException if {@code minLength} exceeds {@link #pending()}
     */
    <T> void flush(long minLength, IoCallback<T> callback, T attachment) throws IllegalArgumentException;

    default void flush(IoCallback<?> callback) {
        flush(callback, null);
    }

    default <T> void flush(IoCallback<T> callback, T attachment) {
        flush(0L, callback, attachment);
    }

    // blocking operations

    default void flushBlocking() throws IllegalArgumentException, IOException {
        flushBlocking(0L);
    }

    void flushBlocking(long minLength) throws IllegalArgumentException, IOException;

    static MessageOutputChannel around(StreamOutputChannel streamOutputChannel) {
        return new StreamMessageOutputChannel(streamOutputChannel);
    }
}
