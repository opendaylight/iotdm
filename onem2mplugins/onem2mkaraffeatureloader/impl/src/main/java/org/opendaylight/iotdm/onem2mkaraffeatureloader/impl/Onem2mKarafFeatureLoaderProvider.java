/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2mkaraffeatureloader.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
//import org.apache.karaf.kar.KarService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginCommonInterface;
import org.opendaylight.iotdm.onem2m.plugins.IotdmPluginLoader;
import org.opendaylight.iotdm.onem2m.plugins.Onem2mPluginManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mkaraffeatureloader.config.rev161220.KarafFeatureLoadersConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveInstallInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveInstallOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveListStartupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveListStartupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveReloadInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveReloadOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveUninstallInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.ArchiveUninstallOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.KarafFeatureLoaderStartupConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.Onem2mkaraffeatureloaderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.KarafFeatureLoaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.KarafFeatureLoadersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.KarafArchives;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.KarafArchivesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.karaf.archives.KarafFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.karaf.archives.KarafFeaturesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.karaf.archives.karaf.features.FeatureBundles;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.karaf.archives.karaf.features.FeatureBundlesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.karaf.archives.karaf.features.FeatureDependencies;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.archive.list.output.karaf.feature.loaders.karaf.archives.karaf.features.FeatureDependenciesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.features.to.install.list.FeaturesToInstall;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.StartupKarafFeatureLoaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.StartupKarafFeatureLoadersKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.startup.karaf.feature.loaders.StartupKarafArchives;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.startup.karaf.feature.loaders.StartupKarafArchivesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.startup.karaf.feature.loaders.StartupKarafArchivesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.startup.karaf.feature.loaders.startup.karaf.archives.StartupKarafFeatures;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.startup.karaf.feature.loaders.startup.karaf.archives.StartupKarafFeaturesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;


public class Onem2mKarafFeatureLoaderProvider implements IotdmPluginLoader, Onem2mkaraffeatureloaderService {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mKarafFeatureLoaderProvider.class);
    private static final String KARSUFFIX = ".kar";

    private final DataBroker dataBroker;
    private final KarafFeatureLoadersConfigs configs;
    protected final BundleContext bundleContext;

    protected final BundleService karafService;
    protected final FeaturesService karafFeaturesService;
