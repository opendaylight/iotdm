/*
 * Copyright © 2016 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2moic.impl;


import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

public class Onem2mOicProvider {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mOicProvider.class);
    protected Onem2mService onem2mService;
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;

    private Onem2mOicClient oicClient;
    private Onem2mOicIPE oicIpe;

    private int OIC_MULTICAST_PORT = 5683;
    private String OIC_MULTICAST_ADDRESS = "224.0.1.187";
    private String oicURI = "coap://192.168.238.145:5683/oic/d";


    public Onem2mOicProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = dataBroker;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }

    /*
     * Method called when the blueprint container is created.
     */
    public void init() {
        /* create an instance of Oic Client */
        oicClient = new Onem2mOicClient(Onem2mOicClient.OicClientType.COAP);
        onem2mService = rpcProviderRegistry.getRpcService(Onem2mService.class);
        oicIpe = new Onem2mOicIPE(onem2mService);

        /* For now discover from init, need to move it to session */
        discoverOicDevices();
    }

    public void discoverOicDevices() {
        oicClient.coapClient.setURI(oicURI);

        /* Write the response handler */
        CoapHandler discoverHandler = new CoapHandler() {
            @Override
            public void onLoad(CoapResponse coapResponse) {
                try {
                    Onem2mOicClient.OicDevice oicDevice = oicClient.oicParseDevice(coapResponse.advanced().getPayload());
                    if (!oicIpe.createOicAe(oicDevice)) {
                        LOG.info("Create AE Failed");
                    } else {
                        LOG.info("Create AE Passed");
                    }
                } catch (IOException ioe) {
                    System.out.println(ioe);
                    ioe.printStackTrace();
                }
            }
            @Override
            public void onError() {

                oicClient.oicErrResHandler(Onem2mOicClient.OicMsgType.OIC_DEVICE);
            }
        };

        oicClient.oicDeviceReq(discoverHandler);
        /* try {
            mulitcastReceive();
        } catch (IOException ioe) {
            System.out.println(ioe);
        } */

    }

    public void mulitcastReceive()
            throws IOException
    {
        InetSocketAddress socketAddress =
                new InetSocketAddress(OIC_MULTICAST_ADDRESS, OIC_MULTICAST_PORT);
        MulticastSocket socket = new MulticastSocket(OIC_MULTICAST_PORT);
        Enumeration<NetworkInterface> ifs =
                NetworkInterface.getNetworkInterfaces();

        while (ifs.hasMoreElements()) {
            NetworkInterface xface = ifs.nextElement();
            Enumeration<InetAddress> addrs = xface.getInetAddresses();
            String name = xface.getName();

            while (addrs.hasMoreElements()) {
                InetAddress addr = addrs.nextElement();
                //System.out.println(name + " ... has addr " + addr);
            }

            if("vmnet8".contains(name)) {
            //System.out.println("Adding " + name + " to our interface set");
            socket.joinGroup(socketAddress, xface);
            }
        }

        byte[] buffer = new byte[1500];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        try {
            packet.setData(buffer, 0, buffer.length);


            //if the socket does not receive anything in 1 second,
            //it will timeout and throw a SocketTimeoutException
            //you can catch the exception if you need to log, or you can ignore it
            socket.setSoTimeout(100000);
            socket.receive(packet);
            //System.out.println("Received pkt from " + packet.getAddress() +
            //            " of length " + packet.getLength());
            //System.out.println(packet.getData().toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Onem2mOicProvider Closed");
    }

}