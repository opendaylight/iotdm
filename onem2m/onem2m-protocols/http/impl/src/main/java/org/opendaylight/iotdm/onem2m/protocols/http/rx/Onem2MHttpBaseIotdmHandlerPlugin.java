/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.rx;

import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpChannelProviderService;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpDescriptorBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpPlugin;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpRequest;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpResponse;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpsConfigBuilder;
import org.opendaylight.iotdm.onem2m.commchannels.http.api.Onem2mHttpsDescriptorBuilder;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mRxRequestAbstractFactory;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors.IotdmRxDescriptorApplication;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurable;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.PluginSpecificConfiguration;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.HttpProtocolProviderConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev170110.iotdm.plugin.manager.plugin.data.output.iotdm.plugin.manager.plugins.table.iotdm.plugin.manager.plugin.instances.plugin.configuration.plugin.specific.configuration.Onem2mHttpHttpsConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2MHttpBaseIotdmHandlerPlugin
    implements Onem2mHttpPlugin, IotdmPluginConfigurable, Onem2mProtocolRxChannel {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2MHttpBaseIotdmHandlerPlugin.class);
    private final String pluginName;

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,Onem2mHttpRequest,Onem2mHttpResponse>
        requestFactory;
    private final Onem2mService onem2mService;
    private final HttpProtocolProviderConfig pluginConfig;

    private SecurityLevel securityLevel = SecurityLevel.L0;
    protected final Onem2mHttpChannelProviderService channelProviderService;
    protected IotdmRxDescriptorApplication descriptorInUse = null;

    public Onem2MHttpBaseIotdmHandlerPlugin(
                            @Nonnull final Onem2mProtocolRxHandler requestHandler,
                            @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,
                                                         Onem2mHttpRequest,
                                                         Onem2mHttpResponse> requestFactory,
                            @Nonnull final Onem2mService onem2mService,
                            @Nonnull final HttpProtocolProviderConfig configuration,
                            final Onem2mHttpChannelProviderService channelProviderService) {

        this(requestHandler, requestFactory, onem2mService, configuration, "http(s)-base",
             channelProviderService);
    }

    public Onem2MHttpBaseIotdmHandlerPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                            @Nonnull final Onem2mRxRequestAbstractFactory<Onem2mHttpRxRequest,Onem2mHttpRequest,Onem2mHttpResponse> requestFactory,
                                            @Nonnull final Onem2mService onem2mService,
                                            @Nonnull final HttpProtocolProviderConfig configuration,
                                            @Nonnull final String pluginName,
                                            final Onem2mHttpChannelProviderService channelProviderService) {
        if (null == configuration) {
            throw new IllegalArgumentException("No configuration passed for HTTP base plugin");
        }
        this.pluginConfig = configuration;

        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
        this.pluginName = pluginName;
        this.channelProviderService = channelProviderService;
    }

    @Override
    public String getPluginName() {
        return this.pluginName;
    }

    @Override
    public void start() throws RuntimeException {
        this.securityLevel = pluginConfig.getServerConfig().getServerSecurityLevel();
        // Check whether HTTP or HTTPS is used
        if ((null != pluginConfig.getServerConfig().isSecureConnection()) &&
            (true == pluginConfig.getServerConfig().isSecureConnection())) {
            Onem2mHttpsConfigBuilder cfgBuilder = new Onem2mHttpsConfigBuilder();

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
                // TODO need to change this
                Onem2mHttpsDescriptorBuilder builder = this.channelProviderService.getHttpsDescriptorBuilder();
                builder.setPortNumber(pluginConfig.getServerConfig().getServerPort().getValue());
                builder.setModeExclusive();
                builder.setEndpoint("/");
                builder.setConfiguration(cfgBuilder.build());

                this.descriptorInUse = builder.build();
                this.channelProviderService.registerPluginInManager(this, this.descriptorInUse);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to start HTTPS Base IoTDM plugin: {}", e);
                return;
            }

            LOG.info("Started HTTPS Base IoTDM plugin at port: {}, security level: {}",
                     pluginConfig.getServerConfig().getServerPort().getValue(),
                     pluginConfig.getServerConfig().getServerSecurityLevel());
        } else {
            try {
                // TODO need to change this
                Onem2mHttpDescriptorBuilder builder = this.channelProviderService.getHttpDescriptorBuilder();
                builder.setPortNumber(pluginConfig.getServerConfig().getServerPort().getValue());
                builder.setModeExclusive();
                builder.setEndpoint("/");

                this.descriptorInUse = builder.build();
                this.channelProviderService.registerPluginInManager(this, this.descriptorInUse);
            } catch (IotdmPluginRegistrationException e) {
                LOG.error("Failed to start HTTP Base IoTDM plugin: {}", e);
                return;
            }

            LOG.info("Started HTTP Base IoTDM plugin at port: {}, security level: {}",
                     pluginConfig.getServerConfig().getServerPort().getValue(),
                     pluginConfig.getServerConfig().getServerSecurityLevel());
        }
    }

    @Override
    public void close() {
        IotdmPluginManager mgr = IotdmPluginManager.getInstance();

        this.channelProviderService.deregisterPluginInManager(this, this.descriptorInUse);
        this.descriptorInUse = null;

        LOG.info("Closed HTTP Base IoTDM plugin at port: {}, security level: {}",
                 pluginConfig.getServerConfig().getServerPort().getValue(),
                 pluginConfig.getServerConfig().getServerSecurityLevel());
    }

    @Override
    public void handle(Onem2mHttpRequest request, Onem2mHttpResponse response) {
        // TODO: how to remove the type casting ?
        Onem2mHttpRxRequest rxRequest =
                requestFactory.createRxRequest(request,
                                                   response,
                                                   onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }

    @Override
    public PluginSpecificConfiguration getRunningConfig() {
        return new Onem2mHttpHttpsConfigBuilder()
            .setHttpsConfig(pluginConfig.getHttpsConfig())
            .setServerConfig(pluginConfig.getServerConfig())
            .setNotifierPluginConfig(pluginConfig.getNotifierPluginConfig())
            .setRouterPluginConfig(pluginConfig.getRouterPluginConfig())
            .build();
    }
}
