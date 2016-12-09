# Infrastructure for IoTDM plugins

This file describes particular interfaces which defines API for
IoTDM plugins and RPC services provided by this infrastructure.

This documentation uses example plugin implementation in
[Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
There are also provided testing requests as Postman collection for this
example plugin:
    iotdm/onem2mplugins/onem2mexample/Onem2mExample.postman_collection.json
Requests in this collection have set also tests so the whole collection
can be executed by Postman's Runner tool.

NOTE: If you are implementing new plugin then don't forget to edit your
impl/pom.xml file properly in order to be able to use plugins
infrastructure features.
See [Onem2mPlugins README.md](../../../../../../../../../../onem2mplugins/README.md),
section Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/pom.xml


# 1. Plugin Architecture and Structure

In the Figure1 there is class diagram which shows example implementation
of IoTDM plugin using all provided classes and interfaces which
represent plugin infrastructure.

![](plugin_docs/plugin_example.png)
Figure1: Class diagram of example plugin implementation.

Example plugin implementation class Onem2mExampleCustomProtocol
implements interfaces required by Onem2mPluginManager for plugin
registrations. All interfaces extends one common interface called
IotdmPluginCommonInterface which describes methods used by
Onem2mPluginManager for registration maintenance. There is e.g. method
getPluginName() which returns unique IoTDM plugin implementation name.

Search Example-1 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
to see example implementation of IotdmPluginCommonInterface.

Plugins uses also abstract class Onem2mDatastoreListener and shows
example of implementation of custom listener.
Onem2mPluginManager (singleton) instance is used for registration and
un-registrations of the plugin.


## 1.1 IotdmPlugin

IotdmPlugin is generic interface which describes methods needed for
handling of protocol requests. There is only one method which must be
implemented, the handle(request, response) method which receives generic
request and response as arguments.
The choice of specific request and response type depends on the
communication protocol which will be supported (or expected) by plugin,
e.g. HTTP, CoAP, MQTT, etc.

Plugin infrastructure implements specializations of interfaces
IotdmPluginRequest<TOriginalRequest> and IotdmPluginResponse for
requests and responses of particular communication protocol.
It means that IotDM plugin implementing the IotdmPlugin interface should
use them if the plugin will handle requests of specific protocol only.

If the plugin will handle requests of more than one communication
protocol then the plugin should implement handle method using the
IotdmPluginRequest and IotdmPluginResponse interfaces as arguments.

NOTE: MQTT and WS communication protocols uses common request and
response specialization classes IotdmPluginOnem2mBaseRequest and
IotdmPluginOnem2mBaseResponse.

Search Example-2 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
to see example implementation of IotdmPlugin.


## 1.2 IotdmPluginDbClient

This interface is intended for plugins which will need to directly read
and/or write data to data store. It is also best practice to use this
interface when the plugin will need to use DatastoreListener.

IotdmPluginDbClient interface implements two pairs of default methods.
The getter methods should not be overridden since they are just helper
methods for getting ResourceTree Reader and Writer objects.
These methods are suitable for plugins which need to use the
Reader/Writer objects but implementation of the dbClient Start/Stop
methods is not desired.

Second pair of methods are dbClient Start/Stop methods with default
implementations which does nothing and should be overridden by
implementing plugin. The Start method is called when the new current
Reader and Writer objects are available. Stop method is called when the
current Reader/Writer objects are not valid anymore.

Search Example-4 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
to see example implementation of IotdmPluginDbClient.

NOTE: Plugin infrastructure implements also another way how to perform
operations with data in data store. This approach does not require
implementation of the IotdmPluginDbClient interface.
See section 1.3 CRUD operations support.


## 1.3 CRUD operations support

In the package org.opendaylight.iotdm.onem2m.client there is collection
of classes representing resources and related builder classes which
should be used for preparation of resource object. CRUD operations are
then performed by object of class Onem2mRequestPrimitiveClient and
results are retrieved as Onem2mResponsePrimitiveClient object.

It is needed to obtain Onem2mService implementation use by this approach,
see section 1.3.1 where it is described.

Search Example-7 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
Where are implemented some methods using this approach.


### 1.3.1 Add reference to RPC registry

This section is required if the plugin will use CRUD operations with
resources. If the plugin will not do any operations with data store or
will use direct write/read operation using ResourceTree Reader/Writer
objects then this section can be skipped (and it can be still used
later).

There should be XML file with BluePrint configuration in directory of
your plugin module:
    
    impl/src/main/resources/org/opendaylight/blueprint/impl-blueprint.xml

In the XML file you can see that there is created one bean for your
provider class which uses one argument reference to dataBroker object.

    <blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
      xmlns:odl="http://opendaylight.org/xmlns/blueprint/v1.0.0"
      odl:use-default-for-reference-types="true">
    
      <reference id="dataBroker"
        interface="org.opendaylight.controller.md.sal.binding.api.DataBroker"
        odl:type="default" />
    
      <bean id="provider"
        class="org.opendaylight.iotdm.onem2mexample.impl.Onem2mExampleProvider"
        init-method="init" destroy-method="close">
        <argument ref="dataBroker"/>
      </bean>
    </blueprint>

It's needed to add second argument and pass reference to RPC registry: 

    <blueprint xmlns="http://www.osgi.org/xmlns/blueprint/v1.0.0"
      xmlns:odl="http://opendaylight.org/xmlns/blueprint/v1.0.0"
      odl:use-default-for-reference-types="true">
    
      <reference id="dataBroker"
        interface="org.opendaylight.controller.md.sal.binding.api.DataBroker"
        odl:type="default" />
    
      <reference id="rpcProviderRegistry"
                 interface="org.opendaylight.controller.sal.binding.api.RpcProviderRegistry"/>
    
      <bean id="provider"
        class="org.opendaylight.iotdm.onem2mexample.impl.Onem2mExampleProvider"
        init-method="init" destroy-method="close">
        <argument ref="dataBroker"/>
        <argument ref="rpcProviderRegistry"/>
      </bean>
    </blueprint>
    
Now the provider class of the plugin must be modified as well. It must
accept the new argument and it is needed to get also Onem2mService
instance which provides RPCs implementing CRUD operations with
resources.

Here is a code snippet example of Onem2mExampleProvider of example
plugin from IoTDM sources (non-important parts have been removed):

    public class Onem2mExampleProvider implements AutoCloseable {
    
        protected Onem2mService onem2mService;
        private final DataBroker dataBroker;
        private final RpcProviderRegistry rpcProviderRegistry;
    
        public Onem2mExampleProvider(final DataBroker dataBroker, final RpcProviderRegistry rpcProviderRegistry) {
            this.dataBroker = dataBroker;
            this.rpcProviderRegistry = rpcProviderRegistry;
        }
    
        /**
         * Method called when the blueprint container is created.
         */
        public void init() {
            onem2mService = rpcProviderRegistry.getRpcService(Onem2mService.class);    
            LOG.info("Onem2mExampleProvider Session Initiated");
        }
    
        /**
         * Method called when the blueprint container is destroyed.
         */
        public void close() {
            LOG.info("Onem2mExampleProvider Closed");
        }
    }

See OpenDaylight documentation https://wiki.opendaylight.org/view/Using_Blueprint
for more details.


## 1.4 IotdmPluginConfigurable

The only method defined by this interface is getRunningConfig() which
returns current (running) configuration of plugin. The returned current
configuration is modelled in onem2mpluginmanager.yang file. It is in an
output of RPC call onem2m-plugin-manager-plugin-data. The related data
structure is: choice plugin-configuration. It includes simple-config
case by default and it should be augmented in yang models of other
plugins.

See yang model of Onem2m HTTP protocol provider (onem2m-protocol-http.yang)
where is an example of augmentation of the plugin-configuration:

    augment "/onem2m-plugin-manager:onem2m-plugin-manager-plugin-data
             /onem2m-plugin-manager:output
             /onem2m-plugin-manager:onem2m-plugin-manager-plugins-table
             /onem2m-plugin-manager:onem2m-plugin-manager-plugin-instances
             /onem2m-plugin-manager:plugin-configuration" {
        case http-https-config {
            uses http-protocol-provider-config;
        }
    }

