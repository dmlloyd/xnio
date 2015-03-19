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
import org.xnio.XnioIoThread;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class StreamMessageOutputChannel implements WrappedOptionChannel<StreamOutputChannel>, MessageOutputChannel {

    private final StreamOutputChannel streamOutputChannel;

    StreamMessageOutputChannel(final StreamOutputChannel streamOutputChannel) {
        this.streamOutputChannel = streamOutputChannel;
    }

    public StreamOutputChannel getChannel() {
        return streamOutputChannel;
    }

    public void enqueueBuffer(final ByteBuffer buffer) throws IllegalArgumentException {

    }

    public void finishMessage() {

    }

    public void shutdown() {

    }

    public long pending() {
        return 0;
    }

    public <T> void flush(final long minLength, final IoCallback<T> callback, final T attachment) throws IllegalArgumentException {

    }

    public void flushBlocking(final long minLength) throws IllegalArgumentException, IOException {

    }

    public long checkResult() throws IOException {
        return 0;
    }

    public XnioIoThread getIoThread() {
        return streamOutputChannel.getIoThread();
    }

    public int getRunCount() {
        return streamOutputChannel.getRunCount();
    }

    public <T> void yield(final IoCallback<T> callback, final T attachment) {
        streamOutputChannel.yield(callback, attachment);
    }

    public <T> void call(final IoCallback<T> callback, final T attachment) {
        streamOutputChannel.call(callback, attachment);
    }

    public void close() throws IOException {
        streamOutputChannel.close();
    }

    public boolean isOpen() {
        return streamOutputChannel.isOpen();
    }

    public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {
        streamOutputChannel.addCloseCallback(callback, attachment);
    }
}
