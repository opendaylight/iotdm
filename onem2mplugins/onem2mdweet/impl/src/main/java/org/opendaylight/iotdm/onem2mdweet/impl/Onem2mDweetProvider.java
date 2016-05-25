/*
 * Copyright © 2015 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mdweet.impl;

import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mDatastoreListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mdweet.rev150105.Onem2mDweetConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mdweet.rev150105.onem2m.dweet.config.Onem2mTargetDesc;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Onem2mDweetProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mDweetProvider.class);
    private DataBroker dataBroker;
    private Onem2mService onem2mService;
    private Onem2mDataStoreChangeHandler onem2mDataStoreChangeHandler;
    private Onem2mDweetAsyncManager onem2mDweetAsyncManager;
    private HashSet<String> targetResourceIdSet;
    private Onem2mDweetConfigDatastoreHandler onem2mDweetConfigDatastoreHandler;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("Onem2mDweetProvider Session Initiated");
        this.dataBroker = session.getSALService(DataBroker.class);
        this.onem2mService = session.getRpcService(Onem2mService.class);
        this.onem2mDweetAsyncManager = new Onem2mDweetAsyncManager();
        targetResourceIdSet = new HashSet<String>();
        onem2mDataStoreChangeHandler = new Onem2mDataStoreChangeHandler(dataBroker);
        onem2mDweetConfigDatastoreHandler = new Onem2mDweetConfigDatastoreHandler();

    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mDweetProvider Closed");
    }

    // listen for changes to the onem2m resource tree, only the async manager needs to be informed
    private class Onem2mDataStoreChangeHandler extends Onem2mDatastoreListener {

        public Onem2mDataStoreChangeHandler(DataBroker dataBroker) {
            super(dataBroker);
        }

        @Override
        public void onem2mResourceCreated(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mTsdrProvider: onem2mResourceCreated h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            // only async cares about onem2m data tree changes
            for (String targetResourceId : targetResourceIdSet) {
                if (Onem2mDb.getInstance().isResourceIdUnderTargetId(targetResourceId, onem2mResource.getResourceId())) {
                    onem2mDweetAsyncManager.sendResourceUpdate(hierarchicalResourceName, onem2mResource);
                }
            }
        }

        @Override
        public void onem2mResourceChanged(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mTsdrProvider: onem2mResourceChanged h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            for (String targetResourceId : targetResourceIdSet) {
                if (Onem2mDb.getInstance().isResourceIdUnderTargetId(targetResourceId, onem2mResource.getResourceId())) {
                    onem2mDweetAsyncManager.sendResourceUpdate(hierarchicalResourceName, onem2mResource);
                }
            }        }

        @Override
        public void onem2mResourceDeleted(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mTsdrProvider: onem2mResourceDeleted h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            //onem2mDweetAsyncManager.sendResourceDelete(hierarchicalResourceName, onem2mResource);
        }
    }



    /*
     * The yang model onem2mdweet.yang has config info and changes to it are handled by these routines
     */
    private class Onem2mDweetConfigDatastoreHandler implements ClusteredDataTreeChangeListener<Onem2mDweetConfig> {

        private final Logger LOG = LoggerFactory.getLogger(Onem2mDweetConfigDatastoreHandler.class);

        private final InstanceIdentifier<Onem2mDweetConfig> CONFIG_IID =
                InstanceIdentifier.builder(Onem2mDweetConfig.class).build();
        private ListenerRegistration<Onem2mDweetConfigDatastoreHandler> dcReg;

        public Onem2mDweetConfigDatastoreHandler() {
            dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    CONFIG_IID), this);
        }

        /**
         * When the onem2m-dweet.yang model datastore is updated, these routines are called.  Example: a root/target is
         * added so that anything new onem2m resource created, updated, deleted
         *
         * @param changes
         */
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Onem2mDweetConfig>> changes) {
            LOG.info("DweetConfigDataStoreHandler: onDataTreeChanged(Onem2mDweetConfig) called");
            Onem2mDweetConfig dweetParmsAfter;
            Onem2mDweetConfig dweetParmsBefore;
            for (DataTreeModification<Onem2mDweetConfig> change : changes) {

                dweetParmsAfter = change.getRootNode().getDataAfter();
                dweetParmsBefore = change.getRootNode().getDataBefore();
                switch (change.getRootNode().getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        dweetParmsChanged(dweetParmsBefore, dweetParmsAfter);
                        break;
                    case DELETE:
                        dweetParmsDeleted(dweetParmsBefore);
                        break;
                    default:
                        LOG.error("DweetConfigDataStoreHandler: onDataTreeChanged(Onem2mDweetConfig) non handled modification {}",
                                change.getRootNode().getModificationType());
                        break;
                }
            }
        }

        private void dweetParmsChanged(Onem2mDweetConfig dweetParmsBefore, Onem2mDweetConfig dweetParmsAfter) {

            LOG.info("dweetParmsChanged: before: {}, after: {}",
                    //dweetParmsBefore.getOnem2mTargetDesc(),
                    dweetParmsBefore,
                    dweetParmsAfter.getOnem2mTargetDesc());


            List<Onem2mTargetDesc> onem2mTargetDescList = dweetParmsAfter.getOnem2mTargetDesc();
            for (Onem2mTargetDesc onem2mTargetDesc : onem2mTargetDescList) {
                String onem2mTargetResourceName = onem2mTargetDesc.getOnem2mTargetId();
                String onem2mResourceId = Onem2mDb.getInstance().findResourceIdUsingURI(onem2mTargetResourceName);
                if (onem2mResourceId != null) {
                    targetResourceIdSet.add(onem2mResourceId);
                }
            }
        }

        private void dweetParmsDeleted(Onem2mDweetConfig dweetParms) {

            LOG.info("dweetParmsDeleted: {}", dweetParms.getOnem2mTargetDesc());
            List<Onem2mTargetDesc> onem2mTargetDescList = dweetParms.getOnem2mTargetDesc();
            for (Onem2mTargetDesc onem2mTargetDesc : onem2mTargetDescList) {
                String onem2mTargetResourceName = onem2mTargetDesc.getOnem2mTargetId();
                String onem2mResourceId = Onem2mDb.getInstance().findResourceIdUsingURI(onem2mTargetResourceName);
                if (onem2mResourceId != null) {
                    targetResourceIdSet.remove(onem2mResourceId);
                }
            }
        }
    }




}
