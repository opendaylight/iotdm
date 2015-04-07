/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(PerfHttpClient.class);
    public long createsPerSec, retrievesPerSec, updatesPerSec, deletesPerSec;

    public PerfHttpClient() {
    }
    /**
     * Run a performance test that create 'numResources' and records how long it took, then it will retrieve
     * each of the created resources, then update each of the resources, then finally delete each of the originally
     * created resources.  This test creates resources under a special cseBase that has been specially designed
     * to hold resoources for this performance test.  The other cseBase's in the system are unaffected.
     *
     * I was thinking that when one deploys this feature, they might want to have some notion of how well it will
     * perform in their environment.  Conceivably, an administration/diagnostic function could be implemented that
     * would invoke the rpc with some number of resources, and the operator could know what performance to expect.
     * @param numResources
     * @return
     */
    public boolean runPerfTest(int numResources) {
        LOG.error("runPerfTest: NOT IMPLEMENTED");
        return false;
    }

    /**
     * There must be a better way of discerning how many ops per sec.  The delta is measured in nanoseconds.
     *
     * @param num
     * @param delta
     * @return
     */
    private long nPerSecond(int num, long delta) {
        long n;

        if (delta >= 1000000000) {
            n = num / (delta / 1000000000);
        } else if (delta >= 100000000) {
            n =  (num * 10)/ (delta / 100000000);
        } else if (delta >= 10000000) {
            n =  (num * 100)/ (delta / 10000000);
        } else if (delta >= 1000000) {
            n =  (num * 1000)/ (delta / 1000000);
        } else if (delta >= 100000) {
            n =  (num * 10000)/ (delta / 100000);
        } else if (delta >= 10000) {
            n = (num * 100000)/ (delta / 10000);
        } else if (delta >= 1000) {
            n = (num * 1000000)/ (delta / 1000);
        } else if (delta >= 100) {
            n = (num * 10000000)/ (delta / 100);
        } else if (delta >= 10) {
            n = (num * 100000000)/ (delta / 10);
        } else {
            n = num * 1000000000 / delta;
        }
        return n;
    }
}
