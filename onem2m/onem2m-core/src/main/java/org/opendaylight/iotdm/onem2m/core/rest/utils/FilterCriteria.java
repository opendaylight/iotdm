/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContainer;
import org.opendaylight.iotdm.onem2m.core.resource.BaseResource;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContentInstance;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterCriteria {

    private static final Logger LOG = LoggerFactory.getLogger(FilterCriteria.class);

    private FilterCriteria() { }

    /**
     * See if this resource passes each filter if specified
     * @param onem2mRequest request
     * @param onem2mResource response
     * @return matches true or false
     */
    public static boolean matches(RequestPrimitive onem2mRequest, Onem2mResource onem2mResource, ResponsePrimitive onem2mResponse) {

        // if there is NO filter criteria specified then return early
        if (!onem2mRequest.getHasFilterCriteria()) {
            return true;
        }

        Integer resourceType = Integer.valueOf(onem2mResource.getResourceType());

        JSONObject jsonResourceContent = onem2mResponse.getJsonResourceContent();

        String crb = onem2mRequest.getPrimitiveFilterCriteriaCreatedBefore();
        if (crb != null) {
            String ct = jsonResourceContent.optString(BaseResource.CREATION_TIME);
            if (ct != null && Onem2mDateTime.dateCompare(ct, crb) >= 0) {
                return false;
            }
        }

        String cra = onem2mRequest.getPrimitiveFilterCriteriaCreatedAfter();
        if (cra != null) {
            String ct = jsonResourceContent.optString(BaseResource.CREATION_TIME);
            if (ct != null && Onem2mDateTime.dateCompare(ct, cra) <= 0) {
                return false;
            }
        }

        String ms = onem2mRequest.getPrimitiveFilterCriteriaModifiedSince();
        if (ms != null) {
            String mt = jsonResourceContent.optString(BaseResource.LAST_MODIFIED_TIME);
            if (mt != null && Onem2mDateTime.dateCompare(mt, ms) <= 0) {
                return false;
            }
        }

        String ums = onem2mRequest.getPrimitiveFilterCriteriaUnModifiedSince();
        if (ums != null) {
            String mt = jsonResourceContent.optString(BaseResource.LAST_MODIFIED_TIME);
            if (mt != null && Onem2mDateTime.dateCompare(mt, ums) >= 0) {
                return false;
            }
        }

        Integer sts = onem2mRequest.getPrimitiveFilterCriteriaStateTagSmaller();
        if (sts != -1) {
            Integer st = jsonResourceContent.optInt(BaseResource.STATE_TAG, -1);
            if (st != -1) {
                if (st >= Integer.valueOf(sts))
                return false;
            }
        }

        Integer stb = onem2mRequest.getPrimitiveFilterCriteriaStateTagBigger();
        if (stb != -1) {
            Integer st = jsonResourceContent.optInt(BaseResource.STATE_TAG, -1);
            if (st != -1) {
                if (st <= Integer.valueOf(stb))
                    return false;
            }
        }

        // hack, see if resource has a cbs or cs attr
        Integer sza = onem2mRequest.getPrimitiveFilterCriteriaSizeAbove();
        if (sza != -1) {
            Integer cbs = jsonResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE, -1);
            if (cbs != -1) {
                if (cbs <= Integer.valueOf(sza))
                    return false;
            } else {
                Integer cs = jsonResourceContent.optInt(ResourceContentInstance.CONTENT_SIZE, -1);
                if (cs != -1) {
                    if (cs <= Integer.valueOf(sza))
                        return false;
                }
            }
        }

        // hack, see if resource has a cbs or cs attr
        Integer szb = onem2mRequest.getPrimitiveFilterCriteriaSizeBelow();
        if (szb != -1) {
            Integer cbs = jsonResourceContent.optInt(ResourceContainer.CURR_BYTE_SIZE, -1);
            if (cbs != -1) {
                if (cbs >= Integer.valueOf(szb))
                    return false;
            } else {
                Integer cs = jsonResourceContent.optInt(ResourceContentInstance.CONTENT_SIZE, -1);
                if (cs != -1) {
                    if (cs >= Integer.valueOf(szb))
                        return false;
                }
            }
        }

        // for each resource in the filter criteria array, see if it matches our resource rtype
        List<Integer> filterResourceTypes = onem2mRequest.getPrimitiveFilterCriteriaResourceTypes();
        if (filterResourceTypes != null) {
            boolean foundResource = false;
            for (Integer filterResource : filterResourceTypes) {
                if (resourceType == filterResource) {
                    foundResource = true;
                    break;
                }
            }
            if (!foundResource) {
                return false;
            }
        }

        // for each label in the filter criteria array, see if it is in the labels array
        List<String> filterLabels = onem2mRequest.getPrimitiveFilterCriteriaLabels();
        if (filterLabels != null) {
            // if no labels in data store, then it does not pass the filter
            JSONArray dbLabels = jsonResourceContent.optJSONArray(BaseResource.LABELS);
            if (dbLabels == null) {
                return false;
            }
            boolean foundLabel = false;
            for (String filterLabel : filterLabels) {
                if (foundLabel) {
                    break;
                }
                for (int i = 0; i < dbLabels.length(); i++) {
                    String dbLabel = dbLabels.opt(i).toString();
                    if (dbLabel.contentEquals(filterLabel)) {
                        foundLabel = true;
                        break;
                    }
                }
            }
            if (!foundLabel) {
                return false;
            }
        }

        return true;
    }
}
