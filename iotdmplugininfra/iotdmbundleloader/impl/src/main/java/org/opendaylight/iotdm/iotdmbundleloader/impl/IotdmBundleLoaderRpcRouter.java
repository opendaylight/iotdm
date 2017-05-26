/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.iotdmbundleloader.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.CleanInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.CleanOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.FeaturePutInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.FeaturePutOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.FeatureReloadInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.FeatureReloadOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.FeatureRemoveInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.FeatureRemoveOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.IotdmbundleloaderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.RunningConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.RunningConfigInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.RunningConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.RunningConfigOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.StartupConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.StartupConfigInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.StartupConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.StartupConfigOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.RunningConfigList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.startup.config.content.StartupConfigList;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Implements routing of RPC calls between multiple instances of BundleLoader provider.
 * Instance of this class is registered as implementation of the BundleLoader RPC services in the
 * blueprint XML file of this module.
 */
public class IotdmBundleLoaderRpcRouter implements IotdmbundleloaderService {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmBundleLoaderRpcRouter.class);

    private final Map<String, IotdmBundleLoaderProvider> bundleLoaders = new ConcurrentHashMap<>();
    private static IotdmBundleLoaderRpcRouter instance = null;

    private IotdmBundleLoaderRpcRouter() {
    }

    public static IotdmBundleLoaderRpcRouter getInstance() {
        if (null == instance) {
            instance = new IotdmBundleLoaderRpcRouter();
        }

        return instance;
    }

    /**
     * Registers BundleLoader provider instance.
     * @param instanceName Unique name of the instance
     * @param instance The BundleLoader provider
     * @return True if successful, False otherwise.
     */
    public boolean registerBundleLoader(String instanceName, IotdmBundleLoaderProvider instance) {
        if (bundleLoaders.containsKey(instanceName)) {
            LOG.error("BundleLoader: {} already registered in RPC router", instanceName);
            return false;
        }

        bundleLoaders.put(instanceName, instance);
        return true;
    }

    /**
     * Unregisters BundleLoader provider instance.
     * @param instanceName Name of the provider instance
     */
    public void unregisterBundleLoader(String instanceName) {
        bundleLoaders.remove(instanceName);
    }

    private <T> Future<RpcResult<T>> handleNoInput() {
        LOG.error("RPC called without input");
        return RpcResultBuilder
                       .<T> failed()
                       .withError(RpcError.ErrorType.APPLICATION,
                                  "RPC input is mandatory.")
                       .buildFuture();
    }

    private <T> Future<RpcResult<T>> handleNoBundleLoaderName() {
        LOG.error("RPC call without mandatory BundleLoader name");
        return RpcResultBuilder
                       .<T> failed()
                       .withError(RpcError.ErrorType.APPLICATION,
                                  "BundleLoader instance name not specified.")
                       .buildFuture();
    }

    private <T> Future<RpcResult<T>> handleNotFound(String bundleLoaderInstanceName) {
        LOG.error("RPC called for BundleLoader: {} which doesn't exist", bundleLoaderInstanceName);
        return RpcResultBuilder
                       .<T> failed()
                       .withError(RpcError.ErrorType.APPLICATION,
                                  "BundleLoader: " + bundleLoaderInstanceName +
                                  " does not exist.")
                       .buildFuture();
    }

    @Override
    public Future<RpcResult<RunningConfigOutput>> runningConfig(RunningConfigInput input) {
        if (null != input && null != input.getBundleLoaderInstanceName()) {
            // Route the call to the specific provider
            IotdmBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
            if (null == provider) {
                return this.handleNotFound(input.getBundleLoaderInstanceName());
            }
            return provider.runningConfig(input);
        }

        // Collect outputs from all BundleLoader instances
        List<RunningConfigList> bundleLoadersConfigs = new LinkedList<>();
        for (Map.Entry<String, IotdmBundleLoaderProvider> entry : bundleLoaders.entrySet()) {
            RunningConfigInputBuilder inputBuilder =
                new RunningConfigInputBuilder().setBundleLoaderInstanceName(entry.getKey());
            if (null != input && null != input.getFeatureName()) {
                inputBuilder.setFeatureName(input.getFeatureName());
            }

            RunningConfigOutput output = null;
            try {
                output = entry.getValue().runningConfig(inputBuilder.build()).get().getResult();
            } catch (Exception e) {
                LOG.error("Failed to get running config of BundleLoader: {}", entry.getKey());
                continue;
            }

            if ((null != output) &&
                (null != output.getRunningConfigList()) &&
                (! output.getRunningConfigList().isEmpty())) {
                // List includes only one item for the specific BundleLoader
                bundleLoadersConfigs.add(output.getRunningConfigList().get(0));
            }
        }

        RunningConfigOutputBuilder outputBuilder = new RunningConfigOutputBuilder()
            .setRunningConfigList(bundleLoadersConfigs);

        return RpcResultBuilder.success(outputBuilder).buildFuture();
    }

    @Override
    public Future<RpcResult<FeatureRemoveOutput>> featureRemove(FeatureRemoveInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        IotdmBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }

        return provider.featureRemove(input);
    }

    @Override
    public Future<RpcResult<CleanOutput>> clean(CleanInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        IotdmBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }
        return provider.clean(input);
    }

    @Override
    public Future<RpcResult<FeaturePutOutput>> featurePut(FeaturePutInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        IotdmBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }
        return provider.featurePut(input);
    }

    @Override
    public Future<RpcResult<FeatureReloadOutput>> featureReload(FeatureReloadInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        IotdmBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }
        return provider.featureReload(input);
    }

    @Override
    public Future<RpcResult<StartupConfigOutput>> startupConfig(StartupConfigInput input) {

        if (null != input && null != input.getBundleLoaderInstanceName()) {
            // Route the call to the specific provider
            IotdmBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
            if (null == provider) {
                return this.handleNotFound(input.getBundleLoaderInstanceName());
            }

            return provider.startupConfig(input);
        }

        // Collect outputs from all BundleLoader instances
        List<StartupConfigList> bundleLoadersConfigs = new LinkedList<>();
        for (Map.Entry<String, IotdmBundleLoaderProvider> entry : bundleLoaders.entrySet()) {
            StartupConfigInputBuilder inputBuilder =
                    new StartupConfigInputBuilder()
                            .setBundleLoaderInstanceName(entry.getKey());
            if (null != input && null != input.getFeatureName()) {
                inputBuilder.setFeatureName(input.getFeatureName());
            }

            StartupConfigOutput output = null;
            try {
                output = entry.getValue().startupConfig(inputBuilder.build()).get().getResult();
            } catch (Exception e) {
                LOG.error("Failed to get startup config of BundleLoader: {}", entry.getKey());
                continue;
            }

            if ((null != output) &&
                (null != output.getStartupConfigList()) &&
                (! output.getStartupConfigList().isEmpty())) {
                // List includes only one item for the specific BundleLoader
                bundleLoadersConfigs.add(output.getStartupConfigList().get(0));
            }
        }

        StartupConfigOutputBuilder outputBuilder =
                new StartupConfigOutputBuilder().setStartupConfigList(bundleLoadersConfigs);

        return RpcResultBuilder.success(outputBuilder).buildFuture();
    }
}
