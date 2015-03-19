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

import java.util.EnumSet;

/**
 * Possible file access modes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum FileAccess {
    READ_ONLY(true, false),
    READ_WRITE(true, true),
    WRITE_ONLY(false, true),
    ;
    private final boolean read;
    private final boolean write;

    private FileAccess(final boolean read, final boolean write) {
        this.read = read;
        this.write = write;
    }

    boolean isRead() {
        return read;
    }

    boolean isWrite() {
        return write;
    }

    private static final int fullSize = values().length;

    /**
     * Determine whether the given set is fully populated (or "full"), meaning it contains all possible values.
     *
     * @param set the set
     *
     * @return {@code true} if the set is full, {@code false} otherwise
     */
    public static boolean isFull(final EnumSet<FileAccess> set) {
        return set != null && set.size() == fullSize;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     *
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final FileAccess v1) {
        return this == v1;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @param v2 the second instance
     *
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final FileAccess v1, final FileAccess v2) {
        return this == v1 || this == v2;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     * @param v2 the second instance
     * @param v3 the third instance
     *
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final FileAccess v1, final FileAccess v2, final FileAccess v3) {
        return this == v1 || this == v2 || this == v3;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param values the possible values
     *
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final FileAccess... values) {
        if (values != null) for (FileAccess value : values) {
            if (this == value) return true;
        }
        return false;
    }
}