This way the plugin-configuration can be dynamically augmented by new
modules loaded to IoTDM and the RPC call onem2m-plugin-manager-plugin-data
can return also configurations of these new plugins which were not
known in time of compilation of IoTDM.

NOTE: Plugins implementing this interface are responsible to
persistently store their configuration in order to make it available
after the restart of IoTDM. Services provided by ODL and BluePrint
can be used, see ODL documentation for more information and checkout
protocol providers implementations as examples.

TODO: Onem2mHttpProvider does not implement the IotdmPluginConfigurable
interface, this is TBD, i.e. the RPC call output does not show current
configuration of the HTTP provider even its yang model augments the
plugin-configuration. TODO NOTE: But we should probably migrate from
config subsystem to BluePrint before.


## 1.5 IotdmPluginSimpleConfigClient

The interface IotdmPluginSimpleConfigClient is extension of the interface
IotdmPluginConfigurable described in section 1.4.

Interface defines configure() method which receives configuration for
plugin stored in instance of class IotdmSimpleConfig. Configuration
itself is described as list of key-value pairs where the unique keys
identifies specific configuration value.

Next defined method is getSimpleConfig() method which returns current
configuration as instance of the IotdmSimpleConfig class. This is
similar to getRunningConfig() method from IotdmPluginConfigurable
interface but getRunningConfig() method returns the PluginConfiguration
object modelled by yang.

