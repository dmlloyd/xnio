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

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class OptionSet extends AbstractSet<Option<?>> {
    private final Option<?>[] specifiedOptions;
    private final Option<?>[] sortedOptions;

    OptionSet(final Option<?>[] specifiedOptions) {
        this.specifiedOptions = specifiedOptions;
        final Option<?>[] sortedOptions = specifiedOptions.clone();
        Arrays.sort(sortedOptions, Option.OPTION_COMPARATOR);
        this.sortedOptions = sortedOptions;
    }

    public int size() {
        return sortedOptions.length;
    }

    public boolean containsAll(final Collection<?> c) {
        if (c instanceof OptionSet) {
            OptionSet other = (OptionSet) c;
            final int mySize = size();
            final int otherSize = other.size();
            if (otherSize > mySize) {
                return false;
            } else if (otherSize == mySize) {
                return Arrays.equals(sortedOptions, other.sortedOptions);
            } else {
                return super.containsAll(c);
            }
        } else {
            return super.containsAll(c);
        }
    }

    public boolean contains(final Object o) {
        return o instanceof Option && contains((Option<?>) o);
    }

    boolean contains(final Option<?> o) {
        return Arrays.binarySearch(sortedOptions, o, Option.OPTION_COMPARATOR) >= 0;
    }

    public Iterator<Option<?>> iterator() {
        return new Iterator<Option<?>>() {
            int next = 0;

            public boolean hasNext() {
                return next < specifiedOptions.length;
            }

            public Option<?> next() {
                final int next = this.next;
                if (next == specifiedOptions.length) {
                    throw new NoSuchElementException();
                }
                this.next = next + 1;
                return specifiedOptions[next];
            }
        };
    }

    public Option<?>[] toArray() {
        return specifiedOptions.clone();
    }

    public <T> T[] toArray(T[] a) {
        final Option<?>[] specifiedOptions = this.specifiedOptions;
        final int length = specifiedOptions.length;
        if (a.length >= length) {
            a = Arrays.copyOf(a, length);
        }
        System.arraycopy(specifiedOptions, 0, a, 0, length);
        return a;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder();
        b.append("Set of options: ");
        final int length = specifiedOptions.length;
        for (int i = 0; i < length; i++) {
            b.append(specifiedOptions[i].getName());
            if (i < length - 1) {
                b.append(", ");
            }
        }
        return b.toString();
    }
}
