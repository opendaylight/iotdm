/*
 * Copyright © 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.iotdmkaraffeatureloader.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveInstallInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveInstallOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveListInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveListOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveListStartupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveListStartupOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveListStartupOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveReloadInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveReloadOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveUninstallInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.ArchiveUninstallOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.CleanInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.CleanOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.IotdmkaraffeatureloaderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.archive.list.output.KarafFeatureLoaders;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdmkaraffeatureloader.rev150105.karaf.feature.loader.startup.config.definition.StartupKarafFeatureLoaders;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements routing of RPC calls to registered KarafFeatureLoader instances
 * according to KarafFeatureLoader instance name specified in the RPC call input.
 */
public class IotdmKarafFeatureLoaderRpcRouter implements IotdmkaraffeatureloaderService {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmKarafFeatureLoaderRpcRouter.class);

    private final Map<String, IotdmKarafFeatureLoaderProvider> karafFeatureLoaders = new ConcurrentHashMap<>();
    private static IotdmKarafFeatureLoaderRpcRouter instance = null;

    private IotdmKarafFeatureLoaderRpcRouter() {
    }

    public static IotdmKarafFeatureLoaderRpcRouter getInstance() {
        if (null == instance) {
            instance = new IotdmKarafFeatureLoaderRpcRouter();
        }

        return instance;
    }

    /**
     * Registers KarafFeatureLoader provider instance.
     * @param instanceName Unique name of the instance
     * @param instance The KarafFeatureLoader provider
     * @return True if successful, False otherwise.
     */
    public boolean registerKarafFeatureLoader(String instanceName, IotdmKarafFeatureLoaderProvider instance) {
        if (karafFeatureLoaders.containsKey(instanceName)) {
            LOG.error("KarafFeatureLoader: {} already registered in RPC router", instanceName);
            return false;
        }

        karafFeatureLoaders.put(instanceName, instance);
        return true;
    }

    /**
     * Unregisters KarafFeatureLoader provider instance.
     * @param instanceName Name of the provider instance
     */
    public void unregisterKarafFeatureLoader(String instanceName) {
        karafFeatureLoaders.remove(instanceName);
    }

    private <T> Future<RpcResult<T>> handleNoInput() {
        LOG.error("RPC called without input");
        return RpcResultBuilder
            .<T> failed()
            .withError(RpcError.ErrorType.APPLICATION,
                       "RPC input is mandatory.")
            .buildFuture();
    }

    private <T> Future<RpcResult<T>> handleNotFound(String karafFeatureLoaderName) {
        LOG.error("RPC called for KarafFeatureLoader: {} which doesn't exist", karafFeatureLoaderName);
        return RpcResultBuilder
            .<T> failed()
            .withError(RpcError.ErrorType.APPLICATION,
                       "KarafFeatureLoader: " + karafFeatureLoaderName + " does not exist.")
            .buildFuture();
    }

    /**
     * Calls archiveReload RPC of specific instance.
     */
    @Override
    public Future<RpcResult<ArchiveReloadOutput>> archiveReload(ArchiveReloadInput input) {
        if (input == null) {
            return handleNoInput();
        }

        IotdmKarafFeatureLoaderProvider loader =
            this.karafFeatureLoaders.get(input.getKarafFeatureLoaderName());
        if (null == loader) {
            return handleNotFound(input.getKarafFeatureLoaderName());
        }

        return loader.archiveReload(input);
    }

    /**
     * Calls archiveInstall of specific instance.
     */
    @Override
    public Future<RpcResult<ArchiveInstallOutput>> archiveInstall(ArchiveInstallInput input) {
        if (input == null) {
            return handleNoInput();
        }

        IotdmKarafFeatureLoaderProvider loader =
            this.karafFeatureLoaders.get(input.getKarafFeatureLoaderName());
        if (null == loader) {
            return handleNotFound(input.getKarafFeatureLoaderName());
        }

        return loader.archiveInstall(input);
    }

    /**
     * Calls archiveList of single instance if specified or all registered
     * instances if the input is missing or is empty.
     */
    @Override
    public Future<RpcResult<ArchiveListOutput>> archiveList(ArchiveListInput input) {
        if (null != input && null != input.getKarafFeatureLoaderName()) {
            // Get list for specific KarafFeatureLoader instance
            IotdmKarafFeatureLoaderProvider loader =
                this.karafFeatureLoaders.get(input.getKarafFeatureLoaderName());
            if (null == loader) {
                return handleNotFound(input.getKarafFeatureLoaderName());
            }

            return loader.archiveList(input);
        }

        // Walk all registered KarafFeatureLoaders and collect list of all outputs
        List<KarafFeatureLoaders> outputList = new LinkedList<>();
        for (Map.Entry<String, IotdmKarafFeatureLoaderProvider> loader : this.karafFeatureLoaders.entrySet()) {
            Future<RpcResult<ArchiveListOutput>> output = loader.getValue().archiveList(input);
            ArchiveListOutput result = null;

            try {
                if (! output.get().isSuccessful()) {
                    Collection<RpcError> errors = null;
                    errors = output.get().getErrors();

                    LOG.error("KarafFeatureLoader {}: Failed to get list of archives.", loader.getKey());
                    if (null != errors && !errors.isEmpty()) {
                        int i = 0;
                        for (RpcError error : errors) {
                            LOG.error("KarafFeatureLoader {}: Failed to get list of archives, Error {}: {}",
                                      loader.getKey(),
                                      ++i,
                                      error.toString());
                        }
                    }
                    continue;
                }

                result = output.get().getResult();
            }
            catch (Exception e) {
                LOG.error("KarafFeatureLoader {}: Failed to get list of archives: {}", loader.getKey(), e);
                continue;
            }

            // Only one item (for the one KarafFeatureLoader instance) should be returned
            if (result.getKarafFeatureLoaders().size() != 1) {
                LOG.error("KarafFeatureLoader {}: Invalid number of items of output list of " +
                          "KarafFeatureLoader instances: {}",
                          loader.getKey(), result.getKarafFeatureLoaders().size());
                continue;
            }

            // Add the one item into the output list
            outputList.add(result.getKarafFeatureLoaders().get(0));
        }

        ArchiveListOutputBuilder out = new ArchiveListOutputBuilder()
            .setKarafFeatureLoaders(outputList);
        return RpcResultBuilder.success(out.build()).buildFuture();
    }

    /**
     * Calls archiveUninstall RPC of specific instance.
     */
    @Override
    public Future<RpcResult<ArchiveUninstallOutput>> archiveUninstall(ArchiveUninstallInput input) {
        if (input == null) {
            return handleNoInput();
        }

        IotdmKarafFeatureLoaderProvider loader =
            this.karafFeatureLoaders.get(input.getKarafFeatureLoaderName());
        if (null == loader) {
            return handleNotFound(input.getKarafFeatureLoaderName());
        }

        return loader.archiveUninstall(input);
    }

    /**
     * Calls archiveListStartup RPC of single instance if specified
     * or all instances if the input is missing or is empty.
     */
    @Override
    public Future<RpcResult<ArchiveListStartupOutput>> archiveListStartup(ArchiveListStartupInput input) {
        if (null != input && null != input.getKarafFeatureLoaderName()) {
            // Get list for specific KarafFeatureLoader instance
            IotdmKarafFeatureLoaderProvider loader =
                this.karafFeatureLoaders.get(input.getKarafFeatureLoaderName());
            if (null == loader) {
                return handleNotFound(input.getKarafFeatureLoaderName());
            }

            return loader.archiveListStartup(input);
        }

        // Walk all registered KarafFeatureLoaders and collect list of all outputs
        List<StartupKarafFeatureLoaders> outputList = new LinkedList<>();
        for (Map.Entry<String, IotdmKarafFeatureLoaderProvider> loader : this.karafFeatureLoaders.entrySet()) {
            Future<RpcResult<ArchiveListStartupOutput>> output = loader.getValue().archiveListStartup(input);
            ArchiveListStartupOutput result = null;

            try {
                if (! output.get().isSuccessful()) {
                    Collection<RpcError> errors = null;
                    errors = output.get().getErrors();

                    LOG.error("KarafFeatureLoader {}: Failed to get list of startup archives.", loader.getKey());
                    if (null != errors && !errors.isEmpty()) {
                        int i = 0;
                        for (RpcError error : errors) {
                            LOG.error("KarafFeatureLoader {}: Failed to get list of startup archives, Error {}: {}",
                                      loader.getKey(),
                                      ++i,
                                      error.toString());
                        }
                    }
                    continue;
                }

                result = output.get().getResult();
            }
            catch (Exception e) {
                LOG.error("KarafFeatureLoader {}: Failed to get list of startup archives: {}", loader.getKey(), e);
                continue;
            }

            // Only one item (for the one KarafFeatureLoader instance) should be returned
            if (result.getStartupKarafFeatureLoaders().size() != 1) {
                LOG.error("KarafFeatureLoader {}: Invalid number of items of output list of " +
                              "KarafFeatureLoader startup instances: {}",
                          loader.getKey(), result.getStartupKarafFeatureLoaders().size());
                continue;
            }

            // Add the one item into the output list
            outputList.add(result.getStartupKarafFeatureLoaders().get(0));
        }

        ArchiveListStartupOutputBuilder out = new ArchiveListStartupOutputBuilder()
            .setStartupKarafFeatureLoaders(outputList);
        return RpcResultBuilder.success(out.build()).buildFuture();
    }

    /**
     * Calls clean RPC of specific instance.
     */
    @Override
    public Future<RpcResult<CleanOutput>> clean(CleanInput input) {
        if (input == null) {
            return handleNoInput();
        }

        IotdmKarafFeatureLoaderProvider loader =
            this.karafFeatureLoaders.get(input.getKarafFeatureLoaderName());
        if (null == loader) {
            return handleNotFound(input.getKarafFeatureLoaderName());
        }

        return loader.clean(input);
    }
}
