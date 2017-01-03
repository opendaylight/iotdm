# Onem2mBundleLoader

Onem2mBundleLoader module provides ability to load set of OSGi bundles
dynamically (during runtime) by RPC calls. This set of bundles is called
feature and has unique name (this is analogical to Apache Karaf features
which describes dependencies on OSGi bundles as well).

Every bundle in the feature has specified priority number. Lower
priority value means higher priority and bundle with the highest
priority is loaded first. It's responsibility of user to set correct
priorities to all bundles according to their dependency chain.

One Onem2mBundleLoader instance can load multiple features and serves
RPC requests with operations with the loaded features.
IoTDM can spawn multiple instances of Onem2mBundleLoader, each instance
have configured unique name. New instances of Onem2mBundleLoader and
their names can be configured in [BluePrint XML file of the module.](impl/src/main/resources/org/opendaylight/blueprint/impl-blueprint.xml)

Onem2mBundleLoader implements check whether the currently loaded
bundle isn't already loaded in the system (by IoTDM or dynamically).
If the conflict is detected then installation of whole feature fails
and none bundle is installed and user is notified about it by error
message in the response of the RPC call.

Examples of all provided RPC calls can be found in the example Postman
collection: ./IoTDM_BundleLoader.postman_collection.json

Although the Onem2mBundleLoader is intended to be used to load mainly
IoTDM plugins, it can be used for whichever OSGi bundle.

Next sections describes all RPC calls provided.


## Provided RPC calls

Following descriptions of RPC calls shows target HTTP endpoint which
must be set in target URL of POST request, e.g.:

    http://localhost:8181/restconf/operations/onem2mbundleloader:onem2m-bundle-loader-feature-put/

NOTE: All requests must have set also content type (JSON or XML) and
authorization header. There is used JSON content type in following
examples but it is also possible to use XML.


#### onem2m-bundle-loader-feature-put

This call replaces current configuration of the same feature or creates
new one and loads all bundles from the list.

    restconf/operations/onem2mbundleloader:onem2m-bundle-loader-feature-put/

Input example:

    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault",
            "feature-name": "NewFeature",
            "bundles-to-load": [
                {
                    "priority": 10,
                    "iotdm-bundle-jar-location": "file:///ws/plugins/onem2mtest2bundle-impl-0.1.0-SNAPSHOT.jar"
                },
                {
                    "priority": 1,
                    "iotdm-bundle-jar-location": "file:///ws/plugins/onem2mtest2bundle-api-0.1.0-SNAPSHOT.jar"
                }
            ]
        }
    }

Successful result is indicated by result code 200 OK.


#### onem2m-bundle-loader-running-config

This RPC call provides list of running configurations of all features
of all BundleLoader instances. Optional input can be used to filter
instances of specific BundleLoader instance and specific feature.

    restconf/operations/onem2mbundleloader:onem2m-bundle-loader-running-config/

Input example:

    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault",
            "feature-name": "NewFeature"
        }
    }


#### onem2m-bundle-loader-startup-config

This RPC call provides list of running configurations of all features
of all BundleLoader instances. Optional input can be used to filter
instances of specific BundleLoader instance and specific feature.

    restconf/operations/onem2mbundleloader:onem2m-bundle-loader-startup-config/
   
Input example:
    
    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault",
            "feature-name": "NewFeature"
        }
    }


#### onem2m-bundle-loader-feature-remove

This call removes specific feature from specific BundleLoader instance.
Input is mandatory.

    restconf/operations/onem2mbundleloader:onem2m-bundle-loader-feature-remove/

Input example:
    
    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault",
            "feature-name": "NewFeature"
        }
    }

Successful result is indicated by result code 200 OK.


#### onem2m-bundle-loader-feature-reload

This call uninstalls all bundles of current feature and installs them
again. Input is mandatory.

    /restconf/operations/onem2mbundleloader:onem2m-bundle-loader-feature-reload/
    
Input example:
        
    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault",
            "feature-name": "NewFeature"
        }
    }

Successful result is indicated by result code 200 OK.


#### onem2m-bundle-loader-clean

Removes all features of specified BundleLoader instance.
Input is mandatory.

    restconf/operations/onem2mbundleloader:onem2m-bundle-loader-clean/

Input example:
        
    {
        "input": {
            "bundle-loader-instance-name": "BundleLoaderInstanceDefault",
        }
    }

Successful result is indicated by result code 200 OK.
