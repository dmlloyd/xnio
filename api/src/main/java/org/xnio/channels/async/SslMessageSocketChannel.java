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

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.xnio.IoCallback;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SslMessageSocketChannel extends MessageSocketChannel, SslSocketChannel {

    MessageInputChannel getInputChannel();

    MessageOutputChannel getOutputChannel();

    /**
     * Create an SSL/TLS socket channel around an SSL/TLS stream socket channel.
     *
     * @param streamSocketChannel the channel
     * @return the wrapped channel
     */
    static SslMessageSocketChannel around(SslStreamSocketChannel streamSocketChannel) {
        return new SslMessageSocketChannel() {
            final MessageSocketChannel messageSocketChannel = MessageSocketChannel.around(streamSocketChannel);

            public MessageInputChannel getInputChannel() {
                return messageSocketChannel.getInputChannel();
            }

            public MessageOutputChannel getOutputChannel() {
                return messageSocketChannel.getOutputChannel();
            }

            public <T> void startHandshake(final IoCallback<T> callback, final T attachment) {
                streamSocketChannel.startHandshake(callback, attachment);
            }

            public void checkHandshakeResult() throws SSLException {
                streamSocketChannel.checkHandshakeResult();
            }

            public boolean handshakeRequiredForRead() {
                return streamSocketChannel.handshakeRequiredForRead();
            }

            public boolean handshakeRequiredForWrite() {
                return streamSocketChannel.handshakeRequiredForWrite();
            }

            public SSLSession getSslSession() {
                return streamSocketChannel.getSslSession();
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
        };
    }
}
