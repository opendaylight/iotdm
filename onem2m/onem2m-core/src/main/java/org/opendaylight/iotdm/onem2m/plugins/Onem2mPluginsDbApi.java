/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeWriter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Created by bjanosik on 9/21/16.
 */
public class Onem2mPluginsDbApi {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPluginsDbApi.class);

    private ResourceTreeWriter twc;
    private ResourceTreeReader trc;
    private static Onem2mPluginsDbApi api;
    private boolean ready = false;


    public static Onem2mPluginsDbApi getInstance() {
        if (api == null) {
            api = new Onem2mPluginsDbApi();
        }
        return api;
    }

    private Onem2mPluginsDbApi() {
    }

    public boolean isApiReady() {
        return ready;
    }

    public void registerDbReaderAndWriter(ResourceTreeWriter twc, ResourceTreeReader trc) {
        this.trc = trc;
        this.twc = twc;
        ready = true;
    }

    public ResourceTreeWriter getTransactionWriter() {
        return this.twc;
    }

    public ResourceTreeReader getTransactionReader() {
        return this.trc;
    }

    public List<String> getCseList() {
        return Onem2mDb.getInstance().getCseList(this.trc);
    }

    public String findResourceIdUsingURI(String uri) {
        return Onem2mDb.getInstance().findResourceIdUsingURI(this.trc, Onem2m.translateUriToOnem2m(uri));
    }

    public String getHierarchicalNameForResource(Onem2mResource onem2mResource) {
        return Onem2mDb.getInstance().getHierarchicalNameForResource(this.trc, onem2mResource);
    }

    public List<String> getHierarchicalResourceList(String startResourceId, int limit) {
        return Onem2mDb.getInstance().getHierarchicalResourceList(this.trc, startResourceId, limit);
    }

    public Onem2mResource getResource(String resourceId) {
        return Onem2mDb.getInstance().getResource(this.trc, resourceId);
    }

    public Onem2mResource getResourceUsingURI(String targetURI) {
        return Onem2mDb.getInstance().getResourceUsingURI(this.trc, targetURI);
    }

    public boolean isLatestCI(Onem2mResource onem2mResource) {
        return Onem2mDb.getInstance().isLatestCI(this.trc, onem2mResource);
    }

    public boolean isResourceIdUnderTargetId(String targetResourceId, String onem2mResourceId) {
        return Onem2mDb.getInstance().isResourceIdUnderTargetId(this.trc, targetResourceId, onem2mResourceId);
    }

    public boolean registerPlugin(String pluginName) {

        int count = 60;
        while (--count >= 0) {
            if (Onem2mPluginsDbApi.getInstance().isApiReady()) {
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }
        }

        LOG.error("{}: cannot register with Onem2mPluginsDbApi", pluginName);

        return false;
    }
}
