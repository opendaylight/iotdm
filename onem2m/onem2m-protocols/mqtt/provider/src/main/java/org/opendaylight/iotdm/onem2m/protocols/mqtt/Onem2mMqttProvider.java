/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.mqtt;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.utils.Onem2mProtocolConfigException;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.rx.Onem2mMqttIotdmPlugin;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.rx.Onem2mMqttRxRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification.Onem2mMqttNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification.Onem2mMqttNotifierRequestAbstractFactory;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification.Onem2mMqttNotifierRequestFactory;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification.Onem2mMqttTxClient;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.tx.notification.Onem2mMqttTxClientConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev170118.Onem2mProtocolMqttProviders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mMqttProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mMqttProvider.class);
    private Onem2mMqttIotdmPlugin onem2mMqttIotdmPlugin = null;
    private final Onem2mProtocolMqttProviders mqttConfig;

    protected final Onem2mService onem2mService;
    protected Onem2mMqttTxClient notifierClient = null;
    protected Onem2mMqttNotifierRequestAbstractFactory notifierReqFactory = null;
    protected Onem2mMqttNotifierPlugin notifierPlugin = null;

    public Onem2mMqttProvider(RpcProviderRegistry rpcRegistry,
                              Onem2mProtocolMqttProviders mqttConfig) throws Onem2mProtocolConfigException {
        this.onem2mService = rpcRegistry.getRpcService(Onem2mService.class);

        Onem2mMqttConfigurationValidator validator =
            new Onem2mMqttConfigurationValidator(mqttConfig.getMqttClientConfig());
        try {
            validator.validate();
        } catch (Onem2mProtocolConfigException e) {
            LOG.error("Invalid configuration passed: {}", e);
            throw e;
        }

        this.mqttConfig = mqttConfig;
    }

    public void init() {
        try {
            onem2mMqttIotdmPlugin = new Onem2mMqttIotdmPlugin(new Onem2mProtocolRxHandler(),
                                                              new Onem2mMqttRxRequestFactory(),
                                                              onem2mService, this.mqttConfig);
            onem2mMqttIotdmPlugin.start();
        }
        catch (Exception e) {
            LOG.error("Failed to start mqtt server: {}", e);
        }

        if (null != mqttConfig.getMqttClientConfig()) {
            try {
                Onem2mMqttTxClientConfiguration notifierConfig =
                    new Onem2mMqttTxClientConfiguration(
                        mqttConfig.getMqttClientConfig()
                                  .getMqttBrokerIp(),
                        mqttConfig.getMqttClientConfig()
                                  .getMqttBrokerPort());

                this.notifierClient = new Onem2mMqttTxClient(notifierConfig);
                this.notifierClient.start();

                this.notifierReqFactory =
                    new Onem2mMqttNotifierRequestFactory(this.notifierClient, notifierConfig.getPort());
                notifierPlugin = new Onem2mMqttNotifierPlugin(new Onem2mProtocolTxHandler(), this.notifierReqFactory);
                Onem2mNotifierService.getInstance()
                                     .pluginRegistration(notifierPlugin);
            }
            catch (Exception e) {
                LOG.error("Failed to start notifier plugin: {}", e);
            }
        }
        else {
            LOG.info("org.opendaylight.iotdm.onem2m.protocols.mqtt.Onem2mMqttProvider: MQTT client configuration not provided for support of notifications");
        }

        LOG.info("org.opendaylight.iotdm.onem2m.protocols.mqtt.Onem2mMqttProvider instance {}: Initialized", mqttConfig.getMqttProviderInstanceName());
    }

    @Override
    public void close() throws Exception {
        try {
            onem2mMqttIotdmPlugin.close();
        }
        catch (Exception e) {
            LOG.error("Failed to close mqtt plugin: {}", e);
        }

        if (null != mqttConfig.getMqttClientConfig()) {
            try {
                notifierClient.close();
            }
            catch (Exception e) {
                LOG.error("Failed to close MQTT notifier client: {}", e);
            }
        }

        LOG.info("org.opendaylight.iotdm.onem2m.protocols.mqtt.Onem2mMqttProvider instance {}: Closed", mqttConfig.getMqttProviderInstanceName());
    }
}
