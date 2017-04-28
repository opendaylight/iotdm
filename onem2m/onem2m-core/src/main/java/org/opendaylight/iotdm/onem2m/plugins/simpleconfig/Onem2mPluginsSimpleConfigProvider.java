/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.simpleconfig;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.key.val.list.def.KeyValList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.onem2m.simple.config.list.definition.Onem2mSimpleConfigList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2msimpleconfig.rev161122.plugin.simple.config.definition.PluginSimpleConfig;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implementation of SimpleConfig RPC services.
 */
public class Onem2mPluginsSimpleConfigProvider implements Onem2mSimpleConfigService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPluginsSimpleConfigProvider.class);
    private final DataBroker dataBroker;

    public Onem2mPluginsSimpleConfigProvider (final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        Onem2mPluginsSimpleConfigManager.getInstance().setDataBroker(dataBroker);
    }

    @Override
    public void close() {
        Onem2mPluginsSimpleConfigManager.getInstance().unsetDataBroker();
    }

    private <Tout> Future<RpcResult<Tout>> handleRpcError(String format, String... args) {
        String msg = MessageFormatter.arrayFormat(format, args).getMessage();
        LOG.error("SimpleConfigManager RPC error: {}", msg);

        return RpcResultBuilder
                       .<Tout>failed()
                       .withError(RpcError.ErrorType.APPLICATION, msg)
                       .buildFuture();
    }

    private <Terr> Future<RpcResult<Terr>> configureNewCfg(String pluginName, String instanceId,
                                                           IotdmSimpleConfig newConfig) {
        try {
            Onem2mPluginsSimpleConfigManager.getInstance().configurePluginInstance(
                    pluginName, instanceId,
                    newConfig);
        } catch (IotdmPluginSimpleConfigException e) {
            return this.handleRpcError(e.getMessage());
        } catch (Exception e) {
            LOG.error("Internal error: {}", e);
            return this.handleRpcError(
                    "Configuration change of plugin instance failed for pluginName: {}, instanceName: {}",
                    pluginName, instanceId);
        }

        return null;
    }

    private <Terr, Tinput> Future<RpcResult<Terr>> checkInput(final Tinput input,
                                                              final String pluginName, final String instanceName) {
        if (null == input) {
            return this.handleRpcError("Mandatory input not provided");
        }

        if (! Onem2mPluginsSimpleConfigManager.getInstance().isRegistered(pluginName,
                                                                          instanceName)) {
            return this.handleRpcError("No such plugin registered: pluginName: {}, instanceId: {}",
                                       pluginName, instanceName);
        }

        return null;
    }

    /*
     * Configuration RPCs
     */
    @Override
    public Future<RpcResult<IpluginCfgPutOutput>> ipluginCfgPut(IpluginCfgPutInput input) {
        Future<RpcResult<IpluginCfgPutOutput>> errOut = checkInput(input,
                                                                   input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        IotdmSimpleConfig cfg = new IotdmSimpleConfig(input.getPluginSimpleConfig());
        errOut = configureNewCfg(input.getPluginName(), input.getInstanceName(), cfg);
        if (null != errOut) {
            return errOut;
        }

        IpluginCfgPutOutputBuilder output = new IpluginCfgPutOutputBuilder()
            .setPluginName(input.getPluginName())
            .setInstanceName(input.getInstanceName())
            .setPluginSimpleConfig(input.getPluginSimpleConfig());

        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgDelOutput>> ipluginCfgDel(IpluginCfgDelInput input) {
        Future<RpcResult<IpluginCfgDelOutput>> errOut = checkInput(input,
                                                                   input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        errOut = configureNewCfg(input.getPluginName(), input.getInstanceName(),null);
        if (null != errOut) {
            return errOut;
        }

        IpluginCfgDelOutputBuilder output = new IpluginCfgDelOutputBuilder();
        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgGetStartupOutput>> ipluginCfgGetStartup(IpluginCfgGetStartupInput input) {
        Future<RpcResult<IpluginCfgGetStartupOutput>> errOut =
            checkInput(input, input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        PluginSimpleConfig instanceConfig = null;
        try {
            instanceConfig = Onem2mPluginsSimpleConfigManager.getInstance().getInstanceStartupConfig(
                    input.getPluginName(), input.getInstanceName());
        } catch (IotdmPluginSimpleConfigException e) {
            return this.handleRpcError(e.toString());
        }

        IpluginCfgGetStartupOutputBuilder output = new IpluginCfgGetStartupOutputBuilder()
               .setInstanceName(input.getInstanceName())
               .setPluginName(input.getPluginName())
               .setPluginSimpleConfig(instanceConfig);

        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    private <Terr> Optional<IotdmSimpleConfig> getCurrCfg(String pluginName, String instanceId,
                                                AtomicReference<Future<RpcResult<Terr>>> errOutReference) {
        IotdmSimpleConfig currentCfg = null;
        try {
            return Optional.fromNullable(
                        Onem2mPluginsSimpleConfigManager.getInstance().getConfig(pluginName, instanceId));
        } catch (IotdmPluginSimpleConfigException e) {
            errOutReference.set(this.handleRpcError(e.getMessage()));
            return null;
        } catch (Exception e) {
            LOG.error("Internal error: {}", e);
            errOutReference.set(this.handleRpcError(
                    "Failed to get current configuration of plugin instance for pluginName: {}, instanceName: {}",
                    pluginName, instanceId));
            return null;
        }
    }

    @Override
    public Future<RpcResult<IpluginCfgGetOutput>> ipluginCfgGet(IpluginCfgGetInput input) {
        Future<RpcResult<IpluginCfgGetOutput>> errOut = checkInput(input,
                                                                   input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        IotdmSimpleConfig cfg = null;
        AtomicReference<Future<RpcResult<IpluginCfgGetOutput>>> errOutReference = new AtomicReference<>();

        Optional<IotdmSimpleConfig> optCfg = getCurrCfg(input.getPluginName(), input.getInstanceName(),
                                                        errOutReference);
        if (null == optCfg) {
            return errOutReference.get();
        }

        if (optCfg.isPresent()) {
            cfg = optCfg.get();
        }

        IpluginCfgGetOutputBuilder output = new IpluginCfgGetOutputBuilder()
                                                    .setPluginName(input.getPluginName())
                                                    .setInstanceName(input.getInstanceName())
                                                    .setPluginSimpleConfig(cfg == null ? null : cfg.getConfiguration());
        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }


    private <Terr> IotdmSimpleConfigBuilder getNewCfgBuilder(String pluginName, String instanceId,
                                                             AtomicReference<Future<RpcResult<Terr>>> errOutReference) {
        IotdmSimpleConfig currentCfg = null;
        IotdmSimpleConfigBuilder newCfgBuilder = null;

        Optional<IotdmSimpleConfig> optCfg = getCurrCfg(pluginName, instanceId, errOutReference);
        if (null == optCfg) {
            return null;
        }

        if (optCfg.isPresent()) {
            currentCfg = optCfg.get();
        }

        if (null == currentCfg) {
            newCfgBuilder = new IotdmSimpleConfigBuilder();
        } else {
            newCfgBuilder = new IotdmSimpleConfigBuilder(currentCfg.getConfiguration());
        }

        return newCfgBuilder;
    }

    @Override
    public Future<RpcResult<IpluginCfgKeyPutOutput>> ipluginCfgKeyPut(IpluginCfgKeyPutInput input) {
        Future<RpcResult<IpluginCfgKeyPutOutput>> errOut = checkInput(input,
                                                                   input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        IotdmSimpleConfig currentCfg = null;
        AtomicReference<Future<RpcResult<IpluginCfgKeyPutOutput>>> errReference = new AtomicReference<>();
        IotdmSimpleConfigBuilder newCfgBuilder = getNewCfgBuilder(input.getPluginName(), input.getInstanceName(),
                                                                  errReference);

        if (null == newCfgBuilder) {
            return errReference.get();
        }

        newCfgBuilder.setVal(input.getCfgKey(), input.getCfgVal());
        IotdmSimpleConfig newCfg = newCfgBuilder.build();

         errOut = this.configureNewCfg(input.getPluginName(), input.getInstanceName(), newCfg);
        if (null != errOut) {
            return errOut;
        }

        IpluginCfgKeyPutOutputBuilder output = new IpluginCfgKeyPutOutputBuilder()
                                                    .setPluginName(input.getPluginName())
                                                    .setInstanceName(input.getInstanceName())
                                                    .setPluginSimpleConfig(newCfg.getConfiguration());
        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgKeyGetStartupOutput>> ipluginCfgKeyGetStartup(
                                                                            IpluginCfgKeyGetStartupInput input) {
        Future<RpcResult<IpluginCfgKeyGetStartupOutput>> errOut =
            checkInput(input, input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        PluginSimpleConfig instanceConfig = null;
        try {
            instanceConfig = Onem2mPluginsSimpleConfigManager.getInstance().getInstanceStartupConfig(
                                                                input.getPluginName(), input.getInstanceName());
        } catch (IotdmPluginSimpleConfigException e) {
            return this.handleRpcError(e.toString());
        }

        String value = null;
        if (null != instanceConfig.getKeyValList()) {
            for (KeyValList kv : instanceConfig.getKeyValList()) {
                if (kv.getCfgKey().equals(input.getCfgKey())) {
                    value = kv.getCfgVal();
                    break;
                }
            }
        }

        if (null == value) {
            return this.handleRpcError("No such key found: PluginName: {}, InstanceName: {}, Key: {}",
                                       input.getPluginName(), input.getInstanceName(), input.getCfgKey());
        }

        IpluginCfgKeyGetStartupOutputBuilder output = new IpluginCfgKeyGetStartupOutputBuilder()
                .setInstanceName(input.getInstanceName())
                .setPluginName(input.getPluginName())
                .setCfgKey(input.getCfgKey())
                .setCfgVal(value);

        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgKeyDelOutput>> ipluginCfgKeyDel(IpluginCfgKeyDelInput input) {
        Future<RpcResult<IpluginCfgKeyDelOutput>> errOut = checkInput(input,
                                                                      input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        AtomicReference<Future<RpcResult<IpluginCfgKeyDelOutput>>> errReference = new AtomicReference<>();
        IotdmSimpleConfigBuilder newCfgBuilder = getNewCfgBuilder(input.getPluginName(), input.getInstanceName(),
                                                                  errReference);
        if (null == newCfgBuilder) {
            return errReference.get();
        }

        String currVal = newCfgBuilder.getVal(input.getCfgKey());
        if (null == currVal || currVal.isEmpty()) {
            return this.handleRpcError("No such key found: PluginName: {}, InstanceName: {}, Key: {}",
                                       input.getPluginName(), input.getInstanceName(), input.getCfgKey());
        }

        newCfgBuilder.delVal(input.getCfgKey());
        IotdmSimpleConfig newCfg = newCfgBuilder.build();

        errOut = this.configureNewCfg(input.getPluginName(), input.getInstanceName(), newCfg);
        if (null != errOut) {
            return errOut;
        }

        IpluginCfgKeyDelOutputBuilder output = new IpluginCfgKeyDelOutputBuilder();
        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgKeyGetOutput>> ipluginCfgKeyGet(IpluginCfgKeyGetInput input) {
        Future<RpcResult<IpluginCfgKeyGetOutput>> errOut = checkInput(input,
                                                                   input.getPluginName(), input.getInstanceName());
        if (null != errOut) {
            return errOut;
        }

        AtomicReference<Future<RpcResult<IpluginCfgKeyGetOutput>>> errReference = new AtomicReference<>();
        Optional<IotdmSimpleConfig> optCfg = getCurrCfg(input.getPluginName(),
                                                        input.getInstanceName(), errReference);
        if (null == optCfg) {
            return errReference.get();
        }

        IpluginCfgKeyGetOutputBuilder output = new IpluginCfgKeyGetOutputBuilder()
                .setPluginName(input.getPluginName())
                .setInstanceName(input.getInstanceName());

        if (optCfg.isPresent()) {
            String value = optCfg.get().getVal(input.getCfgKey());
            if (null == value || value.isEmpty()) {
                return this.handleRpcError("No such key found: PluginName: {}, InstanceName: {}, Key: {}",
                                           input.getPluginName(), input.getInstanceName(), input.getCfgKey());
            }
            output
                .setCfgKey(input.getCfgKey())
                .setCfgVal(value);
        }

        return RpcResultBuilder
                       .success(output.build())
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgGetStartupConfigOutput>> ipluginCfgGetStartupConfig(
                                                                              IpluginCfgGetStartupConfigInput input) {
        IpluginCfgGetStartupConfigOutputBuilder outBuilder = new IpluginCfgGetStartupConfigOutputBuilder();
        List<Onem2mSimpleConfigList> startupConfig = null;

        try {
            startupConfig = Onem2mPluginsSimpleConfigManager.getInstance()
                                .getStartupConfig((null == input) ? null : input.getPluginName());
        } catch (IotdmPluginSimpleConfigException e) {
            return this.handleRpcError(e.toString());
        }

        if (null == startupConfig) {
            LOG.info("Startup config is empty");
            return RpcResultBuilder.success(outBuilder.build()).buildFuture();
        }

        outBuilder.setOnem2mSimpleConfigList(startupConfig);
        return RpcResultBuilder.success(outBuilder.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<IpluginCfgGetRunningConfigOutput>> ipluginCfgGetRunningConfig(
                                                                              IpluginCfgGetRunningConfigInput input) {
        IpluginCfgGetRunningConfigOutputBuilder outBuilder = new IpluginCfgGetRunningConfigOutputBuilder();
        try {
            outBuilder.setOnem2mSimpleConfigList(
                 Onem2mPluginsSimpleConfigManager.getInstance().getRunningConfig(
                         (null == input) ? null : input.getPluginName()));
        } catch (IotdmPluginSimpleConfigException e) {
            return this.handleRpcError(e.toString());
        }

        return RpcResultBuilder.success(outBuilder.build()).buildFuture();
    }
}
