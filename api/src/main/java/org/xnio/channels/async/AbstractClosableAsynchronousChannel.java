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

import java.util.Arrays;

import org.xnio.IoCallback;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class AbstractClosableAsynchronousChannel implements CloseCallbackChannel {
    private int idx;
    private Object[] closeCallbacks;
    private boolean closed;

    public boolean isOpen() {
        return ! closed;
    }

    public <T> void addCloseCallback(final IoCallback<T> callback, final T attachment) {
        if (closed) {
            runCallback(callback, attachment);
            return;
        }
        Object[] closeCallbacks = this.closeCallbacks;
        final int length = closeCallbacks.length;
        final int idx = this.idx;
        if (length == idx) {
            if (length >= 0x2000_0000) {
                throw new IllegalStateException();
            }
            this.closeCallbacks = Arrays.copyOf(closeCallbacks, (length + length + length) >> 1);
            addCloseCallback(callback, attachment);
            return;
        }
        closeCallbacks[idx] = callback;
        closeCallbacks[idx + 1] = attachment;
        this.idx = idx + 2;
    }

    protected void channelClosed() {
        if (! closed) {
            closed = true;
            final Object[] closeCallbacks = this.closeCallbacks;
            final int idx = this.idx;
            this.closeCallbacks = null;
            this.idx = -1;
            for (int i = 0; i < idx; i += 2) {
                castRunCallback((IoCallback<?>)closeCallbacks[i], closeCallbacks[i + 1]);
            }
        }
    }

    private <T> void castRunCallback(IoCallback<T> callback, Object attachment) {
        runCallback(callback, (T) attachment);
    }

    protected abstract <T> void runCallback(IoCallback<T> callback, T attachment);
}
