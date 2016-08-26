/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common;

/**
 * Specifies steps of the handling requests to be sent. The steps are specified
 * as methods returning True if the handling step was successful and False
 * in case of failure.
 * All methods are defined as protected because they should be visible for
 * classes from this package only.
 *
 * Class doesn't specify any data needed by particular method because they
 * should be implementation specific.
 */
public abstract class Onem2mProtocolTxRequest {

    /**
     * 1. Prepares request for processing.
     * @return True if success False otherwise.
     */
    protected abstract boolean preprocessRequest();

    /**
     * 2. Translates generic onem2m request data into implementation specific form.
     * @return True if success False otherwise.
     */
    protected abstract boolean translateRequestFromOnem2m();

    /**
     * 3. Performs processing of the request in the implementation specific form,
     * i.e. sends the request and stores response data in implementation specific
     * form for further processing.
     * @return True if success False otherwise.
     */
    protected abstract boolean sendRequest();

    /**
     * 4. Translates the response data created in step 3. into the common
     * Onem2m form (ResponsePrimitive).
     * @return True if success False otherwise.
     */
    protected abstract boolean translateResponseToOnem2m();

    /**
     * 5. Some final processing of response in Onem2m form can be performed.
     */
    protected abstract void respondToOnem2mCore();
}
