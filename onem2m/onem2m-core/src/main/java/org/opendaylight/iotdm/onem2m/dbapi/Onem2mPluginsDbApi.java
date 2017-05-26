/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.dbapi;

import java.util.List;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;

public interface Onem2mPluginsDbApi {

    void registerDbClientPlugin(Onem2mDbApiClientPlugin plugin) throws IotdmPluginRegistrationException;

    void unregisterDbClientPlugin(Onem2mDbApiClientPlugin plugin);
//
//    List<PluginDbClientData> getPluginsRegistry();

    void registerDbReaderAndWriter(ResourceTreeWriter twc, ResourceTreeReader trc);

    void unregisterDbReaderAndWriter();

    // TODO throw IotdmDaoReadException if the reader is not there
    ResourceTreeReader getReader();

    // TODO throw IotdmDaoWriteException if the writer is not there
    ResourceTreeWriter getWriter();

    List<String> getCseList();

    String findResourceIdUsingURI(String uri);

    String getHierarchicalNameForResource(Onem2mResource onem2mResource);

    List<String> getHierarchicalResourceList(String startResourceId, int limit);

    Onem2mResource getResource(String resourceId);

    Onem2mResource getResourceUsingURI(String targetURI);

    boolean isLatestCI(Onem2mResource onem2mResource);

    boolean isResourceIdUnderTargetId(String targetResourceId, String onem2mResourceId);

    String findCseForTarget(String targetResourceId);
}