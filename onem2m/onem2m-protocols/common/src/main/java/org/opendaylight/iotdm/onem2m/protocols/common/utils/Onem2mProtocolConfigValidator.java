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
 * Interface defines method validate() which throws exception in case
 * of invalid configuration of communication protocol provider.
 */
public interface Onem2mProtocolConfigValidator {

    /**
     * Throws exception with specified message if the given object is null.
     * @param obj Object to check
     * @param msg Message to set to exception
     * @throws Onem2mProtocolConfigException
     */
    default void checkNotNull(final Object obj, @Nonnull final String msg) throws Onem2mProtocolConfigException {
        if (null == obj) {
            throw new Onem2mProtocolConfigException(msg);
        }
    }

    /**
     * Throws exception with specified message if the given condition is false.
     * @param condition Condition to check
     * @param msg Message set to exception
     * @throws Onem2mProtocolConfigException
     */
    default void checkCondition(final boolean condition, @Nonnull final String msg)
        throws Onem2mProtocolConfigException {

        if (!condition) {
            throw new Onem2mProtocolConfigException(msg);
        }
    }

    /**
     * Validation method which validates complete configuration and
     * throws exception at the first configuration item which is invalid.
     * @throws Onem2mProtocolConfigException
     */
    void validate() throws Onem2mProtocolConfigException;
}