//    protected final KarService karService;

    protected final String karafFeatureLoaderName;
    protected final String karafDataDir;
    protected final String karafSystemDir;
    protected final Map<String, KarafArchiveInfo> archives = new ConcurrentHashMap<>();
    protected final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    protected class KarafArchiveInfo {
        private final String archiveUrl;
        private final String archiveName;
        private final List<String> featureNames;
        private final String repositoryName;
        private final String repositoryUrl;

        public KarafArchiveInfo(final String archiveUrl, final String archiveName,
                                final List<String> featureNames,
                                final String repositoryName, final String repositoryUrl) {
            this.archiveUrl = archiveUrl;
            this.archiveName = archiveName;
            this.featureNames = featureNames;
            this.repositoryName = repositoryName;
            this.repositoryUrl = repositoryUrl;
        }

        public String getArchiveUrl() {
            return archiveUrl;
        }

        public String getArchiveName() {
            return archiveName;
        }

        public List<String> getFeatureNames() {
            return featureNames;
        }

        public String getRepositoryName() {
            return repositoryName;
        }

        public String getRepositoryUrl() {
            return repositoryUrl;
        }
    }

    public Onem2mKarafFeatureLoaderProvider(final DataBroker dataBroker,
                                            final KarafFeatureLoadersConfigs configs,
                                            final BundleContext bundleContext) {

        if (null != configs && null != configs.getLoaderInstanceName()) {
            this.karafFeatureLoaderName = configs.getLoaderInstanceName();
        } else {
            throw new IllegalArgumentException("Invalid configuration");
        }

        this.dataBroker = dataBroker;
        this.configs = configs;
        this.bundleContext = bundleContext;

        String karafBaseDir = System.getProperty("karaf.base");
        if (null == karafBaseDir || karafBaseDir.isEmpty()) {
            throw new RuntimeException("Can't get karaf.base property");
        }

        if (! karafBaseDir.endsWith(File.separator)) {
            karafBaseDir += File.separator;
        }

        this.karafDataDir = karafBaseDir + "data" + File.separator + "kar" + File.separator;
        this.karafSystemDir = karafBaseDir + "system" + File.separator;

        ServiceReference<BundleService> ref = bundleContext.getServiceReference(BundleService.class);
        karafService = bundleContext.getService(ref);

        ServiceReference<FeaturesService> refServ = bundleContext.getServiceReference(FeaturesService.class);
        karafFeaturesService = bundleContext.getService(refServ);

//        ServiceReference<KarService> refKar = bundleContext.getServiceReference(KarService.class);
//        karService = bundleContext.getService(refKar);

        this.rwLock.writeLock().lock();
        try {
            // walk through the startup configuration and load it
            // to the local archives map
            // Not needed to install all features again since it's done
            // by karaf internally
            // TODO
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {

        // Register as IoTDM Plugin loader
        Onem2mPluginManager.getInstance().registerPluginLoader(this);

        // TODO walk startup config and load it to the list of installed karaf archives
        // TODO and check if the features and repositories from startup config are really installed / added
        // TODO install features and add repositories which are missing in the system


        LOG.info("Onem2mKarafFeatureLoaderProvider Session Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        Onem2mPluginManager.getInstance().unregisterPluginLoader(this);
        LOG.info("Onem2mKarafFeatureLoaderProvider Closed");
    }

    private <Tout> Future<RpcResult<Tout>> handleRpcError(String format, String... args) {
        String msg = MessageFormatter.arrayFormat(format, args).getMessage();
        LOG.error("KarafFeatureLoader {}:: {}", this.karafFeatureLoaderName, msg);

        return RpcResultBuilder
            .<Tout>failed()
            .withError(RpcError.ErrorType.APPLICATION, msg)
            .buildFuture();
    }


    /*
     * Implementation of the IotdmPluginLoader interface
     */

    @Override
    public String getLoaderName() {
        return this.karafFeatureLoaderName;
    }

    @Override
    public boolean hasLoadedPlugin(IotdmPluginCommonInterface plugin) {
        Bundle bundle = org.osgi.framework.FrameworkUtil.getBundle(plugin.getClass());
        if (null != bundle) {
            this.rwLock.readLock().lock();
            try {
                for (KarafArchiveInfo info : this.archives.values()) {

                    // Walk all features
                    for (String featureName : info.getFeatureNames()) {
                        Feature feature = null;
                        try {
                            feature = karafFeaturesService.getFeature(featureName);
                        }
                        catch (Exception e) {
                            LOG.error("Failed to get feature: {}, {}", featureName, e);
                            continue;
                        }

                        // Continue if the feature is not installed
                        if (!karafFeaturesService.isInstalled(feature)) {
                            continue;
                        }

                        // Walk all bundles
                        for (BundleInfo bundleInfo : feature.getBundles()) {
                            List<Bundle> bundles = karafService.getBundlesByURL(bundleInfo.getLocation());
                            if (null == bundles) {
                                LOG.error("Failed to get bundle {} of the feature {}",
                                          bundleInfo.getLocation(), featureName);
                                continue;
                            }

                            for (Bundle featureBundle : bundles) {
                                if (featureBundle == bundle) {
                                    // This loader instance has loaded the plugin
                                    return true;
                                }
                            }
                        }
                    }
                }
            } finally {
                this.rwLock.readLock().unlock();
            }
        }

        return false;
    }


    /*
     * Implementation of RPCs
     */

//    private void uninstallFailedArchive(String archiveName) {
        // TODO this is implementation for the approach using kar service, but it's commented out now
//        try {
//            karService.uninstall(archiveName);
//        } catch (Exception e) {
//            LOG.error("Failed to uninstall failed archive: {}: {}", archiveName);
//        }
//    }

    // expects obtained write lock
    private void uninstallFailedArchive(String repositoryUri, List<String> features) {
        // remove the repository
        try {
            this.karafFeaturesService.removeRepository(new URI(repositoryUri));
        } catch (Exception e) {
            LOG.error("Failed to remove repository {} of failed archive", repositoryUri);
        }

        // uninstall features
        if (null != features) {
            for (String feature : features) {
                try {
                    this.karafFeaturesService.uninstallFeature(feature);
                } catch (Exception e) {
                    LOG.error("Failed to uninstall feature {} of failed archive: {}",
                              feature, e.toString());
                }
            }
        }
    }

    @Override
    public Future<RpcResult<ArchiveInstallOutput>> archiveInstall(ArchiveInstallInput input) {

        if (null == input.getFeaturesToInstall() ) {
            return handleRpcError("Mandatory input not provided");
        }

        // Check archive URL
        String archiveUrl = input.getKarafArchiveUrl();
        String[] urlSplit = archiveUrl.split(File.separator);
        String archiveFile = urlSplit[urlSplit.length - 1];
        if (archiveFile.isEmpty() || ! archiveFile.endsWith(KARSUFFIX) || archiveFile.equals(KARSUFFIX)) {
            return handleRpcError("Invalid archive URL: {}, archive must end with {} suffix", archiveUrl, KARSUFFIX);
        }

        String archiveName = archiveFile.substring(0, archiveFile.lastIndexOf(KARSUFFIX));

        rwLock.writeLock().lock();

        try {
            // Check if the archive has not been already installed
            if (this.archives.containsKey(archiveName)) {
                return handleRpcError("Archive with name {} already installed", archiveName);
            }

            // Check list of features whether they are not already known (installed or uninstalled doesn't matter)
            // This avoids overriding of system features by karaf feature loader or
            // among instances of the karaf feature loader
            if (input.getFeaturesToInstall()
                     .isEmpty()) {
                return handleRpcError("List of features to be installed not passed");
            }

            List<String> featureList = new LinkedList<>();
            for (FeaturesToInstall feature : input.getFeaturesToInstall()) {
                featureList.add(feature.getFeatureName());
            }

            for (String feature : featureList) {
                try {
                    if (null != karafFeaturesService.getFeature(feature)) {
                        return handleRpcError("Feature {} is already available. Cannot continue with installation",
                                              feature);
                    }
                }
                catch (Exception e) {
                    LOG.error("Failed to try to get feature");
                }
            }

            // TODO this part just uses kar service to decompress archive and to find repository there
            // TODO but problem occurs in case of re-installation when it will automatically install all features
            // TODO from the new repository since all needed bundles are already copied in the system folder (although older versions)
            // Install the archive by KAR service which do these steps:
            //  1. unzips the archive into <karaf base>/data/kar/<archive name>.
            //  2. Search repository file (features.xml) and adds it as repository in Karaf.
            //  3. Will make an attempt to install all features but problem is that the bundles has not
            //      been copied from the data directory to system directory and the installations of features
            //      will fail. This seems like a bug in Karaf so we have implemented the copying of the files.
            //        try {
            //            karService.install(new URI(archiveUrl));
            //        } catch (URISyntaxException e) {
            //            return handleRpcError("Invalid archive URL: {}: {}", archiveUrl, e.toString());
            //        } catch (Exception e) {
            //            return handleRpcError("KAR archive installation error: {}", e.toString());
            //        }

            String dataDirPath = this.karafDataDir + archiveName;
            Path system = Paths.get(this.karafSystemDir);

            try {
                ZipFile archive = new ZipFile(Paths.get(new URI(archiveUrl))
                                                   .toFile());
                archive.extractAll(dataDirPath);
            }
            catch (URISyntaxException e) {
                return handleRpcError("Invalid archive URL: {}: {}", archiveUrl, e.toString());
            }
            catch (ZipException e) {
                return handleRpcError("Failed to unzip the archive: {}", e.toString());
            }

            // Find features.xml file in the root of unzipped archive and add it as feature repository
            File featuresXml = Paths.get(dataDirPath + File.separator + "features.xml")
                                    .toFile();
            if (null != featuresXml && featuresXml.exists() && featuresXml.isFile()) {
                try {
                    karafFeaturesService.addRepository(featuresXml.toURI());
                }
                catch (Exception e) {
                    return handleRpcError("Failed to add repository from archive {}: {}", archiveUrl, e.toString());
                }
            }
            else {
                return handleRpcError("Repository file features.xml not found in archive: {}", archiveUrl);
            }

            // Find the installed repository and verify whether it brings requested features
            Optional<Repository> optRepo =
                Arrays.stream(karafFeaturesService.listRepositories())
                      .filter(repo -> repo.getURI()
                                          .toString()
                                          .startsWith("file:" + dataDirPath))
                      .findFirst();
            if (!optRepo.isPresent()) {
                return handleRpcError("No feature repository added from archive {}", archiveUrl);
            }

            String repositoryName = optRepo.get()
                                           .getName();
            String repositoryUrl = optRepo.get()
                                          .getURI()
                                          .toString();

            // Verify features of the repository
            for (String featureName : featureList) {
                boolean found = false;
                try {
                    for (Feature feature : optRepo.get()
                                                  .getFeatures()) {
                        if (feature.getName()
                                   .equals(featureName)) {
                            found = true;
                            break;
                        }
                    }
                }
                catch (Exception e) {
                    uninstallFailedArchive(repositoryUrl, null);
                    return handleRpcError("Failed to check if feature {} is provided by archive repository: {}",
                                          featureName, e.toString());
                }

                if (!found) {
                    uninstallFailedArchive(repositoryUrl, null);
                    return handleRpcError("Repository of karaf archive {} does not provide feature {}",
                                          archiveUrl, featureName);
                }
            }

            // Copy the content of data/kar/<archive name> into system/ directory
            try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(Paths.get(dataDirPath + File.separator + "repository"))) {
                for (Path entry : stream) {
                    if (!Files.isDirectory(entry)) {
                        LOG.info("KarafFeatureLoader {}:: Archive: {} skipping file: {}",
                                 this.karafFeatureLoaderName,
                                 entry.toAbsolutePath()
                                      .toString(),
                                 archiveName);
                        continue;
                    }

                    FileUtils.copyDirectoryToDirectory(entry.toFile(), system.toFile());
                    LOG.info("KarafFeatureLoader {}:: Archive: {} copied directory from {} to {}",
                             this.karafFeatureLoaderName, archiveName,
                             entry.toAbsolutePath()
                                  .toString(),
                             system);
                }
            }
            catch (IOException e) {
                this.uninstallFailedArchive(archiveUrl, null);
                return handleRpcError("Failed to copy archive files from data directory of archive ({}): {}",
                                      archiveUrl, e.toString());
            }

            // Install all features from list
            if (null != featureList) {
                for (String feature : featureList) {
                    try {
                        karafFeaturesService.installFeature(feature);
                    }
                    catch (Exception e) {
                        this.uninstallFailedArchive(archiveUrl, featureList);
                        return handleRpcError("Failed to install feature {}: {}", feature, e.toString());
                    }
                }
            }

            // Store the the information about the new archive installed
            KarafArchiveInfo info = new KarafArchiveInfo(archiveUrl, archiveName,
                                                         featureList,
                                                         repositoryName, repositoryUrl);

            this.archives.put(archiveName, info);

            // Store the information also in data store so they can be used
            // after restart of IoTDM
            if (!putArchiveStartupInfo(info)) {
                this.uninstallFailedArchive(archiveUrl, featureList);
                return handleRpcError("Failed to store startup configuration. Uninstalling the archive {}",
                                      archiveUrl);
            }

        } finally {
            this.rwLock.writeLock().unlock();
        }

        return RpcResultBuilder.<ArchiveInstallOutput>success().buildFuture();
    }

    private boolean putArchiveStartupInfo(KarafArchiveInfo info) {
        // Prepare list of karaf features
        List<StartupKarafFeatures> features = new LinkedList<>();
        for (String featureName : info.getFeatureNames()) {
            features.add(new StartupKarafFeaturesBuilder().setFeatureName(featureName).build());
        }

        StartupKarafArchivesBuilder builder = new StartupKarafArchivesBuilder()
            .setKarafArchiveName(info.getArchiveName())
            .setKarafArchiveUrl(info.getArchiveUrl())
            .setStartupKarafFeatures(features)
            .setRepositoryName(info.getRepositoryName())
            .setRepositoryUrl(info.getRepositoryUrl());

        InstanceIdentifier<StartupKarafArchives> iid =
            InstanceIdentifier.create(KarafFeatureLoaderStartupConfig.class)
                .child(StartupKarafFeatureLoaders.class, new StartupKarafFeatureLoadersKey(this.karafFeatureLoaderName))
                .child(StartupKarafArchives.class, new StartupKarafArchivesKey(info.getArchiveName()));

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, iid, builder.build(), true);
        try {
            writeTransaction.submit().checkedGet();
        } catch (Exception e) {
            LOG.error("Failed to write startup configuration: {}", e);
            return false;
        }

        return true;
    }

    @Override
    public Future<RpcResult<ArchiveListOutput>> archiveList(ArchiveListInput input) {
        ArchiveListOutputBuilder out = new ArchiveListOutputBuilder();
        List<KarafFeatureLoaders> loadersList = new LinkedList<>();

        KarafFeatureLoadersBuilder featureLoaderBuilder = new KarafFeatureLoadersBuilder();
        featureLoaderBuilder.setKarafFeatureLoaderName(karafFeatureLoaderName);

        // Walk all archives installed
        List<KarafArchives> karafArchivesList = new LinkedList<>();
        this.rwLock.readLock().lock();
        try {
            for (KarafArchiveInfo info : this.archives.values()) {
                KarafArchivesBuilder archivesBuilder = new KarafArchivesBuilder()
                    .setKarafArchiveName(info.getArchiveName())
                    .setKarafArchiveUrl(info.getArchiveUrl())
                    .setRepositoryName(info.getRepositoryName())
                    .setRepositoryUrl(info.getRepositoryUrl());

                // Check whether the repository is still added
                Repository repo = karafFeaturesService.getRepository(info.getRepositoryName());
                if (null == repo) {
                    archivesBuilder.setRepositoryState(KarafArchives.RepositoryState.Removed);
                }
                else if (!repo.isValid()) {
                    archivesBuilder.setRepositoryState(KarafArchives.RepositoryState.Invalid);
                }
                else {
                    archivesBuilder.setRepositoryState(KarafArchives.RepositoryState.Added);
                }

                // Walk all features
                List<KarafFeatures> karafFeaturesList = new LinkedList<>();
                for (String featureName : info.getFeatureNames()) {
                    KarafFeaturesBuilder featureBuilder = new KarafFeaturesBuilder();
                    featureBuilder.setFeatureName(featureName);

                    Feature feature = null;
                    try {
                        feature = karafFeaturesService.getFeature(featureName);
                    }
                    catch (Exception e) {
                        LOG.error("Failed to get feature: {}, {}", featureName, e);
                        karafFeaturesList.add(featureBuilder.build());
                        continue;
                    }

                    // Set feature state
                    if (karafFeaturesService.isInstalled(feature)) {
                        featureBuilder.setFeatureState(KarafFeatures.FeatureState.Installed);
                    }
                    else {
                        featureBuilder.setFeatureState(KarafFeatures.FeatureState.Uninstalled);
                    }

                    // Set other feature attributes
                    featureBuilder.setFeatureDescription(feature.getDescription())
                                  .setFeatureDetails(feature.getDetails())
                                  .setFeatureId(feature.getId())
                                  .setFeatureVersion(feature.getVersion());

                    // Set list of feature bundles
                    List<FeatureBundles> featureBundlesList = new LinkedList<>();
                    for (BundleInfo bundleInfo : feature.getBundles()) {
                        List<Bundle> bundles = karafService.getBundlesByURL(bundleInfo.getLocation());
                        if (null == bundles) {
                            LOG.error("Failed to get bundle {} of the feature {}",
                                      bundleInfo.getLocation(), featureName);
                            continue;
                        }

                        for (Bundle bundle : bundles) {
                            FeatureBundlesBuilder bundleBuilder = new FeatureBundlesBuilder()
                                .setBundleName(bundle.getSymbolicName())
                                .setBundleLocation(bundle.getLocation())
                                .setBundleId(String.valueOf(bundle.getBundleId()))
                                .setBundleVersion(bundle.getVersion()
                                                        .toString())
                                .setBundleState(karafService.getInfo(bundle)
                                                            .getState()
                                                            .toString())
                                .setBundleDiagnosticInfo(karafService.getDiag(bundle));
                            featureBundlesList.add(bundleBuilder.build());
                        }
                    }
                    featureBuilder.setFeatureBundles(featureBundlesList);

                    // Set list of feature dependencies
                    List<FeatureDependencies> featureDependenciesList = new LinkedList<>();
                    for (Dependency dependency : feature.getDependencies()) {
                        FeatureDependenciesBuilder dependencyBuilder = new FeatureDependenciesBuilder()
                            .setDependencyName(dependency.getName())
                            .setDependencyVersion(dependency.getVersion());
                        featureDependenciesList.add(dependencyBuilder.build());
                    }
                    featureBuilder.setFeatureDependencies(featureDependenciesList);

                    karafFeaturesList.add(featureBuilder.build());
                }

                archivesBuilder.setKarafFeatures(karafFeaturesList);
                karafArchivesList.add(archivesBuilder.build());
            }
        } finally {
            this.rwLock.readLock().unlock();
        }

        featureLoaderBuilder.setKarafArchives(karafArchivesList);

        loadersList.add(featureLoaderBuilder.build());
        out.setKarafFeatureLoaders(loadersList);
        return RpcResultBuilder.success(out.build()).buildFuture();
    }

    @Override
    public Future<RpcResult<ArchiveUninstallOutput>> archiveUninstall(ArchiveUninstallInput input) {
        return RpcResultBuilder
            .<ArchiveUninstallOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION,
                       "Not implemented")
            .buildFuture();
    }

    @Override
    public Future<RpcResult<ArchiveReloadOutput>> archiveReload(ArchiveReloadInput input) {
        return RpcResultBuilder
            .<ArchiveReloadOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION,
                       "Not implemented")
            .buildFuture();
    }

    @Override
    public Future<RpcResult<ArchiveListStartupOutput>> archiveListStartup(ArchiveListStartupInput input) {
        return RpcResultBuilder
            .<ArchiveListStartupOutput>failed()
            .withError(RpcError.ErrorType.APPLICATION,
                       "Not implemented")
            .buildFuture();
    }
}