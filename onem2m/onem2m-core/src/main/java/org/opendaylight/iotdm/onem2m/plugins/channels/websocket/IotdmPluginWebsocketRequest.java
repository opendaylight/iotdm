/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins.channels.websocket;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRequest;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class IotdmPluginWebsocketRequest implements IotdmPluginRequest<String> {
    private String request = null;
    private String contentType = null;
    private JSONObject rqpBody = null;

    IotdmPluginWebsocketRequest(@Nonnull final String rx, final String ctType) {
        request = rx;
        contentType = ctType;
    }

    private void initRqpJson() {
        if(Objects.isNull(rqpBody)) {
            rqpBody = new JSONObject(Onem2m.getRqpJsonPrimitive(request));
        }
    }

    @Override
    public String getOnem2mOperation() {
        initRqpJson();
        return rqpBody.getString(RequestPrimitive.OPERATION);
    }

    @Override
    public String getOnem2mUri() {
        initRqpJson();
        return Onem2m.translateUriToOnem2m(rqpBody.getString(RequestPrimitive.TO));
    }

    @Override
    public String getUrl() {
        initRqpJson();
        return rqpBody.getString(RequestPrimitive.TO);
    }

    @Override
    public String getPayLoad() {
        initRqpJson();
        return rqpBody.getString(RequestPrimitive.CONTENT);
    }

    @Override
    public String getContentType() {
        initRqpJson();
        String ret;
        try {
            ret = rqpBody.getString(RequestPrimitive.CONTENT_FORMAT);
        }
        catch(JSONException e) {
            ret = contentType;
        }
        return ret;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getOriginalRequest() {
        return request;
    }

    /**
     * not implemented for websockets
     * @param payload payload
     */
    @Override
    public void setPayLoad(String payload) {}

    /**
     * not implemented for websockets - there are no special websocket methods
     * @return null
     */
    @Override
    public String getMethod() {
        return null;
    }
}
