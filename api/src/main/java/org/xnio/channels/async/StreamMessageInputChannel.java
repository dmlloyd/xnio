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
import org.xnio.QueueBufferSource;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class StreamMessageInputChannel implements MessageInputChannel, WrappedOptionChannel<StreamInputChannel> {

    final QueueBufferSource bufferQueue;
    private final StreamInputChannel streamInputChannel;

    public StreamMessageInputChannel(final StreamInputChannel streamInputChannel) {
        this.streamInputChannel = streamInputChannel;
        bufferQueue = new QueueBufferSource(8);
    }

    public StreamInputChannel getChannel() {
        return streamInputChannel;
    }

    public long getByteCounter() {
        return streamInputChannel.getByteCounter();
    }

    public <T> void read(final QueueBufferSource bufferQueue, final int maxLength, final IoCallback<T> callback, final T attachment) {

    }

    public long checkResult() throws IOException {
        streamInputChannel.checkResult();
        return 0;
    }

    public XnioIoThread getIoThread() {
        return streamInputChannel.getIoThread();
    }

    public ByteBufferPool.Set getBufferPool() {
        return streamInputChannel.getBufferPool();
    }

    public int getRunCount() {
        return streamInputChannel.getRunCount();
    }

    public <T> void yield(final IoCallback<T> callback, final T attachment) {
        streamInputChannel.yield(callback, attachment);
    }

    public <T> void call(final IoCallback<T> callback, final T attachment) {
        streamInputChannel.call(callback, attachment);
    }

    public void close() throws IOException {
        streamInputChannel.close();
    }
}
