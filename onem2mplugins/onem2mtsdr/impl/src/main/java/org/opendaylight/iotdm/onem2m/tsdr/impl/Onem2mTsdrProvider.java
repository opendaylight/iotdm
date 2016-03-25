/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.tsdr.impl;

import java.util.Collection;
import java.util.HashMap;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mDatastoreListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mtsdr.rev160210.Onem2mTsdrConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mtsdr.rev160210.TsdrParmsDesc;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mtsdr.rev160210.onem2m.tsdr.config.Onem2mTargetDesc;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mTsdrProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mTsdrProvider.class);
    private Onem2mTsdrSender onem2mTsdrSender;
    private Onem2mDataStoreChangeHandler onem2mDataStoreChangeHandler;
    private TsdrTargetDescDataStoreChangeHandler tsdrTargetDataStoreChangeHandler;
    private TsdrConfigDataStoreChangeHandler tsdrConfigDataStoreChangeHandler;
    private Onem2mTsdrPeriodicManager onem2mTsdrPeriodicManager;
    private Onem2mTsdrAsyncManager onem2mTsdrAsyncManager;

    private HashMap<String,Onem2mTargetDesc> tsdrMap;


    @Override
    public void onSessionInitiated(ProviderContext session) {

        LOG.info("Onem2mTsdrProvider Session Initiated");
        tsdrMap = new HashMap<String,Onem2mTargetDesc>();

        DataBroker dataBroker = session.getSALService(DataBroker.class);
        TsdrCollectorSpiService tsdrService = session.getRpcService(TsdrCollectorSpiService.class);

        onem2mTsdrSender = new Onem2mTsdrSender(tsdrService);
        onem2mTsdrPeriodicManager = new Onem2mTsdrPeriodicManager(onem2mTsdrSender);
        onem2mTsdrAsyncManager = new Onem2mTsdrAsyncManager(onem2mTsdrSender);
        onem2mDataStoreChangeHandler = new Onem2mDataStoreChangeHandler(dataBroker);
        tsdrTargetDataStoreChangeHandler = new TsdrTargetDescDataStoreChangeHandler(dataBroker);
        tsdrConfigDataStoreChangeHandler = new TsdrConfigDataStoreChangeHandler(dataBroker);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mTsdrProvider Closed");
        onem2mTsdrPeriodicManager.close();
        onem2mTsdrAsyncManager.close();
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
            onem2mTsdrAsyncManager.onem2mResourceUpdate(hierarchicalResourceName, onem2mResource);
        }

        @Override
        public void onem2mResourceChanged(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mTsdrProvider: onem2mResourceChanged h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            onem2mTsdrAsyncManager.onem2mResourceUpdate(hierarchicalResourceName, onem2mResource);
        }

        @Override
        public void onem2mResourceDeleted(String hierarchicalResourceName, Onem2mResource onem2mResource) {
            LOG.info("Onem2mTsdrProvider: onem2mResourceDeleted h={}, id:{}, type:{}",
                    hierarchicalResourceName,
                    onem2mResource.getResourceId(),
                    onem2mResource.getResourceType());
            // no processing required as resource is now gone
        }
    }

    // handle changes to configuration of what and how data is sent to the TSDR subsystem
    private class TsdrTargetDescDataStoreChangeHandler implements ClusteredDataTreeChangeListener<Onem2mTargetDesc> {

        private final Logger LOG = LoggerFactory.getLogger(TsdrConfigDataStoreChangeHandler.class);

        private final InstanceIdentifier<Onem2mTargetDesc> TARGET_IID =
                InstanceIdentifier.builder(Onem2mTsdrConfig.class)
                        .child(Onem2mTargetDesc.class).build();
        private ListenerRegistration<TsdrTargetDescDataStoreChangeHandler> dcReg;
        private DataBroker dataBroker;

        public TsdrTargetDescDataStoreChangeHandler(DataBroker dataBroker) {
            this.dataBroker = dataBroker;
            dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    TARGET_IID), this);
        }

        // handle changes to the targetDesc list
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Onem2mTargetDesc>> changes) {

            Onem2mTargetDesc targetDescAfter;
            Onem2mTargetDesc targetDescBefore;

            for (DataTreeModification<Onem2mTargetDesc> change : changes) {
                switch (change.getRootNode().getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        targetDescAfter = change.getRootNode().getDataAfter();
                        targetDescBefore = change.getRootNode().getDataBefore();
                        if (targetDescBefore == null) {
                            onem2mTargetDescCreated(targetDescAfter);
                        } else {
                            String beforeUri = targetDescBefore.getOnem2mTargetUri();
                            String afterUri = targetDescAfter.getOnem2mTargetUri();
                            TsdrParmsDesc.TsdrMode beforeMode = change.getRootNode().getDataBefore().getTsdrMode();
                            TsdrParmsDesc.TsdrMode afterMode = change.getRootNode().getDataAfter().getTsdrMode();
                            if (beforeUri != afterUri || beforeMode != afterMode) {
                                onem2mTargetDescDeleted(targetDescBefore);
                                onem2mTargetDescCreated(targetDescAfter);
                            } else {
                                onem2mTargetDescChanged(targetDescAfter);
                            }
                        }
                        break;
                    case DELETE:
                        targetDescBefore = change.getRootNode().getDataBefore();
                        onem2mTargetDescDeleted(targetDescBefore);
                        break;
                    default:
                        LOG.error("Onem2mDatastoreListener: onDataTreeChanged(Onem2mResource) non handled modification {}",
                                change.getRootNode().getModificationType());
                        break;
                }
            }
        }

        private void onem2mTargetDescCreated(Onem2mTargetDesc t) {
            LOG.info("onem2mTargetDescCreated: {}, mode: {}...", t.getOnem2mTargetUri(), t.getTsdrMode());
            if (t.getTsdrMode() == TsdrParmsDesc.TsdrMode.POLL) {
                onem2mTsdrPeriodicManager.onem2mTargetDescCreated(t);
            } else if (t.getTsdrMode() == TsdrParmsDesc.TsdrMode.ASYNC) {
                onem2mTsdrAsyncManager.onem2mTargetDescCreated(t);
            }
        }

        private void onem2mTargetDescChanged(Onem2mTargetDesc t) {
            LOG.info("onem2mTargetDescChanged: {}, mode: {}...", t.getOnem2mTargetUri(), t.getTsdrMode());
            if (t.getTsdrMode() == TsdrParmsDesc.TsdrMode.POLL) {
                onem2mTsdrPeriodicManager.onem2mTargetDescChanged(t);
            } else if (t.getTsdrMode() == TsdrParmsDesc.TsdrMode.ASYNC) {
                onem2mTsdrAsyncManager.onem2mTargetDescChanged(t);
            }
        }

        private void onem2mTargetDescDeleted(Onem2mTargetDesc t) {
            LOG.info("onem2mTargetDescDeleted: {}, mode: {}...", t.getOnem2mTargetUri(), t.getTsdrMode());
            if (t.getTsdrMode() == TsdrParmsDesc.TsdrMode.POLL) {
                onem2mTsdrPeriodicManager.onem2mTargetDescDeleted(t);
            } else if (t.getTsdrMode() == TsdrParmsDesc.TsdrMode.ASYNC) {
                onem2mTsdrAsyncManager.onem2mTargetDescDeleted(t);
            }
        }
    }

    // handle changes to top-level config changes like batch-interval, tsdt node id, and any others added to model
    private class TsdrConfigDataStoreChangeHandler implements ClusteredDataTreeChangeListener<Onem2mTsdrConfig> {

        private final Logger LOG = LoggerFactory.getLogger(TsdrConfigDataStoreChangeHandler.class);

        private final InstanceIdentifier<Onem2mTsdrConfig> CONFIG_IID =
                InstanceIdentifier.builder(Onem2mTsdrConfig.class).build();
        private ListenerRegistration<TsdrConfigDataStoreChangeHandler> dcReg;
        private DataBroker dataBroker;

        public TsdrConfigDataStoreChangeHandler(DataBroker dataBroker) {
            this.dataBroker = dataBroker;
            dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    CONFIG_IID), this);
        }

        // handle changes to the batch timer here, or any other new parameters to the top level
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Onem2mTsdrConfig>> changes) {

            Onem2mTsdrConfig c;

            for (DataTreeModification<Onem2mTsdrConfig> change : changes) {
                switch (change.getRootNode().getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        c = change.getRootNode().getDataAfter();
                        onem2mTsdrSender.tsdrNodeIdChanged(c.getTsdrNodeId());
                        onem2mTsdrSender.batchTimerChanged(c.getBatchPeriodSeconds().longValue());
                        break;
                    case DELETE:
                        onem2mTsdrSender.tsdrNodeIdChanged("");
                        onem2mTsdrSender.batchTimerChanged(Onem2mTsdrSender.ONEM2M_TSDR_SENDER_BATCH_SEND_TIMER);
                        break;
                    default:
                        LOG.error("Onem2mDatastoreListener: onDataTreeChanged(Onem2mResource) non handled modification {}",
                                change.getRootNode().getModificationType());
                        break;
                }
            }
        }

    }
}