Since the IotdmPluginSimpleConfigClient extends IotdmPluginConfigurable
the getRunningConfig() method is available also for
IotdmPluginSimpleConfigClient instances but it is not needed to
implement it explicitly for each IotdmPluginSimpleConfigClient because
there is a default implementation which returns result of
getSimpleConfig() translated into result of getRunningConfig() method.

Plugins implementing this interface are not responsible to
persistently store their configuration because plugin infrastructure
takes care of it.

Search Example-3 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
to see example implementation of IotdmPluginSimpleConfigClient.

IoTDM Plugin infrastructure provides set of RPCs for configuration
of plugins implementing  this interface. See section 2.2.


## 1.6 Onem2mDataStoreListener

Figure 2 depicts a sample architecture where messages arrive via message
number #1 in the figure, move through to the oneM2M data store, then
Sample Plugin#1 receives a message via the oneM2mDatastoreListener
via message number #2.

![](plugin_docs/Slide1.jpg)
Figure2: ODL OneM2M Plugin Architecture

The following snippet of code can be added to your application's
provider class or another of its classes to be informed when data store
changes have occurred. See Onem2mTsdrProvider in the onem2mtsdr plugin
for how it is used. The Onem2mDatastoreListener is an example of a
helper function/class that can be used to aid in writing your plugin.

     private class Onem2mDataStoreChangeHandler extends Onem2mDatastoreListener {
 
         public Onem2mDataStoreChangeHandler(DataBroker dataBroker) {
             super(dataBroker);
         }
 
         @Override
         public void onem2mResourceCreated(String hierarchicalResourceName, Onem2mResource onem2mResource) {
             LOG.info("Onem2mTsdrProvider: onem2mResourceCreated h={}, id:{}, type:{}",
                     hierarchicalResourceName,
                     onem2mResource.getResourceId(),
                     onem2mResource.getResourceType());
             // handle the create ...
         }
 
         @Override
         public void onem2mResourceChanged(String hierarchicalResourceName, Onem2mResource onem2mResource) {
             LOG.info("Onem2mTsdrProvider: onem2mResourceChanged h={}, id:{}, type:{}",
                     hierarchicalResourceName,
                     onem2mResource.getResourceId(),
                     onem2mResource.getResourceType());
             // handle the change
         }
 
         @Override
         public void onem2mResourceDeleted(String hierarchicalResourceName, Onem2mResource onem2mResource) {
             LOG.info("Onem2mTsdrProvider: onem2mResourceDeleted h={}, id:{}, type:{}",
                     hierarchicalResourceName,
                     onem2mResource.getResourceId(),
                     onem2mResource.getResourceType());
             // handle the delete
         }
     }

Search Example-6 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
to see example implementation of Onem2mDatastoreListener.


## 1.7 Registration of plugin instances

Every instance of IotdmPlugin interfaces described above must register
to Onem2mPluginManager in order to use desired features.
Onem2mPluginManager provides registration methods for each interface
described above which are documented in the Onem2mPluginManager.

Onem2mPluginManager implements also method for un-registration of
instance of the specific interface.

Search Example-5 in [Onem2mExampleCustomProtocol.java](../../../../../../../../../../onem2mplugins/onem2mexample/impl/src/main/java/org/opendaylight/iotdm/onem2mexample/impl/Onem2mExampleCustomProtocol.java)
to see example calls of registration methods. There are also called
un-registration methods in case of registration failure or on the
close() method.

### 1.7.1 Custom configuration of CommunicationChannels

Some of the registration methods for instances of IotdmPlugin interface
are overloaded in order to allow passing custom configuration to used
CommunicationChannel. Registration methods for HTTPS and CoAPS are
examples of such methods.
Such communication channels implements generic interface
IotdmPluginConfigurationBuilder for their specific configuration.
In case of HTTPS we have IotdmHttpsConfigBuilder class nad
IotdmCoapsConfigBuilder for CoAPS.

Checkout implementations of HTTP(S) and CoAP(S) protocol providers as
examples of usage of the mentioned registration methods and builders.


# 2. RPC services provided by IoTDM plugins infrastructure

This chapter describes two main groups of RPCs calls implemented in
plugins infrastructure.

Following descriptions of RPC calls shows target HTTP endpoint which
must be set in target URL of POST request, e.g.:

    http://localhost:8181/restconf/operations/onem2m-plugin-manager:onem2m-plugin-manager-plugin-data

NOTE: All requests must have set also content type (JSON or XML) and
authorization header. There is used JSON content type in following
examples but it is also possible to use XML.

Examples of all described RPC calls are stored in the collection
exported from Postman:
    plugin_docs/IotDM_Plugins_Infrastructure.postman_collection.json
This file can be simply imported to Postman.

## 2.1 Onem2mPluginManager registrations

