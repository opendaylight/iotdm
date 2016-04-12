/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.notifier;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.iotdm.onem2m.core.rest.utils.NotificationPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.ResourceChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SubscriptionDeleted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mNotifierService implements Onem2mListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mNotifierService.class);
    private static Onem2mNotifierService notifierService;
    Map<String, Onem2mNotifierPlugin> onem2mNotifierPluginMap = new HashMap<>();
    private static final String DEFAULT_PLUGIN_NAME = "http";
    private final ExecutorService executor;

    private Onem2mNotifierService() {
        executor = Executors.newFixedThreadPool(32);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mNotifierService Closed");
    }

    public static Onem2mNotifierService getInstance() {
        if (notifierService == null)
            notifierService = new Onem2mNotifierService();
        return notifierService;
    }

    /**
     * Allow plugin modules like CoAP, HTTP, MQTT, and 3rd party karaf apps to register for notifications
     * @param plugin
     */
    public void pluginRegistration(Onem2mNotifierPlugin plugin) {
        onem2mNotifierPluginMap.put(plugin.getNotifierPluginName().toLowerCase(), plugin);
    }

    private void executorOnResourceChanged(ResourceChanged notification) {

        NotificationPrimitive onem2mNotification = new NotificationPrimitive();

        onem2mNotification.setPrimitivesList(notification.getOnem2mPrimitive());

        String payload = onem2mNotification.getPrimitive(NotificationPrimitive.CONTENT);
        LOG.info("ResourceChanged: content: {}", payload);

        List<String> uriList = onem2mNotification.getPrimitiveMany(NotificationPrimitive.URI);
        for (String uri : uriList) {

            LOG.info("ResourceChanged: uri: {}", uri);
            String pluginName = null;
            try {
                URI link = new URI(uri);
                pluginName = link.getScheme() == null ? DEFAULT_PLUGIN_NAME : link.getScheme().toLowerCase();
            } catch (URISyntaxException e) {
                LOG.error("Dropping notification: bad URI: {}", uri);
                return;
            }

            if (onem2mNotifierPluginMap.containsKey(pluginName)) {
                Onem2mNotifierPlugin onem2mNotifierPlugin = onem2mNotifierPluginMap.get(pluginName);
                onem2mNotifierPlugin.sendNotification(uri, payload);
            }
        }
    }

    @Override
    public void onResourceChanged(final ResourceChanged notification) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                executorOnResourceChanged(notification);
            }
        });
    }

    private void executorOnSubscriptionDeleted(SubscriptionDeleted notification) {

        NotificationPrimitive onem2mNotification = new NotificationPrimitive();

        onem2mNotification.setPrimitivesList(notification.getOnem2mPrimitive());

        String payload = onem2mNotification.getPrimitive(NotificationPrimitive.CONTENT);
        LOG.info("ResourceChanged: content: {}", payload);

        List<String> uriList = onem2mNotification.getPrimitiveMany(NotificationPrimitive.URI);
        for (String uri : uriList) {

            LOG.info("ResourceChanged: uri: {}", uri);
            String pluginName = null;
            try {
                URI link = new URI(uri);
                pluginName = link.getScheme() == null ? DEFAULT_PLUGIN_NAME : link.getScheme().toLowerCase();
            } catch (URISyntaxException e) {
                LOG.error("Dropping notification: bad URI: {}", uri);
                return;
            }

            if (onem2mNotifierPluginMap.containsKey(pluginName)) {
                Onem2mNotifierPlugin onem2mNotifierPlugin = onem2mNotifierPluginMap.get(pluginName);
                onem2mNotifierPlugin.sendNotification(uri, payload);
            }
        }
    }

    @Override
    public void onSubscriptionDeleted(final SubscriptionDeleted notification) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                executorOnSubscriptionDeleted(notification);
            }
        });
    }
}