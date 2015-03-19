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

import org.xnio.Option;

/**
 * A channel supporting options.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface OptionChannel {

    default <T> T getOption(Option<T> option) {
        return null;
    }

    default <T> T setOption(Option<T> option, T newValue) {
        return null;
    }

    default boolean hasGettableOption(Option<?> option) {
        return false;
    }

    default boolean hasSettableOption(Option<?> option) {
        return false;
    }
}
