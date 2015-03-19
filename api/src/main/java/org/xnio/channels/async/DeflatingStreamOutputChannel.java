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
import static org.xnio.Bits.allAreSet;
import static org.xnio.Bits.anyAreSet;
import static org.xnio.channels.async.Flags.FL_CLOSE_PROPAGATE;
import static org.xnio.channels.async.Flags.FL_FLUSH;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.zip.Deflater;

import org.xnio.BufferSource;
import org.xnio.Buffers;
import org.xnio.ByteBufferPool;
import org.xnio.QueueBufferSource;
import org.xnio.IoCallback;
import org.xnio.XnioIoThread;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class DeflatingStreamOutputChannel implements StreamOutputChannel, WrappedOptionChannel<StreamOutputChannel> {

    static final long MAX_POSSIBLE_BUFFER_AMOUNT = ((long) Integer.MAX_VALUE) * ByteBufferPool.NORMAL_SIZE;
    private final StreamOutputChannel delegate;
    private final Deflater deflater;
    private final long maximumBufferAmount;
    private final QueueBufferSource bufferQueue;
    private boolean closed;
    private ByteBuffer destBuffer;
    private ByteBuffer srcBuffer;
    private long byteCounter;
    private long minLength;
    private long maxLength;
    private BufferSource bufferSource;
    private IoCallback<?> callback;
    private Object attachment;
    private int flags;
    private IOException ex;

    public void fail(final IOException exception) {
        delegate.fail(exception);
    }

    public void succeed(final long result) {
        delegate.succeed(result);
    }

    DeflatingStreamOutputChannel(final StreamOutputChannel delegate, final Deflater deflater, final long maximumBufferAmount) {
        this.delegate = delegate;
        this.maximumBufferAmount = maximumBufferAmount;
        delegate.addCloseCallback(Deflater::end, deflater);
        delegate.addCloseCallback(DeflatingStreamOutputChannel::closed, this);
        this.deflater = deflater;
        bufferQueue = new QueueBufferSource((int) (min(MAX_POSSIBLE_BUFFER_AMOUNT, maximumBufferAmount) / ByteBufferPool.NORMAL_SIZE));
    }

    void closed() {
        closed = true;
    }

    public StreamOutputChannel getChannel() {
        return delegate;
    }

    public long getByteCounter() {
        return deflater.getBytesRead();
    }

    private long fillFrom(final BufferSource bufferSource, final long minLength, final long maxLength, int flags) {
        // steps:
        // do
        //   buffer -> input
        //   finish (if FL_SHUTDOWN)
        //   input -> deflater
        // until
        // deflate -> output
        // output -> write (w/flush if FL_FLUSH)








        ByteBuffer buffer, copyBuffer;
        boolean ok;
        while (bufferQueue.bytesAvailable() < maximumBufferAmount) {
            if (deflater.needsInput()) {
                buffer = bufferSource.get();
                if (! buffer.hasArray()) {
                    copyBuffer = ByteBufferPool.NORMAL_HEAP.allocate();
                    ok = false;
                    try {
                        Buffers.copy(copyBuffer, buffer);
                        copyBuffer.flip();
                        inputBuffer = copyBuffer;
                        ok = true;
                    } finally {
                        if (! ok) ByteBufferPool.free(copyBuffer);
                    }
                } else {
                    inputBuffer = buffer = bufferSource.take();
                }
                deflater.setInput(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());

                deflater.finish();
            }
        }




    }

    public <T> void write(final BufferSource bufferSource, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment, final int flags) {
        if (closed) {
            ex = new ClosedChannelException();
            delegate.call(callback, attachment);
            return;
        }
        final QueueBufferSource bufferQueue = this.bufferQueue;
        final Deflater deflater = this.deflater;
        final long maximumBufferAmount = this.maximumBufferAmount;
        // deflate bytes into buffer
        long scnt = deflater.getBytesRead();
        for (;;) {
            if (bufferQueue.bytesAvailable() >= maximumBufferAmount) {
                this.bufferSource = bufferSource;
                this.minLength = minLength;
                this.maxLength = maxLength;
                this.callback = callback;
                this.attachment = attachment;
                this.flags = flags;
                delegate.write(bufferQueue, ByteBufferPool.NORMAL_SIZE, Long.MAX_VALUE, DeflatingStreamOutputChannel::retryWrite, this, flags);
                return;
            }
            ByteBuffer destBuffer = this.destBuffer;
            if (destBuffer == null) {
                destBuffer = this.destBuffer = ByteBufferPool.NORMAL_HEAP.allocate();
            }
            // convert part or all of the first buffer to a heap buffer
            bufferSource.prefillHeap((int) min(maxLength, bufferSource.get().remaining()));
            ByteBuffer srcBuffer = bufferSource.get();
            assert srcBuffer.hasArray();
            assert destBuffer.hasArray();
            // set/reset the input
            deflater.setInput(srcBuffer.array(), srcBuffer.arrayOffset() + srcBuffer.position(), srcBuffer.remaining());
            // do the deflate operation
            boolean doFlush = maxLength >= srcBuffer.remaining() && maxLength >= bufferSource.bytesAvailable() && allAreSet(flags, FL_FLUSH);
            final int cnt = deflater.deflate(destBuffer.array(), destBuffer.arrayOffset() + destBuffer.position(), destBuffer.remaining(), doFlush ? Deflater.SYNC_FLUSH : Deflater.NO_FLUSH);
            Buffers.skip(srcBuffer, (int) (-scnt + (scnt = deflater.getBytesRead())));
            Buffers.skip(destBuffer, cnt);
            if (! destBuffer.hasRemaining()) {
                destBuffer.flip();
                bufferQueue.add(destBuffer);
                this.destBuffer = null;
            }
            assert deflater.needsInput() == ! srcBuffer.hasRemaining();
            if (deflater.needsInput()) {
                ByteBufferPool.free(srcBuffer);
                this.srcBuffer = null;
            }
        }
    }

    private void retryWrite() {
        retryWriteGeneric(callback, attachment);
    }

    @SuppressWarnings("unchecked")
    private <T> void retryWriteGeneric(IoCallback<T> callback, Object attachment) {
        write(bufferSource, minLength, maxLength, callback, (T) attachment, flags);
    }

    public long writeBlocking(final BufferSource bufferSource, final long minLength, final long maxLength, final int flags) throws IOException {
        return 0;
    }

    void writeBlocking(final byte[] bytes, final int offs, final int len) throws IOException {

    }

    void writeBlocking(final int b) throws IOException {
        flushBlocking();
    }

    public <T> void transferFrom(final StreamInputChannel inputChannel, final long minLength, final long maxLength, final IoCallback<T> callback, final T attachment, final int flags) {

    }

    public long transferFromBlocking(final StreamInputChannel inputChannel, final long minLength, final long maxLength, final int flags) throws IOException {
        return 0;
    }

    public ChannelOutputStream getOutputStream(final int flags) {
        return new ChannelOutputStream(this) {
            public void write(final byte[] b, final int off, final int len) throws IOException {
                writeBlocking(b, off, len);
            }

            public void write(final int b) throws IOException {
                writeBlocking(b);
            }

            public void close() throws IOException {
                flush();
                if (allAreSet(flags, FL_CLOSE_PROPAGATE)) {
                    DeflatingStreamOutputChannel.this.close();
                }
            }
        };
    }

    public long checkResult() throws IOException {
        return 0;
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
