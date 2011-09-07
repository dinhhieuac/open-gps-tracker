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

package org.apache.ogt.http.impl.conn;

import java.io.IOException;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpMessage;
import org.apache.ogt.http.HttpResponseFactory;
import org.apache.ogt.http.NoHttpResponseException;
import org.apache.ogt.http.ProtocolException;
import org.apache.ogt.http.StatusLine;
import org.apache.ogt.http.annotation.ThreadSafe;
import org.apache.ogt.http.conn.params.ConnConnectionPNames;
import org.apache.ogt.http.impl.io.AbstractMessageParser;
import org.apache.ogt.http.io.SessionInputBuffer;
import org.apache.ogt.http.message.LineParser;
import org.apache.ogt.http.message.ParserCursor;
import org.apache.ogt.http.params.HttpParams;
import org.apache.ogt.http.util.CharArrayBuffer;

/**
 * Default HTTP response parser implementation.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link org.apache.ogt.http.params.CoreConnectionPNames#MAX_HEADER_COUNT}</li>
 *  <li>{@link org.apache.ogt.http.params.CoreConnectionPNames#MAX_LINE_LENGTH}</li>
 *  <li>{@link org.apache.ogt.http.conn.params.ConnConnectionPNames#MAX_STATUS_LINE_GARBAGE}</li>
 * </ul>
 *
 * @since 4.0
 */
@ThreadSafe // no public methods
public class DefaultResponseParser extends AbstractMessageParser {

    private final Log log = LogFactory.getLog(getClass());

    private final HttpResponseFactory responseFactory;
    private final CharArrayBuffer lineBuf;
    private final int maxGarbageLines;

    public DefaultResponseParser(
            final SessionInputBuffer buffer,
            final LineParser parser,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        super(buffer, parser, params);
        if (responseFactory == null) {
            throw new IllegalArgumentException
                ("Response factory may not be null");
        }
        this.responseFactory = responseFactory;
        this.lineBuf = new CharArrayBuffer(128);
        this.maxGarbageLines = params.getIntParameter(
            ConnConnectionPNames.MAX_STATUS_LINE_GARBAGE, Integer.MAX_VALUE);
    }


    @Override
    protected HttpMessage parseHead(
            final SessionInputBuffer sessionBuffer) throws IOException, HttpException {
        //read out the HTTP status string
        int count = 0;
        ParserCursor cursor = null;
        do {
            // clear the buffer
            this.lineBuf.clear();
            int i = sessionBuffer.readLine(this.lineBuf);
            if (i == -1 && count == 0) {
                // The server just dropped connection on us
                throw new NoHttpResponseException("The target server failed to respond");
            }
            cursor = new ParserCursor(0, this.lineBuf.length());
            if (lineParser.hasProtocolVersion(this.lineBuf, cursor)) {
                // Got one
                break;
            } else if (i == -1 || count >= this.maxGarbageLines) {
                // Giving up
                throw new ProtocolException("The server failed to respond with a " +
                        "valid HTTP response");
            }
            if (this.log.isDebugEnabled()) {
                this.log.debug("Garbage in response: " + this.lineBuf.toString());
            }
            count++;
        } while(true);
        //create the status line from the status string
        StatusLine statusline = lineParser.parseStatusLine(this.lineBuf, cursor);
        return this.responseFactory.newHttpResponse(statusline, null);
    }

}
