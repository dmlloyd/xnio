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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.io.IOException;
import java.nio.channels.MembershipKey;

/**
 * A multicast-capable channel.
 *
 * @apiviz.landmark
 */
public interface MulticastSocketChannel extends DatagramSocketChannel {

    InetSocketAddress getLocalAddress();

    /**
     * Join a multicast group to begin receiving all datagrams sent to the group.
     * 
     * A multicast channel may join several multicast groups, including the same group on more than one interface.  An
     * implementation may impose a limit on the number of groups that may be joined at the same time.
     *
     * @param group the multicast address to join
     * @param iface the network interface to join on
     * @return a new key
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if the channel is already a member of the group on this interface
     * @throws IllegalArgumentException if the {@code group} parameters is not a multicast address, or is an unsupported address type
     * @throws SecurityException if a security manager is set, and its {@link SecurityManager#checkMulticast(InetAddress)} method denies access to the group
     */
    MembershipKey join(InetAddress group, NetworkInterface iface) throws IOException;

    /**
     * Join a multicast group to begin receiving all datagrams sent to the group from a given source address.
     *
     * A multicast channel may join several multicast groups, including the same group on more than one interface.  An
     * implementation may impose a limit on the number of groups that may be joined at the same time.
     *
     * @param group the multicast address to join
     * @param iface the network interface to join on
     * @param source the source address to listen for
     * @return a new key
     * @throws IOException if an I/O error occurs
     * @throws IllegalStateException if the channel is already a member of the group on this interface
     * @throws IllegalArgumentException if the {@code group} parameters is not a multicast address, or is an unsupported address type
     * @throws SecurityException if a security manager is set, and its {@link SecurityManager#checkMulticast(InetAddress)} method denies access to the group
     * @throws UnsupportedOperationException if the implementation does not support source filtering
     */
    MembershipKey join(InetAddress group, NetworkInterface iface, InetAddress source) throws IOException;
}
