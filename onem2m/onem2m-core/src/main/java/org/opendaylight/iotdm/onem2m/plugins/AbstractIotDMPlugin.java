/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins;

import java.util.HashMap;

public interface AbstractIotDMPlugin<Treq extends IotDMPluginRequest,
                                     Trsp extends IotDMPluginResponse> extends AutoCloseable {
    void init();
    String pluginName();
    void handle(Treq request, Trsp response);
}