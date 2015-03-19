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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.zip.Inflater;

import org.xnio.BufferSource;
import org.xnio.CompressionType;
import org.xnio.IoCallback;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.channels.Channels;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface StreamInputChannel extends AsynchronousChannel, BufferSource {

    /**
     * Get the number of bytes read by this channel over all time.
     *
     * @return the number of bytes ever read by this channel
     */
    long getByteCounter();

    default void transferTo(FileChannel channel, long minLength, long maxLength, IoCallback<?> callback) {
        transferTo(channel, minLength, maxLength, callback, null);
    }

    <T> void transferTo(FileChannel channel, long minLength, long maxLength, IoCallback<T> callback, T attachment);

    default long transferToBlocking(FileChannel channel, long minLength, long maxLength) throws IOException {
        ByteBuffer buffer;
        int pos;
        int lim;
        long t = 0L;
        while (t < minLength) {
            if (bytesAvailable() > 0) {
                buffer = get();
                lim = buffer.limit();
                pos = buffer.remaining();
                if (lim - pos > maxLength - t) {
                    buffer.limit((int) (maxLength - t));
                    try {
                        t += channel.write(buffer);
                    } finally {
                        buffer.limit(lim);
                    }
                } else {
                    t += channel.write(get());
                }
            }
            if (readBlocking(1L, maxLength - t) == -1) {
                return t;
            }
        }
        return t;
    }

    default void skip(long length, IoCallback<?> callback) {
        skip(length, callback, null);
    }

    default <T> void skip(long length, IoCallback<T> callback, T attachment) {
        transferTo(Channels.getNullFileChannel(), length, length, callback);
    }

    default long skipBlocking(long length) throws IOException {
        return transferToBlocking(Channels.getNullFileChannel(), length, length);
    }

    <T> void read(long minLength, long maxLength, IoCallback<T> callback, T attachment);

    default void read(long minLength, long maxLength, IoCallback<?> callback) {
        read(minLength, maxLength, callback, null);
    }

    default <T> void read(long minLength, IoCallback<T> callback, T attachment) {
        read(minLength, Long.MAX_VALUE, callback, attachment);
    }

    default void read(long minLength, IoCallback<?> callback) {
        read(minLength, callback, null);
    }

    default <T> void read(IoCallback<T> callback, T attachment) {
        read(0L, callback, attachment);
    }

    default void read(IoCallback<?> callback) {
        read(0L, callback, null);
    }

    long readBlocking(long minLength, long maxLength) throws IOException;

    default long readBlocking(long minLength) throws IOException {
        return readBlocking(minLength, Long.MAX_VALUE);
    }

    default long readBlocking() throws IOException {
        return readBlocking(0, Long.MAX_VALUE);
    }

    /**
     * Create a sub-channel that limits the amount of remaining input data to the given length, after which the
     * sub-channel will close.  The parent channel may continue to be read.  If the end of stream is reached before
     * the sub-channel's limit is reached, both the parent and sub-channel will close.
     *
     * @param length the sub-channel's length limit
     * @return the sub-channel
     */
    default StreamInputChannel limitedLength(long length) {
        return new LimitedStreamInputChannel(this, length);
    }

    default StreamInputChannel uncompress(final Inflater inflater) {
        return new InflatingStreamInputChannel(this, inflater);
    }

    default StreamInputChannel uncompress(final OptionMap optionMap) {
        final boolean nowrap;
        switch (optionMap.get(Options.COMPRESSION_TYPE, CompressionType.DEFLATE)) {
            case DEFLATE: nowrap = false; break;
            case GZIP: nowrap = true; break;
            default: throw msg.badCompressionFormat();
        }
        return uncompress(new Inflater(nowrap));
    }

    /**
     * Get an input stream for this channel.
     *
     * @return the input stream
     */
    ChannelInputStream getInputStream();
}
