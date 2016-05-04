/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.router;


import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceRemoteCse;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Class implements the Router service for Onem2m CSE
 */
public class Onem2mRouterService {
    private static final String defaultPluginName = "http";
    // baseCSE resource names which are used for testing/debugging purposes and
    // should not be used for routing
    private static final Set<String> baseCseBlackList = ImmutableSet.of("SYS_PERF_TEST_CSE");

    private static Onem2mRouterService routerService = new Onem2mRouterService();
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mRouterService.class);
    Map<String, Onem2mRouterPlugin> routerServicePluginMap = new HashMap<>();
    private final ExecutorService executor;
    private static final Onem2mRoutingTable routingTable = new Onem2mRoutingTable();


    private Onem2mRouterService() {
        executor = Executors.newFixedThreadPool(32);
    }

    public static Onem2mRouterService getInstance() {
        return routerService;
    }

    /**
     * All routing data are deleted
     */
    public static void cleanRoutingTable() {
        LOG.debug("Cleaning routing table");
        routingTable.cleanRoutingTable();
    }

    /**
     * Registers plugin
     * @param plugin The router plugin
     */
    public void pluginRegistration(Onem2mRouterPlugin plugin) {
        routerServicePluginMap.put(plugin.getRouterPluginName().toLowerCase(), plugin);
    }

    /**
     * Forwards Onem2m request according to the resource locator
     * @param request Onem2m request to be forwarded
     * @param resourceLocator Data about location of the target resource
     * @return Future to get Onem2m response from next hop
     */
    public Future<ResponsePrimitive> forwardRequest(RequestPrimitive request,
                                                    Onem2mDb.CseBaseResourceLocator resourceLocator) {
        return this.executor.submit(() -> routerServiceJob(request, resourceLocator));
    }

    /**
     * Resolves plugin from URL.
     * @param nextHopUrl The URL of the next hop.
     * @return Resolved plugin if success, null otherwise
     */
    private Onem2mRouterPlugin resolveRouterPlugin(String nextHopUrl) {
        // resolve plugin name
        String pluginName = null;
        try {
            URI link = new URI(nextHopUrl);
            if (null != link.getScheme()) {
                pluginName = link.getScheme().toLowerCase();
            } else {
                LOG.trace("Unable to resolve protocol plugin name from URL: {}", nextHopUrl);
                LOG.trace("Using default plugin: {}", Onem2mRouterService.defaultPluginName);
                pluginName = Onem2mRouterService.defaultPluginName;
            }
        } catch (URISyntaxException e) {
            LOG.error("Dropping notification: bad URL: {}", nextHopUrl);
            return null;
        }

        // check if the plugin is supported
        if (! routerServicePluginMap.containsKey(pluginName)) {
            LOG.trace("Unsupported protocol for forwarding: {}, URL: {}", pluginName, nextHopUrl);
            return null;
        }

        return routerServicePluginMap.get(pluginName);
    }

    /**
     * Implements the routing logic, makes decision to which remoteCSE the
     * request will be forwarded.
     * @param request Onem2m request to be routed
     * @param resourceLocator Locator or the target resource
     * @return Onem2m response
     */
    private ResponsePrimitive routerServiceJob(RequestPrimitive request,
                                               Onem2mDb.CseBaseResourceLocator resourceLocator) {
        ResponsePrimitive responseToOrigin = new ResponsePrimitive();

        // if the request had a REQUEST_IDENTIFIER, return it in the response so client can correlate
        // this must be the first statement as the rqi must be in the error response
        String rqi = request.getPrimitive(RequestPrimitive.REQUEST_IDENTIFIER);
        if (rqi != null) {
            responseToOrigin.setPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER, rqi);
        } else {
            responseToOrigin.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                                  "REQUEST_IDENTIFIER(" + RequestPrimitive.REQUEST_IDENTIFIER + ") not specified");
            return responseToOrigin;
        }

        // get remote CSE routing data
        CseRoutingDataRemote routingData = routingTable.findFirstRemoteCse(resourceLocator.getRemoteCseCseId());
        if (null == routingData) {
            responseToOrigin.setRSC(Onem2m.ResponseStatusCode.NOT_FOUND,
                                    "No routing data found for remoteCSE: " + resourceLocator.getRemoteCseCseId());
            LOG.debug("Failed to found routing data for remoteCSE ({}), target URI: {}",
                      resourceLocator.getRemoteCseCseId(), resourceLocator.getTargetURI());
            return responseToOrigin;
        }

        // call the blocking send implemented by the plugin
        LOG.debug("Forwarding request, RID: {}, URI: {}", rqi, resourceLocator.getTargetURI());
        ResponsePrimitive response = null;
        response = forwardRequest(request, responseToOrigin, routingData);

        // Check the status code
        if (response.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE).equals(
                Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE)) {

            // Forward to registrar CSE of this cseBase, if the cseBase is MN-CSE type
            LOG.trace("Target is unreachable through remoteCSE: {}", resourceLocator.getRemoteCseCseId());

            try {
                CseRoutingDataBase cseBase = routingTable.getCseBase(routingData.parentCseBaseName);

                if (cseBase.cseType.equals(Onem2m.CseType.MNCSE)) {
                    // Forward to registrar CSE of the cseBase
                    LOG.debug("Forwarding to the registrar CSE ({}) of the cseBase ({})",
                              cseBase.registrarCseId, cseBase.name);

                    routingData = cseBase.getRemoteCse(cseBase.registrarCseId);
                    response = forwardRequest(request, responseToOrigin, routingData);
                }
            } catch (NullPointerException e) {
                LOG.trace("No data to forward to registrar CSE");
            }
        }

        responseToOrigin = response;

        // forwarding end
        LOG.debug("Forwarding end, response: RID: {}, statusCode: {}",
                  responseToOrigin.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER),
                  responseToOrigin.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE));
        return responseToOrigin;
    }

    /**
     * Implements routing logic for one remoteCSE according to routingData
     * @param requestNextHop Request primitive for the next hop CSE
     * @param responseOrigin Response to forwarded request
     * @param routingData RemoteCSE routing data
     * @return
     */
    private ResponsePrimitive forwardRequest(RequestPrimitive requestNextHop,
                                             ResponsePrimitive responseOrigin,
                                             CseRoutingDataRemote routingData) {

        Onem2mRouterPlugin routerPlugin = null;
        try {
            if (routingData.requestReachable && (null != routingData.pointOfAccess)) {
                // loop over the pointOfAccess URIs and try to forward the request there
                ResponsePrimitive response = null;
                for (String nextHopUrl : routingData.pointOfAccess) {

                    routerPlugin = resolveRouterPlugin(nextHopUrl);
                    if (null == routerPlugin) {
                        LOG.trace("Failed to resolve plugin from next hop URL: {}", nextHopUrl);
                        continue;
                    }

                    response = routerPlugin.sendRequestBlocking(requestNextHop, nextHopUrl);
                    if (null == response) {
                        LOG.trace("No response returned by plugin: {}", routerPlugin.getRouterPluginName());
                        continue;
                    }

                    // continue if target is not reachable through this next hop
                    switch (response.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE)) {
                        case Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE:
                            LOG.trace("Target unreachable through next hop: {}", nextHopUrl);
                            continue;

                        default:
                            return response;
                    }
                }
            } else if (null != routingData.polingChannel){
                // TODO use PolingChannel
                LOG.error("Forwarding through pooling channel not implemented");
                responseOrigin.setRSC(Onem2m.ResponseStatusCode.NOT_IMPLEMENTED,
                                      "Forwarding by polling channel not implemented");
                return responseOrigin;
            }
        } catch(Exception ex) {
            // let's catch all exceptions to avoid crash of onem2mCore because of buggy plugin
            LOG.error("Forwarding of request by plugin: {}, failed: {}",
                      ((null != routerPlugin) ? routerPlugin.getRouterPluginName() : "null"),
                      ex);
            responseOrigin.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Forwarding failed");
            return responseOrigin;
        }

        // forwarding failed
        LOG.trace("Failed to forward request to the remoteCSE");
        responseOrigin.setRSC(Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE,
                              "RemoteCSE unreachable");
        return responseOrigin;
    }

    /**
     * Method checks whether the routing table includes cseBase with
     * given name.
     * @param name The cseBase name.
     * @return true if exists, false otherwise
     */
    public boolean hasCseBaseName(String name) {
        return (null != routingTable.getCseBase(name));
    }

    /**
     * Method checks whether the routing table includes cseBase with
     * cseId passed as argument.
     * @param cseId The cseId of cseBase
     * @return true if exists, false otherwise
     */
    public boolean hasCseBaseCseId(String cseId) {
        return (null != routingTable.getCseBaseByCseId(cseId));
    }

    /**
     * Method checks whether the routing table includes cseBase with
     * name and cseId passed as arguments.
     * @param name The name of cseBase
     * @param cseId The cseId of cseBase
     * @return true if exists, false otherwise
     */
    public boolean hasCseBaseNameCseId(String name, String cseId) {
        CseRoutingDataBase data = routingTable.getCseBase(name);
        if (null == data) {
            return false;
        }
        return (data.cseId.equals(cseId));
    }


    /*
     * Methods processing request primitives and updates
     * routing table
     */

    /**
     * Method sets FQDN for cseBase specified by name
     * @param cseBaseName Name of the cseBase
     * @param FQDN New FQDN to be set
     */
    public void updateRoutingDataCseBaseFqdn(@Nonnull String cseBaseName, String FQDN) {
        // get builder for update
        CseRoutingDataBaseBuilder builder = routingTable.getCseBaseUpdateBuilder(cseBaseName);
        if (null == builder) {
            LOG.error("Failed to obtain CSEBase routing data builder for name: {}, FQDN: {}", cseBaseName, FQDN);
            return;
        }

        // get old value for debugging
        String oldValue = routingTable.getCseBase(cseBaseName).FQDN;

        // updated and check if successful
        if (null == routingTable.updateCseBase(builder.setFQDN(FQDN).build())) {
            LOG.error("Failed to update CSEBase routing data, name: {}, FQDN {}", cseBaseName, FQDN);
        } else {
            LOG.trace("RoutingData CSEBase: {}, FQDN changed: old: {}, new: {}",
                      cseBaseName, oldValue, FQDN);
            routingTable.dumpDebug("UPDATED CSEBase: " + cseBaseName + " with FQDN: " + FQDN);
        }
    }

    /**
     * Updates registrar CSE of cseBase identified by name
     * @param cseBaseName The name of the cseBase
     * @param registrarCseId New registrar CSE of the cseBase
     */
    public void updateRoutingDataCseBaseRegistrarCse(@Nonnull String cseBaseName, String registrarCseId) {
        // get builder for update
        CseRoutingDataBaseBuilder builder = routingTable.getCseBaseUpdateBuilder(cseBaseName);
        if (null == builder) {
            LOG.error("Failed to obtain CSEBase routing data builder for name: {}, registrarCse: {}",
                      cseBaseName, registrarCseId);
            return;
        }

        // get old value for debugging
        String oldValue = routingTable.getCseBase(cseBaseName).registrarCseId;

        // updated and check if successful
        if (null == routingTable.updateCseBase(builder.setRegistrarCseId(registrarCseId).build())) {
            LOG.error("Failed to update CSEBase routing data, name: {}, registrarCse {}", cseBaseName, registrarCseId);
        } else {
            LOG.trace("RoutingData CSEBase ({}) registrarCSE changed: old: {}, new: {}", cseBaseName, oldValue, registrarCseId);
            routingTable.dumpDebug("UPDATED CSEBase: " + cseBaseName + " with registrarCse: " + registrarCseId);
        }
    }

    /**
     * Updates poling channel of the remoteCSE specified by name of its
     * cseBase and cseId of the remoteCSE itself.
     * @param cseBaseName Name of the cseBase of the remoteCSE
     * @param remoteCseId cseId of the remoteCSE
     * @param polingChannel New poling channel to be set
     */
    public void updateRoutingDataRemoteCsePolingChannel(@Nonnull String cseBaseName, @Nonnull String remoteCseId,
                                                        String polingChannel) {
        // get builder for update
        CseRoutingDataRemoteBuilder builder = routingTable.getCseRemoteUpdateBuilder(cseBaseName, remoteCseId);
        if (null == builder) {
            LOG.error("Failed to obtain remoteCse routing data builder for cseBaseName: {}, remoteCseId: {}",
                      cseBaseName, remoteCseId);
            return;
        }

        // get old value for debugging
        String oldValue = routingTable.getRemoteCse(cseBaseName, remoteCseId).polingChannel;

        // update and check if successful
        if (null == routingTable.updateRemoteCse(builder.setPolingChannel(polingChannel).build())) {
            LOG.error("Failed to update remoteCSE poling channel, cseBaseName: {}, remoteCseId: {}",
                      cseBaseName, remoteCseId);
        } else {
            LOG.trace("RoutingData CSEBase: {}, remoteCSE: {}, polingChannel changed: old: {}, new: {}",
                      cseBaseName, remoteCseId, oldValue, polingChannel);
            routingTable.dumpDebug("UPDATED RemoteCSE, base: " + remoteCseId + ", cseId: " + remoteCseId);
        }
    }

    /**
     * Updates routing table by data from request primitive.
     * Only updates of cseBase and remoteCSE resources are expected.
     * @param request The request primitive
     */
    public void updateRoutingTable(RequestPrimitive request) {
        if (Onem2m.ResourceType.CSE_BASE.equals(request.getOnem2mResource().getResourceType())) {
            updateRoutingTableCseBase(request);
            return;
        }

        if (Onem2m.ResourceType.REMOTE_CSE.equals(request.getOnem2mResource().getResourceType())) {
            updateRoutingTableRemoteCse(request);
            return;
        }

        LOG.error("Request primitive with unexpected resource type passed, RT: {}",
                  request.getOnem2mResource().getResourceType());
    }

    /**
     * Updates routing table by data from request primitive including some CUD
     * operation with cseBase resource.
     * @param request The request primitive
     */
    private void updateRoutingTableCseBase(RequestPrimitive request) {
        final String baseCseTypeAttribute = "CSE_TYPE";
        final String baseCseIDAttribute = "CSE_ID";

        String name = request.getResourceName();
        if (baseCseBlackList.contains(name)) {
            LOG.debug("BaseCSE name ({}) at blacklist, skipping", name);
            return;
        }

        //String operation = request.getPrimitive(RequestPrimitive.OPERATION);
        String operation = null;
        CseRoutingDataBase result = null;
        if (request.isCreate) { operation = Onem2m.Operation.CREATE; }
        if (request.isUpdate) { operation = Onem2m.Operation.UPDATE; }

        if (null == operation) {
            LOG.error("Failed to resolve operation type");
            return;
        }

        switch(operation) {
            case Onem2m.Operation.CREATE:
                // this is create, so get create builder for cseBase routing data
                CseRoutingDataBase newRoutingData = routingTable.getCseBaseAddBuilder()
                    .setName(name)
                    .setResourceId(request.getResourceId())
                    .setCseId(request.getPrimitive(baseCseIDAttribute))
                    .setCseType(request.getPrimitive(baseCseTypeAttribute))
                    .build();
                if (null == newRoutingData) {
                    LOG.error("Failed to create CSEBase routing data from request");
                    break;
                }

                result = routingTable.addCseBase(newRoutingData);
                break;

            //case Onem2m.Operation.UPDATE:
                // FQDN and registrar CSE can be updated from other places
                // there's nothing else to be updated

            case Onem2m.Operation.DELETE:
                result = routingTable.removeCseBase(name);
                break;

            default:
                LOG.error("Request primitive with unexpected operation passed: Operation: {}, CseBase name: {}",
                          operation, name);
                return;
        }

        if (null == result) {
            LOG.error("Routing table update failed, name: {}, operation: {}", name, operation);
        } else {
            routingTable.dumpDebug("RoutingTable Changed: CSEBase: " + name);
        }
    }

    /**
     * Updates routing table by data from request primitive including some CUD
     * operation with remoteCSE resource
     * @param request The request primitive
     */
    private void updateRoutingTableRemoteCse(RequestPrimitive request) {
        // retrieve parent of the remoteCSE
        Onem2mResource parent = Onem2mDb.getInstance().getResource(request.getOnem2mResource().getParentId());
        if (null == parent) {
            LOG.error("Failed to get parent of the RemoteCSE resource");
            return;
        }

        // parent of the remoteCSE must be cseBase
        if (! parent.getResourceType().equals(Onem2m.ResourceType.CSE_BASE)) {
            LOG.error("Invalid RemoteCSE parent resource type: {}, expected CSEBase",
                      request.getOnem2mResource().getResourceType());
            return;
        }

        // get some data common for all operations
        String cseBaseName = parent.getName();
        String remoteCseId = request.getContentAttributeString(ResourceRemoteCse.CSE_ID);
        String operation = request.getPrimitive(RequestPrimitive.OPERATION);
        String cseType = request.getContentAttributeString(ResourceRemoteCse.CSE_TYPE);
        CseRoutingDataRemote result = null;
        CseRoutingDataRemote newRoutingData = null;

        switch (operation) {
            case Onem2m.Operation.CREATE:
                // get builder for create
                newRoutingData = routingTable.getCseRemoteAddBuilder()
                        .setName(request.getResourceName())
                        .setResourceId(request.getResourceId())
                        .setCseId(remoteCseId)
                        .setCseType(cseType)
                        .setParentCseBaseName(cseBaseName)
                        .setRequestReachable(
                                request.getContentAttributeBoolean(ResourceRemoteCse.REQUEST_REACHABILITY))
                        .setPointOfAccess(
                                request.getContentAttributeArray(ResourceRemoteCse.POINT_OF_ACCESS))
                        .build();
                if (null == newRoutingData) {
                    LOG.error("Failed to create remoteCSE routing table data from request");
                    break;
                }

                result = routingTable.addRemoteCse(newRoutingData);

                /*
                 * If this is IN-CSE, then this is considered to be a registrar CSE of the cseBase (parent)
                 */
                if (null != result && cseType.equals(Onem2m.CseType.INCSE)) {
                    updateRoutingDataCseBaseRegistrarCse(cseBaseName, remoteCseId);
                }
                break;

            case Onem2m.Operation.UPDATE:
                /* According to TS-0004, 7.3.4.1, only RR and PoA can be
                 * updated (from attributes that are interesting for
                 * routing table)
                 * NOTE: polingChanel is child resource of remoteCSE,
                 *       should be updated directly by appropriate method
                 */
                Boolean newValue = request.getContentAttributeBoolean(ResourceRemoteCse.REQUEST_REACHABILITY);
                String[] newPoA = request.getContentAttributeArray(ResourceRemoteCse.POINT_OF_ACCESS);
                if (null == newValue && null == newPoA) {
                    LOG.trace("Update without routing data, nothing to change");
                    return;
                }

                CseRoutingDataRemoteBuilder builder = routingTable.getCseRemoteUpdateBuilder(cseBaseName, remoteCseId);
                if (null == builder) {
                    LOG.trace("Failed to create builder for remoteCSE routing data update, cseBase: {}, remoteCse: {}",
                              cseBaseName, remoteCseId);
                    break;
                }

                // don't overwrite old values if they are not being updated
                if (null != newValue) {
                    builder.setRequestReachable(newValue);
                }
                if (null != newPoA) {
                    builder.setPointOfAccess(newPoA);
                }

                newRoutingData = builder.build();
                if (null == newRoutingData) {
                    LOG.error("Failed to update remoteCSE routing table data from request");
                    break;
                }

                result = routingTable.updateRemoteCse(newRoutingData);
                break;

            case Onem2m.Operation.DELETE:
                result = routingTable.removeRemoteCse(cseBaseName, remoteCseId);
                break;
        }

        if (null == result) {
            LOG.error("Routing table of CSEBase {} update failed, remoteCseId: {}, operation: {}",
                      cseBaseName, remoteCseId, operation);
        } else {
            routingTable.dumpDebug("RoutingTable changed: remoteCSE: " + remoteCseId + " CSEBase: " + cseBaseName);
        }
    }
}
