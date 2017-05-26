/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins;

/**
 * This interface describes IoTDM plugin which implements handling
 * of specific protocol requests.
 * @param <Treq> Type of the supported request.
 * @param <Trsp> Type of the supported response.
 */
public interface IotdmPlugin<Treq extends IotdmPluginRequest,
                             Trsp extends IotdmPluginResponse> extends IotdmPluginCommonInterface {

    /**
     * Implementation of the handler method.
     * @param request Received request to be handled.
     * @param response Response to be filled by result of the handling.
     */
    void handle(Treq request, Trsp response);
}