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

import static java.lang.Math.min;

import java.io.IOException;

import org.wildfly.common.Assert;
import org.xnio.BufferSource;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;
import org.xnio.channels.FixedLengthOverflowException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class LimitedStreamOutputChannel extends AbstractClosableAsynchronousChannel implements StreamOutputChannel, WrappedOptionChannel<StreamOutputChannel> {

    private final StreamOutputChannel delegate;
    private final long finalByteCount;

    LimitedStreamOutputChannel(final StreamOutputChannel delegate, final long length) {
        this.delegate = delegate;
        finalByteCount = delegate.getByteCounter() + length;
    }

    public StreamOutputChannel getChannel() {
        return delegate;
    }

    public void fail(final IOException exception) {
        delegate.fail(exception);
    }

    public long getByteCounter() {
        return min(finalByteCount, delegate.getByteCounter());
    }

    public <T> void flush(final IoCallback<T> callback, final T attachment, final int flags) {
        delegate.flush(callback, attachment, flags);
    }

    public void flushBlocking(final int flags) throws IOException {
        delegate.flushBlocking(flags);
    }

    public <T> void write(final BufferSource bufferSource, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment, final int flags) {
        Assert.checkMinimumParameter("minLength", 0L, minLength);
        Assert.checkMinimumParameter("maxLength", minLength, maxLength);
        final long remaining = finalByteCount - delegate.getByteCounter();
        if (remaining < minLength) {
            fail(new FixedLengthOverflowException());
            call(callback, attachment);
        } else {
            write(bufferSource, min(remaining, minLength), min(remaining, maxLength), callback, attachment, flags);
        }
    }

    public long writeBlocking(final BufferSource bufferSource, final long minLength, final long maxLength, final int flags) throws IOException {
        Assert.checkMinimumParameter("minLength", 0L, minLength);
        Assert.checkMinimumParameter("maxLength", minLength, maxLength);
        final long remaining = finalByteCount - delegate.getByteCounter();
        if (remaining < minLength) {
            throw new FixedLengthOverflowException();
        }
        return delegate.writeBlocking(bufferSource, min(remaining, minLength), min(remaining, maxLength), flags);
    }

    public <T> void transferFrom(final StreamInputChannel inputChannel, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment, final int flags) {
        final long remaining = finalByteCount - delegate.getByteCounter();
        delegate.transferFrom(inputChannel, min(remaining, minLength), min(remaining, maxLength), callback, attachment, flags);
    }

    public long transferFromBlocking(final StreamInputChannel inputChannel, final long minLength, final long maxLength, final int flags) throws IOException {
        final long remaining = finalByteCount - delegate.getByteCounter();
        return delegate.transferFromBlocking(inputChannel, min(remaining, minLength), min(remaining, maxLength), flags);
    }

    public ChannelOutputStream getOutputStream(final int flags) {
        final ChannelOutputStream delegateOutputStream = delegate.getOutputStream(FL_NONE);
        return new ChannelOutputStream(this) {
            public void write(final int b) throws IOException {
                if (delegate.getByteCounter() >= finalByteCount) {
                    throw new FixedLengthOverflowException();
                }
                delegateOutputStream.write(b);
            }

            public void write(final byte[] b, final int off, final int len) throws IOException {
                if (delegate.getByteCounter() + len > finalByteCount) {
                    throw new FixedLengthOverflowException();
                }
                delegateOutputStream.write(b, off, len);
            }
        };
    }

    public long checkResult() throws IOException {
        return delegate.checkResult();
    }

    public XnioIoThread getIoThread() {
        return delegate.getIoThread();
    }

    public int getRunCount() {
        return delegate.getRunCount();
    }

    public <T> void yield(final IoCallback<T> callback, final T attachment) {
        delegate.yield(callback, attachment);
    }

    public <T> void call(final IoCallback<T> callback, final T attachment) {
        delegate.call(callback, attachment);
    }

    public void close() throws IOException {
        channelClosed();
    }

    protected <T> void runCallback(final IoCallback<T> callback, final T attachment) {
        delegate.call(callback, attachment);
    }
}
