# IoTDM: OneM2M Dweet

# Overview

The onem2m Dweet plugin was created to provide a way to send data from the oneM2M resource tree to the Dweet datasource
(dweet.io). For any updates under the "onem2m target id" node which is configured at http://localhost:8181/index.html#/yangui/index,
this plugin will send a post request to dweet io. For <parentContainer>/<contentInstance> creation, a dweet thing named
"PARENTCONTAINER_NAME+latest" will be created/updated. For example, under iotdm resource tree, we create a <contentInstance>
under CSEBase1/Container1, if this resource is under the configured root, a dweet post will send to dweet.io, the "thing",
which is the key of the dweet resource is named "Container1latest", if that dweet resource is already exists, the new
dweet resource will replace the old one. For any other resourcetype, such as <AE>, <Container>, the dweet resource "thing"
(key of the dweet resource) is the same as onem2m resourceName.


# Configuring the behaviour for how onem2m interacts with the Dweet

The onem2mdweet.yang models the descriptors for the interaction with Dweet.  The model accepts a list of
Dweet descriptors that looks something like:

## Sample yang model

module onem2mdweet {
    yang-version 1;
    namespace "urn:opendaylight:params:xml:ns:yang:onem2mdweet";
    prefix "onem2mdweet";

    revision "2015-01-05" {
        description "Initial revision of onem2mdweet model";
    }


    container onem2m-dweet-config {

        list onem2m-target-desc {
            key onem2m-target-id;
            leaf onem2m-target-id {
                type string;
                description "onem2m hierarchical resource name";
            }
        }
    }
}



## Target ID

Each descriptor has as its key, a oneM2M Target ID.  It is presumed that the IOT solution designer has
already created the desired oneM2M resource tree structure.  The target ID is the node in the tree under which
oneM2M resources are candidates to be sent data to the Dweet.

# Example Walk-through
Will put a youtube Link below.
