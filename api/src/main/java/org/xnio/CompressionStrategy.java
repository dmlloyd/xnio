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

import java.util.EnumSet;
import java.util.zip.Deflater;

/**
 * Supported compression types.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum CompressionStrategy {
    /**
     * Use the default compression strategy.
     */
    DEFAULT(Deflater.DEFAULT_STRATEGY),
    /**
     * Use huffman coding only.
     */
    HUFFMAN_ONLY(Deflater.HUFFMAN_ONLY),
    /**
     * Use the filtered strategy, best for many small values with random distribution.
     */
    FILTERED(Deflater.FILTERED),
    ;

    private static final int fullSize = values().length;

    private final int strategy;

    CompressionStrategy(final int strategy) {
        this.strategy = strategy;
    }

    /**
     * Get an integer strategy value usable by {@link Deflater}.
     *
     * @return the strategy value
     */
    public int getDeflaterStrategy() {
        return strategy;
    }

    /**
     * Determine whether the given set is fully populated (or "full"), meaning it contains all possible values.
     *
     * @param set the set
     *
     * @return {@code true} if the set is full, {@code false} otherwise
     */
    public static boolean isFull(final EnumSet<CompressionStrategy> set) {
        return set != null && set.size() == fullSize;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param v1 the first instance
     *
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final CompressionStrategy v1) {
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
    public boolean in(final CompressionStrategy v1, final CompressionStrategy v2) {
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
    public boolean in(final CompressionStrategy v1, final CompressionStrategy v2, final CompressionStrategy v3) {
        return this == v1 || this == v2 || this == v3;
    }

    /**
     * Determine whether this instance is equal to one of the given instances.
     *
     * @param values the possible values
     *
     * @return {@code true} if one of the instances matches this one, {@code false} otherwise
     */
    public boolean in(final CompressionStrategy... values) {
        if (values != null) for (CompressionStrategy value : values) {
            if (this == value) return true;
        }
        return false;
    }
}
