# IoTDM: OneM2M Plugins

# Overview

The onem2m plugin family comes in many flavors and enables capabilities that are not part of the core set of
onem2m features.  An example of a capability would be to provide the ability to send oneM2M data to the Time Series Data
Repository (TSDR) system of ODL.  The flow of data will determine how your plugin is structured.  For instance,
if data arrives into the system from "Things" communicating with IoTDM via oneM2M CoAP/MQTT/HTTP messages, then
the Create/Retrieve/Update/Delete (CRUD) handling could result in data being written to the data store.  At this point, your
app may be interested in receiving notifications of data store modifications so that it, in turn, can
notify other subsystems.  For instance, it can forward a kafka message, or update an external database, send data
off to TSDR.

The goal of some of the plugin's is to avoid having to write code.  The YANG files for each plugin allow a set of
functionality to drive the plugin to behave in a certain way, to interact with the oneM2M resource tree, etc.  It might
be possible to write plugin's this way.  However, you might have to write a custom plugin for your app.  It is up to 
you if you think the community will find your plugin useful.  If so, add it to the iotdm/onem2mplugins folder
using the instructions: HowToAddANewOneM2MPlugin below.
It is also possible to keep your plugin as standalone project and load the plugin dynamically to running IoTDM
without restart.

Plugin implementations can use also simple configuration feature implemented in core described below.

Sources of plugins can be stored as part of IoTDM sources or can be separated into own project. New plugins can
be loaded dynamically to running IoTDM system (without restart) using provided loader modules (see section: Deploying of Onem2m plugins on IoTDM)
Plugins which are implemented as new module of IoTDM sources can be also default part of the new IoTDM Karaf distribution.


# OneM2M Plugins infrastructure 
Next chapters describes functionality provided by plugin infrastructure to plugins. For more details see
Onem2mExampleCustomProtocol class which provides example implementation of plugin using all provided
features and [Onem2mPluginManager README.md](../onem2m/onem2m-core/src/main/java/org/opendaylight/iotdm/onem2m/plugins/README.md)

## Protocol Handling

The onem2m system handles onem2m-compliant "things" that interact with it via onem2m-formatted messages over the 3
supported wire-protocols (CoAP, MQTT, HTTP).  If your "things" are onem2m compliant then no plugin's are required
as the onem2m system supports these protocols out-of-the-box.  However, what if your "things" do not support onem2m.

The iotdm/onem2m infrastructure provides support to handle data running over HTTP, CoAP, or MQTT.  We have two kinds
of protocol handlers.  A plugin is provided to support simple protocol adapter plugin which does not required any
coding.  The yang model has API's to tell the system how to handle basic messages and how to map them to the onem2m
resource tree.  If you need a more complex model where you need to write code to handle your messages, then some
infrastructure is provided to make it easier to handle your custom protocols.  An example of a more complex plugin is
adapting LWM2M messages to ONEM2M.  There are some standard documented ways to adapt these message types to onem2m but
code is required.  A onem2mlwm2m plugin is being developed and will illustrate how to use this infrastructure.

## Operations with data stored in resource tree

Plugin developers can use two ways of writing and reading data from/to resource tree database of IoTDM.
    1. Performing CRUD operations using provided builder classes and classes implementing code for
    resource tree clients.
    2. Using ResourceTree Reader/Writer objects providing methods for direct Read and Writer operations
    with resource tree.

### CRUD operations using resource builders and client classes

Plugin infrastructure provides builder classes which helps building resources for CRUD operations. 
These operations are passed through standard processing of request primitives and security checks and verifications
of originating entities are performed.

### ResourceTree Reader and Writer objects

Objects of classes ResourceTreeReader and ResourceTreeWriter provides methods which write and read data to and from
resource tree directly (without any security checks and verifications of originator).

## Simple configuration of plugins

Plugin infrastructure implements RPCs which can be used to configure plugins by simple configuration described by
list of key-value pairs of strings.

## OneM2M Datastore Listener

In order to support multiple onem2m plugins's, some functions are supplied.  For instance, when oneM2M data is added to the oneM2M
datastore, it is possible to inform applications that CRUDs have happened to the datastore.
Plugin infrastructure implements abstract DataStore listener for this purpose.


