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
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
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

    // these are constants for test purposes
    public static final String AE_APP_ID = "APP_ID";
    public static final String AE_ONTOLOGY_REF = "http://ontology/ae";
    public static final String AE_APP_NAME = "APP_NAME";
    public static final String AENAME = "AE_NAME";
    public static final String CONTAINER_NAME = "CONTAINER_NAME";
    public static final Integer CONTAINER_MAX_NR_INSTANCES = 5;
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
            return false;
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
        if (!createContainerTest(resourceList, resourceNameList.get(resourceNameList.size() - 1), resourceNameList))
            return false;
        if (!createContentInstanceTest(resourceList, resourceNameList.get(resourceNameList.size() - 1), resourceNameList))
            return false;
        return true;
    }

    /**
     * Basic AE
     * @param resourceList
     * @return
     */
    private boolean createAETest2(List<String> resourceList, String toUri, List<String> resourceNameList) {

        Onem2mRequestPrimitiveClient onem2mRequest;
        boolean success = true;

        String aeString = new ResourceAEBuilder()
            .setAppName(AE_APP_NAME).setAppId(AE_APP_ID)
            .setRequestReachability(true)
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
                .setPrimitiveContent(aeString)
                .setResultContent("1")
                .setName(AE_APP_NAME)
                .build();

        resourceNameList.add(toUri + "/" + AE_APP_NAME);

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);
        String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
        try {
            Onem2mResponse or = new Onem2mResponse(responseContent);
            JSONObject j = or.getJSONObject();
            String resourceId = j.getString(BaseResource.RESOURCE_ID);
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

    private boolean createAETest(List<String> resourceList, String toUri, List<String> resourceNameList) {

        Onem2mRequestPrimitiveClient aeRequest;
        AE aeBuilder;

        resourceNameList.add(toUri + "/" + AE_APP_NAME);

        aeBuilder = new AE();
        aeBuilder.setTo(toUri);
        aeBuilder.setOperationCreate();
        aeBuilder.setName(AE_APP_NAME);
        aeBuilder.setAppName(AE_APP_NAME);
        aeBuilder.setAppId(AE_APP_ID);
        aeBuilder.setRequestReachability(true);
        aeBuilder.setOntologyRef(AE_ONTOLOGY_REF);
        aeBuilder.setResultContent("1");
        aeRequest = aeBuilder.build();
        Onem2mResponsePrimitiveClient onem2mResponse = aeRequest.send(onem2mService);
        if (!onem2mResponse.responseOk()) {
            LOG.error("AE create request: {}", onem2mResponse.getError());
            return false;
        }
        Onem2mAEResponse aeResponse = new Onem2mAEResponse(onem2mResponse.getContent());
        if (!aeResponse.responseOk()) {
            LOG.error("AE create request: {}", aeResponse.getError());
            return false;
        }

        String resourceId = aeResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for AE create");
            return false;
        }
        resourceList.add(resourceId);

//        if (!AE_APP_NAME.contentEquals(aeResponse.getAppName())) {
//            LOG.error("ae_app_name mismatch: expected: {}, received: {}", AE_APP_NAME, aeResponse.getAppName());
//            return false;
//        }

        return true;
    }

    /**
     * Basic Container
     * @param resourceList
     * @return
     */
    private boolean createContainerTest(List<String> resourceList, String toUri, List<String> resourceNameList) {

        Container b;

        b = new Container();
        b.setTo(toUri);
        b.setOperationCreate();
        b.setMaxNrInstances(1);
        b.setCreator(null);
        b.setMaxByteSize(100);
        b.setOntologyRef("http:/whoa/nelly");
        b.setName(CONTAINER_NAME);
        Onem2mRequestPrimitiveClient req = b.build();

        resourceNameList.add(toUri + "/" + CONTAINER_NAME);

        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mContainerResponse ctrResponse = new Onem2mContainerResponse(res.getContent());
        if (!ctrResponse.responseOk()) {
            LOG.error("Container create request: {}", ctrResponse.getError());
            return false;
        }

        String resourceId = ctrResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for Container create");
            return false;
        }
        resourceList.add(resourceId);

        return true;
    }

    /**
     * Basic ContentInstance
     * @param resourceList
     * @return
     */
    private boolean createContentInstanceTest(List<String> resourceList, String toUri, List<String> resourceNameList) {

        ContentInstance b;

        b = new ContentInstance();
        b.setTo(toUri);
        b.setOperationCreate();
        b.setName(CONTENT_INSTANCE_NAME);
        b.setContent(CONTENT_INSTANCE_CONTENT);
        b.setContentInfo(CONTENT_INSTANCE_CONTENT_INFO);
        b.setOntologyRef(CONTENT_INSTANCE_ONTOLOGY_REF);
        Onem2mRequestPrimitiveClient req = b.build();

        resourceNameList.add(toUri + "/" + CONTENT_INSTANCE_NAME);


        Onem2mResponsePrimitiveClient res = req.send(onem2mService);
        if (!res.responseOk()) {
            LOG.error(res.getError());
            return false;
        }
        Onem2mContentInstanceResponse ciResponse = new Onem2mContentInstanceResponse(res.getContent());
        if (!ciResponse.responseOk()) {
            LOG.error("Container create request: {}", ciResponse.getError());
            return false;
        }

        String resourceId = ciResponse.getResourceId();
        if (resourceId == null) {
            LOG.error("Create cannot parse resourceId for ContentInstance create");
            return false;
        }
        resourceList.add(resourceId);

        return true;
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
                    //.setTo(resourceId)
                    .setTo(resourceName)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setOperationRetrieve()
                    .setResultContent("1")
                    .build();

            ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                Onem2mResponse or = new Onem2mResponse(responseContent);
                JSONObject j = or.getJSONObject();
                if (!resourceId.contentEquals(j.getString(BaseResource.RESOURCE_ID))) {
                    LOG.error("Retrieve resourceId: {} NOT FOUND id: {} expected: {}",
                            resourceId, resourceName, j.getString(BaseResource.RESOURCE_ID));
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
                    .setTo(resourceName)
                    .setFrom("")
                    .setRequestIdentifier("RQI_1234")
                    .setResultContent("1")
                    .setOperationDelete()
                    .build();

            ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);
            String rscString = onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE);
            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                Onem2mResponse or = new Onem2mResponse(responseContent);
                JSONObject j = or.getJSONObject();
                if (!resourceId.contentEquals(j.getString(BaseResource.RESOURCE_ID))) {
                    LOG.error("Retrieve resourceId: {} NOT FOUND id: {} expected: {}",
                            resourceId, resourceName, j.getString(BaseResource.RESOURCE_ID));
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
                .setCreator(null)
                .setMaxNrInstances(CONTAINER_MAX_NR_INSTANCES)
                .setOntologyRef(CONTAINER_ONTOLOGY_REF)
                .setMaxByteSize(100)
                .build();

        onem2mRequest = new Onem2mRequestPrimitiveClientBuilder()
                .setProtocol(Onem2m.Protocol.NATIVEAPP)
                .setContentFormat(Onem2m.ContentFormat.JSON)
                .setTo(toURI)
                .setFrom("")
                .setRequestIdentifier("RQI_1234")
                .setResourceType(Onem2m.ResourceType.CONTAINER)
                .setOperationCreate()
                .setPrimitiveContent(containerString)
                .setName(CONTAINER_NAME)
                .setResultContent("1")
                .build();

        ResponsePrimitive onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);

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
                    .setPrimitiveContent(contentString)
                    .setResultContent("1")
                    .build();

            onem2mResponse = Onem2m.serviceOnem2mRequest(onem2mRequest, onem2mService);

            String responseContent = onem2mResponse.getPrimitive(ResponsePrimitive.CONTENT);
            try {
                Onem2mResponse or = new Onem2mResponse(responseContent);
                JSONObject j = or.getJSONObject();
                String resourceId = j.getString(BaseResource.RESOURCE_ID);
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
