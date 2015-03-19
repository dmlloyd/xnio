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
import java.net.SocketAddress;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.xnio.BufferSource;
import org.xnio.QueueBufferSource;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class SslStreamSocketChannelImpl implements SslStreamSocketChannel, WrappedOptionChannel<StreamSocketChannel> {

    private final SSLEngine engine;
    private final StreamSocketChannel streamSocketChannel;
    private final AtomicBoolean started = new AtomicBoolean();
    private final Input inputChannel = new Input();
    private final Output outputChannel = new Output();
    private SSLException handshakeResult;

    SslStreamSocketChannelImpl(final SSLEngine engine, final StreamSocketChannel streamSocketChannel) {
        this.engine = engine;
        this.streamSocketChannel = streamSocketChannel;
    }

    public <T> void startHandshake(final IoCallback<T> callback, final T attachment) {
        try {
            engine.beginHandshake();
        } catch (SSLException e) {
            handshakeResult = e;
        }
    }

    public void checkHandshakeResult() throws SSLException {
        SSLException handshakeResult = this.handshakeResult;
        if (handshakeResult != null) {
            this.handshakeResult = null;
            throw handshakeResult;
        }
    }

    public boolean handshakeRequiredForRead() {
        return inputChannel.handshakeNeeded;
    }

    public boolean handshakeRequiredForWrite() {
        return outputChannel.handshakeNeeded;
    }

    public SSLSession getSslSession() {
        return engine.getSession();
    }

    public SocketAddress getPeerAddress() {
        return streamSocketChannel.getPeerAddress();
    }

    public SocketAddress getLocalAddress() {
        return streamSocketChannel.getLocalAddress();
    }

    public boolean isOpen() {
        return streamSocketChannel.isOpen();
    }

    public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {
        streamSocketChannel.addCloseCallback(callback, attachment);
    }

    public StreamInputChannel getInputChannel() {
        return inputChannel;
    }

    public StreamOutputChannel getOutputChannel() {
        return outputChannel;
    }

    public StreamSocketChannel getChannel() {
        return streamSocketChannel;
    }

    class Input implements StreamInputChannel, WrappedOptionChannel<StreamInputChannel> {
        // queue for decrypted incoming data
        final QueueBufferSource readQueue = new QueueBufferSource(4);
        boolean handshakeNeeded;
        long result;
        IOException e;
        boolean closed;

        long byteCounter = streamSocketChannel.getInputChannel().getByteCounter();

        public StreamInputChannel getChannel() {
            return streamSocketChannel.getInputChannel();
        }

        public long getByteCounter() {
            return byteCounter;
        }

        public <T> void transferTo(final FileChannel channel, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment) {
            if (bytesAvailable() >= minLength) {
                try {
                    this.result = writeTo(channel, maxLength);
                } catch (IOException e) {
                    this.e = e;
                }
                call(callback, attachment);
                return;
            }

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
            if (e != null) {
                try {
                    throw e;
                } finally {
                    e = null;
                }
            }
            try {
                return result;
            } finally {
                result = 0L;
            }
        }

        public XnioIoThread getIoThread() {
            return streamSocketChannel.getInputChannel().getIoThread();
        }

        public int getRunCount() {
            return streamSocketChannel.getInputChannel().getRunCount();
        }

        public <T> void yield(final IoCallback<T> callback, final T attachment) {
            streamSocketChannel.getInputChannel().yield(callback, attachment);
        }

        public <T> void call(final IoCallback<T> callback, final T attachment) {
            streamSocketChannel.getInputChannel().call(callback, attachment);
        }

        public ByteBuffer get() {
            return readQueue.get();
        }

        public ByteBuffer take() {
            return readQueue.take();
        }

        public long bytesAvailable() {
            return readQueue.bytesAvailable();
        }

        public void prefill(final int size) throws IllegalArgumentException, BufferOverflowException {
            readQueue.prefill(size);
        }

        public long writeTo(final GatheringByteChannel channel, final long maxLength) throws IOException {
            return readQueue.writeTo(channel, maxLength);
        }

        public void close() throws IOException {
            streamSocketChannel.getInputChannel().close();
        }

        public boolean isOpen() {
            return streamSocketChannel.getInputChannel().isOpen();
        }

        public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {

        }
    }

    class Output implements StreamOutputChannel, WrappedOptionChannel<StreamOutputChannel> {
        boolean handshakeNeeded;

        public StreamOutputChannel getChannel() {
            return streamSocketChannel.getOutputChannel();
        }

        public long getByteCounter() {
            return 0;
        }

        public <T> void write(final BufferSource bufferSource, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment, final int flags) {

        }

        public long writeBlocking(final BufferSource bufferSource, final long minLength, final long maxLength, final int flags) throws IOException {
            return 0;
        }

        public <T> void transferFrom(final StreamInputChannel inputChannel, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment, final int flags) {

        }

        public long transferFromBlocking(final StreamInputChannel inputChannel, final long minLength, final long maxLength, final int flags) throws IOException {
            return 0;
        }

        public void fail(final IOException exception) {

        }

        public void succeed(final long result) {

        }

        public ChannelOutputStream getOutputStream(final int flags) {
            return null;
        }

        public long checkResult() throws IOException {
            return 0;
        }

        public XnioIoThread getIoThread() {
            return null;
        }

        public int getRunCount() {
            return 0;
        }

        public <T> void yield(final IoCallback<T> callback, final T attachment) {

        }

        public <T> void call(final IoCallback<T> callback, final T attachment) {

        }

        public void close() throws IOException {

        }

        public boolean isOpen() {
            return false;
        }

        public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {

        }
    }
}
