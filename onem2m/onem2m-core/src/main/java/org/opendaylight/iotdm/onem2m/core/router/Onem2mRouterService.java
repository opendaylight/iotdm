/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.router;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceCse;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceRemoteCse;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
//import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginDbClient;
//import org.opendaylight.iotdm.plugininfra.pluginmanager.IotdmPluginRegistrationException;
//import org.opendaylight.iotdm.plugininfra.pluginmanager.Onem2mPluginManager;
import org.opendaylight.iotdm.onem2m.dbapi.Onem2mDbApiClientPlugin;
import org.opendaylight.iotdm.onem2m.dbapi.Onem2mDbApiProvider;
import org.opendaylight.iotdm.onem2m.dbapi.Onem2mPluginsDbApi;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginRegistrationException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredRemoteCses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Class implements the Router service for Onem2m CSE
 */
public class Onem2mRouterService implements Onem2mDbApiClientPlugin {
    private static final String defaultPluginName = "http";
    private static final Onem2mRouterService routerService = new Onem2mRouterService();
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mRouterService.class);
    private final Map<String, Onem2mRouterPlugin> routerServicePluginMap = new ConcurrentHashMap<>();
    private final AtomicBoolean cleanTable = new AtomicBoolean(false);
    private final ExecutorService executor;
    private static final Onem2mRoutingTable routingTable = new Onem2mRoutingTable();

    private Onem2mRouterService() {
        executor = Executors.newFixedThreadPool(32);

        // Register this instance
        try {
            Onem2mDbApiProvider.getInstance().registerDbClientPlugin(this);
        } catch (IotdmPluginRegistrationException e) {
            LOG.error("Failed to register Onem2mRouterService as DB API client: {}", e);
        }
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


    @Override
    public void dbClientStart() throws Exception {

        if (this.cleanTable.get()) {
            this.cleanRoutingTable();
        }

        List<Onem2mCse> cseBaseList = Onem2mDb.getInstance().retrieveCseBaseList();
        if (null == cseBaseList || cseBaseList.isEmpty()) {
            return;
        }

        for (Onem2mCse cseBase : cseBaseList) {
            // add cseBase into routing table
            Onem2mResource cseBaseResource = Onem2mDb.getInstance().getResource(cseBase.getResourceId());
            if (null == cseBaseResource) {
                LOG.error("Failed to get cseBaseResource of cseBase: resourceName: {}, resourceId: {}",
                          cseBase.getName(), cseBase.getResourceId());
                continue;
            }

            // retrieve CSE-ID and CSE-Type of the cseBase resource
            Optional<JSONObject> attributes =
                JsonUtils.stringToJsonObject(cseBaseResource.getResourceContentJsonString());
            if (! attributes.isPresent()) {
                LOG.error("Failed to get attributes of cseBase resource: resourceName: {}, resourceId: {}",
                          cseBase.getName(), cseBase.getResourceId());
                continue;
            }

            final String cseBaseCseId = attributes.get().optString(ResourceCse.CSE_ID, null);
            final String cseType = attributes.get().optString(ResourceCse.CSE_TYPE, null);
            if (null == cseBaseCseId || null == cseType) {
                LOG.error("Failed to get CSE-ID or CSE-Type attribute of cseBase resource: " +
                          "resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}",
                          cseBase.getName(), cseBase.getResourceId(), cseBaseCseId, cseType);
                continue;
            }

            // add the record about cseBase into routing table
            CseRoutingDataBase result = null;
            result = this.addCseBase(cseBase.getName(), cseBase.getResourceId(), cseBaseCseId, cseType);
            if (null == result) {
                LOG.error("Failed to add cseBase into routing table: " +
                          "resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}",
                          cseBase.getName(), cseBase.getResourceId(), cseBaseCseId, cseType);
                continue;
            }

            // add remoteCSEs into routing table
            List<Onem2mRegisteredRemoteCses> remoteCsesList = cseBase.getOnem2mRegisteredRemoteCses();
            if (null == remoteCsesList || remoteCsesList.isEmpty()) {
                LOG.info("Added cseBase into routing table: resourceName: {}, resourceId: {}, CSE-ID: {}, " +
                         "CSE-Type: {}, no remoteCSE resources",
                         cseBase.getName(), cseBase.getResourceId(), cseBaseCseId, cseType);
                continue;
            }

            for (Onem2mRegisteredRemoteCses remoteCse : remoteCsesList) {

                Onem2mResource remoteCseResource = Onem2mDb.getInstance().getResource(remoteCse.getResourceId());
                if (null == remoteCseResource) {
                    LOG.error("Failed to retrieve resource of remoteCse type: resourceId: {}, CSE-ID: {}, " +
                              "cseBaseCseId: {}",
                              remoteCse.getResourceId(), remoteCse.getRegisteredCseId(), cseBaseCseId);
                    continue;
                }

                // retrieve remoteCse attributes
                Optional<JSONObject> attributesRemoteCse =
                    JsonUtils.stringToJsonObject(remoteCseResource.getResourceContentJsonString());
                if (! attributesRemoteCse.isPresent()) {
                    LOG.error("Failed to get attributes of remoteCse resource: resourceId: {}, CSE-ID: {}, " +
                              "cseBaseCseId: {}",
                              remoteCse.getResourceId(), remoteCse.getRegisteredCseId(), cseBaseCseId);
                    continue;
                }

                String remoteCseResourceName = remoteCseResource.getName();
                String remoteCseCseType = attributesRemoteCse.get().optString(ResourceRemoteCse.CSE_TYPE, null);
                Boolean remoteCseRequestReachable =
                    attributesRemoteCse.get().optBoolean(ResourceRemoteCse.REQUEST_REACHABILITY);
                if (null == remoteCseResourceName || null == remoteCseCseType || null == remoteCseRequestReachable) {
                    LOG.error("Failed to get mandatory attributes of remoteCse resource: " +
                              "resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}, RequestReachability: {}, " +
                              "cseBaseCseId: {}",
                              remoteCseResourceName, remoteCse.getResourceId(), remoteCse.getRegisteredCseId(),
                              remoteCseCseType, remoteCseRequestReachable, cseBaseCseId);
                    continue;
                }

                // retrieve PoA if request unreachable
                String[] remoteCsePointOfAccess = null;
                if (! remoteCseRequestReachable) {
                    JSONArray array = attributesRemoteCse.get().optJSONArray(ResourceRemoteCse.POINT_OF_ACCESS);

                    if (null == array) {
                        LOG.error("Failed to get PoA of remoteCse resource: " +
                                  "resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}, " +
                                  "RequestReachability: {}, cseBaseCseId: {}",
                                  remoteCseResourceName, remoteCse.getResourceId(), remoteCse.getRegisteredCseId(),
                                  remoteCseCseType, remoteCseRequestReachable, cseBaseCseId);
                        // continue with next remoteCSE of the same cseBase
                        continue;
                    }

                    List<String> poaList = new LinkedList<>();
                    for(int i=0; i < array.length(); i++) {
                        poaList.add(array.getString(i));
                    }
                    remoteCsePointOfAccess = poaList.toArray(remoteCsePointOfAccess);
                }

                CseRoutingDataRemote resultRemoteCse =
                    this.addRemoteCse(remoteCseResourceName, remoteCse.getResourceId(), remoteCse.getRegisteredCseId(),
                                      remoteCseCseType, cseBase.getName(), cseBaseCseId, remoteCseRequestReachable,
                                      remoteCsePointOfAccess);
                if (null == resultRemoteCse) {
                    LOG.error("Failed to add new remoteCSE record into routing table: " +
                              "resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}, " +
                              "RequestReachability: {}, cseBaseCseId: {}",
                              remoteCseResourceName, remoteCse.getResourceId(), remoteCse.getRegisteredCseId(),
                              remoteCseCseType, remoteCseRequestReachable, cseBaseCseId);
                    continue;
                }

                LOG.info("Added new remoteCSE record into routing table: " +
                         "resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}, " +
                         "RequestReachability: {}, cseBaseCseId: {}",
                         remoteCseResourceName, remoteCse.getResourceId(), remoteCse.getRegisteredCseId(),
                         remoteCseCseType, remoteCseRequestReachable, cseBaseCseId);
            }

            LOG.info("Added cseBase into routing table: resourceName: {}, resourceId: {}, CSE-ID: {}, CSE-Type: {}",
                     cseBase.getName(), cseBase.getResourceId(), cseBaseCseId, cseType);
        }
    }

    @Override
    public void dbClientStop() {
        this.cleanTable.set(true);
        return;
    }

    @Override
    public String getPluginName() { return "Onem2mRouterService"; }

    @Override
    public void close() {
        Onem2mDbApiProvider.getInstance().unregisterDbClientPlugin(this);
    }

    public void pluginRegistration(Onem2mRouterPlugin plugin) {
        String name = plugin.getRouterPluginName().toLowerCase();
        if (routerServicePluginMap.containsKey(name) &&
            (routerServicePluginMap.get(name) != plugin)) {
            throw new IllegalArgumentException("Multiple router plugin registrations with name " + name);
        }

        routerServicePluginMap.put(name, plugin);
        LOG.info("Added default router plugin: {}", name);
    }

    public void unregister(Onem2mRouterPlugin plugin) {
        String name = plugin.getRouterPluginName().toLowerCase();
        if (routerServicePluginMap.containsKey(name)) {
            if (routerServicePluginMap.get(name) == plugin) {
                routerServicePluginMap.remove(name);
                LOG.info("Default router plugin removed: {}", name);
            }
        }
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
        String rqi = request.getPrimitiveRequestIdentifier();
        if (rqi != null) {
            responseToOrigin.setPrimitiveRequestIdentifier(rqi);
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
        response = forwardRequestRemoteCse(request, responseToOrigin, routingData);

        // Check the status code
        if (response.getPrimitiveResponseStatusCode().equals(
                Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE)) {

            // Forward to registrar CSE of this cseBase, if the cseBase is MN-CSE type
            LOG.trace("Target is unreachable through remoteCSE: {}", resourceLocator.getRemoteCseCseId());

            try {
                CseRoutingDataBase cseBase = routingTable.getCseBase(routingData.parentCseBaseName);

                if (cseBase.cseType.equals(Onem2m.CseType.MNCSE)) {
                    // Forward to registrar CSE of the cseBase
                    LOG.debug("Forwarding to the registrar CSE ({}) of the cseBase ({})",
                              cseBase.registrarCseId, cseBase.name);

                    CseRoutingDataRemote routingDataRegistrar = cseBase.getRemoteCse(cseBase.registrarCseId);
                    // Forward to registrar CSE only if it's not the same
                    if (routingDataRegistrar != routingData) {
                        response = forwardRequestRemoteCse(request, responseToOrigin, routingDataRegistrar);
                    }
                }
            } catch (NullPointerException e) {
                LOG.trace("No data to forward to registrar CSE");
            }
        }

        responseToOrigin = response;

        // forwarding end
        LOG.debug("Forwarding end, response: RID: {}, statusCode: {}",
                  responseToOrigin.getPrimitive(ResponsePrimitive.REQUEST_IDENTIFIER),
                  responseToOrigin.getPrimitiveResponseStatusCode());
        return responseToOrigin;
    }

    /**
     * Implements routing logic for one remoteCSE according to routingData
     * @param requestNextHop Request primitive for the next hop CSE
     * @param responseOrigin Response to forwarded request
     * @param routingData RemoteCSE routing data
     * @return
     */
    private ResponsePrimitive forwardRequestRemoteCse(RequestPrimitive requestNextHop,
                                                      ResponsePrimitive responseOrigin,
                                                      CseRoutingDataRemote routingData) {

        Onem2mRouterPlugin routerPlugin = null;
        CseRoutingDataBase cseBaseData = routingTable.getCseBase(routingData.parentCseBaseName);

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

                    response = routerPlugin.sendRequestBlocking(requestNextHop, nextHopUrl,
                                                                routingData.parentCseBaseCseId);
                    if (null == response) {
                        LOG.trace("No response returned by plugin: {}", routerPlugin.getRouterPluginName());
                        continue;
                    }

                    // continue if target is not reachable through this next hop
                    String statusCode = response.getPrimitiveResponseStatusCode();
                    if (null == statusCode) {
                        LOG.error("Response without status code, content: {}",
                                  response.getPrimitive(ResponsePrimitive.CONTENT));
                        continue;
                    }

                    switch (statusCode) {
                        case Onem2m.ResponseStatusCode.TARGET_NOT_REACHABLE:
                            LOG.trace("Target unreachable through next hop: {}", nextHopUrl);
                            continue;

                        case Onem2m.ResponseStatusCode.ACCESS_DENIED:
                            LOG.info("This CSEBase is unauthorized ath next hop: {}", nextHopUrl);
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

    public boolean hasRemoteCse(String cseBaseCseId, String remoteCseCseId) {
        CseRoutingDataBase data = routingTable.getCseBaseByCseId(cseBaseCseId);
        if (null == data) {
            return false;
        }

        if (null == data.getRemoteCse(remoteCseCseId)) {
            return false;
        }

        return true;
    }

    // Ineffective and ambiguous version, because cseBaseCseId is not specified
    public boolean hasRemoteCse(String remoteCseCseId) {
        CseRoutingData data = routingTable.findFirstRemoteCse(remoteCseCseId);
        if (null != data) {
            return true;
        }

        return false;
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
        builder.setFQDN(FQDN);
        if ((! builder.verify()) || (null == routingTable.updateCseBase(builder.build()))) {
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
        builder.setRegistrarCseId(registrarCseId);
        if ((! builder.verify()) || (null == routingTable.updateCseBase(builder.build()))) {
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
        builder.setPolingChannel(polingChannel);
        if ((! builder.verify()) || (null == routingTable.updateRemoteCse(builder.build()))) {
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
        if (Onem2m.ResourceType.CSE_BASE == request.getResourceType()) {
            updateRoutingTableCseBase(request);
            return;
        }

        if (Onem2m.ResourceType.REMOTE_CSE == request.getResourceType()) {
            updateRoutingTableRemoteCse(request);
            return;
        }

        LOG.trace("Request primitive with unexpected resource type passed, RT: {}",
                  request.getOnem2mResource().getResourceType());
    }

    private CseRoutingDataBase addCseBase(String resourceName, String resourceId, String cseId, String cseType) {
        // this is create, so get create builder for cseBase routing data
        CseRoutingDataBase newRoutingData = routingTable.getCseBaseAddBuilder()
                                                        .setName(resourceName)
                                                        .setResourceId(resourceId)
                                                        .setCseId(cseId)
                                                        .setCseType(cseType)
                                                        .build();

        if (null == newRoutingData) {
            LOG.error("Failed to create CSEBase routing data from request");
            return null;
        }

        return routingTable.addCseBase(newRoutingData);
    }

    /**
     * Updates routing table by data from request primitive including some CUD
     * operation with cseBase resource.
     * @param request The request primitive
     */
    private void updateRoutingTableCseBase(RequestPrimitive request) {
        final String baseCseTypeAttribute = "CSE_TYPE";
        final String baseCseIDAttribute = "CSE_ID";
        final String baseCsePassword = "CSE_PASSWORD";

        String name = request.getResourceName();
        Integer operation = -1;
        CseRoutingDataBase result = null;
        if (request.isCreate) { operation = Onem2m.Operation.CREATE; }
        if (request.isUpdate) { operation = Onem2m.Operation.UPDATE; }

        if (-1 == operation) {
            LOG.error("Failed to resolve operation type");
            return;
        }

        switch(operation) {
            case Onem2m.Operation.CREATE:
                result = addCseBase(name, request.getResourceId(),
                                    request.getPrimitive(baseCseIDAttribute),
                                    request.getPrimitive(baseCseTypeAttribute));
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

    private CseRoutingDataRemote addRemoteCse(String resourceName, String resourceId, String cseId, String cseType,
                                              String cseBaseName, String cseBaseCseId,
                                              boolean requestReachable, String[] pointOfAccess) {
        CseRoutingDataRemote result = null;
        CseRoutingDataRemote newRoutingData = null;

        // get builder for create
        newRoutingData = routingTable.getCseRemoteAddBuilder()
                                     .setName(resourceName)
                                     .setResourceId(resourceId)
                                     .setCseId(cseId)
                                     .setCseType(cseType)
                                     .setParentCseBaseName(cseBaseName)
                                     .setCseBaseCseId(cseBaseCseId)
                                     .setRequestReachable(requestReachable)
                                     .setPointOfAccess(pointOfAccess)
                                     .build();
        if (null == newRoutingData) {
            LOG.error("Failed to create remoteCSE routing table data from request");
            return result;
        }

        result = routingTable.addRemoteCse(newRoutingData);

        /*
         * If this is IN-CSE, then this is considered to be a registrar CSE of the cseBase (parent)
         */
        if (null != result && cseType.equals(Onem2m.CseType.INCSE)) {
            updateRoutingDataCseBaseRegistrarCse(cseBaseName, cseId);
        }

        return result;
    }

    /**
     * Updates routing table by data from request primitive including some CUD
     * operation with remoteCSE resource
     * @param request The request primitive
     */
    private void updateRoutingTableRemoteCse(RequestPrimitive request) {
        // retrieve parent of the remoteCSE
        //Onem2mResource parent = Onem2mDb.getInstance().getResource(trc, request.getOnem2mResource().getParentId());
        Onem2mResource parent = request.getParentOnem2mResource();
        if (null == parent) {
            LOG.error("Failed to get parent of the RemoteCSE resource");
            return;
        }

        // parent of the remoteCSE must be cseBase
        String rt = ((Integer)Onem2m.ResourceType.CSE_BASE).toString();
        if (! parent.getResourceType().equals(rt)) {
            LOG.error("Invalid RemoteCSE parent resource type: {}, expected CSEBase",
                      request.getOnem2mResource().getResourceType());
            return;
        }

        CseRoutingDataBase base = routingTable.getCseBase(parent.getName());
        if (null == base) {
            LOG.error("No such cseBase in routing table: {}", parent.getName());
            return;
        }

        // get some data common for all operations
        String cseBaseName = parent.getName();
        String remoteCseId = request.getContentAttributeString(ResourceRemoteCse.CSE_ID);
        Integer operation = request.getPrimitiveOperation();
        String cseType = request.getContentAttributeString(ResourceRemoteCse.CSE_TYPE);
        CseRoutingDataRemote result = null;
        CseRoutingDataRemote newRoutingData = null;

        switch (operation) {
            case Onem2m.Operation.CREATE:
                result = addRemoteCse(request.getResourceName(), request.getResourceId(), remoteCseId, cseType,
                                      cseBaseName, base.cseId,
                                      request.getContentAttributeBoolean(ResourceRemoteCse.REQUEST_REACHABILITY),
                                      request.getContentAttributeArray(ResourceRemoteCse.POINT_OF_ACCESS));
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
