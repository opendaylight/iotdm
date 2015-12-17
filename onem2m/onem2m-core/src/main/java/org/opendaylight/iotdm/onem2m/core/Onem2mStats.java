/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

public class Onem2mStats {

    public static final int HTTP_REQUESTS = 0;
    public static final int HTTP_REQUESTS_OK = 1;
    public static final int HTTP_REQUESTS_ERROR = 2;
    public static final int HTTP_REQUESTS_CREATE = 3;
    public static final int HTTP_REQUESTS_RETRIEVE = 4;
    public static final int HTTP_REQUESTS_UPDATE = 5;
    public static final int HTTP_REQUESTS_DELETE = 6;
    public static final int HTTP_REQUESTS_NOTIFY = 7;

    public static final int COAP_REQUESTS = 8;
    public static final int COAP_REQUESTS_OK = 9;
    public static final int COAP_REQUESTS_ERROR = 10;
    public static final int COAP_REQUESTS_CREATE = 11;
    public static final int COAP_REQUESTS_RETRIEVE = 12;
    public static final int COAP_REQUESTS_UPDATE = 13;
    public static final int COAP_REQUESTS_DELETE = 14;
    public static final int COAP_REQUESTS_NOTIFY = 15;

    public static final int RESOURCE_AE_CREATE = 16;
    public static final int RESOURCE_AE_RETRIEVE = 17;
    public static final int RESOURCE_AE_UPDATE = 18;
    public static final int RESOURCE_AE_DELETE = 19;

    public static final int RESOURCE_CONTAINER_CREATE = 20;
    public static final int RESOURCE_CONTAINER_RETRIEVE = 21;
    public static final int RESOURCE_CONTAINER_UPDATE = 22;
    public static final int RESOURCE_CONTAINER_DELETE = 23;

    public static final int RESOURCE_CONTENT_INSTANCE_CREATE = 24;
    public static final int RESOURCE_CONTENT_INSTANCE_RETRIEVE = 25;
    public static final int RESOURCE_CONTENT_INSTANCE_UPDATE = 26;
    public static final int RESOURCE_CONTENT_INSTANCE_DELETE = 27;

    public static final int RESOURCE_SUBSCRIPTION_CREATE = 28;
    public static final int RESOURCE_SUBSCRIPTION_RETRIEVE = 29;
    public static final int RESOURCE_SUBSCRIPTION_UPDATE = 30;
    public static final int RESOURCE_SUBSCRIPTION_DELETE = 31;

    public static final int RESOURCE_CSE_BASE_CREATE = 32;
    public static final int RESOURCE_CSE_BASE_RETRIEVE = 33;
    public static final int RESOURCE_CSE_BASE_UPDATE = 34;
    public static final int RESOURCE_CSE_BASE_DELETE = 35;

    public static final int MQTT_REQUESTS = 36;
    public static final int MQTT_REQUESTS_OK = 37;
    public static final int MQTT_REQUESTS_ERROR = 38;
    public static final int MQTT_REQUESTS_CREATE = 39;
    public static final int MQTT_REQUESTS_RETRIEVE = 40;
    public static final int MQTT_REQUESTS_UPDATE = 41;
    public static final int MQTT_REQUESTS_DELETE = 42;
    public static final int MQTT_REQUESTS_NOTIFY = 43;

    public static final int RESOURCE_NODE_CREATE = 44;
    public static final int RESOURCE_NODE_RETRIEVE = 45;
    public static final int RESOURCE_NODE_UPDATE = 46;
    public static final int RESOURCE_NODE_DELETE = 47;

    public static final int RESOURCE_GROUP_CREATE = 48;
    public static final int RESOURCE_GROUP_RETRIEVE = 49;
    public static final int RESOURCE_GROUP_UPDATE = 50;
    public static final int RESOURCE_GROUP_DELETE = 51;

    public static final int RESOURCE_ACCESS_CONTROL_POLICY_CREATE = 52;
    public static final int RESOURCE_ACCESS_CONTROL_POLICY_RETRIEVE = 53;
    public static final int RESOURCE_ACCESS_CONTROL_POLICY_UPDATE = 54;
    public static final int RESOURCE_ACCESS_CONTROL_POLICY_DELETE = 55;



    // this should be the latest entry one plus 1
    private static final int MAX_STATS = 56;

    private static Onem2mStats s;

    private Integer[] statsArray;
    private HashMap<String,Integer> endpointMap;

    public static Onem2mStats getInstance() {
        if (s == null) {
            s = new Onem2mStats();
        }
        return s;
    }
    private Onem2mStats() {
        statsArray = new Integer[MAX_STATS];
        for (int i = 0; i < MAX_STATS; i++) {
            statsArray[i] = new Integer(0);
        }
        endpointMap = new HashMap<String,Integer>(1000);
    }

    public static final int ONEM2M_STATS_HTTP_OPS = 0;

    public synchronized void endpointInc(String ep) {
            Integer v = endpointMap.get(ep);
            if (v == null) {
                endpointMap.put(ep, 1);
            } else {
                endpointMap.put(ep, v + 1);
            }
    }
    public synchronized void inc(int statType) {

        statsArray[statType]++;
    }

