/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.ogt.http.impl.auth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


import org.apache.ogt.http.HeaderElement;
import org.apache.ogt.http.annotation.NotThreadSafe;
import org.apache.ogt.http.auth.MalformedChallengeException;
import org.apache.ogt.http.message.BasicHeaderValueParser;
import org.apache.ogt.http.message.HeaderValueParser;
import org.apache.ogt.http.message.ParserCursor;
import org.apache.ogt.http.util.CharArrayBuffer;

/**
 * Abstract authentication scheme class that lays foundation for all
 * RFC 2617 compliant authentication schemes and provides capabilities common
 * to all authentication schemes defined in RFC 2617.
 *
 * @since 4.0
 */
@NotThreadSafe // AuthSchemeBase, params
public abstract class RFC2617Scheme extends AuthSchemeBase {

    /**
     * Authentication parameter map.
     */
    private Map<String, String> params;

    /**
     * Default constructor for RFC2617 compliant authentication schemes.
     */
    public RFC2617Scheme() {
        super();
    }

    @Override
    protected void parseChallenge(
            final CharArrayBuffer buffer, int pos, int len) throws MalformedChallengeException {
        HeaderValueParser parser = BasicHeaderValueParser.DEFAULT;
        ParserCursor cursor = new ParserCursor(pos, buffer.length());
        HeaderElement[] elements = parser.parseElements(buffer, cursor);
        if (elements.length == 0) {
            throw new MalformedChallengeException("Authentication challenge is empty");
        }

        this.params = new HashMap<String, String>(elements.length);
        for (HeaderElement element : elements) {
            this.params.put(element.getName(), element.getValue());
        }
    }

    /**
     * Returns authentication parameters map. Keys in the map are lower-cased.
     *
     * @return the map of authentication parameters
     */
    protected Map<String, String> getParameters() {
        if (this.params == null) {
            this.params = new HashMap<String, String>();
        }
        return this.params;
    }

    /**
     * Returns authentication parameter with the given name, if available.
     *
     * @param name The name of the parameter to be returned
     *
     * @return the parameter with the given name
     */
    public String getParameter(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null");
        }
        if (this.params == null) {
            return null;
        }
        return this.params.get(name.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Returns authentication realm. The realm may not be null.
     *
     * @return the authentication realm
     */
    public String getRealm() {
        return getParameter("realm");
    }

}
