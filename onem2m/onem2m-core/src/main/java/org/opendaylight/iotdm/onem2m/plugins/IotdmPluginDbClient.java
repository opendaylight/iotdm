/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;

/**
 * Interface describes methods which must be implemented by
 * plugins using Onem2mPluginDbApi.
 * Every plugin must register to the Onem2mPluginDbApi by
 * registerPlugin() method.
 */
public interface IotdmPluginDbClient extends IotdmPluginCommonInterface {
    /**
     * This method is called immediately during registration of the plugin
     * if the API is prepared. If the API is not prepared then the registration
     * pass without calling this method and method is called asynchronously
     * when API becomes prepared.
     * This method can be called multiple times during runtime and dbClientStop()
     * method is called between each call of this method.
     * @param twc ResourceTreeWriter instance.
     * @param trc ResourceTreeReader instance.
     * @return True if successful, False otherwise.
     */
    boolean dbClientStart(final ResourceTreeWriter twc, final ResourceTreeReader trc);

    /**
     * This method is called by API and it means that plugin must stop
     * to use the ResourceTree reader / writer provided by API when
     * dbClientStart() method called.
     * This method can be used e.g. in case of reconfiguration of API and
     * creation of new ResourceTree reader / writer and the dbClientStart()
     * method can be called again.
     * This method might be called multiple times before the dbClientStart() method
     * is called again.
     */
    void dbClientStop();
}
