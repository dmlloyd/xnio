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

import java.io.IOException;

/**
 * A handler for the result of an operation.  May be used to populate an {@link IoFuture}.
 */
public interface Result<T> {

    /**
     * Set the result for this operation.  Any threads blocking on this instance will be unblocked.
     *
     * @param result the result to set
     *
     * @return {@code false} if the operation was already completed, {@code true} otherwise
     */
    boolean setResult(T result);

    /**
     * Set the exception for this operation.  Any threads blocking on this instance will be unblocked.
     *
     * @param exception the exception to set
     *
     * @return {@code false} if the operation was already completed, {@code true} otherwise
     */
    boolean setException(IOException exception);

    /**
     * Acknowledge the cancellation of this operation.
     *
     * @return {@code false} if the operation was already completed, {@code true} otherwise
     */
    boolean setCancelled();
}
