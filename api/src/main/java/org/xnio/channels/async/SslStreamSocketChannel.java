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

import javax.net.ssl.SSLEngine;

import org.xnio.IoCallback;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SslStreamSocketChannel extends StreamSocketChannel, SslSocketChannel {
    StreamInputChannel getInputChannel();

    StreamOutputChannel getOutputChannel();

    /**
     * Create an SSL/TLS socket channel around a plain stream socket channel, with the given engine.  SSL/TLS negotiation
     * will not begin until the first call to {@link #startHandshake(IoCallback, Object)}.
     *
     * @param streamSocketChannel the channel
     * @param engine the SSL/TLS engine
     * @return the wrapped channel
     */
    static SslStreamSocketChannel around(StreamSocketChannel streamSocketChannel, SSLEngine engine) {
        return new SslStreamSocketChannelImpl(engine, streamSocketChannel);
    }
}
