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

package org.apache.http.contrib.sip;


import org.apache.http.HttpException;


/**
 * Signals that an SIP exception has occurred.
 * This is for protocol errors specific to SIP,
 * as opposed to protocol errors shared with HTTP.
 *
 *
 */
public class SipException extends HttpException {

    private static final long serialVersionUID = 337592534308025800L;

    /**
     * Creates a new SipException with a <tt>null</tt> detail message.
     */
    public SipException() {
        super();
    }

    /**
     * Creates a new SipException with the specified detail message.
     *
     * @param message the exception detail message
     */
    public SipException(final String message) {
        super(message);
    }

    /**
     * Creates a new SipException with the specified detail message and cause.
     *
     * @param message the exception detail message
     * @param cause   the <tt>Throwable</tt> that caused this exception, or
     *                <tt>null</tt> if the cause is unavailable, unknown, or
     *                not a <tt>Throwable</tt>
     */
    public SipException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
