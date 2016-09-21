/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2moic.impl;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2moic.rev160929.Onem2mOicConfig;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static org.opendaylight.iotdm.onem2moic.impl.Onem2mOicClient.WELL_KNOWN_DEVICE_QUERY;


public class Onem2mOicProvider {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mOicProvider.class);
    protected Onem2mService onem2mService;
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private ExecutorService pollerExecutor;
    private Onem2mOicClient oicClient;
    private Onem2mOicIPE oicIpe;
    private OicConfigParams oicConfigParams;

    public Onem2mOicProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        oicConfigParams = new OicConfigParams();
        try {
            oicClient = new Onem2mOicClient(Onem2mOicClient.OicClientType.COAP, oicConfigParams.getLocalIpAddr());
        } catch (IOException ioe) {
            LOG.error("Cannot instantiate OIC Client: {}" + ioe.toString());
            ioe.printStackTrace();
        }
        onem2mService = rpcProviderRegistry.getRpcService(Onem2mService.class);
        oicIpe = new Onem2mOicIPE(onem2mService);

        discoverOicDevices();

        pollerExecutor = Executors.newFixedThreadPool(1);
        startOicDevicePolling();
    }

    /**
     * OIC Device discovery routine by sending /oic/d to "All Coap nodes" multicast group
     */
    public void discoverOicDevices() {

        CoapHandler discoverHandler = new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                Onem2mOicClient.OicDevice oicDevice;
                try {
                    if (CoAP.ResponseCode.isSuccess(coapResponse.getCode())) {
                        oicDevice = oicClient.oicParseDevicePayload(coapResponse.advanced().getPayload());
                        oicIpe.createOicAe(oicDevice, oicConfigParams.getCseName());
                    } else {
                        LOG.error("CoAP Request failure: ", coapResponse.getCode().toString());
                    }
                } catch (IOException ioe) {
                    LOG.error("Cannot parse OIC discovery payload: {}", ioe.toString());
                    ioe.printStackTrace();
                }
            }
            @Override
            public void onError() {
            }
        };

        /* Write the response handler */
        oicClient.oicDeviceDiscovery(null, 0, WELL_KNOWN_DEVICE_QUERY, discoverHandler);
    }

    /**
     * Periodically discover the OIC devices and update it to M2M resource tree
     * @param pollPeriod
     */
    private void poller(int pollPeriod) {
        while (true) {
            try {
                sleep(pollPeriod);
            } catch (InterruptedException e) {
                LOG.error("Timer was interrrupted {} ", e.toString());
            }
            discoverOicDevices();
        }
    }

    /**
     * Start the periodic polling of OIC devices
     */
    private void startOicDevicePolling() {
        pollerExecutor.execute(new Runnable() {
            @Override
            public void run() {
                poller(oicConfigParams.getPollPeriod());
            }
        });
    }

    /**
     * Gracefully Stop the periodic polling of OIC devices
     */
    private void stopOicDevicePolling() {
        pollerExecutor.shutdown();

        try {
            while(!pollerExecutor.awaitTermination(100, TimeUnit.MICROSECONDS)) {
            }
        } catch (InterruptedException ie) {
            LOG.error("Could not stop Poller executor service");
        }
    }

    /**
     * Datachange listener has notified a change in polling period, stop and start polling
     */
    private void pollPeriodChanged() {
        LOG.info("Polling period changed to " + oicConfigParams.getPollPeriod());
        stopOicDevicePolling();
        startOicDevicePolling();
    }

    /**
     * Datachange listener has notified a local interface change, bind oic client to new local ip address,
     * stop and restart the polling for oic devices
     * TODO Need to verify if we need to clean up exsisting OIC devices/resources
     */
    private void localIpAddrChanged() {
        LOG.info("Local interface changed to " + oicConfigParams.getLocalIpAddr());
        stopOicDevicePolling();
        try {
            oicClient = new Onem2mOicClient(Onem2mOicClient.OicClientType.COAP, oicConfigParams.getLocalIpAddr());
        } catch (IOException ioe) {
            LOG.error("Could not instantiate new oic client");
        }
        startOicDevicePolling();
    }
    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Onem2mOicProvider Closed");
    }

    /**
     * OicConfigParams class is defined to maintain a local copy of config parameters
     */
    private class OicConfigParams {
        private int ipeMode;
        private int pollPeriod;
        private String cseName;
        private String localIpAddr;

        public OicConfigParams() {
            ipeMode = 0;
            pollPeriod = 10000;
            cseName = "";
            localIpAddr = null;
        }

        private int getIpeMode() {
            return ipeMode;
        }

        private int getPollPeriod() {
            return pollPeriod;
        }

        private String getCseName() {
            return cseName;
        }

        private String getLocalIpAddr() {
            return localIpAddr;
        }

        private void setCseName(String cseName) {
            this.cseName = cseName;
        }

        private void setIpeMode(int ipeMode) {
            this.ipeMode = ipeMode;
        }

        public void setLocalIpAddr(String localIpAddr) {
            this.localIpAddr = localIpAddr;
        }

        public void setPollPeriod(int pollPeriod) {
            this.pollPeriod = pollPeriod;
        }
    }

    /**
     * handle changes to top-level config changes like pollperiod, localipaddr, csename.. others added to model
     */
    private class OicConfigDataStoreChangeHandler implements ClusteredDataTreeChangeListener<Onem2mOicConfig> {

        private final Logger LOG = LoggerFactory.getLogger(OicConfigDataStoreChangeHandler.class);

        private final InstanceIdentifier<Onem2mOicConfig> CONFIG_IID =
                InstanceIdentifier.builder(Onem2mOicConfig.class).build();
        private ListenerRegistration<OicConfigDataStoreChangeHandler> dcReg;
        private DataBroker dataBroker;

        public OicConfigDataStoreChangeHandler(DataBroker dataBroker) {
            this.dataBroker = dataBroker;
            dcReg = dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION,
                    CONFIG_IID), this);
        }

        // handle changes to the batch timer here, or any other new parameters to the top level
        @Override
        public void onDataTreeChanged(Collection<DataTreeModification<Onem2mOicConfig>> changes) {

            Onem2mOicConfig c;
            String cseName = oicConfigParams.getCseName();

            for (DataTreeModification<Onem2mOicConfig> change : changes) {
                switch (change.getRootNode().getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        c = change.getRootNode().getDataAfter();
                        if (c.getPollPeriod() != oicConfigParams.getPollPeriod()) {
                            //oicConfigParams.setPollPeriod((int) c.getPollPeriod());
                            pollPeriodChanged();
                        }
                        if(!c.getLocalIpAddr().equals(oicConfigParams.getLocalIpAddr())) {
                            oicConfigParams.setLocalIpAddr(c.getLocalIpAddr());
                            localIpAddrChanged();
                        }
                        /*
                         * TODO Add functionality to handle cseName and mode change
                         */
                        if(!c.getCseName().equals(oicConfigParams.getCseName())) {
                            if (cseName == null || cseName.isEmpty()) {
                                oicConfigParams.setCseName(c.getCseName());
                            }
                        }
                        break;
                    case DELETE:
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