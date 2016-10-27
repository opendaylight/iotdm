/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins.channels.common;

import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRequest;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Base implementation of IotdmPluginRequest. It can maintain the basic onem2m request primitive in json format.
 */
public class IotdmPluginOnem2mBaseRequest implements IotdmPluginRequest<String> {
    private String request = null;
    private String contentType = null;
    private JSONObject rqpBody = null;

    /**
     * basic onem2m request in json format
     * @param rx onem2m compliant request string
     * @param ctType contentType of given string
     */
    public IotdmPluginOnem2mBaseRequest(@Nonnull final String rx, final String ctType) {
        request = rx;
        contentType = ctType;
    }

    private void initRqpJson() {
        if(Objects.isNull(rqpBody)) {
            rqpBody = new JSONObject(Onem2m.getRqpJsonPrimitive(request));
        }
    }

    /**
     * @return onem2m operation
     */
    @Override
    public String getOnem2mOperation() {
        initRqpJson();
        return rqpBody.getString(RequestPrimitive.OPERATION);
    }

    /**
     * @return "to" value from onem2m request translated to onem2m format
     */
    @Override
    public String getOnem2mUri() {
        initRqpJson();
        return Onem2m.translateUriToOnem2m(rqpBody.getString(RequestPrimitive.TO));
    }

    /**
     * @return "to" value from onem2m request
     */
    @Override
    public String getUrl() {
        initRqpJson();
        return rqpBody.getString(RequestPrimitive.TO);
    }

    /**
     * @return onem2m request payload
     */
    @Override
    public String getPayLoad() {
        initRqpJson();
        return rqpBody.getString(RequestPrimitive.CONTENT);
    }

    /**
     * get contentType if set or try get from request
     * @return contentType
     */
    @Override
    public String getContentType() {
        initRqpJson();
        String ret;
        try {
            ret = Objects.nonNull(contentType) ? contentType:rqpBody.getString(RequestPrimitive.CONTENT_FORMAT);
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

    /**
     * return original onem2m request string
     * @return request
     */
    @Override
    public String getOriginalRequest() {
        return request;
    }

    /**
     * there is not specific method, so return onem2m
     * @return onem2m operation
     */
    @Override
    public String getMethod() {
        return getOnem2mOperation();
    }

    /**
     * not implemented, payload is retrieved from request, see this.getPayLoad() method
     * @param s payload
     */
    @Override
    public void setPayLoad(String s) {}
}
