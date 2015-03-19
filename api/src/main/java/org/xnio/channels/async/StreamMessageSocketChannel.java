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

import java.net.SocketAddress;

import org.xnio.IoCallback;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class StreamMessageSocketChannel implements MessageSocketChannel, WrappedOptionChannel<StreamSocketChannel> {

    final MessageInputChannel inputChannel;
    final MessageOutputChannel outputChannel;
    private final StreamSocketChannel streamSocketChannel;

    public StreamMessageSocketChannel(final StreamSocketChannel streamSocketChannel) {
        this.streamSocketChannel = streamSocketChannel;
        inputChannel = MessageInputChannel.around(streamSocketChannel.getInputChannel());
        outputChannel = MessageOutputChannel.around(streamSocketChannel.getOutputChannel());
    }

    public StreamSocketChannel getChannel() {
        return streamSocketChannel;
    }

    public MessageInputChannel getInputChannel() {
        return inputChannel;
    }

    public MessageOutputChannel getOutputChannel() {
        return outputChannel;
    }

    public SocketAddress getPeerAddress() {
        return streamSocketChannel.getLocalAddress();
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
}
