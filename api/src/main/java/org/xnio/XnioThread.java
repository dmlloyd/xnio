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

import static org.xnio._private.Messages.msg;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class XnioThread extends Thread {

    protected final XnioWorker worker;

    public XnioThread(final Runnable target, final XnioWorker worker) {
        super(target);
        this.worker = worker;
    }

    public XnioThread(final String name, final Runnable target, final XnioWorker worker) {
        super(target, name);
        this.worker = worker;
    }

    public XnioThread(final ThreadGroup group, final Runnable target, final String name, final XnioWorker worker) {
        super(group, target, name);
        this.worker = worker;
    }

    public XnioThread(final ThreadGroup group, final Runnable target, final String name, final long stackSize, final XnioWorker worker) {
        super(group, target, name, stackSize);
        this.worker = worker;
    }

    public final void run() {
        try {
            super.run();
        } finally {
            // clear all thread locals
            ThreadUtil.clearThreadLocals();
        }
    }

    /**
     * Get the XNIO worker associated with this thread.
     *
     * @return the XNIO worker
     */
    public XnioWorker getWorker() {
        return worker;
    }

    /**
     * Get the current XNIO thread.  If the current thread is not an XNIO thread, {@code null} is returned.
     *
     * @return the current XNIO thread
     */
    public static XnioThread currentThread() {
        final Thread thread = Thread.currentThread();
        if (thread instanceof XnioThread) {
            return (XnioThread) thread;
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
    public static XnioThread requireCurrentThread() throws IllegalStateException {
        final XnioThread thread = currentThread();
        if (thread == null) {
            throw msg.xnioThreadRequired();
        }
        return thread;
    }

}
