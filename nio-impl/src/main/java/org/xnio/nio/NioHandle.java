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

package org.xnio.nio;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
import static org.xnio.Bits.allAreClear;
import static org.xnio.Bits.allAreSet;
import static org.xnio.Bits.anyAreClear;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class NioHandle extends AtomicInteger {
    private final WorkerThread workerThread;
    private final SelectionKey selectionKey;

    protected NioHandle(final WorkerThread workerThread, final SelectionKey selectionKey) {
        this.workerThread = workerThread;
        this.selectionKey = selectionKey;
        set(selectionKey.interestOps());
    }

    void resume(final int ops) {
        try {
            int oldVal, newVal;
            do {
                oldVal = get();
                if (allAreSet(oldVal, ops)) {
                    return;
                }
                newVal = oldVal | ops;
            } while (! compareAndSet(oldVal, newVal));
            if (anyAreClear(selectionKey.interestOps(), ops)) {
                // there are new interest ops, so we must set them
                workerThread.setOps(selectionKey, ops);
            }
        } catch (CancelledKeyException ignored) {}
    }

    void wakeup(final int ops) {
        try {
            int oldVal, newVal;
            do {
                oldVal = get();
                if (allAreSet(oldVal, ops)) {
                    return;
                }
                newVal = oldVal | ops;
            } while (! compareAndSet(oldVal, newVal));
            if (anyAreClear(selectionKey.interestOps(), ops)) {
                // there are new interest ops, so we must set them
                workerThread.setOps(selectionKey, ops);
            }
        } catch (CancelledKeyException ignored) {}
        workerThread.queueTask(new Runnable() {
            public void run() {
                preHandleReady(ops);
            }
        });
    }

    void suspend(final int ops) {
        try {
            int oldVal, newVal;
            do {
                oldVal = get();
                if (allAreClear(oldVal, ops)) {
                    return;
                }
                newVal = oldVal & ~ops;
            } while (! compareAndSet(oldVal, newVal));
            if (allAreSet(ops, SelectionKey.OP_CONNECT)) {
                // this is an oddball that we always have to clear aggressively
                workerThread.clearOps(selectionKey, ops);
            }
        } catch (CancelledKeyException ignored) {}
    }

    boolean isResumed(final int ops) {
        try {
            return allAreSet(get(), ops);
        } catch (CancelledKeyException ignored) {
            return false;
        }
    }

    void preHandleReady(int ops) {
        assert currentThread() == workerThread;
        final int spuriousOps = ops & ~get();
        if (spuriousOps != 0) {
            workerThread.clearOps(selectionKey, spuriousOps);
        }
        ops &= ~spuriousOps;
        if (ops != 0) {
            handleReady(ops);
        }
    }

    abstract void handleReady(final int ops);

    abstract void forceTermination();

    abstract void terminated();

    WorkerThread getWorkerThread() {
        return workerThread;
    }

    SelectionKey getSelectionKey() {
        return selectionKey;
    }
}
