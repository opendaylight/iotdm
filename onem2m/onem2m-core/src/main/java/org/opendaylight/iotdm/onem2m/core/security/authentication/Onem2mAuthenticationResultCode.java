/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.security.authentication;

public enum Onem2mAuthenticationResultCode {

    // Initial value
    INIT,

    // Authentication passed
    AUTHPASSED,

    // Request doesn't have all required authentication data
    NOAUTHDATA,

    // There's not authentication method available
    NOAUTHMETHOD,

    // Authentication has failed
    AUTHFAILED,

    // Authentication method needs to perform intermediate step.
    // The result of this step is response which should be returned to client immediately.
    PROXYRSP,

    // Authentication results has already been used
    USED
}
