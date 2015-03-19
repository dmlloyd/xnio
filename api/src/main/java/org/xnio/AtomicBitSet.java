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

import static org.xnio.Bits.allAreClear;
import static org.xnio.Bits.allAreSet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class AtomicBitSet extends AtomicInteger {

    private static final long serialVersionUID = 8262383994879226738L;

    public final boolean compareAndToggleBit(int bit, boolean expect) {
        final int b = 1 << bit;
        int oldVal;
        do {
            oldVal = get();
            if (allAreSet(oldVal, b) != expect) {
                return false;
            }
        } while (! compareAndSet(oldVal, oldVal ^ b));
        return true;
    }

    public final boolean isBitSet(int bit) {
        return (get() & 1 << bit) != 0;
    }

    public final void assignBit(int bit, boolean value) {
        final int b = 1 << bit;
        assignBits(b, value);
    }

    public final void assignBits(int bitMask, boolean value) {
        int oldVal;
        do {
            oldVal = get();
            if (value ? allAreSet(oldVal, bitMask) : allAreClear(oldVal, bitMask)) {
                return;
            }
        } while (! compareAndSet(oldVal, value ? oldVal | bitMask : oldVal & ~bitMask));
    }

    public final void setBit(int bit) {
        assignBit(bit, true);
    }

    public final void clearBit(int bit) {
        assignBit(bit, false);
    }

    public final void setBits(int bitMask) {
        int oldVal;
        do {
            oldVal = get();
            if (allAreSet(oldVal, bitMask)) {
                return;
            }
        } while (! compareAndSet(oldVal, oldVal | bitMask));
    }

    public final void clearBits(int bitMask) {
        int oldVal;
        do {
            oldVal = get();
            if (allAreClear(oldVal, bitMask)) {
                return;
            }
        } while (! compareAndSet(oldVal, oldVal & ~bitMask));
    }
}
