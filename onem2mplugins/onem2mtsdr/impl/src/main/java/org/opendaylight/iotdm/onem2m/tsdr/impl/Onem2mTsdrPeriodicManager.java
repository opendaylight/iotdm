/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.tsdr.impl;

import static java.lang.Thread.sleep;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mtsdr.rev160210.onem2m.tsdr.config.Onem2mTargetDesc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mTsdrPeriodicManager {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mTsdrPeriodicManager.class);
    private ExecutorService pollerExecutor;
    private Onem2mTsdrSender onem2mTsdrSender;
    private final int ONEM2M_TSDR_POLLER_RESOURCE_LIMIT = 1000;

    private HashMap<String,TsdrPollingDesc> tsdrMap;

    public Onem2mTsdrPeriodicManager(Onem2mTsdrSender onem2mTsdrSender) {
        this.onem2mTsdrSender = onem2mTsdrSender;
        tsdrMap = new HashMap<String,TsdrPollingDesc>();
        LOG.info("Created Onem2mTsdrPeriodicManager");
        pollerExecutor = Executors.newFixedThreadPool(1);
        pollerExecutor.execute(new Runnable() {
            @Override
            public void run() {
                poller();
            }
        });
    }

    public void close() {
        tsdrMap.clear();
        pollerExecutor.shutdown();
    }

    public void onem2mTargetDescCreated(Onem2mTargetDesc onem2mTargetDesc) {
        TsdrPollingDesc t = new TsdrPollingDesc(onem2mTargetDesc);
        tsdrMap.put(onem2mTargetDesc.getOnem2mTargetUri(), t);
    }

    public void onem2mTargetDescChanged(Onem2mTargetDesc onem2mTargetDesc) {
        TsdrPollingDesc t = tsdrMap.get(onem2mTargetDesc.getOnem2mTargetUri());
        if (t == null) {
            onem2mTargetDescCreated(onem2mTargetDesc);
        } else {
            t.adjustTimer(onem2mTargetDesc.getPollPeriod().longValue());
            t.onem2mTargetDesc = onem2mTargetDesc;
        }
    }

    public void onem2mTargetDescDeleted(Onem2mTargetDesc onem2mTargetDesc) {
        tsdrMap.remove(onem2mTargetDesc.getOnem2mTargetUri());
    }

    // for now one poller, one thread ... let's see how it scales
    // future: timerWheel, many threads to poll and send???
    private void poller() {
        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                LOG.error("Timer was interrrupted {}", e.toString());
            }
            /*
            ** See if this descriptor has expired, if so, process the descriptor
             */
            for (Map.Entry<String, TsdrPollingDesc> entry : tsdrMap.entrySet()) {
                TsdrPollingDesc t = entry.getValue();
                if (t.decrementTimerAndIsExpired()) {
                    sendPollingDataToTsdr(t.onem2mTargetDesc);
                }
            }
        }
    }

    private void sendPollingDataToTsdr(Onem2mTargetDesc t) {
        Onem2mResource onem2mResource;
        Onem2mResource onem2mTargetResource = Onem2mDb.getInstance().getResourceUsingURI(t.getOnem2mTargetUri());
        if (onem2mTargetResource != null) {
            List<String> onem2mResourceIdList = Onem2mDb.getInstance().getHierarchicalResourceList(
                    onem2mTargetResource.getResourceId(), ONEM2M_TSDR_POLLER_RESOURCE_LIMIT);
            for (String onem2mResourceId : onem2mResourceIdList) {
                onem2mResource = Onem2mDb.getInstance().getResource(onem2mResourceId);
                if (onem2mResource == null) {
                    LOG.error("sendDataToTsdr: unexpected null resource for id: {}", onem2mResourceId);
                    continue;
                }
                String h = Onem2mDb.getInstance().getHierarchicalNameForResource(onem2mResource);
                onem2mTsdrSender.sendDataToTsdr(t, h, onem2mResource);
            }
        }
    }

    private class TsdrPollingDesc {

        private final long ONEM2M_TSDR_POLLER_INTERVAL_SECONDS_DEFAULT = 15;
        private Onem2mTargetDesc onem2mTargetDesc;
        private long secondsCountDown;

        private TsdrPollingDesc(Onem2mTargetDesc t) {
            onem2mTargetDesc = t;
            resetTimer();
        }
        void resetTimer() {
            secondsCountDown = onem2mTargetDesc.getPollPeriod().longValue();
            if (secondsCountDown <= 0) {
                secondsCountDown = ONEM2M_TSDR_POLLER_INTERVAL_SECONDS_DEFAULT;
            }
        }
        boolean decrementTimerAndIsExpired() {
            if (--secondsCountDown == 0) {
                resetTimer();
                return true;
            }
            return false;
        }
        void adjustTimer(long newSeconds) {
            if (newSeconds <= 0) {
                newSeconds = ONEM2M_TSDR_POLLER_INTERVAL_SECONDS_DEFAULT;
            }
            long oldSeconds = onem2mTargetDesc.getPollPeriod();
            if (oldSeconds <= 0) {
                oldSeconds = ONEM2M_TSDR_POLLER_INTERVAL_SECONDS_DEFAULT;
            }
            if (newSeconds >= oldSeconds) {
                secondsCountDown += (newSeconds - oldSeconds);
            } else {
                secondsCountDown = (oldSeconds - newSeconds);
                if (secondsCountDown <= 0) {
                    secondsCountDown = 1; // next poll, expire, and send data
                }
            }
        }
    }
}
