/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common;

/**
 * This interfaces describes generic methods which should be
 * implemented by the receiving part of the protocol implementation.
 */
public interface Onem2mProtocolRxChannel extends AutoCloseable {

    /**
     * Configures and starts the RxChannel.
     * @throws RuntimeException in case of internal error during the start
     */
    void start() throws RuntimeException;

    /**
     * Implements the AutoCloseable interface.
     */
    void close();
}
