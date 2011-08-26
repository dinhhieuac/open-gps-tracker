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

package org.apache.ogt.http.localserver;

import org.apache.commons.codec.BinaryDecoder;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpRequest;
import org.apache.ogt.http.ProtocolException;
import org.apache.ogt.http.auth.AUTH;
import org.apache.ogt.http.util.EncodingUtils;

public class BasicAuthTokenExtractor {

    public String extract(final HttpRequest request) throws HttpException {
        String auth = null;

        Header h = request.getFirstHeader(AUTH.WWW_AUTH_RESP);
        if (h != null) {
            String s = h.getValue();
            if (s != null) {
                auth = s.trim();
            }
        }

        if (auth != null) {
            int i = auth.indexOf(' ');
            if (i == -1) {
                throw new ProtocolException("Invalid Authorization header: " + auth);
            }
            String authscheme = auth.substring(0, i);
            if (authscheme.equalsIgnoreCase("basic")) {
                String s = auth.substring(i + 1).trim();
                try {
                    byte[] credsRaw = EncodingUtils.getAsciiBytes(s);
                    BinaryDecoder codec = new Base64();
                    auth = EncodingUtils.getAsciiString(codec.decode(credsRaw));
                } catch (DecoderException ex) {
                    throw new ProtocolException("Malformed BASIC credentials");
                }
            }
        }
        return auth;
    }

}
