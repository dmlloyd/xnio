/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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

package org.xnio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.wildfly.common.Assert;
import org.xnio.channels.BoundChannel;
import org.xnio.channels.StreamChannel;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.channels.async.MessageSocketChannel;
import org.xnio.channels.async.StreamInputChannel;
import org.xnio.channels.async.StreamOutputChannel;
import org.xnio.channels.async.StreamSocketChannel;
import org.xnio.channels.async.StreamDuplexChannel;

import static org.xnio._private.Messages.msg;

/**
 * An XNIO thread.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("unused")
public abstract class XnioIoThread extends XnioThread implements XnioExecutor, XnioIoFactory {

    private final int number;
    private final Kind kind;

    /**
     * Construct a new instance.
     *
     * @param target the main thread task
     * @param worker the XNIO worker to associate with
     * @param kind the kind of I/O thread
     * @param number the thread number
     */
    protected XnioIoThread(final Runnable target, final XnioWorker worker, final Kind kind, final int number) {
        super(target, worker);
        this.number = number;
        this.kind = kind;
    }

    /**
     * Construct a new instance.
     *
     * @param target the main thread task
     * @param worker the XNIO worker to associate with
     * @param kind the kind of I/O thread
     * @param number the thread number
     * @param name the thread name
     */
    protected XnioIoThread(final Runnable target, final XnioWorker worker, final Kind kind, final int number, final String name) {
        super(name, target, worker);
        this.kind = kind;
        this.number = number;
    }

    /**
     * Construct a new instance.
     *
     * @param target the main thread task
     * @param worker the XNIO worker to associate with
     * @param kind the kind of I/O thread
     * @param number the thread number
     * @param group the thread group
     * @param name the thread name
     */
    protected XnioIoThread(final Runnable target, final XnioWorker worker, final Kind kind, final int number, final ThreadGroup group, final String name) {
        super(group, target, name, worker);
        this.number = number;
        this.kind = kind;
    }

    /**
     * Construct a new instance.
     *
     * @param target the main thread task
     * @param worker the XNIO worker to associate with
     * @param kind the kind of I/O thread
     * @param number the thread number
     * @param group the thread group
     * @param name the thread name
     * @param stackSize the thread stack size
     */
    protected XnioIoThread(final Runnable target, final XnioWorker worker, final Kind kind, final int number, final ThreadGroup group, final String name, final long stackSize) {
        super(group, target, name, stackSize, worker);
        this.kind = kind;
        this.number = number;
    }

    /**
     * Get the current XNIO thread.  If the current thread is not an XNIO thread, {@code null} is returned.
     *
     * @return the current XNIO thread
     */
    public static XnioIoThread currentThread() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof XnioIoThread) {
            return (XnioIoThread) thread;
        } else {
            return null;
        }
    }

    /**
     * Get the current XNIO thread.  If the current thread is not an XNIO thread, an {@link IllegalStateException} is
     * thrown.
     *
     * @return the current XNIO thread
     * @throws IllegalStateException if the current thread is not an XNIO thread
     */
    public static XnioIoThread requireCurrentThread() throws IllegalStateException {
        final XnioIoThread thread = currentThread();
        if (thread == null) {
            throw msg.xnioIoThreadRequired();
        }
        return thread;
    }

    /**
     * Get the number of this thread.  In each XNIO worker, every IO thread is given a unique, sequential number.
     *
     * @return the number of this thread
     */
    public int getNumber() {
        return number;
    }

    //============================
    // Stream connection: outbound
    //============================

    public IoFuture<StreamSocketChannel> openAsynchronousStreamConnection(final SocketAddress bindAddress, final SocketAddress destination, final OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        Assert.checkNotNullParam("optionMap", optionMap);
        if (bindAddress != null && bindAddress.getClass() != destination.getClass()) {
            throw msg.mismatchSockType(bindAddress.getClass(), destination.getClass());
        }
        if (destination instanceof InetSocketAddress) {
            return openTcpAsynchronousStreamConnection((InetSocketAddress) bindAddress, (InetSocketAddress) destination);
        } else if (destination instanceof LocalSocketAddress) {
            return openLocalAsynchronousStreamConnection((LocalSocketAddress) bindAddress, (LocalSocketAddress) destination);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to connect to a TCP server.
     *
     * @param bindAddress the bind address
     * @param destinationAddress the destination address
     * @return the future result of this operation
     */
    protected IoFuture<StreamSocketChannel> openTcpAsynchronousStreamConnection(InetSocketAddress bindAddress, InetSocketAddress destinationAddress) {
        throw Assert.unsupported();
    }

    /**
     * Implementation helper method to connect to a local (UNIX domain) server.
     *
     * @param bindAddress the bind address
     * @param destinationAddress the destination address
     * @return the future result of this operation
     */
    protected IoFuture<StreamSocketChannel> openLocalAsynchronousStreamConnection(LocalSocketAddress bindAddress, LocalSocketAddress destinationAddress) {
        throw Assert.unsupported();
    }

    @Deprecated
    public IoFuture<StreamConnection> openStreamConnection(SocketAddress destination, ChannelListener<? super StreamConnection> openListener, OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (destination instanceof InetSocketAddress) {
            return openTcpStreamConnection(Xnio.ANY_INET_ADDRESS, (InetSocketAddress) destination, openListener, null, optionMap);
        } else if (destination instanceof LocalSocketAddress) {
            return openLocalStreamConnection(Xnio.ANY_LOCAL_ADDRESS, (LocalSocketAddress) destination, openListener, null, optionMap);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    @Deprecated
    public IoFuture<StreamConnection> openStreamConnection(SocketAddress destination, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (destination instanceof InetSocketAddress) {
            return openTcpStreamConnection(Xnio.ANY_INET_ADDRESS, (InetSocketAddress) destination, openListener, bindListener, optionMap);
        } else if (destination instanceof LocalSocketAddress) {
            return openLocalStreamConnection(Xnio.ANY_LOCAL_ADDRESS, (LocalSocketAddress) destination, openListener, bindListener, optionMap);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    @Deprecated
    public IoFuture<StreamConnection> openStreamConnection(SocketAddress bindAddress, SocketAddress destination, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (bindAddress != null && bindAddress.getClass() != destination.getClass()) {
            throw msg.mismatchSockType(bindAddress.getClass(), destination.getClass());
        }
        if (destination instanceof InetSocketAddress) {
            return openTcpStreamConnection((InetSocketAddress) bindAddress, (InetSocketAddress) destination, openListener, bindListener, optionMap);
        } else if (destination instanceof LocalSocketAddress) {
            return openLocalStreamConnection((LocalSocketAddress) bindAddress, (LocalSocketAddress) destination, openListener, bindListener, optionMap);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to connect to a TCP server.
     *
     * @param bindAddress the bind address
     * @param destinationAddress the destination address
     * @param openListener the listener which will be notified when the channel is open, or {@code null} for none
     * @param bindListener the listener which will be notified when the channel is bound, or {@code null} for none
     * @param optionMap the option map    @return the future result of this operation
     * @return the future result of this operation
     */
    @Deprecated
    protected IoFuture<StreamConnection> openTcpStreamConnection(InetSocketAddress bindAddress, InetSocketAddress destinationAddress, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    /**
     * Implementation helper method to connect to a local (UNIX domain) server.
     *
     * @param bindAddress the bind address
     * @param destinationAddress the destination address
     * @param openListener the listener which will be notified when the channel is open, or {@code null} for none
     * @param bindListener the listener which will be notified when the channel is bound, or {@code null} for none
     * @param optionMap the option map
     * @return the future result of this operation
     */
    @Deprecated
    protected IoFuture<StreamConnection> openLocalStreamConnection(LocalSocketAddress bindAddress, LocalSocketAddress destinationAddress, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    /**
     * Implementation helper method to connect to a local (UNIX domain) server.
     *
     * @param bindAddress the bind address
     * @param destinationAddress the destination address
     * @param openListener the listener which will be notified when the channel is open, or {@code null} for none
     * @param optionMap the option map
     * @return the future result of this operation
     */
    @Deprecated
    protected IoFuture<MessageConnection> openLocalMessageConnection(LocalSocketAddress bindAddress, LocalSocketAddress destinationAddress, ChannelListener<? super MessageConnection> openListener, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    //===========================
    // Stream connection: inbound
    //===========================

    public IoFuture<StreamSocketChannel> acceptAsynchronousStreamConnection(SocketAddress destination, OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (destination instanceof InetSocketAddress) {
            return acceptTcpAsynchronousStreamConnection((InetSocketAddress) destination, optionMap);
        } else if (destination instanceof LocalSocketAddress) {
            return acceptLocalAsynchronousStreamConnection((LocalSocketAddress) destination, optionMap);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to accept a TCP connection.
     *
     * @param destination the destination (bind) address
     *
     * @return the future connection
     */
    protected IoFuture<StreamSocketChannel> acceptTcpAsynchronousStreamConnection(InetSocketAddress destination, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    /**
     * Implementation helper method to accept a local (UNIX domain) stream connection.
     *
     * @param destination the destination (bind) address
     *
     * @return the future connection
     */
    protected IoFuture<StreamSocketChannel> acceptLocalAsynchronousStreamConnection(LocalSocketAddress destination, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    @Deprecated
    public IoFuture<StreamConnection> acceptStreamConnection(SocketAddress destination, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (destination instanceof InetSocketAddress) {
            return acceptTcpStreamConnection((InetSocketAddress) destination, openListener, bindListener, optionMap);
        } else if (destination instanceof LocalSocketAddress) {
            return acceptLocalStreamConnection((LocalSocketAddress) destination, openListener, bindListener, optionMap);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to accept a local (UNIX domain) stream connection.
     *
     * @param destination the destination (bind) address
     *
     * @return the future connection
     */
    @Deprecated
    protected IoFuture<StreamConnection> acceptLocalStreamConnection(LocalSocketAddress destination, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    /**
     * Implementation helper method to accept a TCP connection.
     *
     * @param destination the destination (bind) address
     *
     * @return the future connection
     */
    @Deprecated
    protected IoFuture<StreamConnection> acceptTcpStreamConnection(InetSocketAddress destination, ChannelListener<? super StreamConnection> openListener, ChannelListener<? super BoundChannel> bindListener, OptionMap optionMap) {
        throw Assert.unsupported();
    }

    //=============================
    // Message connection: outbound
    //=============================

    @Deprecated
    public IoFuture<MessageConnection> openMessageConnection(final SocketAddress destination, final ChannelListener<? super MessageConnection> openListener, final OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (destination instanceof LocalSocketAddress) {
            return openLocalMessageConnection(null, (LocalSocketAddress) destination);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to connect to a local (UNIX domain) server.
     *
     * @param bindAddress the bind address, or {@code null} to connect without binding
     * @param destination the destination address
     * @return the configurable builder to produce the connected channel
     */
    @Deprecated
    protected IoFuture<MessageConnection> openLocalMessageConnection(final LocalSocketAddress bindAddress, final LocalSocketAddress destination) {
        throw Assert.unsupported();
    }

    public IoFuture<MessageSocketChannel> openAsynchronousMessageConnection(final SocketAddress bindAddress, final SocketAddress destination, final OptionMap optionMap) {
        Assert.checkNotNullParam("destination", destination);
        if (destination instanceof LocalSocketAddress) {
            return openLocalAsynchronousMessageConnection(null, (LocalSocketAddress) destination);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to connect to a local (UNIX domain) server.
     *
     * @param bindAddress the bind address, or {@code null} to connect without binding
     * @param destination the destination address
     * @return the future connected channel
     */
    protected IoFuture<MessageSocketChannel> openLocalAsynchronousMessageConnection(LocalSocketAddress bindAddress, LocalSocketAddress destination) {
        throw Assert.unsupported();
    }

    //============================
    // Message connection: inbound
    //============================

    public IoFuture<MessageSocketChannel> acceptAsynchronousMessageConnection(final SocketAddress destination, final OptionMap optionMap) {
        if (destination instanceof LocalSocketAddress) {
            return acceptLocalAsynchronousMessageConnection((LocalSocketAddress) destination);
        } else {
            throw msg.badSockType(destination.getClass());
        }
    }

    /**
     * Implementation helper method to accept a local (UNIX domain) datagram connection.
     *
     * @param destination the destination (bind) address
     *
     * @return the future connection
     */
    protected IoFuture<MessageSocketChannel> acceptLocalAsynchronousMessageConnection(LocalSocketAddress destination) {
        throw Assert.unsupported();
    }

    @Deprecated
    public IoFuture<MessageConnection> acceptMessageConnection(final SocketAddress destination, final ChannelListener<? super MessageConnection> openListener, final ChannelListener<? super BoundChannel> bindListener, final OptionMap optionMap) {
        throw Assert.unsupported();
    }

    //==================
    // Pipe: full-duplex
    //==================

    public ChannelPipe<StreamDuplexChannel, StreamDuplexChannel> createFullDuplexAsynchronousPipe() throws IOException {
        throw Assert.unsupported();
    }

    public ChannelPipe<StreamDuplexChannel, StreamDuplexChannel> createFullDuplexAsynchronousPipe(final XnioIoFactory peer) throws IOException {
        throw Assert.unsupported();
    }

    @Deprecated
    public ChannelPipe<StreamChannel, StreamChannel> createFullDuplexPipe() throws IOException {
        throw Assert.unsupported();
    }

    @Deprecated
    public ChannelPipe<StreamConnection, StreamConnection> createFullDuplexPipeConnection() throws IOException {
        throw Assert.unsupported();
    }

    @Deprecated
    public ChannelPipe<StreamConnection, StreamConnection> createFullDuplexPipeConnection(final XnioIoFactory peer) throws IOException {
        throw Assert.unsupported();
    }

    //==================
    // Pipe: half-duplex
    //==================

    public ChannelPipe<StreamInputChannel, StreamOutputChannel> createHalfDuplexAsynchronousPipe() throws IOException {
        throw Assert.unsupported();
    }

    public ChannelPipe<StreamInputChannel, StreamOutputChannel> createHalfDuplexAsynchronousPipe(final XnioIoFactory peer) throws IOException {
        throw Assert.unsupported();
    }

    @Deprecated
    public ChannelPipe<StreamSourceChannel, StreamSinkChannel> createHalfDuplexPipe() throws IOException {
        throw Assert.unsupported();
    }

    @Deprecated
    public ChannelPipe<StreamSourceChannel, StreamSinkChannel> createHalfDuplexPipe(final XnioIoFactory peer) throws IOException {
        throw Assert.unsupported();
    }

    // ============
    // Other
    // ============

    public enum Kind {
        SIMPLEX_READ,
        SIMPLEX_WRITE,
        DUPLEX,
    }
}
