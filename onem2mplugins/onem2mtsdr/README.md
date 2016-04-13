# IoTDM: OneM2M TSDR

# Overview

The onem2m TSDR plugin was created to provide a way to send data from the oneM2M resource tree to the ODL Time Series
Data Repository (TSDR).  The oneM2M resource tree can contain many oneM2M resource types.  Typically, these resources
exist to provide a tree structure and the leafs of the tree are called contentInstances which are created under oneM2M
containers.  These contentInstances are JSON objects where one of the fields hold the actual content.

# Configuring the behaviour for how onem2m interacts with the TSDR

The onem2mtsdr.yang models the descriptors for the interaction with TSDR.  The model accepts a list of
TSDR descriptors that looks something like:

## Sample yang model

    grouping tsdr-parms-desc {

        leaf poll-period {
            mandatory true;
            type uint32;
            description "Number of seconds between sending data to TSDR in POLL mode, N/A for ASYNC";
        }

        /*
        ** The content-type is used to filter the type of data that gets sent to the TSDR.  The oneM2M resource tree
        ** models many different types of resources and it is likely that only "real" IOT data is sent to the tsdr.
        ** Example: an IOT sensor puts a latest value for a temperature reading in to the content instance resource
        ** of a container.  The url might be something like /cseName/tempSensorDeviceApp/thermometer10/latest/con/98.6
        ** The TSDR would like a time series of temperature readings.  The type would be LATEST-CI-CON-METRIC.  However,
        ** if the con field contains a JSON rep of the reading like {"temp":98.6}, then the type might be
        ** LATEST-CI-CON-LOG as the whole JSON rep is logged to tsdr.  The TSDR system can handle LOG and METRIC where
        ** log is basically a blob of data, and METRIC is a (decimal) value.  The METRIC is typically used to handle
        ** pkt counters, byte counters, like for networking devices.  It can also be used for IOT sensor readings that
        ** are interpreted as numbers.
        **
        ** My hope is that these enums are enough to handle ALL cases ... if not, maybe we add some more, or you
        ** need to write some custom handlers for your data before you send it to TSDR.
        */
        leaf content-type {
            mandatory true;
            type enumeration {
                enum "LATEST-CI" {
                    value 1;
                    description "Send the content instance resource json record using tsdr log api";
                }
                enum "LATEST-CI-CON-TSDR-LOG" {
                    value 2;
                    description "Send only the content instance con field using tsdr log api";
                }
                enum "LATEST-CI-CON-TSDR-METRIC" {
                    value 3;
                    description "Send only the content instance con field using tsdr metric api";
                }
                enum "ANY-RESOURCE" {
                    value 4;
                    description "Send any onem2m resource json record using tsdr log api";
                }
            }
            description "What kind of onem2m content should be sent";
        }

        leaf parse-json-content-into-tsdr-attrs-enabled {
            mandatory true;
            type boolean;
            description "If TRUE, parse json content and put key/value into tsdr attributes";
        }

        /*
        ** The mode dictates how data makes its way into the tsdr database.
        ** 1) POLL: the plugin will POLL the onem2m tree based on the setting of contentType based on the poll period.
        **    When the timer goes off, all content under the onem2m-target will be polled and the data is
        **    sent to the tsdr.  The content is subject to the contentType described above.
        ** 2) ASYNC: asynchronous updates occur to the onem2m datastore.  As these updates occur, these updates are
        **    sent to the tsdr.
        */
        leaf tsdr-mode {
            mandatory true;
            type enumeration {
                enum "POLL" {
                    value 1;
                    description "Find all appropriate data every polling period and send it to tsdr";
                }
                enum "ASYNC" {
                    value 2;
                    description "As data arrives in the data store, buffer it up, and send it every n batch seconds";
                }
            }
            description "What kind of onem2m content should be sent";
        }
    }

    container onem2m-tsdr-config {

        leaf tsdr-node-id {
            type string;
            description "The ID of the network element";
        }

        /*
        ** To be nice to the TSDR system, data is batched and sent periodically.  As ASYNC data is processed, it it
        ** queued/batched by the onem2mtsdr module until the time expires.  For POLL data, it is possible that the poll
        ** cycle collects alot of data, it too is queued/batched and sent when the batch timer expires. Note that all
        ** data to be collected is timestamped when it is POLL-ed or when AYNC changes happen, NOT when it is sent as a
        ** batch to the TSDR module.
        */
        leaf batch-period-seconds {
            type uint32;
            default 15;
            description "Regardless of tsdr-mode, tsdr data is batched and sent periodically using this timer";
        }

        /*
        ** The oneM2M resource tree can be large, it might be desirebale to be flexible as to which nodes in the tree
        ** will be sent to the TSDR and how.  Some nodes might wish to be sent ASYNC when the tree is updated.  Some
        ** nodes might which to be POLLed periodically, and ONLY the current value of the latest content instance is
        ** sent.  It really depends if the user wishes not to miss any updates.
        */
        list onem2m-target-desc {
            key onem2m-target-uri;
            leaf onem2m-target-uri {
                type string;
            }
            uses tsdr-parms-desc;
        }
    }
    
    
    
