/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.nio;

import org.apache.http.nio.reactor.IOEventDispatch;

/**
 * Extended version of the {@link NHttpServerConnection} used by
 * {@link IOEventDispatch} implementations to inform server-side connection
 * objects of I/O events.
 *
 * @since 4.0
 */
public interface NHttpServerIOTarget extends NHttpServerConnection {

    /**
     * Triggered when the connection is ready to consume input.
     *
     * @param handler the server protocol handler.
     */
    void consumeInput(NHttpServiceHandler handler);

    /**
     * Triggered when the connection is ready to produce output.
     *
     * @param handler the server protocol handler.
     */
    void produceOutput(NHttpServiceHandler handler);

}
