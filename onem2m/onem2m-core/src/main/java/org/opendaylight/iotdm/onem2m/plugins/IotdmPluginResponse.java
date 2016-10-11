/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;

/**
 * Interfaces describes implementation of classes wrapping protocol specific
 * response allowing unified way of setting important parameters.
 */
public interface IotdmPluginResponse {
    /**
     * Sets return code.
     * @param returnCode The return code value to be set.
     */
    void setReturnCode(int returnCode);

    /**
     * Sets the payload of the response.
     * @param responsePayload Payload as a string to be set.
     */
    void setResponsePayload(String responsePayload);

    /**
     * Sets the content type.
     * @param contentType Type of the content as string value.
     */
    void setContentType(String contentType);

    /**
     * Sets response parameter from the generic Onem2m response primitive.
     * @param onem2mResponse The Onem2m response primitive.
     * @return True is returned in case of success, False otherwise.
     */
    boolean setFromResponsePrimitive(ResponsePrimitive onem2mResponse);
}