    public JSONObject getStats() {
        JSONObject js = new JSONObject();

        js.put("http_requests", statsArray[HTTP_REQUESTS]);
        js.put("http_requests_ok", statsArray[HTTP_REQUESTS_OK]);
        js.put("http_requests_error", statsArray[HTTP_REQUESTS_ERROR]);
        js.put("http_requests_create", statsArray[HTTP_REQUESTS_CREATE]);
        js.put("http_requests_retrieve", statsArray[HTTP_REQUESTS_RETRIEVE]);
        js.put("http_requests_update", statsArray[HTTP_REQUESTS_UPDATE]);
        js.put("http_requests_delete", statsArray[HTTP_REQUESTS_DELETE]);
        js.put("http_requests_notify", statsArray[HTTP_REQUESTS_NOTIFY]);

        js.put("coap_requests", statsArray[COAP_REQUESTS]);
        js.put("coap_requests_ok", statsArray[COAP_REQUESTS_OK]);
        js.put("coap_requests_error", statsArray[COAP_REQUESTS_ERROR]);
        js.put("coap_requests_create", statsArray[COAP_REQUESTS_CREATE]);
        js.put("coap_requests_retrieve", statsArray[COAP_REQUESTS_RETRIEVE]);
        js.put("coap_requests_update", statsArray[COAP_REQUESTS_UPDATE]);
        js.put("coap_requests_delete", statsArray[COAP_REQUESTS_DELETE]);
        js.put("coap_requests_notify", statsArray[COAP_REQUESTS_NOTIFY]);

        js.put("resource_ae_create", statsArray[RESOURCE_AE_CREATE]);
        js.put("resource_ae_retrieve", statsArray[RESOURCE_AE_RETRIEVE]);
        js.put("resource_ae_update", statsArray[RESOURCE_AE_UPDATE]);
        js.put("resource_ae_delete", statsArray[RESOURCE_AE_DELETE]);

        js.put("resource_container_create", statsArray[RESOURCE_CONTAINER_CREATE]);
        js.put("resource_container_retrieve", statsArray[RESOURCE_CONTAINER_RETRIEVE]);
        js.put("resource_container_update", statsArray[RESOURCE_CONTAINER_UPDATE]);
        js.put("resource_container_delete", statsArray[RESOURCE_CONTAINER_DELETE]);

        js.put("resource_content_instance_create", statsArray[RESOURCE_CONTENT_INSTANCE_CREATE]);
        js.put("resource_content_instance_retrieve", statsArray[RESOURCE_CONTENT_INSTANCE_RETRIEVE]);
        js.put("resource_content_instance_update", statsArray[RESOURCE_CONTENT_INSTANCE_UPDATE]);
        js.put("resource_content_instance_delete", statsArray[RESOURCE_CONTENT_INSTANCE_DELETE]);

        js.put("resource_subscription_create", statsArray[RESOURCE_SUBSCRIPTION_CREATE]);
        js.put("resource_subscription_retrieve", statsArray[RESOURCE_SUBSCRIPTION_RETRIEVE]);
        js.put("resource_subscription_update", statsArray[RESOURCE_SUBSCRIPTION_UPDATE]);
        js.put("resource_subscription_delete", statsArray[RESOURCE_SUBSCRIPTION_DELETE]);

        js.put("resource_cse_base_create", statsArray[RESOURCE_CSE_BASE_CREATE]);
        js.put("resource_cse_base_retrieve", statsArray[RESOURCE_CSE_BASE_RETRIEVE]);
        js.put("resource_cse_base_update", statsArray[RESOURCE_CSE_BASE_UPDATE]);
        js.put("resource_cse_base_delete", statsArray[RESOURCE_CSE_BASE_DELETE]);


        js.put("unique_endpoints", endpointMap.size());
        Histogram h = new Histogram();
        int max = 0;
        String maxEp = "";
        for (Map.Entry<String,Integer> entry : endpointMap.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            if (value > max) {
                maxEp = key;
                max = value;
            }
            h.add(value);
        }
        js.put("top_talker", maxEp + ":" + max);
        for (int i = 0; i < Histogram.NUM_BUCKETS; i++) {
            js.put("talkers_10_exp_" + i, h.get(i));
        }
        js.put("talkers_avg", h.avg());
        return js;
    }

    private class Histogram {
        private static final int NUM_BUCKETS = 7;
        public Integer[] bucketArray;
        int n;
        int sum;
        private Histogram () {
            bucketArray = new Integer[NUM_BUCKETS];
            for (int i = 0; i < NUM_BUCKETS; i++) {
                bucketArray[i] = new Integer(0);
            }
        }
        public void add(int value) {
            sum += value;
            n++;
            double d = value;
            double dlog = Math.log10(d);
            int bucket = (int)dlog;
            if (bucket >= NUM_BUCKETS) bucket = NUM_BUCKETS-1;
            bucketArray[bucket]++;
        }
        public Integer get(int i) {
            return bucketArray[i];
        }
        public Integer avg() {
            if (n == 0) return 0;
            return sum/n;
        }
    }
}

