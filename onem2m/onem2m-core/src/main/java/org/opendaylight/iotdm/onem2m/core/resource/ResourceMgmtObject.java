/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceMgmtObject {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceMgmtObject.class);

    private ResourceMgmtObject() {
    }

    public static final String MGMT_DEFINITION = "mgd";
    public static final String OBJECT_IDS = "obis";
    public static final String OBJECT_PATHS = "obps";
    public static final String MGMT_LINK = "cmlk";
    public static final String DESCRIPTION = "dc";

    public class Firmware {

        public static final String VERSION = "vr";
        public static final String FIRMWARE_NAME = "fwnnam";
        public static final String URL = "url";
        public static final String UPDATE = "ud";
        public static final String UPDATE_STATUS = "uds";
    }

    public class Software {

        public static final String VERSION = "vr";
        public static final String SOFTWARE_NAME = "swn";
        public static final String URL = "url";
        public static final String INSTALL = "in";
        public static final String UNINSTALL = "un";
        public static final String INSTALL_STATUS = "ins";
        public static final String ACTIVATE = "act";
        public static final String DEACTIVATE = "dea";
        public static final String ACTIVE_STATUS = "acts";
    }

    public class Memory {
        public static final String MEM_AVAILABLE = "mma";
        public static final String MEM_TOTAL = "mmt";
    }

    public class AreaNwkInfo {
        public static final String AREA_NWK_TYPE = "ant";
        public static final String LIST_OF_DEVICES = "ldv";
        // TS4 has the following
        public static final String ACTIVE_STATUS = "acts";
    }

    public class AreaNWkDeviceInfo {
        public static final String DEVID = "dvd";
        public static final String DEV_TYPE = "dvt";
        public static final String AREA_NWK_ID= "awi";
        public static final String SLEEP_INTERVAL= "sli";
        public static final String SLEEP_DURATION = "sld";
        public static final String STATUS= "ss";
        public static final String LIST_OF_NEIGHBORS= "lnh";
    }

    public class Battery {
        public static final String BATTERY_LEVEL = "btl";
        public static final String BATTERY_STATUS = "bts";
    }

    public class DeviceInfo {
        public static final String DEVICE_LABEL= "dlb";
        public static final String MANUFACTURER = "man";
        public static final String MODEL= "mod";
        public static final String DEVICE_TYPE= "dty";
        public static final String FW_VERSION= "fwr";
        public static final String SW_VERSION= "swv";
        public static final String HW_VERSION= "hwv";
    }

    public class DeviceCapability {
        public static final String CAPABILITY_NAME = "can";
        public static final String ATTACHED = "att";
        public static final String CAPABILITY_ACTION_STATUS = "cas";
        public static final String CURRENT_STATE = "cus";
        public static final String ENABLE = "ena";
        public static final String DISABLE = "dis";
    }

    public class Reboot {
        public static final String REBOOT = "rbo";
        public static final String FACTORY_RESET = "far";
    }

    public class EventLog {
        public static final String LOG_TYPE_ID = "lgt";
        public static final String LOG_DATA = "lgd";
        public static final String LOG_STATUS = "lgst";
        public static final String LOG_START = "lga";
        public static final String LOG_STOP = "lgo";
    }

    public class CmdhPolicy {
        public static final String CMDH_POLICY_NAME = "cpn";
        public static final String MGMT_LINK = "cmlk";
    }

    public class ActiveCmdhPolicy {
        public static final String ACTIVE_CMDH_POLICY_LINK = "acmlk";
    }

    public class CmdhDefaults {
        public static final String MGMT_LINK = "cmlk";
    }

    public class CmdhDefEcValue {
        public static final String ORDER = "od";
        public static final String DEF_EC_VALUE = "dev";
        public static final String REQUEST_ORIGIN = "ror";
        public static final String REQUEST_CONTEXT = "rct";
        public static final String REQUEST_CONTEXT_NOTIFICATION = "rctn";
        public static final String REQUEST_CHARACTERISTICS = "rch";

    }

    public class CmdhEcEefParamValues {
        public static final String APPLICABLE_EVENT_CATEGORY = "aec";
        public static final String DEFAULT_REQUEST_EXP_TIME = "dqet";
        public static final String DEFAULT_RESULT_EXP_TIME = "dset";
        public static final String DEFAULT_OP_EXEC_TIME = "doet";
        public static final String DEFAULT_RESP_PERSISTENCE  = "drp";
        public static final String DEFAULT_DEL_AGGREGATION  = "dda";
    }

    public class CmdhLimits {
        public static final String ORDER = "od";
        public static final String REQUEST_ORIGIN = "ror";
        public static final String REQUEST_CONTEXT = "rct";
        public static final String REQUEST_CONTEXT_NOTIFICATION = "rctn";
        public static final String REQUEST_CHARACTERISTICS = "rch";
        public static final String LIMITS_EVENT_CATEGORY = "lec";
        public static final String LIMITS_REQUEST_EXP_TIME = "lqet";
        public static final String LIMITS_RESULT_EXP_TIME = "lset";
        public static final String LIMITS_OP_EXEC_TIME = "loet";
        public static final String LIMITS_RESP_PERSISTENCE = "lrp";
        public static final String LIMITS_DEL_AGGREGATION = "lda";
    }

    public class CmdhNetworkAccessRules {
        public static final String APPLICABLE_EVENT_CATEGORIES = "aecs";
        public static final String MGMT_LINK = "cmlk";
    }

    public class CmdhNwAccessRule {
        public static final String TARGET_NETWORK = "ttn";
        public static final String MIN_REQ_VOLUME = "mrv";
        //todo: no short name?
        public static final String SPREADING_WAIT_TIME = "swt";
        public static final String BACK_OFF_PARAMETERS = "bop";
        public static final String OTHER_CONDITIONS = "ohc";
        public static final String MGMT_LINK = "cmlk";
    }

    public class CmdhBuffer {
        public static final String APPLICABLE_EVENT_CATEGORY = "aec";
        public static final String MAX_BUFFER_SIZE = "mbfs";
        public static final String STORAGE_PRIORITY = "sgp";
    }

    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            resourceContent.jsonCreateKeys.add(key); // this line is new

            Object o = resourceContent.getInJsonContent().get(key);

            switch (key) {

                case DESCRIPTION:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                    + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                case OBJECT_IDS:
                case OBJECT_PATHS:
                case MGMT_LINK:

                case AreaNwkInfo.LIST_OF_DEVICES:
                case AreaNWkDeviceInfo.LIST_OF_NEIGHBORS:
                case CmdhNwAccessRule.OTHER_CONDITIONS:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof JSONArray)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") array expected for json key: " + key);
                            return;
                        }
                        JSONArray array = (JSONArray) o;
                        for (int i = 0; i < array.length(); i++) {
                            if (!(array.opt(i) instanceof String)) {
                                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                        "CONTENT(" + RequestPrimitive.CONTENT + ") string expected for json array: " + key);
                                return;
                            }
                        }
                    }
                    break;

                case ResourceContent.LABELS:
                case ResourceContent.EXPIRATION_TIME:
                    if (!ResourceContent.parseJsonCommonCreateUpdateContent(key, resourceContent, onem2mResponse)) {
                        return;
                    }
                    break;

                // todo: will need to add "announceTo" "announceAttribute" later,
                // currently we do not support that

                // Special attribtue String
                case Firmware.FIRMWARE_NAME:
                case Firmware.VERSION:
                case Firmware.URL:
                    // todo: update_status special case?
                case Firmware.UPDATE_STATUS:

                case Software.SOFTWARE_NAME:
                case Software.INSTALL_STATUS:
                case Software.ACTIVATE:
                case Software.ACTIVE_STATUS:
                case Software.DEACTIVATE:

                case AreaNwkInfo.AREA_NWK_TYPE:

                case AreaNWkDeviceInfo.DEVID:
                case AreaNWkDeviceInfo.DEV_TYPE:
                case AreaNWkDeviceInfo.AREA_NWK_ID:
                case AreaNWkDeviceInfo.STATUS:

                case DeviceInfo.DEVICE_LABEL:
                case DeviceInfo.MANUFACTURER:
                case DeviceInfo.MODEL:
                case DeviceInfo.DEVICE_TYPE:
                case DeviceInfo.FW_VERSION:
                case DeviceInfo.SW_VERSION:
                case DeviceInfo.HW_VERSION:

                case DeviceCapability.CAPABILITY_NAME:
                case DeviceCapability.CAPABILITY_ACTION_STATUS:

                case EventLog.LOG_DATA:

                case CmdhPolicy.CMDH_POLICY_NAME:

                case ActiveCmdhPolicy.ACTIVE_CMDH_POLICY_LINK:

                case CmdhDefEcValue.ORDER:
                case CmdhDefEcValue.DEF_EC_VALUE:
                case CmdhDefEcValue.REQUEST_ORIGIN:
                case CmdhDefEcValue.REQUEST_CONTEXT:
                case CmdhDefEcValue.REQUEST_CONTEXT_NOTIFICATION:
                case CmdhDefEcValue.REQUEST_CHARACTERISTICS:

                case CmdhEcEefParamValues.APPLICABLE_EVENT_CATEGORY:
                case CmdhEcEefParamValues.DEFAULT_REQUEST_EXP_TIME:
                case CmdhEcEefParamValues.DEFAULT_RESULT_EXP_TIME:
                case CmdhEcEefParamValues.DEFAULT_OP_EXEC_TIME:
                case CmdhEcEefParamValues.DEFAULT_RESP_PERSISTENCE:
                case CmdhEcEefParamValues.DEFAULT_DEL_AGGREGATION:

                case CmdhLimits.LIMITS_EVENT_CATEGORY:
                case CmdhLimits.LIMITS_REQUEST_EXP_TIME:
                case CmdhLimits.LIMITS_RESULT_EXP_TIME:
                case CmdhLimits.LIMITS_OP_EXEC_TIME:
                case CmdhLimits.LIMITS_RESP_PERSISTENCE:
                case CmdhLimits.LIMITS_DEL_AGGREGATION:

                case CmdhNetworkAccessRules.APPLICABLE_EVENT_CATEGORIES:

                case CmdhNwAccessRule.TARGET_NETWORK:
                case CmdhNwAccessRule.MIN_REQ_VOLUME:
                case CmdhNwAccessRule.SPREADING_WAIT_TIME:
                case CmdhNwAccessRule.BACK_OFF_PARAMETERS:

                case CmdhBuffer.MAX_BUFFER_SIZE:
                case CmdhBuffer.STORAGE_PRIORITY:

                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof String)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                    + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                // Special attribute Boolean
                case Firmware.UPDATE:
                case Software.INSTALL:
                case Software.UNINSTALL:
                case DeviceCapability.ATTACHED:
                case DeviceCapability.CURRENT_STATE:
                case DeviceCapability.ENABLE:
                case DeviceCapability.DISABLE:
                case Reboot.REBOOT:
                case Reboot.FACTORY_RESET:
                case EventLog.LOG_START:
                case EventLog.LOG_STOP:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof Boolean)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                    + RequestPrimitive.CONTENT + ") Boolean expected for json key: " + key);
                            return;
                        }
                    }
                    break;

                // Special attribute Number
                case MGMT_DEFINITION:
                case Memory.MEM_AVAILABLE:
                case Memory.MEM_TOTAL:

                case AreaNWkDeviceInfo.SLEEP_INTERVAL:
                case AreaNWkDeviceInfo.SLEEP_DURATION:

                case Battery.BATTERY_LEVEL:
                case Battery.BATTERY_STATUS:

                case EventLog.LOG_TYPE_ID:
                case EventLog.LOG_STATUS:
                    if (!resourceContent.getInJsonContent().isNull(key)) {
                        if (!(o instanceof Integer)) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") number expected for json key: " + key);
                            return;
                        } else if ((Integer) o < 0) {
                            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE,
                                    "CONTENT(" + RequestPrimitive.CONTENT + ") integer must be non-negative: " + key);
                            return;
                        }
                    }
                    break;
                default:
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                            + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
                    return;
            }
        }
    }

    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        // verify this resource can be created under the target resource
        String mgd = "";
        if (onem2mRequest.isCreate) {
            String parentResourceType = onem2mRequest.getOnem2mResource().getResourceType();
            if (parentResourceType == null || !parentResourceType.contentEquals(Onem2m.ResourceType.NODE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create MGMT OBJECT under this resource type: " + parentResourceType);
                return;
            }
            mgd = onem2mRequest.getResourceContent().getInJsonContent().optString(MGMT_DEFINITION, null);
            if(mgd == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "mgd is missing ");
                return;
            }
        }

        if (onem2mRequest.isUpdate) {
            // objectIDs cannot be updated
            String objectIDs = onem2mRequest.getResourceContent().getInJsonContent().optString(OBJECT_IDS, null);
            if (objectIDs != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "ObjectID List cannot be updated according to TS1.");
                return;
            }

            // objectPaths cannot be updated
            String objectPaths = onem2mRequest.getResourceContent().getInJsonContent().optString(OBJECT_PATHS, null);
            if (objectPaths != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "ObjectPath List cannot be updated according to TS1.");
                return;
            }

            // definition cannot be updated
            String mgmtdefinition = onem2mRequest.getResourceContent().getInJsonContent().optString(MGMT_DEFINITION, null);
            if (mgmtdefinition != null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "MmgtDefinition cannot be updated according to TS1.");
                return;
            }

            // get the mgd from the existing mgmt resourcejsonString
            JSONObject mgmtJson = new JSONObject(onem2mRequest.getOnem2mResource().getResourceContentJsonString());
            mgd = mgmtJson.optString(MGMT_DEFINITION, null);
            if (mgd == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST,
                        "The Target mgmtObject does not contain mgmtDefinition");
                return;
            }
        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        // the following method has a problem: if Firmware payload contains other resources' attributes, the system will not reject
        // the following part can only be used to check Mantatody attributes
        switch (mgd) {
            case Onem2m.SpecializedResource.FIRMWARE:
                String fwr = resourceContent.getInJsonContent().optString(Firmware.FIRMWARE_NAME, null);
                String vr = resourceContent.getInJsonContent().optString(Firmware.VERSION, null);
                Boolean ud = resourceContent.getInJsonContent().optBoolean(Firmware.UPDATE);
                String url = resourceContent.getInJsonContent().optString(Firmware.URL, null);
                String uds = resourceContent.getInJsonContent().optString(Firmware.UPDATE_STATUS, null);
                // todo: updateStatus might be a new jsonObject like acr, not supported yet
                if (onem2mRequest.isCreate && (vr == null || ud == null || url == null || fwr == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "vr,ud,url or fwr is missing ");
                    return;
                }
                if (onem2mRequest.isUpdate && (uds != null)){
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "updateStatus cannot be updated.");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.SOFTWARE:
                String swnm = resourceContent.getInJsonContent().optString(Software.SOFTWARE_NAME, null);
                String svr = resourceContent.getInJsonContent().optString(Software.VERSION, null);
                String surl = resourceContent.getInJsonContent().optString(Software.URL, null);
                String installStatus = resourceContent.getInJsonContent().optString(Software.INSTALL_STATUS, null);
                if (onem2mRequest.isCreate && (swnm == null || svr == null || surl == null )) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "swn, svr, surl, install or uninstall is missing ");
                    return;
                }
                if (onem2mRequest.isUpdate && (installStatus != null)){
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "installStatus cannot be updated.");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.MEMORY:
                String mma = resourceContent.getInJsonContent().optString(Memory.MEM_AVAILABLE, null);
                String mmt = resourceContent.getInJsonContent().optString(Memory.MEM_TOTAL, null);
                if (onem2mRequest.isCreate && (mma == null || mmt == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "mma or mmt is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.AREA_NWK_INFO:
                String areaNwkType = resourceContent.getInJsonContent().optString(AreaNwkInfo.AREA_NWK_TYPE, null);
                JSONArray listOfDevices = resourceContent.getInJsonContent().optJSONArray(AreaNwkInfo.LIST_OF_DEVICES);
                if (onem2mRequest.isCreate && (areaNwkType == null || listOfDevices == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "areaNwkType or listOfDevices is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.AREA_NWK_DEVICE_INFO:
                String devID = resourceContent.getInJsonContent().optString(AreaNWkDeviceInfo.DEVID, null);
                String devType = resourceContent.getInJsonContent().optString(AreaNWkDeviceInfo.DEV_TYPE, null);
                String areaNwkID = resourceContent.getInJsonContent().optString(AreaNWkDeviceInfo.AREA_NWK_ID, null);
                JSONArray listOfNeighbors = resourceContent.getInJsonContent().optJSONArray(AreaNWkDeviceInfo.LIST_OF_NEIGHBORS);
                if (onem2mRequest.isCreate && (devID == null || devType == null || areaNwkID == null || listOfNeighbors == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "devID, devType, areaNwkID or listOfNeighbors is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.BATTERY:
                String batteryLevel = resourceContent.getInJsonContent().optString(Battery.BATTERY_LEVEL, null);
                String batteryStatus = resourceContent.getInJsonContent().optString(Battery.BATTERY_STATUS, null);
                if (onem2mRequest.isUpdate && (batteryLevel != null || batteryStatus != null )) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "batteryLevel or batteryStatus is read-only attribute ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.DEVICE_INFO:
                String deviceLable = resourceContent.getInJsonContent().optString(DeviceInfo.DEVICE_LABEL, null);
                String manufacturer = resourceContent.getInJsonContent().optString(DeviceInfo.MANUFACTURER, null);
                String model = resourceContent.getInJsonContent().optString(DeviceInfo.MODEL, null);
                String deviceType = resourceContent.getInJsonContent().optString(DeviceInfo.DEVICE_TYPE, null);
                String fwVersion = resourceContent.getInJsonContent().optString(DeviceInfo.FW_VERSION, null);
                String swVersion = resourceContent.getInJsonContent().optString(DeviceInfo.SW_VERSION, null);
                String hwVersion = resourceContent.getInJsonContent().optString(DeviceInfo.HW_VERSION, null);
                if (onem2mRequest.isCreate && (deviceLable == null || manufacturer == null || model == null||deviceType == null || fwVersion == null|| swVersion == null||hwVersion == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "deviceLable, manufacturer, model, deviceType, fwVersion, swVersion or hwVersion is missing ");
                    return;
                }
                break;

            case Onem2m.SpecializedResource.DEVICE_CAPABILITY:
                String capabilityName = resourceContent.getInJsonContent().optString(DeviceCapability.CAPABILITY_NAME, null);
                String cababilityActionStatus = resourceContent.getInJsonContent().optString(DeviceCapability.CAPABILITY_ACTION_STATUS, null);
                if (onem2mRequest.isCreate && (capabilityName == null || cababilityActionStatus == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "capabilityName, cababilityActionStatus or attached is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.REBOOT:
//                String reboot = resourceContent.getInJsonContent().optString(Reboot.REBOOT, null);
//                String factoryReset = resourceContent.getInJsonContent().optString(Reboot.FACTORY_RESET, null);
//                if (onem2mRequest.isCreate && (reboot == null || factoryReset == null )) {
//                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "reboot or factoryReset is missing ");
//                    return;
//                }
                break;
            case Onem2m.SpecializedResource.EVENT_LOG:
                String logTypeId = resourceContent.getInJsonContent().optString(EventLog.LOG_TYPE_ID, null);
                String logData = resourceContent.getInJsonContent().optString(EventLog.LOG_DATA, null);
                String logStatus = resourceContent.getInJsonContent().optString(EventLog.LOG_STATUS, null);
//                String logStart = resourceContent.getInJsonContent().optString(EventLog.LOG_START, null);
//                String logStop = resourceContent.getInJsonContent().optString(EventLog.LOG_STOP, null);
                if (onem2mRequest.isCreate && (logTypeId == null || logData == null || logStatus == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "logTypeId, logData, logStatus, logStart OR logStop is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDH_POLICY:
                String policyname = resourceContent.getInJsonContent().optString(CmdhPolicy.CMDH_POLICY_NAME, null);
                JSONArray cmdh_poli_mgmtlink = resourceContent.getInJsonContent().optJSONArray(CmdhPolicy.MGMT_LINK);
                if (onem2mRequest.isCreate && (policyname == null || cmdh_poli_mgmtlink == null )) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "cpn, cmlk is missing ");
                    return;
                }
                if (cmdh_poli_mgmtlink.length() < 4) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "mgmtLink should contain at least 4 links");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.ACTIVE_CMDH_POLICY:
                JSONArray act_cmd_mgmtlink = resourceContent.getInJsonContent().optJSONArray(CmdhPolicy.MGMT_LINK);
                if (onem2mRequest.isCreate && (act_cmd_mgmtlink == null )) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "cmlk is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDH_DEFAULTS:
                JSONArray cmdh_defaults = resourceContent.getInJsonContent().optJSONArray(CmdhDefaults.MGMT_LINK);
                if (onem2mRequest.isCreate && (cmdh_defaults == null )) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Mgmtlink(cmlk) is missing ");
                    return;
                }
                if (cmdh_defaults.length() < 2) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "mgmtLink should contain at least 2 links");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDH_DEFEC_VALUE:
                String order = resourceContent.getInJsonContent().optString(CmdhDefEcValue.ORDER, null);
                JSONArray requestOrigin = resourceContent.getInJsonContent().optJSONArray(CmdhDefEcValue.REQUEST_ORIGIN);
                if (onem2mRequest.isCreate && (order == null || requestOrigin == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "order, requestOrigin is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDH_ECDEF_PARAM_VALUES:
                JSONArray applicableEventCategory = resourceContent.getInJsonContent().optJSONArray(CmdhEcEefParamValues.APPLICABLE_EVENT_CATEGORY);
                if (onem2mRequest.isCreate && (applicableEventCategory == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "ApplicableEventCategory(aec) is missing ");
                    return;
                }
                String defaultreqexpTime = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_REQUEST_EXP_TIME, null);
                if (onem2mRequest.isCreate && defaultreqexpTime == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "defaultRequestExpTime(dqet) is missing ");
                    return;
                }
                String defaultresexpTime = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_RESULT_EXP_TIME, null);
                if (onem2mRequest.isCreate && defaultresexpTime == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "defaultResultExpTime(dset) is missing ");
                    return;
                }
                String defaultOpExecTime = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_OP_EXEC_TIME, null);
                if (onem2mRequest.isCreate && defaultOpExecTime == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "defaultOpExecTime(doet) is missing ");
                    return;
                }
                String defaultRespPersistence = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_RESP_PERSISTENCE, null);
                if (onem2mRequest.isCreate && defaultRespPersistence == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "defaultRespPersistence(drop) is missing ");
                    return;
                }
                String defaultDelAggregation = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_DEL_AGGREGATION, null);
                if (onem2mRequest.isCreate && defaultDelAggregation == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "defaultDelAggregation(dda) is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDG_LIMITS:
                String lorder = resourceContent.getInJsonContent().optString(CmdhLimits.ORDER, null);
                JSONArray lrequestOrigin = resourceContent.getInJsonContent().optJSONArray(CmdhDefEcValue.REQUEST_ORIGIN);
                if (onem2mRequest.isCreate && (lorder == null || lrequestOrigin == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "order, requestOrigin is missing ");
                    return;
                }
                JSONArray limitsEventCategory = resourceContent.getInJsonContent().optJSONArray(CmdhLimits.LIMITS_EVENT_CATEGORY);
                if (onem2mRequest.isCreate && (limitsEventCategory == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "LimitsEventCategory(lec) is missing ");
                    return;
                }
                String limitsreqexpTime = resourceContent.getInJsonContent().optString(CmdhLimits.LIMITS_REQUEST_EXP_TIME, null);
                if (onem2mRequest.isCreate && limitsreqexpTime == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "limitsRequestExpTime(lqet) is missing ");
                    return;
                }
                String limitsresexpTime = resourceContent.getInJsonContent().optString(CmdhLimits.LIMITS_RESULT_EXP_TIME, null);
                if (onem2mRequest.isCreate && limitsresexpTime == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "limitsResultExpTime(lset) is missing ");
                    return;
                }
                String limitsOpExecTime = resourceContent.getInJsonContent().optString(CmdhLimits.LIMITS_OP_EXEC_TIME, null);
                if (onem2mRequest.isCreate && limitsOpExecTime == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "limitsOpExecTime(loet) is missing ");
                    return;
                }
                String limitsRespPersistence = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_RESP_PERSISTENCE, null);
                if (onem2mRequest.isCreate && limitsRespPersistence == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "limitsRespPersistence(lrp) is missing ");
                    return;
                }
                String limitsDelAggregation = resourceContent.getInJsonContent().optString(CmdhEcEefParamValues.DEFAULT_DEL_AGGREGATION, null);
                if (onem2mRequest.isCreate && limitsDelAggregation == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "limitsDelAggregation(lda) is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDH_NETWORK_ACCESS_RULES:
                JSONArray applicableEventCategories = resourceContent.getInJsonContent().optJSONArray(CmdhNetworkAccessRules.APPLICABLE_EVENT_CATEGORIES);
                if (onem2mRequest.isCreate && applicableEventCategories == null) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "applicableEventCategories(aecs) is missing ");
                    return;
                }
            case Onem2m.SpecializedResource.CMDH_NW_ACCESS_RULE:
                String targetNetwork = resourceContent.getInJsonContent().optString(CmdhNwAccessRule.TARGET_NETWORK, null);
                String minReqVolume = resourceContent.getInJsonContent().optString(CmdhNwAccessRule.MIN_REQ_VOLUME, null);
                String spreadingWaitTime = resourceContent.getInJsonContent().optString(CmdhNwAccessRule.SPREADING_WAIT_TIME, null);
                String backOffParameters = resourceContent.getInJsonContent().optString(CmdhNwAccessRule.BACK_OFF_PARAMETERS, null);
                String mgmtlink = resourceContent.getInJsonContent().optString(CmdhNwAccessRule.MGMT_LINK, null);
                if (onem2mRequest.isCreate && (targetNetwork == null || minReqVolume == null || spreadingWaitTime == null || backOffParameters == null || mgmtlink == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "targetNetwork, minReqVolume, spreadingWaitTime, spreadingWaitTime or mgmtlink is missing ");
                    return;
                }
                break;
            case Onem2m.SpecializedResource.CMDH_BUFFER:
                String bufferApplicableEventCategory = resourceContent.getInJsonContent().optString(CmdhBuffer.APPLICABLE_EVENT_CATEGORY, null);
                Integer maxbufferSize = resourceContent.getInJsonContent().optInt(CmdhBuffer.MAX_BUFFER_SIZE, -1);
                String storePriority = resourceContent.getInJsonContent().optString(CmdhBuffer.STORAGE_PRIORITY, null);
                if (onem2mRequest.isCreate && (bufferApplicableEventCategory == null || maxbufferSize == -1 || storePriority == null)) {
                    onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "ApplicableEventCategory, maxBufferSize, or storagePriority is missing ");
                    return;
                }
                break;

        default:

            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid parameters");
            return;

        }

        /**
         * The resource has been filled in with any attributes that need to be
         * written to the database
         */

        if (onem2mRequest.isCreate) {
            if (!Onem2mDb.getInstance().createResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot create in data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }
        } else {
            if (!Onem2mDb.getInstance().updateResource(onem2mRequest, onem2mResponse)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.INTERNAL_SERVER_ERROR, "Cannot update the data store!");
                // TODO: what do we do now ... seems really bad ... keep stats
                return;
            }

        }
    }

    public static void handleCreateUpdate(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();
        /**
         * Need to add a new resource in the "Onem2m.ResourceTypeString";
         */
        resourceContent.parse(Onem2m.ResourceTypeString.MGMT_OBJECT, onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;

        if (resourceContent.isJson()) {
            parseJsonCreateUpdateContent(onem2mRequest, onem2mResponse);
            if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
                return;
        }

        resourceContent.processCommonCreateUpdateAttributes(onem2mRequest, onem2mResponse);
        if (onem2mResponse.getPrimitive(ResponsePrimitive.RESPONSE_STATUS_CODE) != null)
            return;
        ResourceMgmtObject.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }
}

