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

import org.xnio.IoCallback;

/**
 * An asynchronous connection server.  The acceptor callback is called whenever a new connection is accepted.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface AsynchronousServerChannel<C extends ConnectedSocketChannel> extends CloseableChannel {

    /**
     * Pause acceptance.  The server will be paused sometime after this method is called.  If accepting is already
     * paused then this method has no effect.
     */
    void pause();

    /**
     * Resume acceptance.  The server will be resumed sometime after this method is called.  If accepting is already
     * resumed then this method has no effect.
     */
    void resume();

    /**
     * Set the acceptor callback.
     *
     * @param callback the acceptor callback
     */
    void setAcceptor(IoCallback<? super C> callback);
}
