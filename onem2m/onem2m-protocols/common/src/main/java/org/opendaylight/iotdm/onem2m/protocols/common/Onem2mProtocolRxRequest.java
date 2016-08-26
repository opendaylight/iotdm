/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common;

/**
 * Specifies steps of the handling of received requests. The steps are
 * specified as methods returning True if the handling step was successful and
 * False in case of failure.
 * All methods are defined as protected because they should be visible for
 * classes from this package only.
 *
 * Class doesn't specify any data needed by particular method because they
 * should be implementation specific.
 */
public abstract class Onem2mProtocolRxRequest {

    /**
     * 1. Prepares request for processing.
     * @return True if success False otherwise.
     */
    protected abstract boolean preprocessRequest();

    /**
     * 2. Translates implementation specific type of request data into
     * common Onem2m type (most likely RequestPrimitive).
     * @return True if success False otherwise.
     */
    protected abstract boolean translateRequestToOnem2m();

    /**
     * 3. Performs processing of the request in Onem2m form and stores
     * response data in common Onem2m form for further processing.
     * @return True if success False otherwise.
     */
    protected abstract boolean processRequest();

    /**
     * 4. Translates the response data created in step 3. from the common
     * Onem2m form into the implementation specific form of response.
     * @return True if success False otherwise.
     */
    protected abstract boolean translateResponseFromOnem2m();

    /**
     * 5. Performs sending of the response prepared in step 4.
     */
    protected abstract void respond();
}
