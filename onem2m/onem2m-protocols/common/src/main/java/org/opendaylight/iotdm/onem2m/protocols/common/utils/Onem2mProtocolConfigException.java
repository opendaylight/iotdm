/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common.utils;

import javax.annotation.Nonnull;

/**
 * Class defines exception thrown by configuration validators in case of
 * invalid configuration passed to communication protocol provider.
 */
public class Onem2mProtocolConfigException extends Exception {
    public Onem2mProtocolConfigException(@Nonnull final String msg) {
        super(msg);
    }
}
