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
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.xnio.QueueBufferSource;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
class LimitedStreamInputChannel implements StreamInputChannel {

    private final StreamInputChannel streamInputChannel;
    private final long length;

    LimitedStreamInputChannel(final StreamInputChannel streamInputChannel, final long length) {
        this.streamInputChannel = streamInputChannel;
        this.length = length;
    }

    long remaining() {
        return length - getByteCounter();
    }

    public long getByteCounter() {
        return streamInputChannel.getByteCounter();
    }

    public <T> void transferTo(final FileChannel channel, final long length, final IoCallback<T> callback, final T attachment) {
        streamInputChannel.transferTo(channel, min(length, remaining()), callback, attachment); // todo: special callback which checks for close condition
    }

    public long transferToBlocking(final FileChannel channel, final long length) throws IOException {
        return streamInputChannel.transferToBlocking(channel, min(length, remaining()));
        // todo check for close
    }

    public long skipBlocking(final long length) throws IOException {
        return streamInputChannel.skipBlocking(min(length, remaining()));
        // todo check for close
    }

    public <T> void read(final QueueBufferSource bufferQueue, final int minLength, final int maxLength, final IoCallback<T> callback, final T attachment) {
        streamInputChannel.read(bufferQueue, (int) min(minLength, remaining()), (int) min(maxLength, remaining()), callback, attachment);
    }

    public void readBlocking(final QueueBufferSource bufferQueue, final int minLength, final int maxLength) throws IOException {
        streamInputChannel.readBlocking(bufferQueue, (int) min(minLength, remaining()), (int) min(maxLength, remaining()));
    }

    public ChannelInputStream getInputStream() {
        final ChannelInputStream delegate = streamInputChannel.getInputStream();
        return new ChannelInputStream(this) {
            public int read() throws IOException {
                return 0;
            }

            public int read(final byte[] b, final int off, final int len) throws IOException {
                return 0;
            }

            public long skip(final long n) throws IOException {
                return 0;
            }

            public int available() throws IOException {
                return 0;
            }

            public long transferTo(final OutputStream output, final long count) throws IOException {
                return 0;
            }
        };
    }

    public long checkResult() throws IOException {
        return streamInputChannel.checkResult();
    }

    public XnioIoThread getIoThread() {
        return streamInputChannel.getIoThread();
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

    public boolean isOpen() {
        return streamInputChannel.isOpen() && remaining() > 0L;
    }

    public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {

    }
}
