
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

package org.xnio.channels;

import java.net.SocketAddress;

import org.xnio.channels.Bound;

/**
 * A channel that has a local and peer endpoint address.
 */
public interface Connected extends Bound {
    /**
     * Get the peer address of this channel.
     *
     * @return the peer address
     */
    SocketAddress getPeerAddress();

    /**
     * Get the peer address of a given type, or {@code null} if the address is not of that
     * type.
     *
     * @param type the address type class
     * @return the peer address, or {@code null} if unknown
     */
    default <A extends SocketAddress> A getPeerAddress(Class<A> type) {
        SocketAddress localAddress = getPeerAddress();
        return type.isInstance(localAddress) ? type.cast(localAddress) : null;
    }
}
