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

import org.xnio.QueueBufferSource;
import org.xnio.IoCallback;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface MessageInputChannel extends AsynchronousChannel {

    /**
     * Get the number of bytes consumed by this channel over all time.  This includes buffered bytes which have not
     * yet been dequeued.
     *
     * @return the number of bytes ever consumed by this channel
     */
    long getByteCounter();

    /**
     * Initiate a read operation, calling the given callback once a certain number of bytes have been read.  Use
     * {@link #checkResult()} to acquire the thrown exception or result byte count.
     * <p>
     * The channel is not thread-safe, and thus should not be accessed until the callback has been called.
     *
     * @param callback the callback to call when the read operation is complete (or an error occurs)
     * @param maxLength the maximum message length to read before truncating the message
     * @param attachment the attachment to pass to the callback
     * @param <T> the attachment type
     */
    <T> void read(QueueBufferSource bufferQueue, int maxLength, IoCallback<T> callback, T attachment);

    default void read(QueueBufferSource bufferQueue, int maxLength, IoCallback<?> callback) {
        read(bufferQueue, maxLength, callback, null);
    }

    default <T> void read(QueueBufferSource bufferQueue, IoCallback<T> callback, T attachment) {
        read(bufferQueue, Integer.MAX_VALUE, callback, attachment);
    }

    default void read(QueueBufferSource bufferQueue, IoCallback<?> callback) {
        read(bufferQueue, Integer.MAX_VALUE, callback, null);
    }

    static MessageInputChannel around(StreamInputChannel streamInputChannel) {
        return new StreamMessageInputChannel(streamInputChannel);
    }
}
