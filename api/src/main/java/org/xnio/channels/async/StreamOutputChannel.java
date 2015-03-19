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

import static org.xnio._private.Messages.msg;
import static org.xnio.channels.async.Flags.FL_FLUSH;
import static org.xnio.channels.async.Flags.FL_NONE;
import static org.xnio.channels.async.Flags.FL_SHUTDOWN;

import java.io.IOException;
import java.util.zip.Deflater;

import org.xnio.BufferSource;
import org.xnio.CompressionStrategy;
import org.xnio.CompressionType;
import org.xnio.IoCallback;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface StreamOutputChannel extends AsynchronousChannel {

    /**
     * Get the number of bytes written to this channel over all time.
     *
     * @return the number of bytes ever written to this channel
     */
    long getByteCounter();

    default <T> void shutdown(IoCallback<T> callback, T attachment) {
        flush(callback, attachment, FL_SHUTDOWN);
    }

    default void shutdown(IoCallback<?> callback) {
        shutdown(callback, null);
    }

    default void shutdownBlocking() throws IOException {
        flushBlocking(FL_SHUTDOWN);
    }

    default void shutdownBlocking(int flags) throws IOException {
        flushBlocking(flags | FL_SHUTDOWN);
    }

    default <T> void flush(IoCallback<T> callback, T attachment, int flags) {
        write(BufferSource.EMPTY, 0L, Long.MAX_VALUE, callback, attachment, flags | FL_FLUSH);
    }

    default <T> void flush(IoCallback<T> callback, T attachment) {
        flush(callback, attachment, FL_NONE);
    }

    default void flush(IoCallback<?> callback, int flags) {
        flush(callback, null, flags);
    }

    default void flush(IoCallback<?> callback) {
        flush(callback, null, FL_NONE);
    }

    default void flushBlocking(int flags) throws IOException {
        writeBlocking(BufferSource.EMPTY, 0L, Long.MAX_VALUE, flags | FL_FLUSH);
    }

    default void flushBlocking() throws IOException {
        flushBlocking(FL_NONE);
    }

    <T> void write(BufferSource bufferSource, long minLength, long maxLength, IoCallback<T> callback, T attachment, int flags);

    default <T> void write(BufferSource bufferSource, long minLength, long maxLength, IoCallback<T> callback, T attachment) {
        write(bufferSource, minLength, maxLength, callback, attachment, FL_NONE);
    }

    default <T> void write(BufferSource bufferSource, long length, IoCallback<T> callback, T attachment, int flags) {
        write(bufferSource, length, length, callback, attachment, flags);
    }

    default <T> void write(BufferSource bufferSource, long length, IoCallback<T> callback, T attachment) {
        write(bufferSource, length, length, callback, attachment, FL_NONE);
    }

    default <T> void write(BufferSource bufferSource, IoCallback<T> callback, T attachment, int flags) {
        write(bufferSource, bufferSource.bytesAvailable(), callback, attachment, flags);
    }

    default <T> void write(BufferSource bufferSource, IoCallback<T> callback, T attachment) {
        write(bufferSource, bufferSource.bytesAvailable(), callback, attachment, FL_NONE);
    }

    default void write(BufferSource bufferSource, long length, IoCallback<?> callback, int flags) {
        write(bufferSource, length, length, callback, null, flags);
    }

    default void write(BufferSource bufferSource, long length, IoCallback<?> callback) {
        write(bufferSource, length, length, callback, null, FL_NONE);
    }

    default void write(BufferSource bufferSource, IoCallback<?> callback, int flags) {
        write(bufferSource, bufferSource.bytesAvailable(), callback, null, flags);
    }

    default void write(BufferSource bufferSource, IoCallback<?> callback) {
        write(bufferSource, bufferSource.bytesAvailable(), callback, null, FL_NONE);
    }

    long writeBlocking(BufferSource bufferSource, long minLength, long maxLength, int flags) throws IOException;

    default long writeBlocking(BufferSource bufferSource, long minLength, long maxLength) throws IOException {
        return writeBlocking(bufferSource, minLength, maxLength, FL_NONE);
    }

    default long writeBlocking(BufferSource bufferSource, long length, int flags) throws IOException {
        return writeBlocking(bufferSource, length, length, flags);
    }

    default long writeBlocking(BufferSource bufferSource, long length) throws IOException {
        return writeBlocking(bufferSource, length, length, FL_NONE);
    }

    default long writeBlocking(BufferSource bufferSource, int flags) throws IOException {
        return writeBlocking(bufferSource, bufferSource.bytesAvailable(), flags);
    }

    default long writeBlocking(BufferSource bufferSource) throws IOException {
        return writeBlocking(bufferSource, bufferSource.bytesAvailable(), FL_NONE);
    }

    <T> void transferFrom(StreamInputChannel inputChannel, long minLength, long maxLength, IoCallback<T> callback, T attachment, int flags);

    default <T> void transferFrom(StreamInputChannel inputChannel, long minLength, long maxLength, IoCallback<T> callback, T attachment) {
        transferFrom(inputChannel, minLength, maxLength, callback, attachment, FL_NONE);
    }

    default <T> void transferFrom(StreamInputChannel inputChannel, long length, IoCallback<T> callback, T attachment, int flags) {
        transferFrom(inputChannel, length, length, callback, attachment, flags);
    }

    default <T> void transferFrom(StreamInputChannel inputChannel, long length, IoCallback<T> callback, T attachment) {
        transferFrom(inputChannel, length, length, callback, attachment, FL_NONE);
    }

    default void transferFrom(StreamInputChannel inputChannel, long minLength, long maxLength, IoCallback<?> callback, int flags) {
        transferFrom(inputChannel, minLength, maxLength, callback, null, flags);
    }

    default void transferFrom(StreamInputChannel inputChannel, long minLength, long maxLength, IoCallback<?> callback) {
        transferFrom(inputChannel, minLength, maxLength, callback, null, FL_NONE);
    }

    default void transferFrom(StreamInputChannel inputChannel, long length, IoCallback<?> callback, int flags) {
        transferFrom(inputChannel, length, length, callback, null, flags);
    }

    default void transferFrom(StreamInputChannel inputChannel, long length, IoCallback<?> callback) {
        transferFrom(inputChannel, length, length, callback, null, FL_NONE);
    }

    long transferFromBlocking(StreamInputChannel inputChannel, long minLength, long maxLength, int flags) throws IOException;

    default long transferFromBlocking(StreamInputChannel inputChannel, long minLength, long maxLength) throws IOException {
        return transferFromBlocking(inputChannel, minLength, maxLength, FL_NONE);
    }

    default long transferFromBlocking(StreamInputChannel inputChannel, long length, int flags) throws IOException {
        return transferFromBlocking(inputChannel, length, length, flags);
    }

    default long transferFromBlocking(StreamInputChannel inputChannel, long length) throws IOException {
        return transferFromBlocking(inputChannel, length, length, FL_NONE);
    }

    /**
     * Create a sub-channel that limits the amount of remaining output data to the given length, after which the
     * sub-channel is automatically closed and cannot be written further without error.  The parent channel may
     * continue to be written afterwards.  If the parent channel is closed, the sub-channel will also be closed.  The
     * sub-channel may be closed before the whole length is written.  A close callback may be used to track the
     * status of the sub-channel.
     *
     * @param length the sub-channel's length limit
     * @return the sub-channel
     */
    default StreamOutputChannel limitedLength(long length) {
        return new LimitedStreamOutputChannel(this, length);
    }

    default StreamOutputChannel compress(final Deflater deflater) {
        return new DeflatingStreamOutputChannel(this, deflater, maximumBufferAmount);
    }

    default StreamOutputChannel compress(final OptionMap optionMap) {
        final int level = optionMap.get(Options.COMPRESSION_LEVEL, -1);
        final boolean nowrap;
        switch (optionMap.get(Options.COMPRESSION_TYPE, CompressionType.DEFLATE)) {
            case DEFLATE: nowrap = false; break;
            case GZIP: nowrap = true; break;
            default: throw msg.badCompressionFormat();
        }
        final Deflater deflater = new Deflater(level, nowrap);
        deflater.setStrategy(optionMap.get(Options.COMPRESSION_STRATEGY, CompressionStrategy.DEFAULT).getDeflaterStrategy());
        return compress(deflater);
    }

    // stream

    ChannelOutputStream getOutputStream(int flags);
}
