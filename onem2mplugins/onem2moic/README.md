# IoTDM: OneM2M OIC Interop

# Overview

The Onem2m OIC Interop plugin was created to provide a way to transparently encode OIC devices and
resources in Onem2m resource tree, which enables interworking of OneM2M applications and OIC devices.
The OIC Interworking reference model is based on the Interworking Proxy application Entity (IPE) 
model architecture as specified in oneM2M TS-0001: "oneM2M; Functional Architecture".

The OIC IPE participation in the OIC Protocol is in the role of an OIC Client to which OIC Server
(OIC Applications/Endpoints) interact. OIC IPE discovers all the OIC Endpoints by periodically 
polling for /oic/res to "All CoAP Node" multicast address (device discovery mechanism as specified
in the section 10 of the OIC-Core-Specification-v1.0.0: "OIC Core Specification"). 
For each OIC device/Server discovered by the OIC Client(IPE), it will instantiate and maintain 
an instance of a Resource of type <AE>. For each of the resources OIC Servers, content sharing 
resources container are maintained. The mapping of the device attributes and resource attributes 
are as per the "OneM2M TS-0024-v0.4.1: OIC Interworking".

OIC IPE is currently WIP and we will keep README updated as more funtionality is added.

# Configuring the behaviour for OIC IPE
The onem2moic.yang models the config descriptors for the interaction with OIC IPE plugin.

    container onem2m-oic-config {
        /*
         * There exist three variants of how interworking through an 
         * Inter-working Proxy Application Entity over
         * Mca can be supported:
         * 1. Interworking with full mapping of the semantic of the non-oneM2M data model to Mca
         * 2. Interworking using containers for transparent transport of encoded 
         *      non-oneM2M data and commands via Mca
         * 3. Interworking using a retargeting mechanism
         * Please check TS-0001-V2.9.1 OneM2M Functional Architecture Appendix F.
         * For now, OIC plugin only supports option 2.
         */
        leaf ipe-mode {
            mandatory true;
            type enumeration {
                enum "FULL_MAPPING" {
                    value 1;
                    description "Interworking with full mapping of the semantic of the 
                                                            non-oneM2M data model to Mca";
                }
                enum "TRANSPARENT_TRANSPORT" {
                    value 2;
                    description "Interworking using containers for transparent transport 
                                        of encoded non-oneM2M data and commands via Mca";
                }
                enum "RETARGET" {
                    value 3;
                    description "Interworking using a retargeting mechanism";
                }
            }
            default 2;
            description "What kind of IPE mode";
        }

        /*
         * OIC Plugin (OIC IPE) participates in OIC protocol in the role of OIC client. 
         * OIC IPE discovers the OIC servers (Endpoints/Applications) by registering to 
         * "All Coap Nodes" multicast group and requesting for OIC devices and resources. 
         * The details that the OIC Client gets become stale or invalid once the time to 
         * live expires. OIC client has to periodically poll for keeping the OIC 
         * devices/resources up to date.
         */
        leaf poll-period {
            mandatory true;
            type uint16;
            description "Number of seconds upon which OIC client updates OIC Devices";
        }

        /*
         * CSE name under which OIC devices will be mapped in Onem2m resource tree
         */
        leaf cse-name {
            mandatory true;
            type string;
            default "InCSE1";
            description "CSE name under which OIC devices will be mapped in Onem2m resource tree";
        }

        /*
         * IP address of the local interface which is used for OIC discovery
         */
         leaf local-ip-addr {
            type string;
            default "";
            description "IP address of the local interface using which OIC performs discovery";
         }
    }


