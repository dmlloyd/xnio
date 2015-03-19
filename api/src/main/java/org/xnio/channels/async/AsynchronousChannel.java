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

import org.xnio.ByteBufferPool;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface AsynchronousChannel extends CloseableChannel {

    /**
     * Check the result of the last operation.  If the last operation resulted in a failure, the exception will be
     * thrown from this method.
     * <p>
     * Once this method is called, the result information is reset and subsequent calls will simply return 0 until
     * another operation is initiated.  Calling a callback-based channel operation will also reset the result even
     * if it hasn't been read.
     *
     * @return the number of bytes transferred (may be 0)
     * @throws IOException if the operation failed
     */
    long checkResult() throws IOException;

    /**
     * Cause the next call to {@link #checkResult()} to throw the given exception.  Any asynchronous operation will
     * immediately return until the exception is cleared.  Any synchronous operation will immediately fail with the
     * given exception.
     *
     * @param exception the exception
     */
    void fail(IOException exception);

    /**
     * Cause the next call to {@link #checkResult()} to return the given value.  Any operation initiated before the value
     * is read will cause the value to be discarded.
     *
     * @param result the result to set
     */
    void succeed(long result);

    /**
     * Get the worker for this channel.
     *
     * @return the worker
     */
    default XnioWorker getWorker() {
        return getIoThread().getWorker();
    }

    /**
     * Get the I/O thread associated with this channel.  This is the default thread for asynchronous callbacks on
     * this channel, though it may change over the life of a channel if the provider supports it.  If an asynchronous
     * operation can not be immediately completed, then it will typically be queued to run on this thread.
     *
     * @return the I/O thread associated with this channel
     */
    XnioIoThread getIoThread();

    /**
     * Get the recommended buffer pool set for this channel.
     *
     * @return the buffer pool set
     */
    default ByteBufferPool.Set getBufferPool() {
        return getWorker().getBufferPool();
    }

    /**
     * Get the number of times this channel has been consecutively enqueued.  Callbacks may choose to
     * {@link #yield(IoCallback, Object)} once this number gets high enough, in order to prevent starvation
     * of other queued tasks.
     *
     * @return the number of times this channel has been enqueued
     */
    int getRunCount();

    /**
     * Yield to the next enqueued channel callback.  This method may also be called to explicitly transfer control
     * back to the asynchronous channel thread.  The channel should not be accessed again until the callback is called.
     *
     * @param callback the callback to call to resume work
     */
    default void yield(IoCallback<?> callback) {
        yield(callback, null);
    }

    /**
     * Yield to the next enqueued channel callback.  This method may also be called to explicitly transfer control
     * back to the asynchronous channel thread.  The channel should not be accessed again until the callback is called.
     *
     * @param callback the callback to call to resume work
     * @param attachment the attachment to pass to the callback
     * @param <T> the attachment type
     */
    <T> void yield(IoCallback<T> callback, T attachment);

    /**
     * Call the given callback without yielding.  If the caller is a currently-called callback and/or is running on this
     * channel's I/O thread, then the given callback may be called by the same thread immediately after the current callback
     * returns. Otherwise this method behaves exactly the same as {@link #yield(IoCallback)}.  The channel should not be
     * accessed again until the callback is called.
     *
     * @param callback the callback to call to resume work
     */
    default void call(IoCallback<?> callback) {
        call(callback, null);
    }

    /**
     * Call the given callback without yielding.  If the caller is a currently-called callback and/or is running on this
     * channel's I/O thread, then the given callback may be called by the same thread immediately after the current callback
     * returns. Otherwise this method behaves exactly the same as {@link #yield(IoCallback)}.  The channel should not be
     * accessed again until the callback is called.
     *
     * @param callback the callback to call to resume work
     * @param attachment the attachment to pass to the callback
     * @param <T> the attachment type
     */
    <T> void call(IoCallback<T> callback, T attachment);
}
