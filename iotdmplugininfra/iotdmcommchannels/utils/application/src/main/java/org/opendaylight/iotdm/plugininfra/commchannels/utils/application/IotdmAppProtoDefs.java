/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application;

public final class IotdmAppProtoDefs {
    public static final String IPv4AllInterfaces = "0.0.0.0";
    public static final String AllEndpoints = "/";

    public static final String IPVersionIPv4 = "IPv4";
    public static final String IPVersionIPv6 = "IPv6";

    public static final String TransportProtocolUdp = "UDP";
    public static final String TransportProtocolTcp = "TCP";

    public static final int PortInit = 0;

    public static final String RoleClient = "Client";
    public static final String RoleServer = "Server";

    // Registration modes (specific registry classes are implemented for particular mode)
    public enum EndpointRegistryMode {

        /* Registered channel is shared by plugins,
         * request is passed to plugin in case of exact match of target URI
         */
        SharedExactMatch,

        /* Registered channel is shared by plugins,
         * request is passed to plugin which is registered for URI which matches the
         * most sub-paths from the begin of the target URI of the request.
         */
        SharedPrefixMatch,

        /* Registered channel is dedicated for one plugin only.
         * All requests are passed to the plugin regardless to the target URI of the request.
         */
        Exclusive
    }

//    public static final class IntKeys {
//        public static final String IPversion = "IPver";
//        public static final String IPaddress = "IpAddr";
//        public static final String TransportProtocol = "TransProto";
//        public static final String PortNumber = "Port";
//        public static final String ClientServer = "Role";
//    }
//
//    public static final class IntVals {
//        public static final String IPv4 = "IPv4";
//        public static final String IPv6 = "IPv6";
//
//        public static final String IPv4All = "0.0.0.0";
//
//        public static final String UDP = "UDP";
//        public static final String TCP = "TCP";
//
//        public static final String PortInit = "0";
//
//        public static final String CLIENT = "Client";
//        public static final String SERVER = "Server";
//    }
//
//    public static final class ProtoKeys {
//        public static final String ChannelProviderName = "ChProvider";
//        public static final String Schema = "Schema";
//    }
//
//    public static final class ProtoVals {
//        public static final String ENDPOINTALL = "*";
//    }
}