# How to Write a onem2m plugin

Please read this section before running any of the commands as you might have to change the versions based on what
you are doing.

This section describes how to create project for new plugin. Developers can choose whether they will create standalone project
for their plugin or they will add the plugin as new module into IoTDM sources.

Next section (Running the archetype) is common for both 

## Running the archetype

First of all, plugin's and apps are generated using a maven archetype.  The archetype is used to generate a base
ODL project with all the required directories.

To generate a new onem2m plugin,

For Carbon,

    mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller \
    -DarchetypeArtifactId=opendaylight-startup-archetype \
    -DarchetypeVersion=1.3.0-SNAPSHOT \
    -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/ \
    -DarchetypeCatalog=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/archetype-catalog.xml

For Boron, 

    mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller \
    -DarchetypeArtifactId=opendaylight-startup-archetype \
    -DarchetypeVersion=1.2.0-SNAPSHOT \
    -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/ \
    -DarchetypeCatalog=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/archetype-catalog.xml
    
For Beryllium, 

    mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller \
    -DarchetypeArtifactId=opendaylight-startup-archetype \
    -DarchetypeVersion=1.1.4-SNAPSHOT \
    -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/ \
    -DarchetypeCatalog=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/archetype-catalog.xml
    
Note the version numbers in the -DarchetypeVersion ... this is what determines which version of the archetype to
generate for which release.  It it important the latest release snapshot is used so use the following link in your
browser.

    http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/archetype-catalog.xml
    
And look for opendaylight-startup-archetype on the web page to see what the latest Boron or Beryllium release is
available for the archetype.  Example: for Boron, above, 1.2.0 is the version of the archetype.  Look to see if there
is a later release like 1.2.2.  If there is then change the mvn archetype command above to reflect the latest
snapshot.

When you run the mvn archetype, it will prompt for information: respond as follows ... note that it is important
to use no spaces between onem2m and your plugin.  The generated files will appear much better.  If you use
a - ie a dash, you will be in a world of hurt as the archetype misbehaves and you will not be able to build it.  Also, it is
important that you prefix your plugin-name with onem2m_ as it will look properly categorized in YangUI.  After all,
you are writing a plugin that interacts with onem2m so none of this should be a problem.  Also, pay close attention to the
CamelCase ie Onem2mPluginName ...

    Define value for property 'groupId': : org.opendaylight.iotdm
    Define value for property 'artifactId': : onem2m<plugin-name>  example: onem2mtsdr
    Define value for property 'package':  org.opendaylight.iotdm.onem2m<plugin-name> example: org.opendaylight.iotdm.onem2mtsdr
    Define value for property 'classPrefix':  Onem2m<PluginName> example: Onem2mTsdr
    Define value for property 'copyright': : Cisco Systems Inc
    
At this point, a directory structure should have been generated under a new directory called onem2m<plugin-name>,
example: onem2mtsdr.


## Create IoTDM module as standalone project

In this case it is not needed to have cloned IoTDM sources. You just need to go into the directory where you want to create the project
for your plugin.

    cd to/your/project/directory

Now you can use the maven archetype to generate directory structure for your plugin (see section Running the archetype).
For this purpose it is recommended to use maven archetype for ODL Carbon modules:
 
     mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller \
     -DarchetypeArtifactId=opendaylight-startup-archetype \
     -DarchetypeVersion=1.3.0-SNAPSHOT \
     -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/ \
     -DarchetypeCatalog=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/archetype-catalog.xml

If the archetype passed successfully then the created module should be buildable. Here is an example of Maven build command
which skips testing and does not do snapshot updates:

    mvn clean install -DskipTests -nsu

In order to be able to use features provided by plugin infrastructure it is important to edit pom.xml file in the impl directory
as described in section: Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/impl/pom.xml

Next steps together with example of plugin implementation are described in [Onem2mPluginManager README.md](../onem2m/onem2m-core/src/main/java/org/opendaylight/iotdm/onem2m/plugins/README.md)


## Add new module into IoTDM sources

This section describes how to add new module for your plugin into IoTDM sources.

    cd /to/your/project/directory

