/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2014 Red Hat, Inc. and/or its affiliates.
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ThreadUtil {
    private static final Field threadLocalField;
    private static final Field inheritableThreadLocalField;
    private static final Field tableField;

    static {
        Field[] data = AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
            public Field[] run() {
                Field threadLocals = null;
                try {
                    threadLocals = Thread.class.getDeclaredField("threadLocals");
                    threadLocals.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // fall out
                }
                Field inheritableThreadLocals = null;
                try {
                    inheritableThreadLocals = Thread.class.getDeclaredField("inheritableThreadLocals");
                    inheritableThreadLocals.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // fall out
                }
                Field tableField = null;
                if (threadLocals != null) try {
                    tableField = threadLocals.getType().getDeclaredField("table");
                    tableField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    // fall out
                }
                if (tableField != null && WeakReference[].class.isAssignableFrom(tableField.getType())) {
                    return new Field[] { threadLocals, inheritableThreadLocals, tableField };
                } else {
                    return new Field[] { threadLocals, inheritableThreadLocals, null };
                }
            }
        });
        threadLocalField = data[0];
        inheritableThreadLocalField = data[1];
        tableField = data[2];
    }

    static void clearThreadLocals() {
        final Thread currentThread = Thread.currentThread();
        doClear(currentThread, threadLocalField);
        doClear(currentThread, inheritableThreadLocalField);
    }

    private static void doClear(final Thread currentThread, final Field field) {
        if (field != null) {
            Object threadLocals = null;
            try {
                threadLocals = field.get(currentThread);
            } catch (IllegalAccessException e) {
                // fall out
            }
            if (threadLocals != null) {
                WeakReference<?>[] table = null;
                try {
                    table = (WeakReference<?>[]) tableField.get(threadLocals);
                } catch (IllegalAccessException e) {
                    // fall out
                }
                if (table != null) {
                    for (WeakReference<?> reference : table.clone()) {
                        if (reference != null) {
                            final ThreadLocal<?> threadLocal = (ThreadLocal<?>) reference.get();
                            if (threadLocal != null) {
                                threadLocal.remove();
                            }
                        }
                    }
                }
            }
            try {
                field.set(currentThread, null);
            } catch (IllegalAccessException e) {
                // fall out
            }
        }
    }
}
