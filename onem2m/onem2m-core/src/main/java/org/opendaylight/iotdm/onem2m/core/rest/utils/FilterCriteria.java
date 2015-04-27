/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.List;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.DbAttr;
import org.opendaylight.iotdm.onem2m.core.database.DbAttrSet;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.utils.Onem2mDateTime;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterCriteria {

    private static final Logger LOG = LoggerFactory.getLogger(FilterCriteria.class);

    private FilterCriteria() { }

    /**
     * See if this resource passes each filter if specified
     * @param onem2mRequest
     * @param onem2mResource
     * @return matches true or false
     */
    public static boolean matches(RequestPrimitive onem2mRequest, Onem2mResource onem2mResource) {

        DbAttr dbAttrs = new DbAttr(onem2mResource.getAttr());
        DbAttrSet dbAttrSets = new DbAttrSet(onem2mResource.getAttrSet());

        String crb = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_CREATED_BEFORE);
        if (crb != null) {
            String ct = dbAttrs.getAttr(ResourceContent.CREATION_TIME);
            if (ct != null && Onem2mDateTime.dateCompare(ct, crb) >= 0) {
                return false;
            }
        }

        String cra = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_CREATED_AFTER);
        if (cra != null) {
            String ct = dbAttrs.getAttr(ResourceContent.CREATION_TIME);
            if (ct != null && Onem2mDateTime.dateCompare(ct, cra) <= 0) {
                return false;
            }
        }

        String ms = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_MODIFIED_SINCE);
        if (ms != null) {
            String mt = dbAttrs.getAttr(ResourceContent.LAST_MODIFIED_TIME);
            if (mt != null && Onem2mDateTime.dateCompare(mt, ms) >= 0) {
                return false;
            }
        }

        String ums = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_UNMODIFIED_SINCE);
        if (ums != null) {
            String mt = dbAttrs.getAttr(ResourceContent.LAST_MODIFIED_TIME);
            if (mt != null && Onem2mDateTime.dateCompare(mt, ums) <= 0) {
                return false;
            }
        }

        // TODO: add state tag checks here

        String rty = onem2mRequest.getPrimitive(RequestPrimitive.FILTER_CRITERIA_RESOURCE_TYPE);
        if (rty != null) {
            String rt = dbAttrs.getAttr(ResourceContent.RESOURCE_TYPE);
            if (rt != null && !rt.contentEquals(rty)) {
                return false;
            }
        }

        // for each label in the filter criteria array, see if it is in the labels array
        List<String> filterLabels = onem2mRequest.getPrimitiveMany(RequestPrimitive.FILTER_CRITERIA_LABELS);
        if (filterLabels != null) {
            boolean foundLabel = false;
            for (String filterLabel : filterLabels) {
                if (foundLabel) {
                    break;
                }
                List<String> dbLabels = null;//dbAttrSets.getAttrSet(ResourceContent.LABELS);
                // if no label in data store, then it does not pass the filter
                if (dbLabels == null) {
                    return false;
                }
                for (String dbLabel : dbLabels) {
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