## Multiple Descriptors

The oneM2M resource tree can be as small or as large as the IOT solution dictates.  Some nodes
in the tree may require TSDR data collection where as some may not.  Some nodes may benefit from one style of data
collection versus another. For this reason, multiple onem2m-tsdr descriptors are supported.

## Target URI

Each descriptor has as its key, a oneM2M Target URI.  It is presumed that the IOT solution designer has
already created the desired oneM2M resource tree structure.  The target URI is the node in the tree under which
oneM2M resources are candidates to be sent data to the TSDR.  The content-type setting is responsible for filtering
these onem2m resource types.

## content-type LATEST-CI ...

The content-type defines a filter on what type of data
is sent to the TSDR.  As mentioned earlier, contentInstance is the resource type that holds the IOT data.  There are 3
categories for content instance.  All resources under the target URI are searched and ONLY resources in the tree that
are the "latest" content instances will be sent to the TSDR.

    
    LATEST-CI: the latest onem2m content instance json object is sent to TSDR using the log API.
 
    LATEST-CI-CON-TSDR-LOG: the latest onem2m content instance "con" field is extracted from the JSON object.  It is sent
    using the log API.
    
    LATEST-CI-CON-TSDR-METRIC: the latest onem2m content instance "con" field is extracted from the JSON object.  It is sent
    using the metric API. It is assumed that the value in the content ("con") field is a decimal value.

## content-type ANY-RESOURCE

The remaining content-type filter is ANY-RESOURCE.  This means that ANY oneM2M resource type will be sent to the TSDR.
I haven't thought of a use case for this yet but it is provided for completeness. Actually, to be more complete, I might
also provide a yang field for this option which allows a "list" of oneM2M resource types.  As mentioned before, the
actual tree structure remains quite static once the "things" in the network are discovered.  Then, only sensor data is
added to the tree as content instances.

## TSDR log API: json/attribute parsing

The TSDR log API has the capability to provide attribute name/value pairs for the blob of data that is being collected.
In our case, the blob of data is typically a oneM2M JSON representation of a oneM2M resource type, or it is a JSON obj
representing the actual data.  In either case, the configurable setting for parse-json-content-into-tsdr-attrs-enabled
allows the onem2mtsdr plugin to parse the json data and populate the TSDR attribute name/value pairs with the JSON
key/value data.  This can give the TSDR system more capabilities to process the JSON data vs simply receiving a JSON
string.
    
## TSDR MODE

There are two modes: POLL, and ASYNC.

    POLL: based on a configurable poll interval, for each poll cycle, the nodes/resource types in the oneM2M resource
    tree are found and filtered based on the content-type.  The "current" value of the resource is sent to the TSDR.
    Example: in the LATEST-CI-CON-TSDR-METRIC content-type mode, the latest value of the resource is found and sent
    to the TSDR.  Let's say it is a temperature sensor.  If the value is updated many times during the polling interval,
    only the latest/current value is sent.  If ALL transitions of the temp sensor need to be recorded, then ASYNC mode
    should be used.  The POLL mode gives a regular periodic time series of the latest/current value of a sensor.
    
    ASYNC: the oneM2M resource tree is a dynamic data structure.  The IOT "things" in the network can send data to the
    oneM2M resource tree as often as they desire (based on device configuration).  It might be desirable to record all
    changes to the tree as the data is updated.  The ASYNC mode allows for this.  As data is updated in the tree, the
    data is sent to the TSDR.
    

## TSDR Batching

In ASYNC mode, updates are sent to the TSDR subsystem as they occur. To be more system resource friendly, updates are
batched and sent to the TSDR in batches based on the configurable setting of the batch-period-seconds field.  Note that
in POLL mode, every polling cycle can also produce MANY values to be sent to the TSDR subsystem.  These values are also
batched and sent as a group to the TSDR.  The timestamps recorded are recorded when the polling interval occurs.  So,
regardless of when the batched set is sent, the timestamps are correct.

# Example Walk-through
    



