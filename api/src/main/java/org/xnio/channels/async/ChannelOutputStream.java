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

import static org.xnio.IoUtils.safeClose;

import java.io.IOException;

import org.xnio.channels.WrappedChannel;

/**
 * An output stream which writes to a stream sink channel.  All write operations are directly
 * performed upon the channel, so for optimal performance, a buffering output stream should be
 * used to wrap this class.
 *
 * @apiviz.exclude
 * 
 * @since 1.2
 */
public abstract class ChannelOutputStream extends AbstractOutputStream implements WrappedChannel<StreamOutputChannel> {
    private final StreamOutputChannel outputChannel;

    protected ChannelOutputStream(final StreamOutputChannel outputChannel) {
        this.outputChannel = outputChannel;
    }

    public final StreamOutputChannel getChannel() {
        return outputChannel;
    }

    public void flush() throws IOException {
        outputChannel.flushBlocking();
    }

    public void close() throws IOException {
        try {
            outputChannel.shutdownBlocking();
        } finally {
            safeClose(outputChannel);
        }
    }
}
