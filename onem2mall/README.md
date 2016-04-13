# IoTDM: OneM2M ALL

# Overview

The onem2m all project is a skeleton project whose sole purpose is to have a karaf distribution that contains
all onem2m features in the base onem2m project as well as all the onem2m-plugins folder.  As new plugin's
are developed, please consider adding your new plugin to this project.  Not sure yet how bizarre feature
interactions may occur BUT consider this before you add your plugin to onem2mall.

## Karaf Features

The karaf features listed in the features.xml exist to group logically a set of functionality.  For any application,
careful thought must be taken how to group features and to include features from other projects into your application.
For your convenience, the build system includes a karaf folder under which your complete application is built, and
the features that automatically start are from the odl-onem2mall-ui from your pom.xml.  This is only for convenience.
You have two choices, change what features ultimately get included by changing the feature under the odl-onem2m-all-ui,
or change which feature automatically gets started in karaf by looking for the line
<karaf.localFeature>odl-onem2mall-ui</karaf.localFeature> in the karaf/pom.xml.

