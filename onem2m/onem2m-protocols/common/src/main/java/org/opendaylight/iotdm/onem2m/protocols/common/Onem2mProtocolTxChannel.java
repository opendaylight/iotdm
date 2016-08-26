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
 * implemented by the sending part of the protocol implementation.
 * The TxChannel can be configured by the configuration data of type T
 * which are passed as parameter of the start() method.
 * @param <T> Type of configuration of the TxChannel
 */
public interface Onem2mProtocolTxChannel<T> extends AutoCloseable {

    /**
     * Configures and starts the TxChannel.
     * @param configuration The configuration of the TxChannel
     * @throws IllegalArgumentException in case of invalid configuration
     * @throws RuntimeException in case of internal error during the start
     */
    void start(T configuration)
            throws IllegalArgumentException, RuntimeException;

    /**
     * Implements the AutoCloseable interface.
     */
    void close();
}
