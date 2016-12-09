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
 * registerDbClientPlugin() method.
 */
public interface IotdmPluginDbClient extends IotdmPluginCommonInterface {

    /**
     * Helper method which returns current ResourceTreeReader instance.
     * Returned value should not be stored and used later since the
     * current ResourceTreeReader instances can be replaced by new instances
     * during runtime.
     * @return ResourceTreeReader object, null is returned if doesn't exist.
     */
    default ResourceTreeReader getReader() {
        return Onem2mPluginsDbApi.getInstance().getReader();
    }

    /**
     * Helper method which returns current ResourceTreeWriter instance.
     * Returned value should not be stored and used later since the
     * current ResourceTreeWriter instances can be replaced by new instances
     * during runtime.
     * @return ResourceTreeWriter object, null is returned if doesn't exist.
     */
    default ResourceTreeWriter getWriter() {
        return Onem2mPluginsDbApi.getInstance().getWriter();
    }

    /**
     * This method is called immediately during registration of the plugin
     * if the API is prepared. If the API is not prepared then the registration
     * pass without calling this method and method is called asynchronously
     * when API becomes prepared.
     * This method can be called multiple times during runtime and dbClientStop()
     * method is called between each call of this method.
     * @param twc ResourceTreeWriter instance.
     * @param trc ResourceTreeReader instance.
     */
    default void dbClientStart(final ResourceTreeWriter twc, final ResourceTreeReader trc) throws Exception {
        // Default implementation does nothing
        return;
    }

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
    default void dbClientStop() {
        // Default implementation does nothing
        return;
    }
}
