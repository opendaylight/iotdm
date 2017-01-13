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
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.plugin.configuration.PluginSpecificConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.HttpProtocolProviderConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.onem2m.plugin.manager.plugin.data.output.onem2m.plugin.manager.plugins.table.onem2m.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.HttpHttpsConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mHttpBaseIotdmPlugin implements IotdmPlugin<IotdmPluginHttpRequest, IotdmPluginHttpResponse>,
                                                  IotdmPluginConfigurable,
                                                  Onem2mProtocolRxChannel {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpBaseIotdmPlugin.class);
    private final String pluginName;

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,IotdmPluginHttpRequest,IotdmPluginHttpResponse> requestFactory;
    private final Onem2mService onem2mService;
    private final HttpProtocolProviderConfig pluginConfig;

    private SecurityLevel securityLevel = SecurityLevel.L0;

    public Onem2mHttpBaseIotdmPlugin(
                                 @Nonnull final Onem2mProtocolRxHandler requestHandler,
                                 @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,
                                                                               IotdmPluginHttpRequest,
                                                                               IotdmPluginHttpResponse> requestFactory,
                                 @Nonnull final Onem2mService onem2mService,
                                 @Nonnull final HttpProtocolProviderConfig configuration) {
        this(requestHandler, requestFactory, onem2mService, configuration, "http(s)-base");
    }

    public Onem2mHttpBaseIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                     @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,IotdmPluginHttpRequest,IotdmPluginHttpResponse> requestFactory,
                                     @Nonnull final Onem2mService onem2mService,
                                     @Nonnull final HttpProtocolProviderConfig configuration,
                                     @Nonnull final String pluginName) {
        if (null == configuration) {
            throw new IllegalArgumentException("No configuration passed for HTTP base plugin");
        }
        this.pluginConfig = configuration;

        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
        this.pluginName = pluginName;
    }

    @Override
    public String getPluginName() {
        return this.pluginName;
    }

    @Override
    public void start() throws RuntimeException {
        this.securityLevel = pluginConfig.getServerConfig().getServerSecurityLevel();

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

        // Check whether HTTP or HTTPS is used
        if ((null != pluginConfig.getServerConfig().isSecureConnection()) &&
            (true == pluginConfig.getServerConfig().isSecureConnection())) {
            IotdmHttpsConfigBuilder cfgBuilder = IotdmPluginConfigurationBuilderFactory.getNewHttpsConfigBuilder();

            if (null == this.pluginConfig.getHttpsConfig() ||
                null == this.pluginConfig.getHttpsConfig().getKeyStoreConfig()) {
                // Use HTTPS server with default configuration
                cfgBuilder.setUseDefault(true);
            } else {
                // Prepare custom configuration for HTTPS server
                cfgBuilder
                    .setKeyStoreFile(this.pluginConfig.getHttpsConfig().getKeyStoreConfig().getKeyStoreFile())
                    .setKeyStorePassword(this.pluginConfig.getHttpsConfig().getKeyStoreConfig().getKeyStorePassword())
                    .setKeyManagerPassword(
                        this.pluginConfig.getHttpsConfig().getKeyStoreConfig().getKeyManagerPassword());
            }

            cfgBuilder.verify();
            try {
                mgr.registerPluginHttps(this, pluginConfig.getServerConfig().getServerPort(),
                                        Onem2mPluginManager.Mode.Exclusive,
                                        null, cfgBuilder);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to start HTTPS Base IoTDM plugin: {}", e);
                return;
            }

            LOG.info("Started HTTPS Base IoTDM plugin at port: {}, security level: {}",
                     pluginConfig.getServerConfig().getServerPort(),
                     pluginConfig.getServerConfig().getServerSecurityLevel());
        } else {
            try {
                mgr.registerPluginHttp(this, pluginConfig.getServerConfig().getServerPort(),
                                       Onem2mPluginManager.Mode.Exclusive, null);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to start HTTP Base IoTDM plugin: {}", e);
                return;
            }

            LOG.info("Started HTTP Base IoTDM plugin at port: {}, security level: {}",
                     pluginConfig.getServerConfig().getServerPort(),
                     pluginConfig.getServerConfig().getServerSecurityLevel());
        }
    }

    @Override
    public void close() {
        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.unregisterIotdmPlugin(this);

        LOG.info("Closed HTTP Base IoTDM plugin at port: {}, security level: {}",
                 pluginConfig.getServerConfig().getServerPort(),
                 pluginConfig.getServerConfig().getServerSecurityLevel());
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

    @Override
    public PluginSpecificConfiguration getRunningConfig() {
        return new HttpHttpsConfigBuilder()
            .setHttpsConfig(pluginConfig.getHttpsConfig())
            .setServerConfig(pluginConfig.getServerConfig())
            .setNotifierPluginConfig(pluginConfig.getNotifierPluginConfig())
            .setRouterPluginConfig(pluginConfig.getRouterPluginConfig())
            .build();
    }
}
