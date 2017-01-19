/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt.rx;

import org.opendaylight.iotdm.onem2m.plugins.IotdmPlugin;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginConfigurable;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRegistrationException;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseRequest;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.plugins.channels.common.IotdmPluginOnem2mBaseResponse;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.plugin.configuration.PluginSpecificConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev170118.Onem2mProtocolMqttProviders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev170118.mqtt.protocol.provider.config.MqttClientConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev170118.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.MqttConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mMqttIotdmPlugin implements IotdmPlugin<IotdmPluginOnem2mBaseRequest, IotdmPluginOnem2mBaseResponse>,
                                              IotdmPluginConfigurable,
                                              Onem2mProtocolRxChannel {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttIotdmPlugin.class);

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mMqttRxRequest,IotdmPluginOnem2mBaseRequest,IotdmPluginOnem2mBaseResponse> requestFactory;
    private final Onem2mService onem2mService;

    protected final SecurityLevel securityLevel;
    protected final Onem2mProtocolMqttProviders configuration;

    public Onem2mMqttIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                 @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mMqttRxRequest,IotdmPluginOnem2mBaseRequest,IotdmPluginOnem2mBaseResponse> requestFactory,
                                 @Nonnull final Onem2mService onem2mService,
                                 @Nonnull final Onem2mProtocolMqttProviders configuration) {
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;

        this.configuration = configuration;
        if (null == configuration.getMqttClientConfig()) {
            this.securityLevel = SecurityLevel.L0;
        } else {
            this.securityLevel = configuration.getMqttClientConfig()
                                              .getSecurityLevel();
        }
    }

    @Override
    public String getPluginName() {
        return "mqtt";
    }

    @Override
    public void start() {
        MqttClientConfig clientCfg = this.configuration.getMqttClientConfig();

        if (null != clientCfg) {
            try {
                Onem2mPluginManager.getInstance()
                                   .registerPluginMQTT(this, clientCfg.getMqttBrokerPort(),
                                                       clientCfg.getMqttBrokerIp(),
                                                       Onem2mPluginManager.Mode.Exclusive, null);
            }
            catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to register at PluginManager: {}", e);
                return;
            }

            LOG.info("Started MQTT Base IoTDM plugin for broker port: {}, ip: {}, security level: {}",
                     clientCfg.getMqttBrokerPort(), clientCfg.getMqttBrokerIp(),
                     clientCfg.getSecurityLevel());
        } else {
            LOG.info("MQTT Base IoTDM plugin started without configuration, connection to MQTT broker not established");
        }
    }

    @Override
    public void close() {
        MqttClientConfig clientCfg = configuration.getMqttClientConfig();

        if (null != clientCfg) {
            Onem2mPluginManager.getInstance()
                               .unregisterIotdmPlugin(this);

            LOG.info("Closed MQTT Base IoTDM plugin for broker port: {}, ip: {}, security level: {}",
                     clientCfg.getMqttBrokerPort(), clientCfg.getMqttBrokerIp(),
                     clientCfg.getSecurityLevel());
        } else {
            LOG.info("Close MQTT Base IoTDM plugin without connection to MQTT broker configured");
        }
    }

    @Override
    public void handle(IotdmPluginOnem2mBaseRequest request, IotdmPluginOnem2mBaseResponse response) {
        Onem2mMqttRxRequest rxRequest =
                requestFactory.createRxRequest(request,
                                                   response,
                                                   onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }

    @Override
    public PluginSpecificConfiguration getRunningConfig() {

        if (null != this.configuration) {
            return new MqttConfigBuilder()
                .setMqttClientConfig(this.configuration.getMqttClientConfig())
                .build();
        }

        return new MqttConfigBuilder().build();
    }
}