If you do not already have iotdm, to get the latest iotdm software:

    git clone https://git.opendaylight.org/gerrit/p/iotdm.git
    cd iotdm
    
If you already have iotdm,

    cd iotdm
    git pull
    
To work on a particular branch, perform one of the following commands ... note the master branch is Boron at the time
of this writing.  There was no plugin infrastructure in the lithium release.

    git checkout master
    git checkout stable/beryllium

Cd into the onem2m-plugins folder, this is the root plugin folder and should contain this README.md :-)

    cd onem2mplugins

Now you can use the maven archetype to generate directory structure for your plugin (see section Running the archetype).


### Adding onem2m capabilities to your plugin

Now, a basic ODL project has been created, some edits to the project are required to ready your project for onem2m.
Ideally, a onem2m mvn archetype could be written that would automatically make all these edits for you, but until
that exists, this manual set of instructions is your only choice.


#### Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/src/main/features/features.xml

Add the following line, after the last repository statement.

        <repository>mvn:org.opendaylight.iotdm/onem2m-features/{{VERSION}}/xml/features</repository>
        
Then add the following line,

        <feature version='${onem2m.version}'>odl-onem2m-core</feature>
        
just above this next line,
        
        <bundle>mvn:org.opendaylight.iotdm/onem2m<plugin-name>-impl/{{VERSION}}</bundle>
            
Your file should look something like this:  note this is the sample file for the onem2mtsdr plugin

    <?xml version="1.0" encoding="UTF-8"?>
    <!-- vi: set et smarttab sw=4 tabstop=4: -->
    <!--
    Copyright © 2015 Cisco Systems, Inc and others. All rights reserved.
    
    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License v1.0 which accompanies this distribution,
    and is available at http://www.eclipse.org/legal/epl-v10.html
    -->
    <features name="odl-onem2mtsdr-${project.version}" xmlns="http://karaf.apache.org/xmlns/features/v1.2.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://karaf.apache.org/xmlns/features/v1.2.0 http://karaf.apache.org/xmlns/features/v1.2.0">
      <repository>mvn:org.opendaylight.yangtools/features-yangtools/{{VERSION}}/xml/features</repository>
      <repository>mvn:org.opendaylight.controller/features-mdsal/{{VERSION}}/xml/features</repository>
      <repository>mvn:org.opendaylight.mdsal.model/features-mdsal-model/{{VERSION}}/xml/features</repository>
      <repository>mvn:org.opendaylight.netconf/features-restconf/{{VERSION}}/xml/features</repository>
      <repository>mvn:org.opendaylight.dlux/features-dlux/{{VERSION}}/xml/features</repository>
      <repository>mvn:org.opendaylight.iotdm/onem2m-features/{{VERSION}}/xml/features</repository>
    
      <feature name='odl-onem2mtsdr-api' version='${project.version}' description='OpenDaylight :: onem2mtsdr :: api'>
        <feature version='${mdsal.model.version}'>odl-mdsal-models</feature>
        <bundle>mvn:org.opendaylight.iotdm/onem2mtsdr-api/{{VERSION}}</bundle>
      </feature>
      <feature name='odl-onem2mtsdr' version='${project.version}' description='OpenDaylight :: onem2mtsdr'>
        <feature version='${mdsal.version}'>odl-mdsal-broker</feature>
        <feature version='${project.version}'>odl-onem2mtsdr-api</feature>
        <feature version='${onem2m.version}'>odl-onem2m-core</feature>
        <bundle>mvn:org.opendaylight.iotdm/onem2m_<plugin-name>-impl/{{VERSION}}</bundle>
        <configfile finalname="${configfile.directory}/onem2mtsdr.xml">mvn:org.opendaylight.iotdm/onem2mtsdr-impl/{{VERSION}}/xml/config</configfile>
      </feature>
      <feature name='odl-onem2mtsdr-rest' version='${project.version}' description='OpenDaylight :: onem2mtsdr :: REST'>
        <feature version="${project.version}">odl-onem2mtsdr</feature>
        <feature version="${restconf.version}">odl-restconf</feature>
      </feature>
      <feature name='odl-onem2mtsdr-ui' version='${project.version}' description='OpenDaylight :: onem2mtsdr :: UI'>
        <feature version="${project.version}">odl-onem2mtsdr-rest</feature>
        <feature version="${restconf.version}">odl-mdsal-apidocs</feature>
        <feature version="${mdsal.version}">odl-mdsal-xsql</feature>
        <feature version="${dlux.version}">odl-dlux-yangui</feature>
      </feature>
    
    </features>

