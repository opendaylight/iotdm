/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.tsdr.impl;

import static java.lang.Thread.sleep;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mtsdr.rev160210.onem2m.tsdr.config.Onem2mTargetDesc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mTsdrSender {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mTsdrSender.class);
    public static final long ONEM2M_TSDR_SENDER_BATCH_SEND_TIMER = 15;

    private TsdrCollectorSpiService tsdrService;
    private List<TSDRLogRecord> logRecords = new LinkedList<>();
    private List<TSDRMetricRecord> metricRecords = new LinkedList<>();
    private Executor senderExecutor;
    private long batchTimer = ONEM2M_TSDR_SENDER_BATCH_SEND_TIMER;
    private final static String ONEM2M_TSDR_CATEGORY_NAME = "oneM2M";
    private String tsdrNodeId = "";
    private int tsdrRollingIndex = 0;

    public Onem2mTsdrSender(TsdrCollectorSpiService tsdrService) {
        this.tsdrService = tsdrService;
        senderExecutor = Executors.newFixedThreadPool(1);
        senderExecutor.execute(new Runnable() {
            @Override
            public void run() {
                store();
            }
        });
        LOG.info("Created Onem2mTsdrSender");
    }

    public void batchTimerChanged(long batchTimerSeconds) {
        if (batchTimerSeconds <= 0) {
            this.batchTimer = ONEM2M_TSDR_SENDER_BATCH_SEND_TIMER;
        } else {
            this.batchTimer = batchTimerSeconds;
        }
        LOG.info("batchTimerChanged: {}", this.batchTimer);
    }

    public void tsdrNodeIdChanged(String tsdrNodeId) {
        this.tsdrNodeId = tsdrNodeId;
        LOG.info("tsdrNodeIdChanged: {}", this.tsdrNodeId);
    }

    /**
     * Used by the onem2m-tsdr-poller and onem2m-tsdr-async to send data.  When the poller expires a timer, then
     * all resources under the target URI that satisfy the filter criteria are sent to this routine.  When an
     * asynchronous update is triggered as a result of a db update, the onem2m-listener used by the onem2m-tdsr-async
     * module finds its descriptor, then calls this routine.
     *
     * Based on some code by sharon@tsdr-project provided, it might be nice to batch many updates then send to interactions.
     *
     * @param t descriptor
     * @param h hierarchical resource string
     * @param onem2mResource resource
     */
    public void sendDataToTsdr(Onem2mTargetDesc t, String h, Onem2mResource onem2mResource) {

        switch (t.getContentType()) {
            case LATESTCICONTSDRMETRIC:
                if (Onem2mDb.getInstance().isLatestCI(onem2mResource)) {
                    sendCIConUsingTsdrMetric(t, h, onem2mResource);
                }
                break;
            case LATESTCICONTSDRLOG:
                if (Onem2mDb.getInstance().isLatestCI(onem2mResource)) {
                    sendCIConUsingTsdrLog(t, h, onem2mResource);
                }
                break;
            case LATESTCI:
                if (Onem2mDb.getInstance().isLatestCI(onem2mResource)) {
                    sendOnem2mResourceUsingTsdrLog(t, h, onem2mResource);
                }
                break;
            case ANYRESOURCE:
                sendOnem2mResourceUsingTsdrLog(t, h, onem2mResource);
                break;
        }

    }

    private void sendCIConUsingTsdrMetric(Onem2mTargetDesc onem2mTargetDesc, String h,
                                          Onem2mResource onem2mResource) {

        // add keys to the tsdr key structures
        List<RecordKeys> keysList = new ArrayList<RecordKeys>();
        RecordKeysBuilder oneM2MURIKey = new RecordKeysBuilder()
                .setKeyName("oneM2MURI")
                .setKeyValue(h);
        keysList.add(oneM2MURIKey.build());
        RecordKeysBuilder oneM2MResourceTypeKey = new RecordKeysBuilder()
                .setKeyName("oneM2MResourceType")
                .setKeyValue(Onem2m.ResourceTypeString.CONTENT_INSTANCE + "-" + ResourceContentInstance.CONTENT);
        keysList.add(oneM2MResourceTypeKey.build());

        String jsonContentString = onem2mResource.getResourceContentJsonString();
        try {
            JSONObject j = new JSONObject(jsonContentString);
            String content = j.optString(ResourceContentInstance.CONTENT, null);
            if (content != null) {
                LOG.info("sendCIConUsingTsdrMetric: topic:{}, content:{}", h, content);
                sendTsdrMetric(keysList, h, content);
            }
        } catch (JSONException e) {
            LOG.error("sendCIConUsingTsdrMetric: {}", e.toString());
        }
    }

    private List<RecordAttributes>
    parseJsonContentIntoTsdrAttributes(Onem2mTargetDesc onem2mTargetDesc, String jsonContentString) {

        List<RecordAttributes> recordAttrsList = new ArrayList<RecordAttributes>();

        if (onem2mTargetDesc.isParseJsonContentIntoTsdrAttrsEnabled()) {
            JSONObject jContent = null;
            try {
                jContent = new JSONObject(jsonContentString);
            } catch (JSONException e) {
                LOG.error("sendCIConUsingTsdrLog: {}", e.toString());
                jContent = null;
            }
            // take the key/value from the json object and add it to the tsdr attributes
            if (jContent != null) {
                Iterator<?> keys = jContent.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    Object o = jContent.opt(key);
                    RecordAttributesBuilder attr = new RecordAttributesBuilder()
                            .setName(key)
                            .setValue(o.toString());
                    recordAttrsList.add(attr.build());
                }
            }
        }
        return recordAttrsList;
    }

    private void sendCIConUsingTsdrLog(Onem2mTargetDesc onem2mTargetDesc, String h,
                                       Onem2mResource onem2mResource) {

        // add keys to the tsdr key structures
        List<RecordKeys> keysList = new ArrayList<RecordKeys>();
        RecordKeysBuilder oneM2MURIKey = new RecordKeysBuilder()
                .setKeyName("oneM2MURI")
                .setKeyValue(h);
        keysList.add(oneM2MURIKey.build());
        RecordKeysBuilder oneM2MResourceTypeKey = new RecordKeysBuilder()
                .setKeyName("oneM2MResourceType")
                .setKeyValue(Onem2m.ResourceTypeString.CONTENT_INSTANCE + "-" + ResourceContentInstance.CONTENT);
        keysList.add(oneM2MResourceTypeKey.build());


        // initialize the tsdr attributes if required
        String jsonContentString = onem2mResource.getResourceContentJsonString();
        try {
            JSONObject j = new JSONObject(jsonContentString);
            String content = j.optString(ResourceContentInstance.CONTENT, null);
            if (content != null) {
                LOG.info("sendCIConUsingTsdrLog: topic:{}, content:{}", h, content);
                List<RecordAttributes> recordAttrsList = parseJsonContentIntoTsdrAttributes(onem2mTargetDesc, content);
                sendTsdrLog(keysList, recordAttrsList, content);
            }
        } catch (JSONException e) {
            LOG.error("sendCIConUsingTsdrMetric: {}", e.toString());
        }
    }

    private void sendOnem2mResourceUsingTsdrLog(Onem2mTargetDesc onem2mTargetDesc, String h,
                                                Onem2mResource onem2mResource) {
        List<RecordKeys> keysList = new ArrayList<RecordKeys>();
        RecordKeysBuilder oneM2MURIKey = new RecordKeysBuilder()
                .setKeyName("oneM2MURI")
                .setKeyValue(h);
        keysList.add(oneM2MURIKey.build());
        RecordKeysBuilder oneM2MResourceTypeKey = new RecordKeysBuilder()
                .setKeyName("oneM2MResourceType")
                .setKeyValue(Onem2m.resourceTypeToString.get(onem2mResource.getResourceType()));
        keysList.add(oneM2MResourceTypeKey.build());
        String jsonContentString = onem2mResource.getResourceContentJsonString();
        List<RecordAttributes> recordAttrsList = parseJsonContentIntoTsdrAttributes(onem2mTargetDesc, jsonContentString);
        sendTsdrLog(keysList, recordAttrsList, jsonContentString);
    }


    public void sendTsdrMetric(List<RecordKeys> tsdrKeys,
                               String h,
                               String content) {
        LOG.info("tsdrMetric:  keys:{}, content:{}", tsdrKeys, content);
        TSDRMetricRecordBuilder recordBuilder = new TSDRMetricRecordBuilder();
        recordBuilder.setTSDRDataCategory(DataCategory.EXTERNAL); // TODO: TSDR needs new enum
        recordBuilder.setNodeID(tsdrNodeId);
        recordBuilder.setRecordKeys(tsdrKeys);
        recordBuilder.setTimeStamp(System.currentTimeMillis());
        recordBuilder.setMetricName(h);
        try {
            BigDecimal bd = new BigDecimal(content);
            recordBuilder.setMetricValue(bd);
            TSDRMetricRecord tsdrMetricRecord = recordBuilder.build();
            synchronized (metricRecords){
                metricRecords.add(tsdrMetricRecord);
            }
        } catch (NumberFormatException e) {
            LOG.error("sendTsdrMetric: metric is incorrect format: {}", e.toString());
        }
    }

    public void sendTsdrLog(List<RecordKeys> tsdrKeys,
                            List<RecordAttributes> recordAttrsList,
                            String content) {
        LOG.info("tsdrLog:  keys:{}, content:{}", tsdrKeys, content);
        TSDRLogRecordBuilder recordBuilder = new TSDRLogRecordBuilder();
                recordBuilder.setTSDRDataCategory(DataCategory.EXTERNAL); // TODO: TSDR needs new enum
                recordBuilder.setNodeID(tsdrNodeId);
                recordBuilder.setRecordKeys(tsdrKeys);
                recordBuilder.setIndex(tsdrRollingIndex++);
                recordBuilder.setRecordAttributes(recordAttrsList);
                recordBuilder.setTimeStamp(System.currentTimeMillis());
                recordBuilder.setRecordFullText(content);
        TSDRLogRecord tsdrLogRecord = recordBuilder.build();
        synchronized (logRecords){
            logRecords.add(tsdrLogRecord);
        }
    }

    private void storeLogRecords(){

        if (logRecords.size() == 0) return;

        InsertTSDRLogRecordInputBuilder builder = new InsertTSDRLogRecordInputBuilder();
        builder.setCollectorCodeName(ONEM2M_TSDR_CATEGORY_NAME);
        synchronized (logRecords){
            List<TSDRLogRecord> tsdrList = logRecords;
            LOG.info("storeLogRecords: storing records ... reccount={}", tsdrList.size());
            logRecords = new LinkedList<TSDRLogRecord>();
            builder.setTSDRLogRecord(tsdrList);
        }
        tsdrService.insertTSDRLogRecord(builder.build());
    }


    private void storeMetricRecords(){

        if (metricRecords.size() == 0) return;

        InsertTSDRMetricRecordInputBuilder builder = new InsertTSDRMetricRecordInputBuilder();
        builder.setCollectorCodeName(ONEM2M_TSDR_CATEGORY_NAME);
        synchronized (metricRecords){
            List<TSDRMetricRecord> tsdrList = metricRecords;
            LOG.info("storeMetricRecords: storing records ... reccount={}", tsdrList.size());
            metricRecords = new LinkedList<TSDRMetricRecord>();
            builder.setTSDRMetricRecord(tsdrList);
        }
        tsdrService.insertTSDRMetricRecord(builder.build());
    }

    // store the batched/cached records when the timer expires
    private void store() {
        while (true) {
            try {
                sleep(1000 * this.batchTimer);
            } catch (InterruptedException e) {
                LOG.error("Timer was interrrupted {}", e.toString());
            }
            storeLogRecords();
            storeMetricRecords();
        }
    }
}
