/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.security.authentication;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

/**
 * Implements the request authentication token which encapsulates authentication result code
 * together with security association data which should be provided by
 * every authentication method used:
 *  - entity type (CSE or AE)
 *  - entity id (CSE-ID or AE-ID) of the entity sending the request (not necessarily originating)
 *  - registrar cseBase CSE-ID
 */
public class Onem2mRequestAuthenticationToken {
    private Onem2mAuthenticationResultCode state = Onem2mAuthenticationResultCode.INIT;
    private boolean cse = false; // AE by default
    private String cseBaseCseId = null;
    private String entityId = null;

    /**
     * The only constructor is protected so the AuthenticationToken must be
     * instantiated by other class from the same package.
     */
    protected Onem2mRequestAuthenticationToken() {

    }

    private String getSecExceptionStr(Onem2mAuthenticationResultCode newState) {
        return("Illegal attempt to change authentication token state from: " +
               this.state.toString() +
               "to: " + newState.toString());
    }

    /**
     * Sets result of authentication together with security association data
     * provided by the authentication method.
     * @param result The authentication result code.
     * @param cseBaseCseId The CSE-ID of cseBase which is a registrar CSE of the
     *                     authenticated entity.
     * @param isCse Specifies whether the authenticated entity is CSE or AE.
     * @param entityId The ID of the authenticated entity (CSE-ID or AE-ID).
     */
    protected void setResult(Onem2mAuthenticationResultCode result,
                             String cseBaseCseId, boolean isCse, String entityId) {

        // The initial state can't be set
        if (state != Onem2mAuthenticationResultCode.INIT) {
            throw new SecurityException(getSecExceptionStr(result));
        }

        // The attempt to re-use AuthenticationToken is security exception
        if (this.state == Onem2mAuthenticationResultCode.USED) {
            throw new SecurityException(getSecExceptionStr(result));
        }

        this.state = result;
        this.cseBaseCseId = cseBaseCseId;
        this.cse = isCse;
        this.entityId = entityId;
    }

    /**
     * Sets result without security association data so the AUTHPASSED result can't
     * be set by this method.
     * @param result The authentication result.
     */
    protected void setResult(Onem2mAuthenticationResultCode result) {

        // The initial state can't be set
        if (state != Onem2mAuthenticationResultCode.INIT) {
            throw new SecurityException(getSecExceptionStr(result));
        }

        // The authentication passed can't be set without security authentication data
        if (result == Onem2mAuthenticationResultCode.AUTHPASSED) {
            throw new SecurityException("Successful authentication must provide security association information");
        }

        // Attempt to re-use authentication token is security exception
        if (this.state == Onem2mAuthenticationResultCode.USED) {
            throw new SecurityException(getSecExceptionStr(result));
        }

        state = result;
    }

    /**
     * Sets the authentication token instance as used.
     * The state of the authentication token can't be changed from the USED state so
     * the token can't be reused.
     */
    public void setUsed() {
        state = Onem2mAuthenticationResultCode.USED;
    }

    /**
     * Checks whether the authentication result if OK for specific security level.
     * @param secLevel The security level.
     * @return True if the authentication result is OK for the specific security level
     *         False otherwise.
     */
    public boolean isAuthOk(SecurityLevel secLevel) {
        // Throw the SecurityException because of the attempt to reuse token instance
        if (state == Onem2mAuthenticationResultCode.USED) {
            throw new SecurityException("Usage of already used token is forbidden");
        }

        // The INIT state is invalid for all security levels
        if (state == Onem2mAuthenticationResultCode.INIT) {
            return false;
        }

        /*
         *  False is returned in case of PROXYRSP for all security levels
         *  so the request processing should be stopped and the response should be
         *  sent to client immediately.
         */
        switch (secLevel) {

            case L0:
                /*
                 * Valid states in case of L0 are:
                 *  - AUTHPASSED
                 *  - AUTHFAILED
                 *  - NOAUTHDATA
                 *  - NOAUTHMETHOD
                 */
                return state != Onem2mAuthenticationResultCode.PROXYRSP;

            case L1:
                /*
                 * Valid states in case of L1 are:
                 *  - AUTHPASSED
                 *  - NOAUTHDATA
                 *  - NOAUTHMETHOD
                 */
                return ((state != Onem2mAuthenticationResultCode.AUTHFAILED) &&
                        (state != Onem2mAuthenticationResultCode.PROXYRSP));

            case L2:
                /*
                 * The only valid state is AUTHPASSED
                 */
                return state == Onem2mAuthenticationResultCode.AUTHPASSED;
        }

        // This should never happen
        throw new SecurityException("Internal error in authentication token");
    }

    public boolean isCse() {
        return this.cse;
    }

    public String getCseBaseCseId() {
        return this.cseBaseCseId;
    }

    public String getEntityId() { return this.entityId; }
}
