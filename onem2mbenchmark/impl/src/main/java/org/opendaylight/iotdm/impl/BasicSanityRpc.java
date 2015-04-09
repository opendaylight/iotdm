/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.client.*;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicSanityRpc {

    private static final Logger LOG = LoggerFactory.getLogger(BasicSanityRpc.class);
    private Onem2mService onem2mService;
    public long createsPerSec, retrievesPerSec, updatesPerSec, deletesPerSec;

    public BasicSanityRpc(Onem2mService onem2mService) {
        this.onem2mService = onem2mService;
    }

    public static final String AE_ID = "AE-ID";
    public static final String AE_APP_ID = "APP_ID";
    public static final String AE_ONTOLOGY_REF = "http://ontology/ae";
    public static final String AE_APP_NAME = "APP_NAME";
    public static final String AENAME = "AE_NAME";
    public static final String CONTAINER_NAME = "CONTAINER_NAME";
    public static final String CONTAINER_CREATOR = "Creator";
    public static final String CONTAINER_MAX_NR_INSTANCES = "5";
    public static final String CONTAINER_MAX_BYTE_SIZE = "5";
    public static final String CONTAINER_INFO = "5";
    public static final String CONTAINER_ONTOLOGY_REF = "http://ontology/container";
    public static final String CONTENT_INSTANCE_NAME = "ContentInstanceName";
    public static final String CONTENT_INSTANCE_CONTENT = "SomeCoolSensorMeasurement";
    public static final String CONTENT_INSTANCE_CONTENT_INFO = "ContentInfo";
    public static final String CONTENT_INSTANCE_ONTOLOGY_REF = "http://ontology/content/instance";

    /**
     * Run various test suites
     * @return
     */
    public boolean runTestSuite() {

        if (!runBasicCrud() || !runContentInstanceLatestOldestTest()) {
            return false;
        }

        return true;
    }

    /**
     * Run a basic CRUD for create AE, container, contentInstance.
     * @return
     */
    public boolean runBasicCrud() {

        List<String> resourceList = new ArrayList<String>();
        List<String> toList = new ArrayList<String>();
        if (createTest(resourceList, toList) &&
                retrieveTest(resourceList, toList) &&
                deleteTest(resourceList, toList)) {
            LOG.info("runTestSuite: all tests finished");
        } else {
            LOG.error("runTestSuite: tests failed early");
        }
        return true;
    }

    /**
     * Basic sanity test
     * @param resourceList
     * @return
     */
    private boolean createTest(List<String> resourceList, List<String> resourceNameList) {

        String to = "/" + Onem2m.SYS_PERF_TEST_CSE;
        if (!createAETest(resourceList, to, resourceNameList))
            return false;
        if (!createContainerTest(resourceList, resourceNameList.get(resourceNameList.size()-1), resourceNameList))
            return false;
        if (!createContentInstanceTest(resourceList, resourceNameList.get(resourceNameList.size()-1), resourceNameList))
            return false;
        return true;
    }

    /**
     * Basic AE
     * @param resourceList
     * @return
     */
    private boolean createAETest(List<String> resourceList, String toUri, List<String> resourceNameList) {

        Onem2mRequestPrimitiveClient onem2mRequest;
        boolean success = true;

        String aeString = new ResourceAEBuilder()
            .setAppName(AE_APP_NAME)
            .setAEId(AE_ID)
            .setAppId(AE_APP_ID)
            .setOntologyRef(AE_ONTOLOGY_REF)
            .build();

        onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(toUri)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType(Onem2m.ResourceType.AE)
                .setOperationCreate()
                .setContent(aeString)
                .setName(AE_APP_NAME)
                .build();

        resourceNameList.add(toUri + "/" + AE_APP_NAME);

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        try {
            JSONObject j = new JSONObject(responseContent);
            String resourceId = j.getString(ResourceContent.RESOURCE_ID);
            if (resourceId == null) {
                LOG.error("Create cannot parse resourceId for AE create");
                success = false;
            }
            resourceList.add(resourceId);
        } catch (JSONException e) {
            LOG.error("Create parse responseContent error: {}", e);
            success = false;
        }

        return success;
    }

    /**
     * Basic Container
     * @param resourceList
     * @return
     */
    private boolean createContainerTest(List<String> resourceList, String toUri, List<String> resourceNameList) {

        Onem2mRequestPrimitiveClient onem2mRequest;
        boolean success = true;

        String containerString = new ResourceContainerBuilder()
            .setCreator(CONTAINER_CREATOR)
            .setMaxNrInstances(CONTAINER_MAX_NR_INSTANCES)
            .setOntologyRef(CONTAINER_ONTOLOGY_REF)
            .setMaxByteSize("100")
            .setMaxInstanceAge("1")
            .build();

        onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(toUri)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType(Onem2m.ResourceType.CONTAINER)
                .setOperationCreate()
                .setContent(containerString)
                .setName(CONTAINER_NAME)
                .build();

        resourceNameList.add(toUri + "/" + CONTAINER_NAME);

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        try {
            JSONObject j = new JSONObject(responseContent);
            String resourceId = j.getString(ResourceContent.RESOURCE_ID);
            if (resourceId == null) {
                LOG.error("Create cannot parse resourceId for Container create");
                success = false;
            }
            resourceList.add(resourceId);
        } catch (JSONException e) {
            LOG.error("Create parse responseContent error: {}", e);
            success = false;
        }

        return success;
    }

    /**
     * Basic ContentInstance
     * @param resourceList
     * @return
     */
    private boolean createContentInstanceTest(List<String> resourceList, String toUri, List<String> resourceNameList) {

        Onem2mRequestPrimitiveClient onem2mRequest;
        boolean success = true;

        String contentString = new ResourceContentInstanceBuilder()
            .setContent(CONTENT_INSTANCE_CONTENT)
            .setContentInfo(CONTENT_INSTANCE_CONTENT_INFO)
            .setOntologyRef(CONTENT_INSTANCE_ONTOLOGY_REF)
            .build();

        onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(toUri)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType(Onem2m.ResourceType.CONTENT_INSTANCE)
                .setOperationCreate()
                .setContent(contentString)
                .setName(CONTENT_INSTANCE_NAME)
                .build();

        resourceNameList.add(toUri + "/" + CONTENT_INSTANCE_NAME);

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        try {
            JSONObject j = new JSONObject(responseContent);
            String resourceId = j.getString(ResourceContent.RESOURCE_ID);
            if (resourceId == null) {
                LOG.error("Create cannot parse resourceId for AE create");
                success = false;
            }
            resourceList.add(resourceId);
        } catch (JSONException e) {
            LOG.error("Create parse responseContent error: {}", e);
            success = false;
        }

        return success;
    }

    /**
     * Retrieve all the created resources (this ensures they were created).
     * @param resourceList
     * @return
     */
    private boolean retrieveTest(List<String> resourceList, List<String> resourceNameList) {

        boolean success = true;

        for (int i = 0; i < resourceList.size(); i++) {
            String resourceId = resourceList.get(i);
            String resourceName = resourceNameList.get(i);

            Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                    .setProtocol(Onem2m.Protocol.NATIVEAPP)
                    .setContentFormat(Onem2m.ContentFormat.JSON)
                    .setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setOperationRetrieve()
                    .build();

            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
                if (!resourceName.contentEquals(j.getString(ResourceContent.RESOURCE_NAME))) {
                    LOG.error("Retrieve resourceId: {} NOT FOUND name: {} expected: {}",
                            resourceId, resourceName, j.getString(ResourceContent.RESOURCE_NAME));
                    success = false;
                    break;
                }
            } catch (JSONException e) {
                LOG.error("Retrieve parse responseContent error: {}", e);
                success = false;
                break;
            }
        }
        return success;
    }

    /**
     * Delete the previously created resources (again, this ensures the resources  actually existed)
     * @param resourceList
     * @return
     */
    private boolean deleteTest(List<String> resourceList, List<String> resourceNameList) {

        boolean success = true;

        // delete in order from leaf to root, otherwise deleting the AE will delete all below it
        for (int i = resourceList.size() - 1; i >= 0; i--) {
            String resourceId = resourceList.get(i);
            String resourceName = resourceNameList.get(i);

            Onem2mRequestPrimitiveClient onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                    .setProtocol(Onem2m.Protocol.NATIVEAPP)
                    .setContentFormat(Onem2m.ContentFormat.JSON)
                    .setTo("/" + Onem2m.SYS_PERF_TEST_CSE + "/" + resourceId)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setOperationDelete()
                    .build();

            ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
                if (!resourceName.contentEquals(j.getString(ResourceContent.RESOURCE_NAME))) {
                    LOG.error("Retrieve resourceId: {} NOT FOUND name: {} expected: {}",
                            resourceId, resourceName, j.getString(ResourceContent.RESOURCE_NAME));
                    success = false;
                    break;
                }
            } catch (JSONException e) {
                LOG.error("Delete parse responseContent error: {}", e);
                success = false;
                break;
            }
        }
        return success;
    }

    /**
     * Run a basic CRUD for create AE, container, contentInstance.
     * @return
     */
    public boolean runContentInstanceLatestOldestTest() {


        Onem2mRequestPrimitiveClient onem2mRequest;
        boolean success = true;

        String toURI = "/" + Onem2m.SYS_PERF_TEST_CSE;
        String containerString = new ResourceContainerBuilder()
                .setCreator(CONTAINER_CREATOR)
                .setMaxNrInstances(CONTAINER_MAX_NR_INSTANCES)
                .setOntologyRef(CONTAINER_ONTOLOGY_REF)
                .setMaxByteSize("100")
                .setMaxInstanceAge("1")
                .build();

        onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(toURI)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType(Onem2m.ResourceType.CONTAINER)
                .setOperationCreate()
                .setContent(containerString)
                .setName(CONTAINER_NAME)
                .build();

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

        toURI += "/" + CONTAINER_NAME;

        for (Integer i = 0; i < 4; i++) {

            String contentString = new ResourceContentInstanceBuilder()
                    .setContent(CONTENT_INSTANCE_CONTENT + i.toString())
                    .setContentInfo(CONTENT_INSTANCE_CONTENT_INFO + i.toString())
                    .setOntologyRef(CONTENT_INSTANCE_ONTOLOGY_REF + i.toString())
                    .build();

            onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                    .setProtocol(Onem2m.Protocol.NATIVEAPP)
                    .setContentFormat(Onem2m.ContentFormat.JSON)
                    .setTo(toURI)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setResourceType(Onem2m.ResourceType.CONTENT_INSTANCE)
                    .setOperationCreate()
                    .setContent(contentString)
                    .build();

            onem2mResponse = Onem2m.serviceOnenm2mRequest(onem2mRequest, onem2mService);

            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                JSONObject j = new JSONObject(responseContent);
                String resourceId = j.getString(ResourceContent.RESOURCE_ID);
                if (resourceId == null) {
                    LOG.error("Create cannot parse resourceId for cin create");
                    success = false;
                }
            } catch (JSONException e) {
                LOG.error("Create parse responseContent error: {}", e);
                success = false;
            }
        }

        // get the content instance resources by using ...//latest nad /oldest
        
        return success;
    }
}
