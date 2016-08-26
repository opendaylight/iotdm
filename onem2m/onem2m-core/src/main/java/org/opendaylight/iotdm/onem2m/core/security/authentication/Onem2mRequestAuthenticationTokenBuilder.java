/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.security.authentication;

/**
 * Builder class for AuthenticationTokens
 */
public class Onem2mRequestAuthenticationTokenBuilder {
    private final Onem2mRequestAuthenticationToken onem2mAuthToken = new Onem2mRequestAuthenticationToken();

    protected void setResult(Onem2mAuthenticationResultCode result) {
        this.onem2mAuthToken.setResult(result);
    }

    protected void setResult(Onem2mAuthenticationResultCode result,
                             boolean cse, String cseBaseId, String entityId) {
        this.onem2mAuthToken.setResult(result, cseBaseId, cse, entityId);
    }

    public Onem2mRequestAuthenticationToken getAuthenticationToken() {
        return this.onem2mAuthToken;
    }
}
