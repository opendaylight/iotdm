/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.rx;

import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev141210.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mMqttIotdmPlugin extends IotdmPlugin<IotdmPluginOnem2mBaseRequest, IotdmPluginOnem2mBaseResponse>
                                       implements Onem2mProtocolRxChannel<Onem2mMqttIotdmPluginConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttIotdmPlugin.class);

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mMqttRxRequest,IotdmPluginOnem2mBaseRequest,IotdmPluginOnem2mBaseResponse> requestFactory;
    private final Onem2mService onem2mService;

    private SecurityLevel securityLevel = SecurityLevel.L0;
    private ServerConfig currentConfig = null;

    public Onem2mMqttIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                 @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mMqttRxRequest,IotdmPluginOnem2mBaseRequest,IotdmPluginOnem2mBaseResponse> requestFactory,
                                 @Nonnull final Onem2mService onem2mService) {
        super(Onem2mPluginManager.getInstance());
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
    }

    @Override
    public String getPluginName() {
        return "mqtt";
    }

    @Override
    public void start(Onem2mMqttIotdmPluginConfig configuration)
            throws IllegalArgumentException {
        if (null == configuration) {
            throw new IllegalArgumentException("Starting Mqtt base server without configuration");
        }

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

        mgr.registerPluginMQTT(this, configuration.getMqttBrokerPort(), configuration.getMqttBrokerIp(), Onem2mPluginManager.Mode.Exclusive, null);

            LOG.info("Started MQTT Base IoTDM plugin for broker port: {}, ip: {}, security level: {}",
                     configuration.getMqttBrokerPort(), configuration.getMqttBrokerIp(), configuration.getServerSecurityLevel());

        this.currentConfig = configuration;
        this.securityLevel = configuration.getServerSecurityLevel();
    }

    @Override
    public void close() {
        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.unregisterPlugin(this);

        LOG.info("Closed MQTT Base IoTDM plugin for broker port: {}, ip: {}, security level: {}",
                 currentConfig.getMqttBrokerPort(), currentConfig.getMqttBrokerIp(), currentConfig.getServerSecurityLevel());
    }

    @Override
    public void handle(IotdmPluginOnem2mBaseRequest request, IotdmPluginOnem2mBaseResponse response) {
        Onem2mMqttRxRequest rxRequest =
                requestFactory.createRxRequest(request,
                                                   response,
                                                   onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }
}
