define(['iotdm-gui.services.module'], function(app) {
    'use strict';
    function Onem2mDescriptionService() {
        var universalDescription = {
            "ty": "Resource Type. This Read Only (assigned at creation time. and then cannot be changed) attribute identifies the type of the resource as specified in clause 9.6. Each resource shall have a resourceType attribute.",
            "ri": "This attribute is an identifier for the resource that is used for 'non-hierarchical addressing method', i.e. this attribute shall contain the 'Unstructured-CSE-relative-Resource-ID' format of a resource ID ",
            "rn": "This attribute is the name for the resource that is used for 'hierarchical addressing method' to represent the parent-child relationships of resources. ",
            "pi": "This attribute is the resourceID of the parent of this resource. This attributes is specified in all resource types except <CSEBase>.",
            "ct": "Time/date of creation of the resource",
            "lt": "Last modification time/date of the resource",
            "et": "Time/date after which the resource will be deleted by the Hosting CSE"
        };

        var commonDescription = {
            "acpi": "The attribute contains a list of identifiers  of an <accessControlPolicy> resource",
            "st": "An incremental counter of modification on the resource. When a resource is created, this counter is set to 0, and it will be incremented on every modification of the resource ",
            "at": "This attribute may be included in a CREATE or UPDATE Request in which case it contains a list of addresses/CSE-IDs where the resource is to be announced",
            "aa": "This attributes shall only be present at the original resource if some Optional Announced (OA) type attributes have been announced to other CSEs. This attribute maintains the list of the announced Optional Attributes (OA type attributes) in the original resource. Updates to this attribute will trigger new attribute announcement if a new attribute is added or de-announcement if the existing attribute is removed.",
            "lbl": "Tokens used to add meta-information to resources."
        };

        var requestPrimitiveDescription = {
            "op": "Operation to be executed: Create (C), Retrieve (R), Update (U), Delete (D)",
            "to": " Address of the target resource or target attribute for the operation",
            "fr": "Identifier representing the Originator",
            "rqi": "Request Identifier",
            "ty": "The resourceType attribute of the resource is the same as the specified  value. It also allows differentiating between normal and announced  resources.",
            "pc": "Resource content to be transferred.Create (C): Content is the content of the new resource with the resource type ResourceType.Update (U): Content is the content to be replaced in an existing resource.",
            "rol": "Required when role based access control is applied. A Role-ID that is allowed by the service subscription shall be provided otherwise the request is considered not valid.",
            "ot": "Originating timestamp of when the message was built.",
            "rqet": "Request message expiration timestamp.",
            "rset": "Result message expiration timestamp",
            "oet": "Indicates the time when the specified operation Operation is to be executed by the target CSE",
            "rt": "response message type: Indicates what type of response shall be sent to the issued request and when the response shall be sent to the Originator",
            "rp": "Indicates the time duration for whichthe response may persist.",
            "rcn": "Indicates what are the expected components of the result of the requested operation. The Originator of a request may not need to get back a result of an operation at all. This shall be indicated in the Result Content parameter",
            "ec": "Indicates the event category that should be used to handle this request. Event categories are impacting how Requests to access remotely hosted resources are processed in the CMDH CSF. Selection and scheduling of connections via CMDH are driven by policies that can differentiate event categories.",
            "da": "Delivery aggregation on/off: Use CRUD operations of <delivery> resources to express forwarding of one or more original requests to the same target CSE(s)",
            "gid": "Identifier optionally added to the group request that is to be fanned out to each member of the group in order to detect loops and avoid duplicated handling of operation in case of loops of group and common members between groups that have parent-child relationship.",
            "fc": "Conditions for filtered retrieve operation ",
            "crb": "The creationTime attribute of the resource is chronologically before the  specified value. ",
            "cra": "The creationTime attribute of the resource is chronologically after the  specified value. ",
            "ms": "The lastModifiedTime attribute of the resource is chronologically after the  specified value. ",
            "us": "The lastModifiedTime attribute of the resource is chronologically before  the specified value. ",
            "sts": "The stateTag attribute of the resource is smaller than the specified value. ",
            "stb": "The stateTag attribute of the resource is bigger than the specified value. ",
            "exb": "The expirationTime attribute of the resource is chronologically before the  specified value. ",
            "exa": "The expirationTime attribute of the resource is chronologically after the  specified value. ",
            "lbl": "The labels attributes of the resource matches the specified value. ",
            "sza": "The contentSize attribute of the <contentInstance> resource is equal to  or greater than the specified value. ",
            "szb": "The contentSize attribute of the <contentInstance> resource is smaller  than the specified value. ",
            "cty": "The  contentInfo attribute of the <contentInstance> resource matches the  specified value. ",
            "atr": "This is an attribute of resource types (clause 9.6). Therefore, a real tag  name is variable and depends on its usage and the value of the attribute  can have wild card *.",
            "fu": "Indicates how the filter criteria is used",
            "lim": "The maximum number of resources to be returned in the response. This  may be modified by the Hosting CSE. When it is modified, then the new  value shall be smaller than the suggested value by the Originator. ",
            "drt": " Optional Discovery result format. This parameter applies to discovery related requests to indicate the preference of the Originator for the format of returned information in the result of the operation."
        };


        var specificDscription = {
            "5": {
                "cst": "Indicates the type of CSE represented by the created resource",
                "csi": "The CSE identifier in SP-relative CSE-ID forma",
                "srt": "List of the resource types which are supported in the CSE",
                "poa": "Represents the list of physical addresses to be used by remote CSEs to connect to this CSE (e.g. IP address, FQDN). This attribute is exposed to its Registree.",
                "nl": "The resourceID of a <node> resource that represents the node specific information. "
            },
            "2": {
                "apn": "The name of the application, as declared by the application developer",
                "api": "The identifier of the Application ",
                "aei": "The identifier of the Application Entity ",
                "poa": "The list of addresses for communicating with the registered Application Entity over Mca reference point via the transport services provided by Underlying Network (e.g. IP address, FQDN, URI). This attribute shall be accessible only by the AE and the Hosting CSE.",
                "or": "A URI of the ontology used to represent the information that is managed and understood by the AE.",
                "rr": "If the AE that created this <AE> resource can receive a request, this attribute is set to \"TRUE\" otherwise \"FALSE\"",
                "nl": "The resourceID of a <node> resource that stores the node specific information where this AE resides.",
                "csz": "The list of supported serializations of the Content primitive parameter for receiving a request from its registrar CSE. (e.g. XML, JSON). The list shall be ordered so that the most preferred format comes first."
            },
            "3": {
                "disableRetrieval": "Boolean value to control RETRIE/UPDATE/DELETE operation on the child <contentInsance> resource.",
                "cr": "The AE-ID or CSE-ID of the entity which created the resource.",
                "mni": "Maximum number of direct child  <contentInstance> resources in the <container> resource.",
                "mbs": "Maximum  size in bytes of data (i.e. content attribute of a <contentInstance> resource) that is allocated for the <container> resource for all direct child <contentInstance> resources in the <container> resource.",
                "mia": "Maximum age of a direct child <contentInstance> resource in the <container> resource. The value is expressed in seconds.",
                "cni": "Current number of direct child <contentInstance> resource in the  <container> resource. It is limited by the maxNrOfInstances.",
                "cbs": "Current size in bytes of data(i.e. content attribute of a <contentInstance> resource) stored in all direct child <contentInstance> resources of a <container> resource. This is the summation of contentSize attribute values of the <contentInstance> resources. It is limited by themaxByteSize.",
                "li": "An ID of the resource where the attributes/policies that define how location information are obtained and managed. This attribute is defined only when the <container> resource is used for containing location information.",
                "or": "A reference (URI) of the ontology used to represent the information that is stored in the child <contentInstance> resources of the present <container> resource (see note)."
            },
            "4": {
                "contentRef": "This attribute contains a list of name-value pairs. Each entry expresses and associative reference to a <contentInstance> resource. The name of the entry indicates the relationship and the value of the entry the reference (URI) to the resource.",
                "cr": "The AE-ID or CSE-ID of the entity which created the resource.",
                "cnf": "Information on the content that is needed to understand the content. This attribute is a composite attribute. It is composed first of an Internet Media Type (as defined in the IETF RFC 6838) describing the type of the data, and second of an encoding information that specifies how to first decode the received content.",
                "cs": "Size in bytes of the content attribute",
                "or": "A reference (URI) of the ontology used to represent the information that is stored in the contentInstances resources of the <container> resource. If this attribute is not present, the contentInstance resource inherits the ontologyRef from the parent <container> resource if present ",
                "con": "Actual content of a contentInstance. This content may be opaque data for understandable with the help of the contentInfo. This may, for example, be an image taken by a security camera, or a temperature measurement taken by a temperature sensor."
            },
            "14": {
                "mgmtClientAddress": "Represents the physical address of management client of the node which is represented by this <node> resource. ",
                "ni": "The M2MMNodeMID of the node which is  represented by this <node>!resource. ",
                "hcl": "The resource ID of a resource where all  of the following applies:(1)The resource is a <CSEBase>  resource or a <remoteCSE> resource.(2)The resource is hosted on the  same CSE as the present <node> resource. (3)The resource represents the  CSE which resides on the  specific node that is represented  by the current <node> resource."
            },
            "9": {
                "semanticSupportIndicator": "Indicator of support for sematic discovery functionality via <semanticFanOutPoint>",
                "cr": "The AE-ID or CSE-ID of the entity which created the resource",
                "mt": "It is the resource type of the member resources of the group, if all member resources (including the member resources in any sub-groups) are of the  same type. Otherwise, it is of type 'mixed'. ",
                "cnm": "Current number of members in a group. It shall not be larger than maxNrOfMembers",
                "mnm": "Maximum number of members in the <group>",
                "mid": "List of member resource IDs referred to in the remaining of the present document as memberID",
                "macp": "List of IDs of <accessControlPolicy> resources defining who is allowed to access the <fanOutPoint> resource",
                "mtv": "Denotes if the resource types of all members resources of the group has been validated by the Hosting CSE",
                "csy": "This attribute determines how to deal with the <group> resource if the memberType validation fails",
                "gn": "Human readable name of the <group>"
            },
            "23": {
                "missingData": "The missingData includes two values: a minimum specified missing number of the Time Series Data within the specified window duration, and the window duration. The condition only applies to subscribed-to resources of type <timeSeries>.",
                "enc": "This attribute (notification policy) indicates the event criteria for which a notification is to be generated.",
                "exc": "This attribute (notification policy) indicates that the subscriber wants to set the life of this subscription to a limit of a maximum number of notifications. When the number of notifications sent reaches the count of this counter, the <subscription> resource shall be deleted, regardless of any other policy.",
                "nu": "This attribute (notification policy) indicates that the subscriber wants to set the life of this subscription to a limit of a maximum number of notifications. When the number of notifications sent reaches the count of this counter, the <subscription> resource shall be deleted, regardless of any other policy.",
                "gpi": "The ID of a <group> resource in case the subscription is made through a group. This attribute may be used in the Filter Criteria to discover all subscription resources created via a <fanoutPoint> resource to a specific groupID",
                "bn": "This attribute (notification policy) indicates that the subscription originator wants to receive batches of notifications rather than receiving them one at a time",
                "rl": "This attribute (notification policy) indicates that the subscriber wants to limit the rate at which it receives notifications. This attribute expresses the subscriber's notification policy and includes two values: a maximum number of events that may be sent within some duration, and the rateLimit window duration",
                "psn": "This attribute (notification policy) indicates that the subscriber wants to be sent notifications for events that were generated prior to the creation of this subscription",
                "pn": "This attribute (notification policy), if set, indicates how missed notifications due to a period of connectivity (according to the reachability and notification schedules). ",
                "nsp": "Indicates that the subscriber wants to set a priority for this subscription relative to other subscriptions belonging to this same subscriber. This attribute sets a number within the priority range.",
                "ln": "This attribute (notification policy) indicates if the subscriber wants only the latest notification. If multiple notifications of this subscription are buffered, and if the value of this attribute is set to true, then only the last notification shall be sent and it shall have the Event Category value set to 'latest'.",
                "nct": "Indicates a notification content type that shall be contained in notifications",
                "nec": "This attribute (notification policy) indicates the subscriber's requested Event Category to be used for notification messages generated by this subscription.",
                "cr": "AE-ID or CSE-ID which created the <subscription> resource.",
                "su": "This attribute shall be configured with the target of the subscriber",
                "crb": "The creationTime attribute of the resource is chronologically after the specified value.",
                "ms": "The lastModifiedTime attribute of the resource is chronologically after the specified value",
                "us": "The lastModifiedTime attribute of the resource is chronologically before the specified value",
                "sts": "The stateTag attribute of the resource is smaller than the specified value.",
                "stb": "The stateTag attribute of the resource is bigger than the specified value.",
                "exb": "The expirationTime attribute of the resource is chronologically before the specified value.",
                "exa": "The expirationTime attribute of the resource is chronologically before the specified value.",
                "sza": "The contentSize attribute of the <contentInstance> resource is equal to or greater than the specified value.",
                "szb": "The contentSize attribute of the <contentInstance> resource is smaller than the specified value.",
                "evt": "The type of event. Possible event type values are: A. Update to attributes of the subscribed-to resource. B. Deletion of the subscribed-to resource, C.Creation of a direct child of the subscribed-to resource, D.Deletion of a direct child of the subscribed-to resourceAn attempt to retrieve a  <contentInstance> direct-child-resource of a subscribed-to <container> resource is performed while this <contentInstance> child resource is an obsolete resource or the reference used for retrieving this resource is not assigned.This retrieval is performed by a RETRIEVE request targeting the subscribed-to resource with the Result Content parameter set to either \"child-resources\" or \"attributes+child-resources\"",
                "om": "The operations accessing the subscribed-to resource matches with the specified value. It allows monitoring which operation is attempted to the subscribed-to resource regardless of whether the operation is performed. This feature is useful when to find malicious AEs. Possible string arguments are: create, retrieve, update, delete.",
                "atr": "A list of attribute names of a subscribed-to-resource."
            },
            "1": {
                "accessControlObjectDetails": "It specifies a subset of child resource types of the targeted resource to which the access control rule applies",
                "actw": "Represents a time window constraint which is compared against the time that the request is received at the Hosting CSE.",
                "acip": "Represents an IP address constraint or IP address block constraint which is compared against the IP address of the Originator of the request.",
                "pv": "A set of access control rules that applies to resources referencing this <accessControlPolicy> resource using the accessControlPolicyID attribute.",
                "pvs": "A set of access control rules that apply to the <accessControlPolicy> resource itself.",
                "acor": "It represents the set of Originators that shall be allowed to use this access control rule. The set of Originators is described as a list of parameters, where the types of the parameter can vary within the list.",
                "acco": "A context that is permitted to use this access control rule",
                "acop": "represents the set of operations that are authorized using this access control rule",
                "aclr": "Represents a location region constraint which is compared against the location of the Originator of the request."
            }
        };

        this.descriptionByResourceType = function(resourceType) {
            var description = {};
            var resourceSpecificDscriptionDescription = specificDscription[resourceType];
            angular.extend(description, requestPrimitiveDescription, universalDescription, commonDescription, resourceSpecificDscriptionDescription);
            return description;
        };
    }

    app.service('Onem2mDescriptionService', Onem2mDescriptionService);
});
