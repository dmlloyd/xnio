/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2009 Red Hat, Inc. and/or its affiliates.
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

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.xnio.IoCallback;
import org.xnio.channels.Connected;

/**
 * A channel which can use SSL/TLS to negotiate a security layer.
 */
public interface SslSocketChannel extends Connected, CloseCallbackChannel {

    /**
     * Start or restart the SSL/TLS handshake.  To force a complete SSL/TLS session renegotiation, the current
     * session should be invalidated prior to calling this method.  This is a read and write operation, and thus both
     * sides of the channel have to be available.
     *
     * @param callback the callback to call when handshake completes (or has failed)
     * @param attachment the object to pass to the callback
     */
    <T> void startHandshake(IoCallback<T> callback, T attachment);

    /**
     * Start or restart the SSL/TLS handshake.  To force a complete SSL/TLS session renegotiation, the current session
     * should be invalidated prior to calling this method.  This is a read and write operation, and thus both sides of
     * the channel have to be available.
     *
     * @param callback the callback to call when handshake completes (or has failed)
     */
    default void startHandshake(IoCallback<?> callback) {
        startHandshake(callback, null);
    }

    void checkHandshakeResult() throws SSLException;

    /**
     * Determine if handshake is required for read to proceed.  If this method returns {@code true}, no more read data
     * will become available until handshake has completed, at which time a new read operation must be initiated.
     *
     * @return {@code true} if handshake is required for reading
     */
    boolean handshakeRequiredForRead();

    /**
     * Determine if handshake is required for write to proceed.  If this method returns {@code true}, no more write data
     * will be accepted until handshake has completed, at which time a new flush operation must be initiated.
     *
     * @return {@code true} if handshake is required for writing
     */
    boolean handshakeRequiredForWrite();

    /**
     * Get the current {@code SSLSession} for this channel.
     *
     * @return the current {@code SSLSession}, or {@code null} if there is no current session
     */
    SSLSession getSslSession();
}
