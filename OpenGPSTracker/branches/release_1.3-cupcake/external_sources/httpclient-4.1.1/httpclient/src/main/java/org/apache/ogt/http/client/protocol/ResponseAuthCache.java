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

package org.apache.ogt.http.client.protocol;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.HttpHost;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.HttpResponseInterceptor;
import org.apache.ogt.http.annotation.Immutable;
import org.apache.ogt.http.auth.AuthScheme;
import org.apache.ogt.http.auth.AuthState;
import org.apache.ogt.http.client.AuthCache;
import org.apache.ogt.http.client.params.AuthPolicy;
import org.apache.ogt.http.impl.client.BasicAuthCache;
import org.apache.ogt.http.protocol.ExecutionContext;
import org.apache.ogt.http.protocol.HttpContext;

/**
 * Response interceptor that adds successfully completed {@link AuthScheme}s
 * to the local {@link AuthCache} instance. Cached {@link AuthScheme}s can be
 * re-used when executing requests against known hosts, thus avoiding
 * additional authentication round-trips.
 *
 * @since 4.1
 */
@Immutable
public class ResponseAuthCache implements HttpResponseInterceptor {

    private final Log log = LogFactory.getLog(getClass());

    public ResponseAuthCache() {
        super();
    }

    public void process(final HttpResponse response, final HttpContext context)
            throws HttpException, IOException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("HTTP context may not be null");
        }
        AuthCache authCache = (AuthCache) context.getAttribute(ClientContext.AUTH_CACHE);

        HttpHost target = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
        AuthState targetState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);
        if (target != null && targetState != null) {
            if (isCachable(targetState)) {
                if (authCache == null) {
                    authCache = new BasicAuthCache();
                    context.setAttribute(ClientContext.AUTH_CACHE, authCache);
                }
                cache(authCache, target, targetState);
            }
        }

        HttpHost proxy = (HttpHost) context.getAttribute(ExecutionContext.HTTP_PROXY_HOST);
        AuthState proxyState = (AuthState) context.getAttribute(ClientContext.PROXY_AUTH_STATE);
        if (proxy != null && proxyState != null) {
            if (isCachable(proxyState)) {
                if (authCache == null) {
                    authCache = new BasicAuthCache();
                    context.setAttribute(ClientContext.AUTH_CACHE, authCache);
                }
                cache(authCache, proxy, proxyState);
            }
        }
    }

    private boolean isCachable(final AuthState authState) {
        AuthScheme authScheme = authState.getAuthScheme();
        if (authScheme == null || !authScheme.isComplete()) {
            return false;
        }
        String schemeName = authScheme.getSchemeName();
        return schemeName.equalsIgnoreCase(AuthPolicy.BASIC) ||
                schemeName.equalsIgnoreCase(AuthPolicy.DIGEST);
    }

    private void cache(final AuthCache authCache, final HttpHost host, final AuthState authState) {
        AuthScheme authScheme = authState.getAuthScheme();
        if (authState.getAuthScope() != null) {
            if (authState.getCredentials() != null) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Caching '" + authScheme.getSchemeName() +
                            "' auth scheme for " + host);
                }
                authCache.put(host, authScheme);
            } else {
                authCache.remove(host);
            }
        }
    }

}
