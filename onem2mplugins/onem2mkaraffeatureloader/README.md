# Onem2mKarafFeatureLoader

Onem2mKarafFeatureLoader allows installation of Karaf features consisting
of OSGi bundles from the Apache Karaf archive files. Every archive file
includes repository file (features.xml) which described provided features
and this description is used during the installation of the feature.

Features are installed persistently so they are automatically loaded
after restart of IoTDM.

KarafFeatureLoader performs these steps during the installation of features
of the given archive:

1. Extracts content of the archive into data/kar/<archive_name> directory of
the running distribution.

2. Adds the data/kar/<archive_name>/features.xml file as repository for new
features. It performs the same action as Karaf console command
feature:repo-add <repository url>.

3. Copies the content of directory data/kar/<archive_name>/repository directly
into system directory of the running distribution.

4. Installs given list of features.

These steps should be performed also by Karaf command kar:install <archive url>
but it seems that the command does not work according to documentation and
does not copy files into system folder.


## How to write new plugin

Steps describing creation of the new project are written in the
[README.md for plugins](../README.md), see chapters "Create IoTDM
module as standalone project" and "Add new module into IoTDM sources".


## Karaf Archive file

Using the steps described in the chapters mentioned above will
create project structure which sets up Maven build the way it produces
Karaf archive file including all features and repository file of the plugin
located in the features/target directory. This archive can be installed by the
KarafFeatureLoader.

It is possible to create karaf archives also another way (see documentation of
Apache Karaf) but the KarafFeatureLoader requires this content of Karaf
archives:
 1. features.xml file in the root directory of the archive. The features.xml
 file is the file from features/src/main/features directory of the plugin and
 is used as repository of features.
 
 2. repository directory in the root directory of the archive. This directory
 includes directory structure of all OSGi bundles as parts of the features
 defined in the features.xml file.

All other files out of this structure are ignored.
Here is example of the directory structure of testing archive:

    .
    ├── features.xml
    ├── LICENSE
    ├── maven-metadata-local.xml
    ├── META-INF
    │   ├── MANIFEST.MF
    │   └── maven
    │       └── org.opendaylight.iotdm
    │           └── onem2mtest2bundle-features
    │               ├── pom.properties
    │               └── pom.xml
    └── repository
        └── org
            └── opendaylight
                └── iotdm
                    ├── onem2mtest2bundle-api
                    │   └── 0.1.0-SNAPSHOT
                    │       ├── maven-metadata-local.xml
                    │       └── onem2mtest2bundle-api-0.1.0-SNAPSHOT.jar
                    ├── onem2mtest2bundle-cli
                    │   └── 0.1.0-SNAPSHOT
                    │       ├── maven-metadata-local.xml
                    │       └── onem2mtest2bundle-cli-0.1.0-SNAPSHOT.jar
                    ├── onem2mtest2bundle-features
                    │   └── 0.1.0-SNAPSHOT
                    │       ├── maven-metadata-local.xml
                    │       └── onem2mtest2bundle-features-0.1.0-SNAPSHOT-features.xml
                    └── onem2mtest2bundle-impl
                        └── 0.1.0-SNAPSHOT
                            ├── maven-metadata-local.xml
                            └── onem2mtest2bundle-impl-0.1.0-SNAPSHOT.jar

The file features.xml defines for example feature odl-onem2mtest2bundle:

    <feature name="odl-onem2mtest2bundle" version="0.1.0-SNAPSHOT" description="OpenDaylight :: onem2mtest2bundle">
     <feature version="1.5.0-SNAPSHOT">odl-mdsal-broker</feature>
     <feature version="0.1.0-SNAPSHOT">odl-onem2mtest2bundle-api</feature>
     <feature version="0.3.0-SNAPSHOT">odl-onem2m-core</feature>
     <bundle>mvn:org.opendaylight.iotdm/onem2mtest2bundle-impl/0.1.0-SNAPSHOT</bundle>
    </feature>


## Provided RPC calls

Following descriptions of RPC calls shows target HTTP endpoint which
must be set in target URL of POST request, e.g.:

    http://localhost:8181/restconf/operations/onem2mkaraffeatureloader:archive-install

NOTE: All requests must have set also content type (JSON or XML) and
authorization header. There is used JSON content type in following
examples but it is also possible to use XML.


#### archive-install

This call performs the steps described in the first chapter using the archive
specified by URL and installs all features from the list.

    restconf/operations/onem2mkaraffeatureloader:archive-install

Input example:

    {
        "input": {
            "karaf-feature-loader-name" : "KarafFeatureLoaderDefault",
            "karaf-archive-url" : "file:///ws/plugins/plugin_bundle2/onem2mtest2bundle/features/target/onem2mtest2bundle-features-0.1.0-SNAPSHOT.kar",
            "features-to-install": [
                {
                    "feature-name" : "odl-onem2mtest2bundle"
                }
            ]
        }
    }

Successful result is indicated by result code 200 OK.


#### archive-list

This call provides list of installed archives and features. Supports optional
input arguments which can be used to define specific KarafFeatureLoader
instance name or archive name.

    restconf/operations/onem2mkaraffeatureloader:archive-list

Input example:

    {
        "input": {
    		"karaf-feature-loader-name" : "KarafFeatureLoaderDefault",
    		"karaf-archive-name" : "onem2mtest2bundle-features-0.1.0-SNAPSHOT"
        }
    }

Successful result returns output including list of installed archives and
related features of KarafFeatureLoader instances.


#### archive-uninstall

This call uninstalls features and removes the repository of the specified
archive. Requires two mandatory arguments specifying KarafFeatureLoader
instance name and archive name.

    restconf/operations/onem2mkaraffeatureloader:archive-uninstall

Input example:

    {
        "input": {
            "karaf-feature-loader-name" : "KarafFeatureLoaderDefault",
            "karaf-archive-name" : "onem2mtest2bundle-features-0.1.0-SNAPSHOT"
        }
    }

Successful result is indicated by result code 200 OK.


#### archive-reload

This call uninstalls and installs all features from the same archive from
the same URL. Can be used to load new version of features.
Two mandatory input arguments are used to specify KarafFeatureLoader instance
name and the specific archive.

    restconf/operations/onem2mkaraffeatureloader:archive-reload

Input example:

    {
        "input": {
            "karaf-feature-loader-name" : "KarafFeatureLoaderDefault",
            "karaf-archive-name" : "onem2mtest2bundle-features-0.1.0-SNAPSHOT"
        }
    }

Successful result is indicated by result code 200 OK.


#### archive-list-startup

This call provides list of startup configurations of KarafFeatureLoader
instances and their archives and related feature names.
Optional argument can be used to filter output for specific loader
instance.

    restconf/operations/onem2mkaraffeatureloader:archive-list-startup

Input example:

    {
        "input": {
            "karaf-feature-loader-name" : "KarafFeatureLoaderDefault"
        }
    }

Successful result includes the startup configuration.
