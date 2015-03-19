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

package org.xnio;

import static org.xnio._private.Messages.listenerMsg;

import java.util.EventListener;

/**
 * A completion callback for an asynchronous I/O operation.  The callback may generally be called from any thread; see
 * the documentation for each asynchronous operation for more information.
 *
 * @param <T> the callback attachment type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface IoCallback<T> extends EventListener {

    /**
     * Indicate that the operation has completed, or that no further progress is possible.
     *
     * @param param a parameter which was passed in to the asynchronous operation method
     */
    void ready(T param);

    /**
     * Safely call a callback with an attachment, logging any exception that is thrown.
     *
     * @param callback the callback to call
     * @param attachment the callback attachment to pass
     * @param <T> the attachment type
     */
    static <T> void safeCall(IoCallback<T> callback, T attachment) {
        try {
            callback.ready(attachment);
        } catch (Throwable t) {
            listenerMsg.listenerException(t);
        }
    }
}
