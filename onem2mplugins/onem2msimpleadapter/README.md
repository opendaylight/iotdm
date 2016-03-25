# IoTDM: OneM2M Simple Adapter

# Overview

The onem2m simple adapter was created to provide a way to accept non-oneM2M compliant protocols from devices that
speak protocols that are HTTP, MQTT, CoAP and have a very straightforward mapping into the onem2m data tree.

When a device wants to send a new "sample" from its sensor, it needs to know the onem2m target, then it needs
to send a JSON string with its "key" in the json object.  The key will be used to create a onem2m container underneath
the target URI, as well as a content instance under the container.  If the container already exists, then the
contentInstance is created only.  The JSON content is put in the "con" field of the content Instance.

The onem2m-simple-adapter.yang models the descriptors for the simple adapter.  The model accepts a list of
simple adataper descriptors that look something like:

    grouping simple-adapter-parms-desc {

        leaf onem2m-target-id {
            type string;
            description "onem2m hierarchical resource name";
        }
        leaf onem2m-container-json-key-name {
            type string;
        }
        leaf onem2m-container-json-string {
            type string;
            description "json rep for the container parms, eg: {m2m:cnt:{mni:1, mbs:32}}";
        }
        leaf onem2m-content-instance-json-string {
            type string;
        }
        leaf wire-protocol {
            mandatory true;
            type enumeration {
                enum "HTTP" {
                    value 1;
                }
                enum "MQTT" {
                    value 2; // not supported yet
                }
                enum "COAP" {
                    value 3; // not supported yet
                }
            }
        }
        leaf http-server-port {
            type uint32;
        }
        leaf secure-http {
            type boolean; // not supported yet
        }
        leaf mqtt-broker {
            type string;
        }
        leaf coap-server-port {
            type uint32;
        }
    }
    
    container onem2m-simple-adapter-config {
    
        list simple-adapter-desc {
    
            key simple-adapter-name;
            leaf simple-adapter-name {
                type string;
            }
            uses simple-adapter-parms-desc;
        }
    }
    
    
Each descriptor has a name, followed by a oneM2M Target URI.  It is presumed that the IOT solution designer has
already created the desired oneM2M resource tree structure.  The target URI is the node in the tree under which
each new container/contentInstance will be created when a HTTP POST operation targeting the URI.  Each adapter
also allows which wire protocol will be used: HTTP, MQTT, CoAP.  For HTTP, the HTTP port must be configred, for MQTT,
the MQT broker URL must be configured, and for CoAP, the CoAP port must ve configured.
 
Because a oneM2M container and content Instance will be created when a HTTP request is POSTed, some default parameters
are allowed to be configured so that 

# Example Walk-through
    