Note: in the feature stanza above, the 'odl-onem2m-core' feature was added.  This gives your plugin onem2m-core
capabilities.  The iotdm/onem2m/onem2m-features/src/main/features/features.xml file shows all features available.  You must
decide if your plugin needs more onem2m featrues.  If so, add them to your features file under the onem2m-core
statement.  If this was confusing, take a look at all the other plugin's in the onem2m_plugins directory.  Often
looking at other examples will help further your understanding of how features.xml/pom.xml files work.


#### Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/pom.xml

Add this line into the <properties> stanza where the other version statements are.  Note: the version number below
should match the <version> from the iotdm/onem2m/onem2m-features/pom.xml

      <onem2m.version>0.3.0-SNAPSHOT</onem2m.version>
      
Then, add the following lines to the <dependencies> section,

    <dependency>
          <groupId>org.opendaylight.iotdm</groupId>
          <artifactId>onem2m-features</artifactId>
          <classifier>features</classifier>
          <version>${onem2m.version}</version>
          <type>xml</type>
          <scope>runtime</scope>
    </dependency>


#### Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/impl/pom.xml

Add the following lines to the <dependencies> section, note the 0.3.0-SNAPSHOT should match the <version>
from the iotdm/onem2m/onem2m-features/pom.xml.

      <dependency>
          <groupId>org.opendaylight.iotdm</groupId>
          <artifactId>onem2m-core</artifactId>
          <version>0.3.0-SNAPSHOT</version>
      </dependency>

It is recommended to also change the version of your new plugin to match the version of onem2m.  So, because the
onem2m version is 0.3.0 (for Carbon) and you are creating your new plugin in Carbon.  You should change the project
version.  In each of the pom.xml files for the folders: api, artifacts, cli, features, impl, it, karaf and the
base pom.xml.

The impl-blueprint.xml file in the impl/src/main/resources/org/opendaylight/blueprint folder needs an
rpcProviderRegistry added to it.  See onem2mExample plugin for an example of what is required so that the onem2mService
can be initialized and used.  Note: your plugin's <yourplugin>Provider.java will also need to support
this rpcProviderRegistry so the onem2mService can be initialized.

At this point, you might want to build your plugin using 'mvn clean install -DskipTests'.  It should be successful,
if not, look at the reasons the build is failing and fix the issues.

Now your are ready to write the rest of your plugin.  Again, looking at other plugin's helps.  Don't forget to
add dependencies to pom.xml files in your impl folder as well as the .../features/pom.xml and features.xml files.

If you want your new plugin to be included in the onem2mall project, then add it to the pom.xml, and features.xml
file for the iotdm/onem2mall project.  You will see examples of teh other plugin's so follow the same steps.


# Deploying Onem2m plugins on IoTDM
    
Plugins which are implemented as new module of IoTDM can be part of resulting Karaf IoTDM distribution and will be
loaded during startup of the Karaf.
The same plugins can be loaded dynamically during runtime of another IoTDM Karaf distribution and this mechanism is
also available also for plugins which are developed as standalone projects (not module of IoTDM).
Next chapters describes provided deployment mechanisms for plugins during runtime.

Plugins which are developed as separated module can be moved into IoTDM sources (onem2mplugins directory) latter following
the steps from sections:
    Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/src/main/features/features.xml
    Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/pom.xml
It is also needed to add your moved plugin module into the iotdm/onem2m_plugins/pom.xml.

## Using IoTDM BundleLoader 

IoTDM BundleLoader allows loading of plugins packed as OSGi bundles together with all required dependencies.
See readme file of [IoTDM BundleLoader](iotdmbundleloader/README.md).

## Using IoTDM KarafFeatureLoader

IoTDM KarafFeatureLoader allows installation of Karaf archives and specific
Karaf features included in the archive.
See readme file of [IoTDM KarafFeatureLoader](iotdmkaraffeatureloader/README.md).
