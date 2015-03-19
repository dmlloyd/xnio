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
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.zip.Inflater;

import org.xnio.QueueBufferSource;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class InflatingStreamInputChannel implements StreamInputChannel, WrappedOptionChannel<StreamInputChannel> {

    private final StreamInputChannel delegate;
    private final Inflater inflater;
    private final QueueBufferSource queue;
    private boolean closed;

    InflatingStreamInputChannel(final StreamInputChannel delegate, final Inflater inflater) {
        this.delegate = delegate;
        this.inflater = inflater;
        delegate.addCloseCallback(Inflater::end, inflater);
        delegate.addCloseCallback(InflatingStreamInputChannel::closed, this);
    }

    private void closed() {
        closed = true;
    }

    public StreamInputChannel getChannel() {
        return delegate;
    }

    public ByteBuffer get() {
        return queue.get();
    }

    public ByteBuffer take() {
        return queue.take();
    }

    public long bytesAvailable() {
        return queue.bytesAvailable();
    }

    public void prefill(final int size) throws IllegalArgumentException, BufferOverflowException {
        queue.prefill(size);
    }

    public long getByteCounter() {
        return inflater.getBytesRead();
    }

    public <T> void transferTo(final FileChannel channel, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment) {

    }

    public <T> void read(final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment) {

    }

    public long readBlocking(final long minLength, final long maxLength) throws IOException {
        return 0;
    }

    public ChannelInputStream getInputStream() {
        return null;
    }

    public long checkResult() throws IOException {
        return 0;
    }

    public long writeTo(final GatheringByteChannel channel, final long maxLength) throws IOException {
        return queue.writeTo(channel, maxLength);
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
        delegate.close();
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

    public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {
        delegate.addCloseCallback(callback, attachment);
    }
}
