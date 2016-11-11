/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2mbundleloader.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mbundleloader.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mbundleloader.rev150105.onem2m.bundle.loader.running.config.output.RunningConfigList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mbundleloader.rev150105.startup.config.content.StartupConfigList;
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
public class Onem2mBundleLoaderRpcRouter implements Onem2mbundleloaderService {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mBundleLoaderRpcRouter.class);

    private final Map<String, Onem2mBundleLoaderProvider> bundleLoaders = new ConcurrentHashMap<>();
    private static Onem2mBundleLoaderRpcRouter instance = null;

    private Onem2mBundleLoaderRpcRouter() {
    }

    public static Onem2mBundleLoaderRpcRouter getInstance() {
        if (null == instance) {
            instance = new Onem2mBundleLoaderRpcRouter();
        }

        return instance;
    }

    /**
     * Registers BundleLoader provider instance.
     * @param instanceName Unique name of the instance
     * @param instance The BundleLoader provider
     * @return True if successful, False otherwise.
     */
    public boolean registerBundleLoader(String instanceName, Onem2mBundleLoaderProvider instance) {
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
    public Future<RpcResult<Onem2mBundleLoaderRunningConfigOutput>> onem2mBundleLoaderRunningConfig(
                                                                           Onem2mBundleLoaderRunningConfigInput input) {
        if (null != input && null != input.getBundleLoaderInstanceName()) {
            // Route the call to the specific provider
            Onem2mBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
            if (null == provider) {
                return this.handleNotFound(input.getBundleLoaderInstanceName());
            }
            return provider.onem2mBundleLoaderRunningConfig(input);
        }

        // Collect outputs from all BundleLoader instances
        List<RunningConfigList> bundleLoadersConfigs = new LinkedList<>();
        for (Map.Entry<String, Onem2mBundleLoaderProvider> entry : bundleLoaders.entrySet()) {
            Onem2mBundleLoaderRunningConfigInputBuilder inputBuilder =
                    new Onem2mBundleLoaderRunningConfigInputBuilder()
                        .setBundleLoaderInstanceName(entry.getKey());
            if (null != input && null != input.getFeatureName()) {
                inputBuilder.setFeatureName(input.getFeatureName());
            }

            Onem2mBundleLoaderRunningConfigOutput output = null;
            try {
                output = entry.getValue().onem2mBundleLoaderRunningConfig(inputBuilder.build()).get().getResult();
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

        Onem2mBundleLoaderRunningConfigOutputBuilder outputBuilder = new Onem2mBundleLoaderRunningConfigOutputBuilder()
            .setRunningConfigList(bundleLoadersConfigs);

        return RpcResultBuilder.success(outputBuilder).buildFuture();
    }

    @Override
    public Future<RpcResult<Onem2mBundleLoaderFeatureRemoveOutput>> onem2mBundleLoaderFeatureRemove(
                                                                        Onem2mBundleLoaderFeatureRemoveInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        Onem2mBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }

        return provider.onem2mBundleLoaderFeatureRemove(input);
    }

    @Override
    public Future<RpcResult<Onem2mBundleLoaderCleanOutput>> onem2mBundleLoaderClean(
                                                                               Onem2mBundleLoaderCleanInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        Onem2mBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }
        return provider.onem2mBundleLoaderClean(input);
    }

    @Override
    public Future<RpcResult<Onem2mBundleLoaderFeaturePutOutput>> onem2mBundleLoaderFeaturePut(
                                                                          Onem2mBundleLoaderFeaturePutInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        Onem2mBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }
        return provider.onem2mBundleLoaderFeaturePut(input);
    }

    @Override
    public Future<RpcResult<Onem2mBundleLoaderFeatureReloadOutput>> onem2mBundleLoaderFeatureReload(
                                                                          Onem2mBundleLoaderFeatureReloadInput input) {
        // BundleLoader instance must be specified
        if (null == input) {
            return this.handleNoInput();
        }
        if (null == input.getBundleLoaderInstanceName()) {
            return this.handleNoBundleLoaderName();
        }

        Onem2mBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
        if (null == provider) {
            return this.handleNotFound(input.getBundleLoaderInstanceName());
        }
        return provider.onem2mBundleLoaderFeatureReload(input);
    }

    @Override
    public Future<RpcResult<Onem2mBundleLoaderStartupConfigOutput>> onem2mBundleLoaderStartupConfig(
                                                                           Onem2mBundleLoaderStartupConfigInput input) {

        if (null != input && null != input.getBundleLoaderInstanceName()) {
            // Route the call to the specific provider
            Onem2mBundleLoaderProvider provider = this.bundleLoaders.get(input.getBundleLoaderInstanceName());
            if (null == provider) {
                return this.handleNotFound(input.getBundleLoaderInstanceName());
            }

            return provider.onem2mBundleLoaderStartupConfig(input);
        }

        // Collect outputs from all BundleLoader instances
        List<StartupConfigList> bundleLoadersConfigs = new LinkedList<>();
        for (Map.Entry<String, Onem2mBundleLoaderProvider> entry : bundleLoaders.entrySet()) {
            Onem2mBundleLoaderStartupConfigInputBuilder inputBuilder =
                    new Onem2mBundleLoaderStartupConfigInputBuilder()
                            .setBundleLoaderInstanceName(entry.getKey());
            if (null != input && null != input.getFeatureName()) {
                inputBuilder.setFeatureName(input.getFeatureName());
            }

            Onem2mBundleLoaderStartupConfigOutput output = null;
            try {
                output = entry.getValue().onem2mBundleLoaderStartupConfig(inputBuilder.build()).get().getResult();
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

        Onem2mBundleLoaderStartupConfigOutputBuilder outputBuilder =
                new Onem2mBundleLoaderStartupConfigOutputBuilder()
                        .setStartupConfigList(bundleLoadersConfigs);

        return RpcResultBuilder.success(outputBuilder).buildFuture();
    }
}
