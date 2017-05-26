/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.iotdmbundleloader.impl;

import org.apache.karaf.bundle.core.BundleInfo;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleState;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginManager;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPlugin;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginLoader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.config.rev161021.BundleLoadersConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.bundle.loader.startup.features.StartupFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.bundle.loader.startup.features.StartupFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.bundle.loader.startup.features.StartupFeaturesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.bundles.to.load.list.BundlesToLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.bundles.to.load.list.BundlesToLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.RunningConfigList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.RunningConfigListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.running.config.list.RunningFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.running.config.list.RunningFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.running.config.list.running.features.RunningFeatureBundles;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.running.config.output.running.config.list.running.features.RunningFeatureBundlesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.startup.config.content.StartupConfigList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.startup.config.content.StartupConfigListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.iotdmbundleloader.rev150105.startup.config.content.StartupConfigListKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of the PluginLoader using OSGi bundles and implementation
 * of RPC services for loading, removing OSGi bundles and retrieve information about
 * current state and persistent configuration.
 */
public class IotdmBundleLoaderProvider implements IotdmPluginLoader, IotdmbundleloaderService {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmBundleLoaderProvider.class);
    private final DataBroker dataBroker;
    private final BundleLoadersConfigs providerConfig;
    private final BundleContext bundleContext;
    protected final String loaderInstanceName;

    // This RWLock avoids concurrent operations with loaded features
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    // <FeatureName, TreeSet<LoadedBundleInfo>>
    protected final Map<String, TreeSet<LoadedBundleInfo>> loadedFeatures = new HashMap<>(); // runningConfig
    protected final BundleService karafService;

    /**
     * Stores data about loaded OSGi bundle.
     */
    class LoadedBundleInfo {
        public final String jarFileLocation;
        public final long priority;
        public final Bundle bundle;

        public LoadedBundleInfo(final String jarFileLocation, final long priority, final Bundle bundle) {
            this.jarFileLocation = jarFileLocation;
            this.priority = priority;
            this.bundle = bundle;
        }
    }

    /**
     * Initialize the provider instance.
     * @param dataBroker MDSAL data broker
     * @param providerConfig Configuration of this provider instance
     * @param bundleContext OSGi bundle context
     */
    public IotdmBundleLoaderProvider(final DataBroker dataBroker,
                                     final BundleLoadersConfigs providerConfig,
                                     final BundleContext bundleContext) {
        if (null == dataBroker || null == providerConfig || null == bundleContext) {
            throw new IllegalArgumentException("No all mandatory parameters passed");
        }

        this.dataBroker = dataBroker;
        this.providerConfig = providerConfig;
        this.bundleContext = bundleContext;
        this.loaderInstanceName = providerConfig.getLoaderInstanceName();

        ServiceReference<BundleService> ref = bundleContext.getServiceReference(BundleService.class);
        karafService = bundleContext.getService(ref);
    }

    @Override
    public String getLoaderName() {
        return this.providerConfig.getLoaderInstanceName();
    }

    @Override
    public boolean hasLoadedPlugin(IotdmPlugin plugin) {
        Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(plugin.getClass());
        if (null != bundle) {
            this.rwLock.readLock().lock();
            try {
                for (TreeSet<LoadedBundleInfo> bundleSet : this.loadedFeatures.values()) {
                    for (LoadedBundleInfo bundleInfo : bundleSet) {
                        if (bundleInfo.bundle == bundle) {
                            return true;
                        }
                    }
                }
            } finally {
                this.rwLock.readLock().unlock();
            }
        }

        return false;
    }

    @Override
    public Future<RpcResult<FeatureRemoveOutput>> featureRemove(FeatureRemoveInput input) {
        this.rwLock.writeLock().lock();
        try {
            if (! this.loadedFeatures.containsKey(input.getFeatureName())) {
                return RpcResultBuilder
                               .<FeatureRemoveOutput> failed()
                               .withError(RpcError.ErrorType.APPLICATION,
                                          "Feature: " + input.getFeatureName() + " not found")
                               .buildFuture();
            }

            // Let's remove the startup config first
            if (! this.delFeatureStartupConfig(input)) {
                LOG.error("BundleLoader: {}, Feature: {}, Failed to remove the startup config",
                          this.loaderInstanceName, input.getFeatureName());
            }

            this.uninstallFeature(input.getFeatureName());
            return RpcResultBuilder.<FeatureRemoveOutput>success().buildFuture();
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    @Override
    public Future<RpcResult<CleanOutput>> clean(CleanInput input) {
        this.rwLock.writeLock().lock();
        try {
            this.uninstallAll();
            this.delBundleLoaderStartupConfig(this.loaderInstanceName);
        } finally {
            this.rwLock.writeLock().unlock();
        }
        return RpcResultBuilder.<CleanOutput>success().buildFuture();
    }

    @Override
    public Future<RpcResult<FeaturePutOutput>> featurePut(FeaturePutInput input) {
        this.rwLock.writeLock().lock();
        try {
            if (this.loadedFeatures.containsKey(input.getFeatureName())) {
                // remove the old featureHTTP_REQUESTS_ERROR
                this.uninstallFeature(input.getFeatureName());
            }

            String errMsg = this.installFeature(input.getFeatureName(), input.getBundlesToLoad());
            if (null != errMsg) {
                return RpcResultBuilder
                           .<FeaturePutOutput> failed()
                           .withError(RpcError.ErrorType.APPLICATION,
                                      "Installation of feature: " + input.getFeatureName() +
                                      " failed with error: " + errMsg)
                           .buildFuture();
            }

            // Write new config into startup configuration
            if (! this.putFeatureStartupConfig(input)) {
                LOG.error("Unable to write startup configuration, uninstalling new feature");
                this.uninstallFeature(input.getFeatureName());
                return RpcResultBuilder
                               .<FeaturePutOutput> failed()
                               .withError(RpcError.ErrorType.APPLICATION,
                                          "Installation of feature: " + input.getFeatureName() +
                                          " failed on startup config write")
                               .buildFuture();
            }

            return RpcResultBuilder.<FeaturePutOutput>success().buildFuture();

        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    @Override
    public Future<RpcResult<FeatureReloadOutput>> featureReload(FeatureReloadInput input) {
        // Check if running, check if startup, uninstall running install startup
        this.rwLock.writeLock().lock();
        try {
            if (! this.loadedFeatures.containsKey(input.getFeatureName())) {
                return RpcResultBuilder
                       .<FeatureReloadOutput> failed()
                       .withError(RpcError.ErrorType.APPLICATION,
                                  "BundleLoader: " + this.loaderInstanceName +
                                  " Feature " + input.getFeatureName() + " is not loaded")
                       .buildFuture();
            }

            StartupFeatures startupCfg = this.getFeatureStartupConfig(this.loaderInstanceName, input.getFeatureName());
            if (null == startupCfg) {
                return RpcResultBuilder
                       .<FeatureReloadOutput> failed()
                       .withError(RpcError.ErrorType.APPLICATION,
                                  "BundleLoader: " + this.loaderInstanceName +
                                  " Feature " + input.getFeatureName() + " not found in startup configuration")
                       .buildFuture();
            }

            List<BundlesToLoad> bundlesToLoad = new LinkedList<>();
            for (BundlesToLoad bundle : startupCfg.getBundlesToLoad()) {
                BundlesToLoadBuilder builder = new BundlesToLoadBuilder()
                        .setIotdmBundleJarLocation(bundle.getIotdmBundleJarLocation())
                        .setPriority(bundle.getPriority());
                bundlesToLoad.add(builder.build());
            }

            this.uninstallFeature(input.getFeatureName());
            this.installFeature(input.getFeatureName(), bundlesToLoad);
        } finally {
            this.rwLock.writeLock().unlock();
        }

        return RpcResultBuilder.<FeatureReloadOutput>success().buildFuture();
    }

    @Override
    public Future<RpcResult<StartupConfigOutput>> startupConfig(StartupConfigInput input) {
        StartupConfigOutput output =
                this.getStartupConfig(input.getBundleLoaderInstanceName(), input.getFeatureName());
        if (null == output) {
            return RpcResultBuilder
                           .<StartupConfigOutput> failed()
                           .withError(RpcError.ErrorType.APPLICATION,
                                      "Failed to read startup config from data store")
                           .buildFuture();
        }

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RunningConfigOutput>> runningConfig(RunningConfigInput input) {
        this.rwLock.readLock().lock();
        try {
            List<RunningFeatures> featureList = new ArrayList<>();
            for (Map.Entry<String, TreeSet<LoadedBundleInfo>> entry : this.loadedFeatures.entrySet()) {
                if (null != input.getFeatureName()) {
                    if (! input.getFeatureName().equals(entry.getKey())) {
                        // filter out
                        continue;
                    }
                }

                RunningFeaturesBuilder featureBuilder = new RunningFeaturesBuilder()
                    .setFeatureName(entry.getKey());

                List<RunningFeatureBundles> runningFeatureList = new LinkedList<>();
                for (LoadedBundleInfo bundleInfo : entry.getValue()) {

                    BundleInfo bInfo = karafService.getInfo(bundleInfo.bundle);
                    String state = "unknown";
                    if (null != bInfo) {
                        state = bInfo.getState().toString();
                    }

                    RunningFeatureBundlesBuilder bundlesBuilder = new RunningFeatureBundlesBuilder()
                            .setPriority(bundleInfo.priority)
                            .setIotdmBundleJarLocation(bundleInfo.jarFileLocation)
                            .setBundleId(String.valueOf(bundleInfo.bundle.getBundleId()))
                            .setBundleName(bundleInfo.bundle.getSymbolicName())
                            .setBundleVersion(bundleInfo.bundle.getVersion().toString())
                            .setBundleState(state)
                            .setBundleDiagnosticInfo(karafService.getDiag(bundleInfo.bundle));
                    runningFeatureList.add(bundlesBuilder.build());
                }

                featureBuilder.setRunningFeatureBundles(runningFeatureList);
                featureList.add(featureBuilder.build());
            }

            List<RunningConfigList> runningConfigList = new LinkedList<>();
            RunningConfigListBuilder runningConfigListBuilder = new RunningConfigListBuilder()
                    .setBundleLoaderInstanceName(input.getBundleLoaderInstanceName())
                    .setRunningFeatures(featureList);
            runningConfigList.add(runningConfigListBuilder.build());

            RunningConfigOutputBuilder builder =
                    new RunningConfigOutputBuilder()
                            .setRunningConfigList(runningConfigList);

            return RpcResultBuilder.success(builder.build()).buildFuture();
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    /*
     * Returns error message string when failed, null in case of success.
     * Expects acquired WriteLock.
     */
    private String installBundle(String bundleJarFile, String featureName,
                                 long priority, TreeSet<LoadedBundleInfo> bundleSet) {
        Bundle newBundle = null;
        try {
            newBundle = bundleContext.getBundle(bundleJarFile);
            if (null != newBundle) {
                /*
                 * This check is very important since it avoids overloading of bundles
                 * between BundleLoader instances but also between BundleLoader instance and system !
                 * This check avoid loading the same bundle even from different locations.
                 */
                StringBuilder builder = new StringBuilder()
                                                .append("BundleLoader: ")
                                                .append(loaderInstanceName)
                                                .append(", Feature: ")
                                                .append(featureName)
                                                .append(", Failed to load bundle: ")
                                                .append(bundleJarFile)
                                                .append(" (priority: ")
                                                .append(priority)
                                                .append("): ")
                                                .append("Bundle already loaded!");
                String errMsg = builder.toString();
                LOG.error(errMsg);
                return errMsg;
            }

            /*
             * Install bundle
             */
            newBundle = bundleContext.installBundle(bundleJarFile);

            if (karafService.getInfo(newBundle).getState() != BundleState.Installed) {
                StringBuilder builder = new StringBuilder()
                                                .append("BundleLoader: ")
                                                .append(loaderInstanceName)
                                                .append(", Feature: ")
                                                .append(featureName)
                                                .append(", Failed to install bundle: ")
                                                .append(bundleJarFile)
                                                .append(" (priority: ")
                                                .append(priority)
                                                .append("), ")
                                                .append("State: ")
                                                .append(karafService.getInfo(newBundle).getState().toString())
                                                .append(", Diagnostics: ")
                                                .append(karafService.getDiag(newBundle));
                String errMsg = builder.toString();
                LOG.error(errMsg);
                return errMsg;
            }

            /*
             * Start the bundle
             */
            newBundle.start();

            if (karafService.getInfo(newBundle).getState() != BundleState.Active) {
                StringBuilder builder = new StringBuilder()
                                                .append("BundleLoader: ")
                                                .append(loaderInstanceName)
                                                .append(", Feature: ")
                                                .append(featureName)
                                                .append(", Failed to start bundle: ")
                                                .append(bundleJarFile)
                                                .append(" (priority: ")
                                                .append(priority)
                                                .append("), ")
                                                .append("State: ")
                                                .append(karafService.getInfo(newBundle).getState().toString())
                                                .append(", Diagnostics: ")
                                                .append(karafService.getDiag(newBundle));
                String errMsg = builder.toString();
                LOG.error(errMsg);

                // Uninstall the bundle
                LOG.info("BundleLoader: {}, Feature: {}, uninstalling failed bundle: {}, bundleId: {}, " +
                         "state: {}, toStr: {}",
                         loaderInstanceName, featureName,
                         newBundle.getLocation(), newBundle.getBundleId(),
                         karafService.getInfo(newBundle).getState().toString(),
                         newBundle.toString());
                newBundle.uninstall();

                return errMsg;
            }

        } catch (Exception e) {
            StringBuilder builder = new StringBuilder()
                    .append("BundleLoader: ")
                    .append(loaderInstanceName)
                    .append(", Feature: ")
                    .append(featureName)
                    .append(", Failed to load bundle: ")
                    .append(bundleJarFile)
                    .append(" (priority: ")
                    .append(priority)
                    .append("): ")
                    .append(e.getMessage());

            if (null != newBundle) {
                LOG.info("BundleLoader: {}, Feature: {}, uninstalling failed bundle: {}",
                         loaderInstanceName, featureName, bundleJarFile);
                try {
                    newBundle.uninstall();
                } catch (Exception ee) {
                    LOG.info("BundleLoader: {}, Feature: {}, JAR: {}, Uninstalling of failed bundle failed: {}",
                             loaderInstanceName, featureName, bundleJarFile, ee);
                }
            }

            String errMsg = builder.toString();
            LOG.error(errMsg);
            return errMsg;
        }

        bundleSet.add(new LoadedBundleInfo(bundleJarFile, priority, newBundle));
        LOG.info("BundleLoader: {}, Feature: {}, new bundle loaded: {}, bundleId: {}, state: {}, toStr: {}",
                 loaderInstanceName, featureName,
                 newBundle.getLocation(), newBundle.getBundleId(),
                 karafService.getInfo(newBundle).getState().toString(),
                 newBundle.toString());

        return null;
    }

    /*
     * ErrorMessage is returned as String in case of failure, null is returned when success.
     * Expects acquired WriteLock
     */
    private String installFeature(String featureName, List<BundlesToLoad> bundleList) {
        // Sort the list according to priority (lower number indicates higher priority)
        bundleList.sort( (bundle1, bundle2) -> {
            if (bundle1.getPriority() < bundle2.getPriority()) {
                return -1;
            }
            if (bundle1.getPriority() > bundle2.getPriority()) {
                return 1;
            }
            return 0;
        });

        TreeSet<LoadedBundleInfo> bundleSet = new TreeSet<>( new Comparator<LoadedBundleInfo>() {
            public int compare(LoadedBundleInfo info1, LoadedBundleInfo info2) {
                if (info1.priority < info2.priority) {
                    return -1;
                }
                if (info1.priority > info2.priority) {
                    return 1;
                }
                return 0;
            }
        });

        this.loadedFeatures.put(featureName, bundleSet);

        Iterator<BundlesToLoad> iter = bundleList.iterator();
        while(iter.hasNext()) {
            BundlesToLoad bundle = iter.next();
            LOG.info("BundleLoader: {}, Feature: {}, Loading bundle: {}",
                     this.loaderInstanceName, featureName, bundle.getIotdmBundleJarLocation());
            String errMsg =
                installBundle(bundle.getIotdmBundleJarLocation(), featureName, bundle.getPriority(), bundleSet);
            if (null != errMsg) {
                LOG.error("BundleLoader: {}, Failed to install feature: {}, removing all bundles already installed",
                          this.loaderInstanceName, featureName);

                uninstallFeature(featureName);

                return new StringBuilder()
                               .append("Installation of feature: ")
                               .append(featureName)
                               .append(" failed at bundle: ")
                               .append(bundle.getIotdmBundleJarLocation())
                               .append(", ErrorMessage: ")
                               .append(errMsg)
                               .toString();
            }
        }

        LOG.info("BundleLoader: {}, Feature {} has been successfully installed", this.loaderInstanceName, featureName);
        return null;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {

        if (! IotdmPluginManager.getInstance().registerPluginLoader(this)) {
            LOG.error("BundleLoader: {}, Failed to register BundleLoader in PluginManager", loaderInstanceName);
            return;
        }

        this.rwLock.writeLock().lock();
        try {
            StartupConfigList cfgList = this.getBundleLoaderStartupConfig(loaderInstanceName);
            if (null != cfgList) {
                for (StartupFeatures feature : cfgList.getStartupFeatures()) {
                    String errorMsg = this.installFeature(feature.getFeatureName(), feature.getBundlesToLoad());
                    if (null != errorMsg) {
                        LOG.error("BundleLoader: {}, Feature: {}, Initial load failed: {}",
                                  loaderInstanceName, feature.getFeatureName(), errorMsg);
                    }
                }
            }
        } finally {
            this.rwLock.writeLock().unlock();
        }

        // Register local RPC services in the BundleLoader RPC router
        if (! IotdmBundleLoaderRpcRouter.getInstance().registerBundleLoader(loaderInstanceName, this)) {
            LOG.error("IotdmBundleLoaderProvider ({}) Failed to register RPC services", loaderInstanceName);
        } else {
            LOG.info("IotdmBundleLoaderProvider ({}) Session Initiated", loaderInstanceName);
        }
    }

    // expects acquired WriteLock
    private void uninstallBundle(Bundle bundle, String featureName) {
        try {
            bundle.stop();
        } catch (Exception e) {
            LOG.error("BundleLoader: {}, Feature: {}, Failed to stop bundle: {}, {}",
                      loaderInstanceName, featureName, bundle.toString(), e);
        }

        try {
            bundle.uninstall();
        } catch (Exception e) {
            LOG.error("BundleLoader: {}, Feature: {}, Failed to uninstall bundle: {}, {}",
                      loaderInstanceName, featureName, bundle.toString(), e);
        }

        LOG.info("BundleLoader: {}, Feature: {}, Bundle uninstalled: {}",
                 loaderInstanceName, featureName, bundle.toString());
    }

    // expects acquired WriteLock
    private void uninstallFeature(String featureName) {
        // uninstall bundles in reverse order
        LOG.info("BundleLoader: {}, Uninstalling feature: {}", loaderInstanceName, featureName);
        TreeSet<LoadedBundleInfo> bundles = this.loadedFeatures.get(featureName);

        bundles.descendingSet().stream().forEachOrdered(bundleInfo -> {
            uninstallBundle(bundleInfo.bundle, featureName);
        });

        bundles.clear();
        this.loadedFeatures.remove(featureName);
        LOG.info("BundleLoader: {}, Uninstalled feature: {}", loaderInstanceName, featureName);
    }

    // expects acquired WriteLock
    private void uninstallAll() {
        for (String featureName : this.loadedFeatures.keySet()) {
            uninstallFeature(featureName);
        }
        LOG.info("All feature uninstalled");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        this.rwLock.writeLock().lock();
        try {
            uninstallAll();

            // unregister this bundle loader
            if (!IotdmPluginManager.getInstance().unregisterPluginLoader(this)) {
                LOG.error("BundleLoader: {}, Failed to unregister BundleLoader", loaderInstanceName);
            }
            LOG.info("BundleLoader: {}, IotdmBundleLoaderProvider Closed", loaderInstanceName);
        } finally {
            this.rwLock.writeLock().unlock();
        }

        IotdmBundleLoaderRpcRouter.getInstance().unregisterBundleLoader(loaderInstanceName);
    }


    /*
     * MDSAL utility methods for persistent Startup configuration
     */
    private boolean putFeatureStartupConfig(FeaturePutInput input) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();

        StartupFeaturesBuilder featuresBuilder = new StartupFeaturesBuilder()
                                                         .setFeatureName(input.getFeatureName())
                                                         .setBundlesToLoad(input.getBundlesToLoad());

        StartupConfigListKey key = new StartupConfigListKey(input.getBundleLoaderInstanceName());

        InstanceIdentifier<StartupFeatures> iid =
                InstanceIdentifier.create(BundleLoaderStartupConfig.class)
                        .child(StartupConfigList.class, key)
                        .child(StartupFeatures.class, new StartupFeaturesKey(input.getFeatureName()));

        writeTransaction.put(LogicalDatastoreType.CONFIGURATION,
                             iid,
                             featuresBuilder.build(), true);
        try {
            writeTransaction.submit().checkedGet();
        } catch (Exception e) {
            LOG.error("BundleLoader: {}, Feature: {}, Failed to write startup config: {}",
                      input.getBundleLoaderInstanceName(), input.getFeatureName(), e);
            return false;
        }

        return true;
    }

    private boolean delFeatureStartupConfig(FeatureRemoveInput input) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<StartupFeatures> iid =
                InstanceIdentifier.create(BundleLoaderStartupConfig.class)
                        .child(StartupConfigList.class, new StartupConfigListKey(input.getBundleLoaderInstanceName()))
                        .child(StartupFeatures.class, new StartupFeaturesKey(input.getFeatureName()));

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION,
                               iid);
        try {
            writeTransaction.submit().checkedGet();
        } catch (Exception e) {
            LOG.error("BundleLoader: {}, Feature: {}, Failed to delete startup config: {}",
                      input.getBundleLoaderInstanceName(), input.getFeatureName(), e);
            return false;
        }

        return true;
    }

    private boolean delBundleLoaderStartupConfig(String bundleLoaderName) {
        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<StartupConfigList> iid =
                InstanceIdentifier.create(BundleLoaderStartupConfig.class)
                        .child(StartupConfigList.class, new StartupConfigListKey(bundleLoaderName));

        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION,
                                iid);
        try {
            writeTransaction.submit().checkedGet();
        } catch (Exception e) {
            LOG.error("BundleLoader: {}, Failed to delete startup config: {}", bundleLoaderName, e);
            return false;
        }

        return true;
    }

    private StartupFeatures getFeatureStartupConfig(String bundleLoaderName, String featureName) {
        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();

        InstanceIdentifier<StartupFeatures> iid =
                InstanceIdentifier.create(BundleLoaderStartupConfig.class)
                        .child(StartupConfigList.class, new StartupConfigListKey(bundleLoaderName))
                        .child(StartupFeatures.class, new StartupFeaturesKey(featureName));

        Optional<StartupFeatures> startupConfig = null;
        try {
            startupConfig = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("BundleLoader: {}, Feature: {}, Failed to read startup config of feature: {}",
                      bundleLoaderName, featureName, e);
            return null;
        }

        if (! startupConfig.isPresent()) {
            LOG.info("BundleLoader: {}, Feature: {}, Startup config does not exist",
                     bundleLoaderName, featureName);
            return null;
        }

        return startupConfig.get();
    }

    private StartupConfigList getBundleLoaderStartupConfig(String bundleLoaderInstanceName) {
        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();

        InstanceIdentifier<StartupConfigList> iid =
                InstanceIdentifier.create(BundleLoaderStartupConfig.class)
                        .child(StartupConfigList.class, new StartupConfigListKey(bundleLoaderInstanceName));

        Optional<StartupConfigList> startupConfig = null;
        try {
            startupConfig = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
        } catch (ReadFailedException e) {
            LOG.error("BundleLoader: {}, Failed to read startup config of all features: {}",
                      bundleLoaderInstanceName, e);
            return null;
        }

        if (! startupConfig.isPresent()) {
            LOG.info("BundleLoader: {}, Startup config does not exist", bundleLoaderInstanceName);
            return null;
        }

        return startupConfig.get();
    }

    private StartupConfigOutput getStartupConfig(String bundleLoaderInstanceName,
                                                                   String featureName) {
        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        StartupConfigOutputBuilder outputBuilder = new StartupConfigOutputBuilder();

        if (null == bundleLoaderInstanceName) {
            // Retrieve complete startup configuration of all BundleLoader instances
            InstanceIdentifier<BundleLoaderStartupConfig> iid =
                    InstanceIdentifier.builder(BundleLoaderStartupConfig.class).build();

            Optional<BundleLoaderStartupConfig> startupConfig = null;
            try {
                startupConfig = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
            } catch (ReadFailedException e) {
                LOG.error("Failed to read startup config of all BundleLoaders: {}", e);
                return null;
            }

            if (! startupConfig.isPresent()) {
                LOG.info("Startup config of BundleLoaders is empty");
                return outputBuilder.build();
            }

            outputBuilder.setStartupConfigList(startupConfig.get().getStartupConfigList());
            return outputBuilder.build();
        } else {
            // Retrieve startup configuration of specific BundleLoader instance
            List<StartupConfigList> startupCfgList = new LinkedList<>();
            if (null == featureName) {
                // Retrieve configuration of all features of the BundleLoader
                StartupConfigList startupConfig = this.getBundleLoaderStartupConfig(bundleLoaderInstanceName);
                if (null != startupConfig) {
                    startupCfgList.add(startupConfig);
                }
                return outputBuilder.setStartupConfigList(startupCfgList).build();
            } else {
                // Retrieve configuration of the specific feature only
                StartupFeatures startupConfig =
                        getFeatureStartupConfig(bundleLoaderInstanceName, featureName);

                if (null != startupConfig) {
                    List<StartupFeatures> feature = new LinkedList<>();
                    feature.add(startupConfig);
                    StartupConfigListBuilder cfgListBuilder =
                            new StartupConfigListBuilder()
                                 .setBundleLoaderInstanceName(bundleLoaderInstanceName)
                                 .setStartupFeatures(feature);
                    startupCfgList.add(cfgListBuilder.build());
                }

                return outputBuilder.setStartupConfigList(startupCfgList).build();
            }
        }
    }
}