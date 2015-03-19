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
import org.xnio.channels.WrappedChannel;

/**
 * A {@linkplain WrappedChannel wrapped} {@link OptionChannel}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface WrappedOptionChannel<C extends OptionChannel> extends WrappedChannel<C>, OptionChannel {

    default <T> T getOption(Option<T> option) {
        return getChannel().getOption(option);
    }

    default <T> T setOption(Option<T> option, T newValue) {
        return getChannel().setOption(option, newValue);
    }

    default boolean hasGettableOption(Option<?> option) {
        return getChannel().hasGettableOption(option);
    }

    default boolean hasSettableOption(Option<?> option) {
        return getChannel().hasSettableOption(option);
    }
}
