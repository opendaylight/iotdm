/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
public class IotDMPluginRequest {
    String method;
    String url;
    String payLoad;
    String contentType;

    HashMap<String,String> headers;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPayLoad() {
        return payLoad;
    }

    public void setPayLoad(String payLoad) {
        this.payLoad = payLoad;
    }

    public String getContentType() { return contentType; }

    public void setContentType(String contentType){ this.contentType = contentType; }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
          return headers.get(key);
    }

    public void addHeader(String key, String value) {
        if(headers == null)
        {
            headers = new HashMap<String,String>();
        }
        this.headers.put(key,value);
    }

}

