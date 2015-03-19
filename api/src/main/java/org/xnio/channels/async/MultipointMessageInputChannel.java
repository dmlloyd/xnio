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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface MultipointMessageInputChannel extends MessageInputChannel {
    /**
     * Get the local address that the last message was received on, or {@code null} if the address is unknown.
     *
     * @return the message local address
     */
    SocketAddress getMessageLocalAddress();

    /**
     * Get the local address that the last message was received on of a given type, or {@code null} if the address is
     * not of that type or the address is unknown.
     *
     * @param type the address type class
     * @param <A> the address type
     * @return the local address, or {@code null} if unknown
     */
    default <A extends SocketAddress> A getMessageLocalAddress(Class<A> type) {
        SocketAddress localAddress = getMessageLocalAddress();
        return type.isInstance(localAddress) ? type.cast(localAddress) : null;
    }
    /**
     * Get the peer address that the last message was received on, or {@code null} if the address is unknown.
     *
     * @return the message peer address
     */
    SocketAddress getMessagePeerAddress();

    /**
     * Get the peer address that the last message was received on of a given type, or {@code null} if the address is
     * not of that type or the address is unknown.
     *
     * @param type the address type class
     * @param <A> the address type
     * @return the peer address, or {@code null} if unknown
     */
    default <A extends SocketAddress> A getMessagePeerAddress(Class<A> type) {
        SocketAddress peerAddress = getMessagePeerAddress();
        return type.isInstance(peerAddress) ? type.cast(peerAddress) : null;
    }
}
