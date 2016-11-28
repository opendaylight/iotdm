/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.rx;

import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmHttpsConfigBuilder;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmPluginHttpRequest;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.IotdmPluginHttpResponse;
import org.opendaylight.iotdm.onem2m.plugins.channels.http.Onem2mHttpsPluginServer;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.iotdm.onem2m.protocols.http.Onem2mHttpSecureConnectionConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mHttpBaseIotdmPlugin implements IotdmPlugin<IotdmPluginHttpRequest, IotdmPluginHttpResponse>,
                                                  Onem2mProtocolRxChannel<Onem2mHttpBaseIotdmPluginConfig> {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpBaseIotdmPlugin.class);

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,IotdmPluginHttpRequest,IotdmPluginHttpResponse> requestFactory;
    private final Onem2mService onem2mService;
    private final Onem2mHttpSecureConnectionConfig secConfig;

    private SecurityLevel securityLevel = SecurityLevel.L0;
    private ServerConfig currentConfig = null;

    public Onem2mHttpBaseIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                     @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,IotdmPluginHttpRequest,IotdmPluginHttpResponse> requestFactory,
                                     @Nonnull final Onem2mService onem2mService,
                                     final Onem2mHttpSecureConnectionConfig secConfig) {
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
        this.secConfig = secConfig;
    }

    @Override
    public String getPluginName() {
        return "http(s)-base";
    }

    @Override
    public void start(Onem2mHttpBaseIotdmPluginConfig configuration)
            throws IllegalArgumentException, RuntimeException {
        if (null == configuration) {
            throw new IllegalArgumentException("Starting Http base server without configuration");
        }

        this.currentConfig = configuration;
        this.securityLevel = configuration.getServerSecurityLevel();

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

        // Check whether HTTP or HTTPS is used
        if ((null != configuration.getSecureConnection()) && (true == configuration.getSecureConnection())) {
            IotdmHttpsConfigBuilder cfgBuilder = IotdmPluginConfigurationBuilderFactory.getNewHttpsConfigBuilder();

            if (null == this.secConfig || null == this.secConfig.getKeyStoreConfig()) {
                // Use HTTPS server with default configuration
                cfgBuilder.setUseDefault(true);
            } else {
                // Prepare custom configuration for HTTPS server
                cfgBuilder
                    .setKeyStoreFile(this.secConfig.getKeyStoreConfig().getKeyStoreFile())
                    .setKeyStorePassword(this.secConfig.getKeyStoreConfig().getKeyStorePassword())
                    .setKeyManagerPassword(this.secConfig.getKeyStoreConfig().getKeyManagerPassword());
            }

            cfgBuilder.verify();
            try {
                mgr.registerPluginHttps(this, configuration.getServerPort(), Onem2mPluginManager.Mode.Exclusive,
                                        null, cfgBuilder);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to start HTTPS Base IoTDM plugin: {}", e);
                return;
            }

            LOG.info("Started HTTPS Base IoTDM plugin at port: {}, security level: {}",
                     configuration.getServerPort(), configuration.getServerSecurityLevel());
        } else {
            try {
                mgr.registerPluginHttp(this, configuration.getServerPort(), Onem2mPluginManager.Mode.Exclusive, null);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to start HTTP Base IoTDM plugin: {}", e);
                return;
            }

            LOG.info("Started HTTP Base IoTDM plugin at port: {}, security level: {}",
                     configuration.getServerPort(), configuration.getServerSecurityLevel());
        }
    }

    @Override
    public void close() {
        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.unregisterIotdmPlugin(this);

        LOG.info("Closed HTTP Base IoTDM plugin at port: {}, security level: {}",
                 currentConfig.getServerPort(), currentConfig.getServerSecurityLevel());
    }

    @Override
    public void handle(IotdmPluginHttpRequest request, IotdmPluginHttpResponse response) {
        // TODO: how to remove the type casting ?
        Onem2mHttpRxRequest rxRequest =
                requestFactory.createRxRequest(request,
                                                   response,
                                                   onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }
}