PluginManager provides data about registered plugins by RPC calls for
specific IotdmPlugin* interface. There is also implemented RPC call
which provides collection of all data known about specific plugin
together and RPC call which provides data about running
CommunicationChannels.

Inputs of these RPC calls are used for filtering according to some
parameters which are described for specific RPC call below.

### 2.1.1 Registrations of IotdmPlugin instances

    restconf/operations/onem2m-plugin-manager:onem2m-plugin-manager-iotdm-plugin-registrations

Optional input can be used to filter plugins loaded by specified
BundleLoader:

    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault"
        }
    }

### 2.1.2 Registrations of IotdmPluginDbClient instances

    restconf/operations/onem2m-plugin-manager:onem2m-plugin-manager-db-api-client-registrations

This RPC call has not implemented any input parameters yet.

### 2.1.3 Registrations of IotdmPluginSimpleConfigClient instances

    restconf/operations/onem2m-plugin-manager:onem2m-plugin-manager-simple-config-client-registrations

This RPC call has not implemented any input parameters yet.

### 2.1.4 Complete set of plugin data

    restconf/operations/onem2m-plugin-manager:onem2m-plugin-manager-plugin-data

Optional input is used to filter according to plugin name and/or
plugin instance name:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "plugin-instance-name" : "default"
        }
    }

### 2.1.5 Running CommunicationChannels data

    restconf/operations/onem2m-plugin-manager:onem2m-plugin-manager-communication-channels

Optional input is used to filter according to protocol name:

    {
        "input": {
            "protocol-name": "http"
        }
    }


## 2.2 Configuration of IotdmPluginSimpleConfigClient instances

Next sections describes and provides examples of RPC calls implemented
for configuration of registered IotdmPluginSimpleConfig instances.


### 2.2.1 Operations with whole plugin config

These RPC calls have mandatory inputs with mandatory attributes
identifying specific plugin instance by plugin-name and instance-name.


#### iplugin-cfg-put

This RPC call replaces configuration of the plugin instance with
the new configuration passed in input.

    restconf/operations/onem2m-simple-config:iplugin-cfg-put

Input example configuring two values:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default",
            "plugin-simple-config" : {
                "key-val-list" : [
                    {
                        "cfg-key": "testKey",
                        "cfg-val": "testVal"
                    },
                    {
                        "cfg-key": "testKey2",
                        "cfg-val": "testVal2"
                    }
                ]
            }
        }
    }

Output includes current running configuration of configured plugin.


#### iplugin-cfg-get

This RPC call returns running configuration of the plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-get

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default"
        }
    }
    
Output includes current running configuration of the plugin instance.


#### iplugin-cfg-del

This RPC call deletes whole configuration of the plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-del

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default"
        }
    }

Output of this RPC call is empty.


#### iplugin-cfg-get-startup

This RPC call returns startup configuration of the plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-get-startup

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default"
        }
    }
    
Output includes current startup configuration of the plugin instance.


### 2.2.2 Operations with single value of plugin config

These RPC calls have mandatory inputs with mandatory attributes
identifying specific plugin instance by plugin-name and
instance-name and specific configuration key.


#### iplugin-cfg-key-put

Configuration of specific value of specific plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-key-put

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default",
            "cfg-key": "testKey2",
            "cfg-val": "testVal2"
        }
    }

Output includes current running configuration of the plugin instance.


#### iplugin-cfg-key-get

This RPC call returns running configuration of specific value of the
specific plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-key-get

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default",
            "cfg-key": "testKey2",
        }
    }

Output includes current value related to the cfg-key from
running configuration of the plugin instance.


#### iplugin-cfg-key-del

Deletes specific value of specific plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-key-del

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default",
            "cfg-key": "testKey2",
        }
    }

Output of this RPC call is empty.


#### iplugin-cfg-key-get-startup

This RPC call returns startup configuration of specific value of the
specific plugin instance.

    restconf/operations/onem2m-simple-config:iplugin-cfg-key-get-startup

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample",
            "instance-name": "default",
            "cfg-key": "testKey2",
        }
    }

Output includes current value related to the cfg-key from
startup configuration of the plugin instance.


### 2.2.3 GET configuration of all plugin instances

These RPC calls have optional input used to filter only configurations
of specific plugin implementation.


#### iplugin-cfg-get-running-config

This RPC call collects all running configurations of all instances.

    restconf/operations/onem2m-simple-config:iplugin-cfg-get-running-config

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample"
        }
    }

Output includes list of running configurations of all instances.


#### iplugin-cfg-get-startup-config

This RPC call collects all startup configurations of all instances.

    restconf/operations/onem2m-simple-config:iplugin-cfg-get-startup-config

Input example:

    {
        "input": {
            "plugin-name": "Onem2mExample"
        }
    }

Output includes list of startup configurations of all instances.
