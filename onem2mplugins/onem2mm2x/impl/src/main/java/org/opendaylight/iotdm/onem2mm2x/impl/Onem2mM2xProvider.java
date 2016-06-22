/*
 * Copyright © 2015 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mm2x.impl;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mm2x.rev150105.Onem2mM2xConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mm2x.rev150105.onem2m.m2x.config.Onem2mTargetDesc;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class Onem2mM2xProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mM2xProvider.class);
    private DataBroker dataBroker;
    private Onem2mService onem2mService;
    private Onem2mDataStoreChangeHandler onem2mDataStoreChangeHandler;
    private Onem2mM2xAsyncManager onem2mM2xAsyncManager;
    private HashSet<String> targetResourceIdSet;
    private Onem2mM2xConfigDatastoreHandler onem2mM2xConfigDatastoreHandler;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("Onem2mM2xProvider Session Initiated");
        this.dataBroker = session.getSALService(DataBroker.class);
        this.onem2mService = session.getRpcService(Onem2mService.class);
        this.onem2mM2xAsyncManager = new Onem2mM2xAsyncManager();
        targetResourceIdSet = new HashSet<String>();
        onem2mDataStoreChangeHandler = new Onem2mDataStoreChangeHandler(dataBroker);
        onem2mM2xConfigDatastoreHandler = new Onem2mM2xConfigDatastoreHandler();

    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mM2xProvider Closed");
    }

    // listen for changes to the onem2m resource tree, only the async manager needs to be informed
    private class Onem2mDataStoreChangeHandler extends Onem2mDatastoreListener {

        public Onem2mDataStoreChangeHandler(DataBroker dataBroker) {
            super(dataBroker);
        }

        @Override
        public void onem2mResourceCreated(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mM2xProvider: onem2mResourceCreated h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            // only async cares about onem2m data tree changes
            for (String targetResourceId : targetResourceIdSet) {
                if (Onem2mDb.getInstance().isResourceIdUnderTargetId(targetResourceId, onem2mResource.getResourceId())) {
                    onem2mM2xAsyncManager.sendResourceUpdate(hierarchicalResourceName, onem2mResource);
                }
            }
        }

        @Override
        public void onem2mResourceChanged(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mM2xProvider: onem2mResourceChanged h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            for (String targetResourceId : targetResourceIdSet) {
                if (Onem2mDb.getInstance().isResourceIdUnderTargetId(targetResourceId, onem2mResource.getResourceId())) {
                    onem2mM2xAsyncManager.sendResourceUpdate(hierarchicalResourceName, onem2mResource);
                }
            }        }

        @Override
        public void onem2mResourceDeleted(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mM2xProvider: onem2mResourceDeleted h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            //onem2mM2xAsyncManager.sendResourceDelete(hierarchicalResourceName, onem2mResource);
        }
    }



    /*
     * The yang model onem2mm2x.yang has config info and changes to it are handled by these routines
     */
    private class Onem2mM2xConfigDatastoreHandler implements ClusteredDataTreeChangeListener<Onem2mM2xConfig> {

        private final Logger LOG = LoggerFactory.getLogger(Onem2mM2xConfigDatastoreHandler.class);

        private final InstanceIdentifier<Onem2mM2xConfig> CONFIG_IID =
                InstanceIdentifier.builder(Onem2mM2xConfig.class).build();
        private ListenerRegistration<Onem2mM2xConfigDatastoreHandler> dcReg;

        public Onem2mM2xConfigDatastoreHandler() {
            dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    CONFIG_IID), this);
        }

        /**
         * When the onem2m-m2x.yang model datastore is updated, these routines are called.  Example: a root/target is
         * added so that anything new onem2m resource created, updated, deleted
         *
         * @param changes
         */
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Onem2mM2xConfig>> changes) {
            LOG.info("M2xConfigDataStoreHandler: onDataTreeChanged(Onem2mM2xConfig) called");
            Onem2mM2xConfig m2xParmsAfter;
            Onem2mM2xConfig m2xParmsBefore;
            for (DataTreeModification<Onem2mM2xConfig> change : changes) {

                m2xParmsAfter = change.getRootNode().getDataAfter();
                m2xParmsBefore = change.getRootNode().getDataBefore();
                switch (change.getRootNode().getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        m2xParmsChanged(m2xParmsBefore, m2xParmsAfter);
                        break;
                    case DELETE:
                        m2xParmsDeleted(m2xParmsBefore);
                        break;
                    default:
                        LOG.error("M2xConfigDataStoreHandler: onDataTreeChanged(Onem2mM2xConfig) non handled modification {}",
                                change.getRootNode().getModificationType());
                        break;
                }
            }
        }

        private void m2xParmsChanged(Onem2mM2xConfig m2xParmsBefore, Onem2mM2xConfig m2xParmsAfter) {

            LOG.info("m2xParmsChanged: before: {}, after: {}",
                    //m2xParmsBefore.getOnem2mTargetDesc(),
                    m2xParmsBefore,
                    m2xParmsAfter.getOnem2mTargetDesc());


            List<Onem2mTargetDesc> onem2mTargetDescList = m2xParmsAfter.getOnem2mTargetDesc();
            for (Onem2mTargetDesc onem2mTargetDesc : onem2mTargetDescList) {
                String onem2mTargetResourceName = onem2mTargetDesc.getOnem2mTargetId();
                String onem2mResourceId = Onem2mDb.getInstance().findResourceIdUsingURI(onem2mTargetResourceName);
                if (onem2mResourceId != null) {
                    targetResourceIdSet.add(onem2mResourceId);
                }
            }

            onem2mM2xAsyncManager.setCustomerDeviceID(m2xParmsAfter.getM2xDeviceId());
            onem2mM2xAsyncManager.setCustomerApiKey(m2xParmsAfter.getM2xApiKey());
        }

        private void m2xParmsDeleted(Onem2mM2xConfig m2xParms) {

            LOG.info("m2xParmsDeleted: {}", m2xParms.getOnem2mTargetDesc());
            List<Onem2mTargetDesc> onem2mTargetDescList = m2xParms.getOnem2mTargetDesc();
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
