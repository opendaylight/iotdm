/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.tsdr;

import com.google.common.util.concurrent.Futures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.AddTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.AddTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.Onem2mbenchmarkTsdrService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class Onem2mTsdrImpl extends Thread implements Onem2mbenchmarkTsdrService{

    private static final long STORE_INTERVAL = 15000;
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mTsdrImpl.class);
    private final TsdrCollectorSpiService tsdrService;
    private final List<AddTSDRLogRecordInput> logRecords = new LinkedList<>();
    private final List<AddTSDRMetricRecordInput> metricRecords = new LinkedList<>();
    private boolean running = true;

    public Onem2mTsdrImpl(TsdrCollectorSpiService service){
        super("Onem2m TSDR Storing Thread");
        this.tsdrService = service;
        this.start();
    }

    @Override
    public Future<RpcResult<Void>> addTSDRMetricRecord(AddTSDRMetricRecordInput input) {
        synchronized (metricRecords){
            metricRecords.add(input);
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }

    @Override
    public Future<RpcResult<Void>> addTSDRLogRecord(AddTSDRLogRecordInput input) {
        synchronized (logRecords){
            logRecords.add(input);
        }
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());

    }

    private void storeLogRecords(){
        InsertTSDRLogRecordInputBuilder builder = new InsertTSDRLogRecordInputBuilder();
        builder.setTSDRLogRecord(new ArrayList<>(logRecords.size()));
        builder.setCollectorCodeName("IoTDM");
        synchronized (logRecords){
            for(AddTSDRLogRecordInput rec:logRecords){
                TSDRLogRecordBuilder recordBuilder = new TSDRLogRecordBuilder();
                recordBuilder.setTSDRDataCategory(rec.getTSDRDataCategory());
                recordBuilder.setNodeID(rec.getNodeID());
                recordBuilder.setRecordKeys(rec.getRecordKeys());
                recordBuilder.setIndex(rec.getIndex());
                recordBuilder.setRecordAttributes(rec.getRecordAttributes());
                recordBuilder.setTimeStamp(rec.getTimeStamp());
                recordBuilder.setRecordFullText(rec.getRecordFullText());
                builder.getTSDRLogRecord().add(recordBuilder.build());
            }
            logRecords.clear();
        }
        tsdrService.insertTSDRLogRecord(builder.build());
    }

    private void storeMetricRecords(){
        InsertTSDRMetricRecordInputBuilder builder = new InsertTSDRMetricRecordInputBuilder();
        builder.setTSDRMetricRecord(new ArrayList<>(metricRecords.size()));
        builder.setCollectorCodeName("IoTDM");
        synchronized (metricRecords){
            for(AddTSDRMetricRecordInput rec:metricRecords){
                TSDRMetricRecordBuilder recordBuilder = new TSDRMetricRecordBuilder();
                recordBuilder.setTSDRDataCategory(rec.getTSDRDataCategory());
                recordBuilder.setNodeID(rec.getNodeID());
                recordBuilder.setRecordKeys(rec.getRecordKeys());
                recordBuilder.setMetricName(rec.getMetricName());
                recordBuilder.setTimeStamp(rec.getTimeStamp());
                recordBuilder.setMetricValue(rec.getMetricValue());
                builder.getTSDRMetricRecord().add(recordBuilder.build());
            }
            metricRecords.clear();
        }
        tsdrService.insertTSDRMetricRecord(builder.build());
    }

    public void store(){
        storeLogRecords();
        storeMetricRecords();
    }

    public void run(){
        while(running){
            try {
                Thread.sleep(STORE_INTERVAL);
                store();
            } catch (InterruptedException e) {
                LOG.error("failed to sleep",e);
                break;
            }
        }
    }

    public void shutdown(){
        this.running = false;
    }
}
