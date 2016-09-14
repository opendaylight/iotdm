define(['iotdm-gui.services.module'], function(app) {
    'use strict';

    function Onem2mHelperService($log, $filter) {

        var MANDOTRY = 'mandotry';
        var PREFIX = 'm2m:';

        var resourceType = {
            accessControlPolicy: 1,
            AE: 2,
            container: 3,
            contentInstance: 4,
            CSEBase: 5,
            delivery: 6,
            eventConfig: 7,
            execInstance: 8,
            group: 9,
            locationPolicy: 10,
            m2mServiceSubscriptionProfile: 11,
            mgmtCmd: 12,
            mgmtObj: 13,
            node: 14,
            pollingChannel: 15,
            remoteCSE: 16,
            request: 17,
            schedule: 18,
            serviceSubscribedAppRule: 19,
            serviceSubscribedNode: 20,
            statsCollect: 21,
            statsConfig: 22,
            subscription: 23,
            accessControlPolicyAnnc: 10001,
            AEAnnc: 10002,
            containerAnnc: 10003,
            contentInstanceAnnc: 10004,
            groupAnnc: 10009,
            locationPolicyAnnc: 10010,
            mgmtObjAnnc: 10013,
            nodeAnnc: 10014,
            remoteCSEAnnc: 10016,
            scheduleAnnc: 10018
        };

        var cseTypeID = {
            IN_CSE: 1,
            MN_CSE: 2,
            ASN_CSE: 3
        };

        var locationSource = {
            Network_based: 1,
            Device_based: 2,
            Sharing_based: 3
        };

        var stdEventCats = {
            Immediate: 2,
            BestEffort: 3,
            Latest: 4
        };

        var operation = {
            create: 1,
            retrieve: 2,
            update: 3,
            delete: 4,
            notify: 5
        };

        var responseType = {
            nonBlockingRequestSynch: 1,
            nonBlockingRequestAsynch: 2,
            blockingRequest: 3
        };
        var resultContent = {
            nothing: 0,
            attributes: 1,
            "hierarchical address": 2,
            "hierarchical address and attributes": 3,
            "attributes and child resources": 4,
            "attributes and child resource references": 5,
            "child resource references": 6,
            "original resource": 7
        };

        var discResType = {
            structured: 1,
            unstructured: 2
        };

        var responseStatusCode = {
            ACCEPTED: 1000,
            OK: 2000,
            CREATED: 2001,
            DELETED: 2002,
            UPDATED: 2004,
            BAD_REQUEST: 4000,
            NOT_FOUND: 4004,
            OPERATION_NOT_ALLOWED: 4005,
            REQUEST_TIMEOUT: 4008,
            SUBSCRIPTION_CREATOR_HAS_NO_PRIVILEGE: 4101,
            CONTENTS_UNACCEPTABLE: 4102,
            ACCESS_DENIED: 4103,
            GROUP_REQUEST_IDENTIFIER_EXISTS: 4104,
            CONFLICT: 4105,
            INTERNAL_SERVER_ERROR: 5000,
            NOT_IMPLEMENTED: 5001,
            TARGET_NOT_REACHABLE: 5103,
            NO_PRIVILEGE: 5105,
            ALREADY_EXISTS: 5106,
            TARGET_NOT_SUBSCRIBABLE: 5203,
            SUBSCRIPTION_VERIFICATION_INITIATION_FAILED: 5204,
            SUBSCRIPTION_HOST_HAS_NO_PRIVILEGE: 5205,
            NON_BLOCKING_REQUEST_NOT_SUPPORTED: 5206,
            NOT_ACCEPTABLE: 5207,
            EXTERNAL_OBJECT_NOT_REACHABLE: 6003,
            EXTERNAL_OBJECT_NOT_FOUND: 6005,
            MAX_NUMBER_OF_MEMBER_EXCEEDED: 6010,
            MEMBER_TYPE_INCONSISTENT: 6011,
            MGMT_SESSION_CANNOT_BE_ESTABLISHED: 6020,
            MGMT_SESSION_ESTABLISHMENT_TIMEOUT: 6021,
            INVALID_CMDTYPE: 6022,
            INVALID_ARGUMENTS: 6023,
            INSUFFICIENT_ARGUMENTS: 6024,
            MGMT_CONVERSION_ERROR: 6025,
            MGMT_CANCELLATION_FAILED: 6026,
            ALREADY_COMPLETE: 6028,
            MGMT_COMMAND_NOT_CANCELLABLE: 6029
        };

        var requestStatus = {
            COMPLETED: 1,
            FAILED: 2,
            PENDING: 3,
            FORWARDED: 4
        };

        var memberType = {
            accessControlPolicy: 1,
            AE: 2,
            container: 3,
            contentInstance: 4,
            CSEBase: 5,
            delivery: 6,
            eventConfig: 7,
            execInstance: 8,
            group: 9,
            locationPolicy: 10,
            m2mServiceSubscription: 11,
            mgmtCmd: 12,
            mgmtObj: 13,
            node: 14,
            pollingChannel: 15,
            remoteCSE: 16,
            request: 17,
            schedule: 18,
            serviceSubscribedAppRule: 19,
            serviceSubscribedNode: 20,
            statsCollect: 21,
            statsConfig: 22,
            subscription: 23,
            mixed: 24
        };

        var consistencyStrategy = {
            ABANDON_MEMBER: 1,
            ABANDON_GROUP: 2,
            SET_MIXED: 3
        };

        var cmdType = {
            RESET: 1,
            REBOOT: 2,
            UPLOAD: 3,
            DOWNLOAD: 4,
            SOFTWAREINSTALL: 5,
            SOFTWAREUNINSTALL: 6,
            SOFTWAREUPDATE: 7
        };

        var execModetType = {
            IMMEDIATEONCE: 1,
            IMMEDIATEREPEAT: 2,
            RANDOMONCE: 3,
            RANDOMREPEAT: 4
        };
        var execStatusType = {
            INITIATED: 1,
            PENDING: 2,
            FINISHED: 3,
            CANCELLING: 4,
            CANCELLED: 5,
            STATUS_NON_CANCELLABLE: 6
        };

        var execResultType = {
            STATUS_REQUEST_UNSUPPORTED: 1,
            STATUS_REQUEST_DENIED: 2,
            STATUS_CANCELLATION_DENIED: 3,
            STATUS_INTERNAL_ERROR: 4,
            STATUS_INVALID_ARGUMENTS: 5,
            STATUS_RESOURCES_EXCEEDED: 6,
            STATUS_FILE_TRANSFER_FAILED: 7,
            STATUS_FILE_TRANSFER_SERVER_AUTHENTICATION_FAILURE: 8,
            STATUS_UNSUPPORTED_PROTOCOL: 9,
            STATUS_UPLOAD_FAILED: 10,
            STATUS_FILE_TRANSFER_FAILED_MULTICAST_GROUP_UNABLE_JOIN: 11,
            STATUS_FILE_TRANSFER_FAILED_SERVER_CONTACT_FAILED: 12,
            STATUS_FILE_TRANSFER_FAILED_FILE_ACCESS_FAILED: 13,
            STATUS_FILE_TRANSFER_FAILED_DOWNLOAD_INCOMPLETE: 14,
            STATUS_FILE_TRANSFER_FAILED_FILE_CORRUPTED: 15,
            STATUS_FILE_TRANSFER_FILE_AUTHENTICATION_FAILURE: 16,
            STATUS_FILE_TRANSFER_WINDOW_EXCEEDED: 19,
            STATUS_INVALID_UUID_FORMAT: 20,
            STATUS_UNKNOWN_EXECUTION_ENVIRONMENT: 21,
            STATUS_DISABLED_EXECUTION_ENVIRONMENT: 22,
            STATUS_EXECUTION_ENVIRONMENT_MISMATCH: 23,
            STATUS_DUPLICATE_DEPLOYMENT_UNIT: 24,
            STATUS_SYSTEM_RESOURCES_EXCEEDED: 25,
            STATUS_UNKNOWN_DEPLOYMENT_UNIT: 26,
            STATUS_INVALID_DEPLOYMENT_UNIT_STATE: 27,
            STATUS_INVALID_DEPLOYMENT_UNIT_UPDATE_DOWNGRADE_DISALLOWED: 28,
            STATUS_INVALID_DEPLOYMENT_UNIT_UPDATE_UPGRADE_DISALLOWED: 29,
            STATUS_INVALID_DEPLOYMENT_UNIT_UPDATE_VERSION_EXISTS: 30
        };

        var pendingNotification = {
            sendLatest: 1,
            sendAllPending: 2
        };

        var notificationContentType = {
            "All Attributes": 1,
            "Modefied Attributes": 2,
            "ResourceID": 3
        };

        var notificationEventType = {
            Update_of_Resource: 1,
            Delete_of_Resource: 2,
            Create_of_Direct_Child_Resource: 3,
            Delete_of_Direct_Child_Resource: 4
        };

        var status = {
            Successful: 1,
            Failure: 2,
            In_Process: 3
        };

        var batteryStatus = {
            NORMAL: 1,
            CHARGING: 2,
            CHARGING_COMPLETE: 3,
            DAMAGED: 4,
            LOW_BATTERY: 5,
            NOT_INSTALLED: 6,
            UNKNOWN: 7
        };

        var mgmtDefinition = {
            firmware: 1001,
            software: 1002,
            memory: 1003,
            areaNwkInfo: 1004,
            areaNwkDeviceInfo: 1005,
            battery: 1006,
            deviceInfo: 1007,
            deviceCapability: 1008,
            reboot: 1009,
            eventLog: 1010,
            cmdhPolicy: 1011,
            activeCmdhPolicy: 1012,
            cmdhDefaults: 1013,
            cmdhDefEcValue: 1014,
            cmdhEcDefParamValues: 1015,
            cmdhLimits: 1016,
            cmdhNetworkAccessRules: 1017,
            cmdhNwAccessRule: 1018,
            cmdhBuffer: 1019,
            unspecified: 0
        };

        var logTypeId = {
            System: 1,
            Security: 2,
            Event: 3,
            Trace: 4,
            Panic: 5
        };

        var logStatus = {
            Started: 1,
            Stopped: 2,
            Unknown: 3,
            NotPresent: 4,
            Error: 5
        };

        var eventType = {
            DATAOPERATION: 1,
            STORAGEBASED: 2,
            TIMERBASED: 3
        };

        var statsRuleStatusType = {
            ACTIVE: 1,
            INACTIVE: 2
        };

        var statModelType = {
            EVENTBASED: 1
        };

        var encodingType = {
            Plain: 0,
            "base64 encoding applied on string data": 1,
            "base64 encoding applied on binary data": 2,
        };

        var accessControlOperations = {
            CREATE: 1,
            RETRIEVE: 2,
            UPDATE: 4,
            DELETE: 8,
            NOTIFY: 16,
            DISCOVERY: 32
        };

        var SRole_ID = {
            "Software Management": "01-001",
            "Device Configuration": "02-001",
            "Device Diagnostics and Management": "02-002",
            "Device Firmware Management": "02-003",
            "Device Topology": "02-004",
            "Location": "03-001",
            "Basic Data": "04-001",
            "Onboarding": "05-001",
            "Security Administration": "06-001",
            "Groups Management": "07-001",
            "Event Collection": "08-001",
        };
        var filterUsage = {
            "Discovery Criteria": 1,
            "Conditional Retrieval": 2

        };

        var boolean = {
            True: true,
            False: false

        };

        var longToShortDictionary = {
            "operation": "op",
            "to": "to",
            "from": "fr",
            "requestIdentifier": "rqi",
            "resourceType": "ty",
            "primitiveContent": "pc",
            "role": "rol",
            "originatingTimestamp": "ot",
            "requestExpirationTimestamp": "rqet",
            "resultExpirationTimestamp": "rset",
            "operationExecutionTime": "oet",
            "responseType": "rt",
            "resultPersistence": "rp",
            "resultContent": "rcn",
            "eventCategory": "ec",
            "deliveryAggregation": "da",
            "groupRequestIdentifier": "gid",
            "filterCriteria": "fc",
            "discoveryResultType": "drt",
            "responseStatusCode": "rsc",
            "requestPrimitive": "rqp",
            "responsePrimitive": "rsp",
            "accessControlPolicyIDs": "acpi",
            "announcedAttribute": "aa",
            "announceTo": "at",
            "creationTime": "ct",
            "expirationTime": "et",
            "labels": "lbl",
            "link": "lnk",
            "lastModifiedTime": "lt",
            "parentID": "pi",
            "resourceID": "ri",
            "stateTag": "st",
            "resourceName": "rn",
            "privileges": "pv",
            "selfPrivileges": "pvs",
            "App-ID": "api",
            "AE-ID": "aei",
            "appName": "apn",
            "pointOfAccess": "poa",
            "ontologyRef": "or",
            "nodeLink": "nl",
            "creator": "cr",
            "maxNrOfInstances": "mni",
            "maxByteSize": "mbs",
            "maxInstanceAge": "mia",
            "currentNrOfInstances": "cni",
            "currentByteSize": "cbs",
            "locationID": "li",
            "contentInfo": "cnf",
            "contentSize": "cs",
            "content": "con",
            "cseType": "cst",
            "CSE-ID": "csi",
            "supportedResourceType": "srt",
            "notificationCongestionPolicy": "ncp",
            "source": "sr",
            "target": "tg",
            "lifespan": "ls",
            "eventCat": "eca",
            "deliveryMetaData": "dmd",
            "aggregatedRequest": "arq",
            "eventID": "evi",
            "eventType": "evt",
            "eventStart": "evs",
            "eventEnd": "eve",
            "operationType": "opt",
            "dataSize": "ds",
            "execStatus": "exs",
            "execResult": "exr",
            "execDisable": "exd",
            "execTarget": "ext",
            "execMode": "exm",
            "execFrequency": "exf",
            "execDelay": "exy",
            "execNumber": "exn",
            "execReqArgs": "exra",
            "execEnable": "exe",
            "memberType": "mt",
            "currentNrOfMembers": "cnm",
            "maxNrOfMembers": "mnm",
            "memberIDs": "mid",
            "membersAccessControlPolicyIDs": "macp",
            "memberTypeValidated": "mtv",
            "consistencyStrategy": "csy",
            "groupName": "gn",
            "locationSource": "los",
            "locationUpdatePeriod": "lou",
            "locationTargetID": "lot",
            "locationServer": "lor",
            "locationContainerID": "loi",
            "locationContainerName": "lon",
            "locationStatus": "lost",
            "serviceRoles": "svr",
            "description": "dc",
            "cmdType": "cmt",
            "mgmtDefinition": "mgd",
            "objectIDs": "obis",
            "objectPaths": "obps",
            "nodeID": "ni",
            "hostedCSELink": "hcl",
            "CSEBase": "cb",
            "M2M-Ext-ID": "mei",
            "Trigger-Recipient-ID": "tri",
            "requestReachability": "rr",
            "originator": "org",
            "metaInformation": "mi",
            "requestStatus": "rs",
            "operationResult": "ors",
            "requestID": "rid",
            "scheduleElement": "se",
            "deviceIdentifier": "di",
            "ruleLinks": "rlk",
            "statsCollectID": "sci",
            "collectingEntityID": "cei",
            "collectedEntityID": "cdi",
            "devStatus": "ss",
            "statsRuleStatus": "srs",
            "statModel": "sm",
            "collectPeriod": "cp",
            "eventNotificationCriteria": "enc",
            "expirationCounter": "exc",
            "notificationURI": "nu",
            "groupID": "gpi",
            "notificationForwardingURI": "nfu",
            "batchNotify": "bn",
            "rateLimit": "rl",
            "preSubscriptionNotify": "psn",
            "pendingNotification": "pn",
            "notificationStoragePriority": "nsp",
            "latestNotify": "ln",
            "notificationContentType": "nct",
            "notificationEventCat": "nec",
            "subscriberURI": "su",
            "version": "vr",
            "URL": "url",
            "URI": "uri",
            "update": "ud",
            "updateStatus": "uds",
            "install": "in",
            "uninstall": "un",
            "installStatus": "ins",
            "activate": "act",
            "deactivate": "dea",
            "activeStatus": "acts",
            "memAvailable": "mma",
            "memTotal": "mmt",
            "areaNwkType": "ant",
            "listOfDevices": "ldv",
            "devID": "dvd",
            "devType": "dvt",
            "areaNwkId": "awi",
            "sleepInterval": "sli",
            "sleepDuration": "sld",
            "listOfNeighbors": "lnh",
            "batteryLevel": "btl",
            "batteryStatus": "bts",
            "deviceLabel": "dlb",
            "manufacturer": "man",
            "model": "mod",
            "deviceType": "dty",
            "fwVersion": "fwv",
            "swVersion": "swv",
            "hwVersion": "hwv",
            "capabilityName": "can",
            "attached": "att",
            "capabilityActionStatus": "cas",
            "enable": "ena",
            "disable": "dis",
            "currentState": "cus",
            "reboot": "rbo",
            "factoryReset": "far",
            "logTypeId": "lgt",
            "logData": "lgd",
            "logActionStatus": "lgs",
            "logStatus": "lgst",
            "logStart": "lga",
            "logStop": "lgo",
            "firmwareName": "fwn",
            "softwareName": "swn",
            "cmdhPolicyName": "cpn",
            "mgmtLink": "cmlk",
            "activeCmdhPolicyLink": "acmlk",
            "order": "od",
            "defEcValue": "dev",
            "requestOrigin": "ror",
            "requestContext": "rct",
            "requestContextNotification": "rctn",
            "requestCharacteristics": "rch",
            "applicableEventCategories": "aecs",
            "applicableEventCategory": "aec",
            "defaultRequestExpTime": "dqet",
            "defaultResultExpTime": "dset",
            "defaultOpExecTime": "doet",
            "defaultRespPersistence": "drp",
            "defaultDelAggregation": "dda",
            "limitsEventCategory": "lec",
            "limitsRequestExpTime": "lqet",
            "limitsResultExpTime": "lset",
            "limitsOpExecTime": "loet",
            "limitsRespPersistence": "lrp",
            "limitsDelAggregation": "lda",
            "targetNetwork": "ttn",
            "minReqVolume": "mrv",
            "backOffParameters": "bop",
            "otherConditions": "ohc",
            "maxBufferSize": "mbfs",
            "storagePriority": "sgp",
            "applicableCredIDs": "apci",
            "allowedApp-IDs": "aai",
            "allowedAEs": "aae",
            "accessControlPolicy": "acp",
            "accessControlPolicyAnnc": "acpA",
            "AE": "ae",
            "AEAnnc": "aeA",
            "container": "cnt",
            "containerAnnc": "cntA",
            "latest": "la",
            "oldest": "ol",
            "contentInstance": "cin",
            "contentInstanceAnnc": "cinA",
            "delivery": "dlv",
            "eventConfig": "evcg",
            "execInstance": "exin",
            "fanOutPoint": "fopt",
            "group": "grp",
            "groupAnnc": "grpA",
            "locationPolicy": "lcp",
            "locationPolicyAnnc": "lcpA",
            "m2mServiceSubscriptionProfile": "mssp",
            "mgmtCmd": "mgc",
            "mgmtObj": "mgo",
            "mgmtObjAnnc": "mgoA",
            "node": "nod",
            "nodeAnnc": "nodA",
            "pollingChannel": "pch",
            "pollingChannelURI": "pcu",
            "remoteCSE": "csr",
            "remoteCSEAnnc": "csrA",
            "request": "req",
            "schedule": "sch",
            "scheduleAnnc": "schA",
            "serviceSubscribedAppRule": "asar",
            "serviceSubscribedNode": "svsn",
            "statsCollect": "stcl",
            "statsConfig": "stcg",
            "subscription": "sub",
            "firmware": "fwr",
            "firmwareAnnc": "fwrA",
            "software": "swr",
            "softwareAnnc": "swrA",
            "memory": "mem",
            "memoryAnnc": "memA",
            "areaNwkInfo": "ani",
            "areaNwkInfoAnnc": "aniA",
            "areaNwkDeviceInfo": "andi",
            "areaNwkDeviceInfoAnnc": "andiA",
            "battery": "bat",
            "batteryAnnc": "batA",
            "deviceInfo": "dvi",
            "deviceInfoAnnc": "dviA",
            "deviceCapability": "dvc",
            "deviceCapabilityAnnc": "dvcA",
            "rebootAnnc": "rboA",
            "eventLog": "evl",
            "eventLogAnnc": "evlA",
            "cmdhPolicy": "cmp",
            "activeCmdhPolicy": "acmp",
            "cmdhDefaults": "cmdf",
            "cmdhDefEcValue": "cmdv",
            "cmdhEcDefParamValues": "cmpv",
            "cmdhLimits": "cml",
            "cmdhNetworkAccessRules": "cmnr",
            "cmdhNwAccessRule": "cmwr",
            "cmdhBuffer": "cmbf",
            "createdBefore": "crb",
            "createdAfter": "cra",
            "modifiedSince": "ms",
            "unmodifiedSince": "us",
            "stateTagSmaller": "sts",
            "stateTagBigger": "stb",
            "expireBefore": "exb",
            "expireAfter": "exa",
            "sizeAbove": "sza",
            "sizeBelow": "szb",
            "contentType": "cty",
            "limit": "lim",
            "attribute": "atr",
            "resourceStatus": "rss",
            "notificationEventType": "net",
            "operationMonitor": "om",
            "representation": "rep",
            "filterUsage": "fu",
            "eventCatType": "ect",
            "eventCatNo": "ecn",
            "number": "num",
            "duration": "dur",
            "notification": "sgn",
            "notificationEvent": "nev",
            "verificationRequest": "vrq",
            "subscriptionDeletion": "sud",
            "subscriptionReference": "sur",
            "accessId": "aci",
            "MSISDN": "msd",
            "action": "acn",
            "status": "sus",
            "childResource": "ch",
            "accessControlRule": "acr",
            "accessControlOriginators": "acor",
            "accessControlOperations": "acop",
            "accessControlContexts": "acco",
            "accessControlWindow": "a",
            "accessControlIpAddresses": "acip",
            "ipv4Addresses": "ipv4",
            "ipv6Addresses": "ipv6",
            "accessControlLocationRegion": "aclr",
            "countryCode": "accc",
            "circRegion": "accr",
            "value": "val",
            "type": "typ",
            "maxNrOfNotify": "mnn",
            "timeWindow": "tww",
            "scheduleEntry": "sce",
            "aggregatedNotification": "agn",
            "attributeList": "atrl",
            "aggregatedResponse": "agr",
            "resource": "rce",
            "URIList": "uril",
            "anyArg": "any",
            "fileType": "ftyp",
            "username": "unm",
            "password": "pwd",
            "filesize": "fsi",
            "targetFile": "tgf",
            "delaySeconds": "dss",
            "successURL": "surl",
            "startTime": "stt",
            "completeTime": "cpt",
            "UUID": "uuid",
            "executionEnvRef": "eer",
            "reset": "rst",
            "upload": "uld",
            "download": "dld",
            "softwareInstall": "swin",
            "softwareUpdate": "swup",
            "softwareUninstall": "swun",
            "tracingOption": "tcop",
            "tracingInfo": "tcin",
            "responseTypeValue": "rtv",
            "contentSerialization": "csz"
        };


        var attributes = {};

        var requestPrimitive = {
            "1": {
                "op": 1,
                "to":null,
                "fr": null,
                "rqi": null,
                "ty": null,
                "pc": null,
                "rol": null,
                "ot": null,
                "rqet": null,
                "rset": null,
                "oet": null,
                "rt": {
                    "rtv": null,
                    "nu": [
                        null
                    ]
                },
                "rp": null,
                "rcn": null,
                "ec": null,
                "da": null,
                "gid": null
            },
            "2": {
                "op": 2,
                "to":null,
                "fr": null,
                "rqi": null,
                "rol": null,
                "ot": null,
                "rqet": null,
                "rset": null,
                "oet": null,
                "rt": {
                    "rtv": null,
                    "nu": [
                        null
                    ]
                },
                "rp": null,
                "rcn": null,
                "ec": null,
                "da": null,
                "gid": null,
                "fc": {
                    "crb": null,
                    "cra": null,
                    "ms": null,
                    "us": null,
                    "sts": null,
                    "stb": null,
                    "exb": null,
                    "exa": null,
                    "lbl": [
                        null
                    ],
                    "ty": null,
                    "sza": null,
                    "szb": null,
                    "cty": [
                        null
                    ],
                    "atr": [{
                        "name": null,
                        "val": null
                    }],
                    "fu": null,
                    "lim": null
                },
                "drt": null
            },
            "3": {
                "op": 3,
                "to":null,
                "fr": null,
                "rqi": null,
                "pc": null,
                "rol": null,
                "ot": null,
                "rqet": null,
                "rset": null,
                "oet": null,
                "rt": {
                    "rtv": null,
                    "nu": [
                        null
                    ]
                },
                "rp": null,
                "rcn": null,
                "ec": null,
                "da": null,
                "gid": null,
                "fc": {
                    "crb": null,
                    "cra": null,
                    "ms": null,
                    "us": null,
                    "sts": null,
                    "stb": null,
                    "exb": null,
                    "exa": null,
                    "lbl": [
                        null
                    ],
                    "ty": null,
                    "sza": null,
                    "szb": null,
                    "cty": [
                        null
                    ],
                    "atr": [{
                        "name": null,
                        "val": null
                    }],
                    "fu": null,
                    "lim": null
                }
            },
            "4": {
                "op": 4,
                "to":null,
                "fr": null,
                "rqi": null,
                "rol": null,
                "ot": null,
                "rqet": null,
                "rset": null,
                "oet": null,
                "rt": {
                    "rtv": null,
                    "nu": [
                        null
                    ]
                },
                "rp": null,
                "rcn": null,
                "ec": null,
                "da": null,
                "gid": null,
                "fc": {
                    "crb": null,
                    "cra": null,
                    "ms": null,
                    "us": null,
                    "sts": null,
                    "stb": null,
                    "exb": null,
                    "exa": null,
                    "lbl": [
                        null
                    ],
                    "ty": null,
                    "sza": null,
                    "szb": null,
                    "cty": [
                        null
                    ],
                    "atr": [{
                        "name": null,
                        "val": null
                    }],
                    "fu": null,
                    "lim": null
                }
            }
        };
        var accessControlPolicy = {
            "1": {
                "rn": null,
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "pv": {
                    "acr": [{
                        "acor": [
                            null
                        ],
                        "acop": null,
                        "acco": [{
                            "actw": [
                                null
                            ],
                            "acip": {
                                "ipv4": [
                                    null
                                ],
                                "ipv6": [
                                    null
                                ]
                            },
                            "aclr": {
                                "accc": [
                                    null
                                ],
                                "accr": [
                                    null
                                ]
                            }
                        }]
                    }]
                },
                "pvs": {
                    "acr": [{
                        "acor": [
                            null
                        ],
                        "acop": null,
                        "acco": [{
                            "actw": [
                                null
                            ],
                            "acip": {
                                "ipv4": [
                                    null
                                ],
                                "ipv6": [
                                    null
                                ]
                            },
                            "aclr": {
                                "accc": [
                                    null
                                ],
                                "accr": [
                                    null
                                ]
                            }
                        }]
                    }]
                }
            },
            "3": {
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "pv": {
                    "acr": [{
                        "acor": [
                            null
                        ],
                        "acop": null,
                        "acco": [{
                            "actw": [
                                null
                            ],
                            "acip": {
                                "ipv4": [
                                    null
                                ],
                                "ipv6": [
                                    null
                                ]
                            },
                            "aclr": {
                                "accc": [
                                    null
                                ],
                                "accr": [
                                    null
                                ]
                            }
                        }]
                    }]
                },
                "pvs": {
                    "acr": [{
                        "acor": [
                            null
                        ],
                        "acop": null,
                        "acco": [{
                            "actw": [
                                null
                            ],
                            "acip": {
                                "ipv4": [
                                    null
                                ],
                                "ipv6": [
                                    null
                                ]
                            },
                            "aclr": {
                                "accc": [
                                    null
                                ],
                                "accr": [
                                    null
                                ]
                            }
                        }]
                    }]
                }
            }
        };

        var AE = {
            "1": {
                "rn": null,
                "acp": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "apn": null,
                "api": null,
                "poa": [
                    null
                ],
                "or": null,
                "rr": null
            },
            "3": {
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "apn": null,
                "poa": [
                    null
                ],
                "or": null
            }
        };

        var container = {
            "1": {
                "rn": null,
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "cr": null,
                "mni": null,
                "mbs": null,
                "mia": null,
                "li": null,
                "or": null
            },
            "3": {
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "mni": null,
                "mbs": null,
                "mia": null,
                "li": null,
                "or": null
            }
        };
        var contentInstance = {
            "1": {
                "rn": null,
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "cr": null,
                "cnf": null,
                "cs": null,
                "or": null,
                "con": null
            }
        };
        var node = {
            "1": {
                "rn": null,
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "ni": null,
                "hcl": null
            },
            "3": {
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "ni": null
            }
        };
        var group = {
            "1": {
                "rn": null,
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "cr": null,
                "mt": null,
                "mnm": null,
                "mid": [
                    null
                ],
                "macp": [
                    null
                ],
                "csy": null,
                "gn": null
            },
            "3": {
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "at": [
                    null
                ],
                "aa": [
                    null
                ],
                "mnm": null,
                "mid": [
                    null
                ],
                "macp": [
                    null
                ],
                "gn": null
            }
        };
        var subscription = {
            "1": {
                "rn": null,
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "enc": {
                    "crb": null,
                    "cra": null,
                    "ms": null,
                    "us": null,
                    "sts": null,
                    "stb": null,
                    "exb": null,
                    "exa": null,
                    "sza": null,
                    "szb": null,
                    "om": [
                        null
                    ],
                    "atr": [{
                        "name": null,
                        "val": null
                    }],
                    "net": [
                        null
                    ]
                },
                "exc": null,
                "nu": [
                    null
                ],
                "gpi": null,
                "nfu": null,
                "bn": {
                    "name": null,
                    "dur": null
                },
                "rl": {
                    "mnn": null,
                    "tww": null
                },
                "psn": null,
                "pn": null,
                "nsp": null,
                "ln": null,
                "nct": null,
                "nec": null,
                "cr": null,
                "su": null
            },
            "3": {
                "acpi": [
                    null
                ],
                "et": null,
                "lbl": [
                    null
                ],
                "enc": {
                    "crb": null,
                    "cra": null,
                    "ms": null,
                    "us": null,
                    "sts": null,
                    "stb": null,
                    "exb": null,
                    "exa": null,
                    "sza": null,
                    "szb": null,
                    "om": [
                        null
                    ],
                    "atr": [{
                        "name": null,
                        "val": null
                    }],
                    "net": [
                        null
                    ]
                },
                "exc": null,
                "nu": [
                    null
                ],
                "gpi": null,
                "nfu": null,
                "bn": {
                    "name": null,
                    "dur": null
                },
                "rl": {
                    "mnn": null,
                    "tww": null
                },
                "pn": null,
                "nsp": null,
                "ln": null,
                "nct": null,
                "nec": null
            }
        };


        var resourceTypeReverse = reverse(resourceType);
        var cseTypeIDReverse = reverse(cseTypeID);
        var locationSourceReverse = reverse(locationSource);
        var stdEventCatsReverse = reverse(stdEventCats);
        var operationReverse = reverse(operation);
        var responseTypeReverse = reverse(responseType);
        var resultContentReverse = reverse(resultContent);
        var discResTypeReverse = reverse(discResType);
        var shortToLongDictionary = reverse(longToShortDictionary);
        var resources = {
            accessControlPolicy: accessControlPolicy,
            AE: AE,
            container: container,
            contentInstance: contentInstance,
            node: node,
            group: group,
            subscription: subscription
        };


        this.NULL = Math.random();

        this.resourceType = resourceType;
        this.cseTypeID = cseTypeID;
        this.locationSource = locationSource;
        this.stdEventCats = stdEventCats;
        this.operation = operation;
        this.responseType = responseType;
        this.resultContent = resultContent;
        this.discResType = discResType;
        this.responseStatusCode = responseStatusCode;
        this.requestStatus = requestStatus;
        this.memberType = memberType;
        this.consistencyStrategy = consistencyStrategy;
        this.cmdType = cmdType;
        this.execModetType = execModetType;
        this.execStatusType = execStatusType;
        this.execResultType = execResultType;
        this.pendingNotification = pendingNotification;
        this.notificationContentType = notificationContentType;
        this.notificationEventType = notificationEventType;
        this.status = status;
        this.batteryStatus = batteryStatus;
        this.mgmtDefinition = mgmtDefinition;
        this.logTypeId = logTypeId;
        this.logStatus = logStatus;
        this.eventType = eventType;
        this.statsRuleStatusType = statsRuleStatusType;
        this.statModelType = statModelType;
        this.encodingType = encodingType;
        this.accessControlOperations = accessControlOperations;
        this.SRole_ID = SRole_ID;
        this.filterUsage = filterUsage;
        this.boolean=boolean;

        this.getRequestPrimitiveByOperation = getRequestPrimitiveByOperation;

        this.id = id;
        this.parentId = parentId;
        this.label = label;
        this.icon = icon;
        this.children = children;
        this.toShort = toShort;
        this.toLong = toLong;
        this.toOnem2mJson = toOnem2mJson;
        this.assignFrom = assignFrom;
        this.assignRequestIdentifier = assignRequestIdentifierHelper();
        this.getResourceByResourceTypeAndOperation = getResourceByResourceTypeAndOperation;
        this.readResourceType = readResourceType;

        function children(node) {
            return node.value.ch;
        }

        function id(node) {
            if (node)
                return node.value.ri;
            return "value.ri";

        }

        function parentId(node) {
            if (node && node.value && node.value.pi) {
                var array = node.value.pi.split("/");
                return array[2];
            }
        }

        function label(node) {
            if (node)
                return node.value.rn;
            return "value.rn";
        }

        function icon(node) {
            if (node && node.value) {
                var resourceTypeValue = node.value.ty;
                var resourceTypeName = resourceTypeReverse[resourceTypeValue];
                return resourceTypeName.toLowerCase();
            }
            return node;
        }

        function readResourceType(node) {
            return node.value.ty;
        }

        function isMandatory(name) {
            return name.contains(MANDOTRY);
        }

        function toShort(long) {
            if (long) {
                long = long.toString();
                long = long.startsWith(PREFIX) ? long.slice(PREFIX.length) : long;
                return longToShortDictionary[long] ? longToShortDictionary[long] : long;
            }
            return long;
        }

        function toLong(short) {
            if (short !== null && short !== undefined) {
                short = short.toString();
                short = short.startsWith(PREFIX) ? short.slice(PREFIX.length) : short;
                return shortToLongDictionary[short] ? shortToLongDictionary[short] : short;
            }
            return short;
        }

        function getRequestPrimitiveByOperation(operation) {
            return angular.copy(requestPrimitive[operation]);
        }

        function toOnem2mJson(json) {

            function handleArray(json) {
                if (angular.isArray(json)) {
                    json.pop();
                    json.forEach(function(j) {
                        handleArray(j);
                    });
                } else if (angular.isObject(json)) {
                    for (var key in json) {
                        handleArray(json[key]);
                    }
                }
            }

            function handleEmptyObject(object) {
                for (var k in object) {
                    var value = object[k];
                    if (angular.isObject(value)) {
                        if (Object.keys(value).length === 0) {
                            delete object[k];
                        } else {
                            handleEmptyObject(value);
                        }
                    }
                }
            }

            function stringToValue(json) {
                if (angular.isObject(json)) {
                    Object.keys(json).forEach(function(key) {
                        var value = json[key];
                        if (value === null || value === undefined || value === "" || (angular.isArray(value) && value.length === 0))
                            delete json[key];
                        else {
                            stringToValue(value);
                        }
                    });
                }
            }
            json = angular.copy(json);
            handleArray(json);
            stringToValue(json);
            handleEmptyObject(json);
            return json;
        }

        function assignFrom() {
            return "//localhost";
        }

        function assignRequestIdentifierHelper() {
            var count = 1;
            return function() {
                count++;
                return count.toString();
            };
        }

        function getResourceByResourceTypeAndOperation(resourceType, operation) {
            var resourceTypeName = resourceTypeReverse[resourceType];
            if (resources[resourceTypeName]) {
                var key = PREFIX + toShort(resourceTypeName);
                var result = {};
                result[key] = resources[resourceTypeName][operation];
                return angular.copy(result);
            }
        }

        function reverse(map) {
            var reverse = {};
            for (var key in map) {
                reverse[map[key]] = key;
            }
            return reverse;
        }
    }

    Onem2mHelperService.$inject = ['$log', '$filter'];
    app.service('Onem2mHelperService', Onem2mHelperService);
});
