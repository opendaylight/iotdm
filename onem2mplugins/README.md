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

## Protocol Handling

The onem2m system handles onem2m-compliant "things" that interact with it via onem2m-formatted messages over the 3
supported wire-protocols (CoAP, MQTT, HTTP).  If your "things" are onem2m compliant then no plugin's are required
as the onem2m system supports these protocols out-of-the-box.  However, what if your "things" do not support onem2m.

The iotdm/onem2m infrastructure provides support to handle data running over HTTP, CoAP, or MQTT.  We have two kinds
of protocol handlers.  A plugin is provided to support simple protocol adatper plugin which does not required any
coding.  The yang model has API's to tell the system how to handle basic messages and how to map them to the onem2m
resource tree.  If you need a more complex model where you need to write code to handle your messages, then some
infrastructure is provided to make it easier to handle your custom protocols.  An example of a more complex plugin is
adapting LWM2M messages to ONEM2M.  There are some standard documented ways to adapt these message types to onem2m but
code is requuired.  A onem2mlwm2m plugin is being developed and will illustrate how to use this infrastucture.

## Plugin Architecture and Structure

In order to support onem2m plugins's, some functions are supplied.  For instance, when oneM2M data is added to the oneM2M
datastore, it is possible to inform applications that CRUDs have happened to the datastore.


Figure 1 depicts a sample architecture where messages arrive via message #1 in the figure, move through to the oneM2M
data store, then Sample Plugin#1 receives a message via the oneM2mDatastoreListener via message #2.

![](plugin_arch/Slide1.jpg)

Figure 1 ODL OneM2M Plugin Architecture

### OneM2M Datastore Listener

The following snippet of code can be added to your application's provider class or another of its classes to be informed
when data store changes have occured.  See Onem2mTsdrProvider in the onem2mtsdr plugin for how it is used.  The
Onem2mDatastoreListener is an example of a helper function/class that can be used to aid in writing your plugin.

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
     
# How to Write a onem2m plugin

First of all, plugin's and apps are generated using a maven archetype.  The archetype is used to generate a base
ODL project with all the required directories.

To get the latest iotdm software, 

    cd /to/your/project/directory
    
If you do not already have iotdm:

    git clone https://git.opendaylight.org/gerrit/p/iotdm.git
    cd iotdm
    
If you already have iotdm,

    cd iotdm
    git pull
    
To work on a particular branch, perform one of the following commands ... note the master branch is Boron at the time
of this writing.  There was no plugin infrastructure in the lithium release.

    git checkout master
    git checkout stable/beryllium
    
    
## Running the archetype

Please read this section before running any of the commands as you might have to change the versions based on what
you are doing.

Now cd into the onem2m-plugins folder, this is the root plugin folder and should contain this README.md :-)

    cd onem2mplugins
    
To generate a new onem2m plugin,

For Boron, 

    mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller \
    -DarchetypeArtifactId=opendaylight-startup-archetype \
    -DarchetypeVersion=1.2.0-SNAPSHOT \
    -DarchetypeRepository=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/ \
    -DarchetypeCatalog=http://nexus.opendaylight.org/content/repositories/opendaylight.snapshot/archetype-catalog.xml
    
For Beryllium, 

    mvn archetype:generate -DarchetypeGroupId=org.opendaylight.controller \
    -DarchetypeArtifactId=opendaylight-startup-archetype \
    -DarchetypeVersion=1.1.2-SNAPSHOT \
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

## Adding onem2m capabilities to your plugin

Now, a basic ODL project has been created, some edits to the project are required to ready your project for onem2m.
Ideally, a onem2m mvn archetype could be written that would automatically make all these edits for you, but until
that exists, this manual set of instructions is your only choice.

### Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/src/main/features/features.xml

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

### Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/features/pom.xml

Add this line into the <properties> stanza where the other version statements are.  Note: the version number below
should match the <version> from the iotdm/onem2m/onem2m-features/pom.xml

      <onem2m.version>0.1.2-SNAPSHOT</onem2m.version>
      
Then, add the following lines to the <dependencies> section,

    <dependency>
          <groupId>org.opendaylight.iotdm</groupId>
          <artifactId>onem2m-features</artifactId>
          <classifier>features</classifier>
          <version>${onem2m.version}</version>
          <type>xml</type>
          <scope>runtime</scope>
    </dependency>
    
### Edit file iotdm/onem2m_plugins/onem2m_<plugin-name>/impl/pom.xml

Add the following lines to the <dependencies> section, note the 0.1.1-SNAPSHOT should match the <version>
from the iotdm/onem2m/onem2m-features/pom.xml.

      <dependency>
          <groupId>org.opendaylight.iotdm</groupId>
          <artifactId>onem2m-core</artifactId>
          <version>0.1.2-SNAPSHOT</version>
      </dependency>

At this point, you might want to build your plugin using 'mvn clean install -SkipTests'.  It should be successful,
if not, look at the reasons the build is failing and fix the issues.

Now your are ready to write the rest of your plugin.  Again, looking at other plugin's helps.  Don't forget to
add dependencies to pom.xml files in your impl folder as well as the .../features/pom.xml and features.xml files.

If you want your new plugin to be included in the onem2mall project, then add it to the pom.xml, and features.xml
file for the iotdm/onem2mall project.  You will see examples of teh other plugin's so follow the same steps.






    

    



    
    
    
    
    



    



