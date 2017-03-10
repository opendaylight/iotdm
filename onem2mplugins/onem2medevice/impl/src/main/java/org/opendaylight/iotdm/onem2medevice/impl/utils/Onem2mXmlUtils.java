/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2medevice.impl.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.resource.*;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author jkosmel
 * TODO: temporary attempt for json/xml conversion stuff - for edevice testing purposes
 */
public class Onem2mXmlUtils {

    public static String xmlRequestToJson(String content) {
        JSONObject cnResourceBody;
        boolean bodyOnly = true;

        //convert xml to json
        JSONObject cnJson = XML.toJSONObject(content);

        //postproccessing is needed to get valid onem2m resource json
        String maybeM2mKey = cnJson.keySet().toArray()[0].toString();
        if(cnJson.length() == 1 && maybeM2mKey.contains("m2m:")) {
            cnResourceBody = cnJson.optJSONObject(maybeM2mKey);
            bodyOnly = false;
        }
        else
            cnResourceBody = cnJson;

        //remove xmlns element property
        cnResourceBody.remove("xmlns:m2m");

        //fix arrays - common fields
        List<String> arrayFields = Arrays.asList(
                BaseResource.LABELS,
                BaseResource.ACCESS_CONTROL_POLICY_IDS,
                ResourceCse.POINT_OF_ACCESS,
                ResourceCse.SUPPORTED_RESOURCE_TYPES,
                ResourceAE.CONTENT_SERIALIZATION,
                ResourceSubscription.NOTIFICATION_URI);
        for (String field:arrayFields) {
            Object o = cnResourceBody.opt(field);
            if(o != null) {
                if(!(o instanceof JSONArray)) {
                    cnResourceBody.put(field, Collections.singletonList(cnResourceBody.get(field).toString()));
                }
                else {
                    JSONArray array = (JSONArray) o;
                    ArrayList<String> strings = new ArrayList<String>(array.length());
                    for (int i = 0; i < array.length(); i++) {
                        strings.add(array.get(i).toString());
                    }
                    cnResourceBody.put(field, strings);
                }
            }
        }

        //fix strings - common fields
        List<String> stringFields = Arrays.asList(
                ResourceContentInstance.CONTENT,
                RequestPrimitive.CONTENT_FORMAT,
                ResourceContentInstance.CONTENT_INFO,
                BaseResource.EXPIRATION_TIME,
                BaseResource.RESOURCE_NAME);
        for (String field:stringFields) {
            Object o = cnResourceBody.opt(field);
            if(o != null && !(o instanceof String)) {
                cnResourceBody.put(field, o.toString());
            }
        }

        //return json string
        return bodyOnly ? cnResourceBody.toString() : cnJson.put(maybeM2mKey, cnResourceBody).toString();
    }

    public static String jsonResponseToXml(String responseContent, String resultContent) {
        String xmlContent;
        Object content = responseContent.startsWith("[") ? new JSONArray(responseContent) : new JSONObject(responseContent);
        if (content instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) content;
            String maybeM2mKey = jsonObject.keySet().toArray()[0].toString();
            if (maybeM2mKey.contains("m2m:")) {
                jsonObject = jsonObject.optJSONObject(maybeM2mKey);
                String rn = jsonObject.optString(BaseResource.RESOURCE_NAME);

                xmlContent = XML.toString(content, null); //convert
                if (!rn.equals("")) { //if exists move resource name to attributes
                    xmlContent = xmlContent
                            .replace("<rn>" + rn + "</rn>", "")
                            .replaceFirst(">", " xmlns:m2m=\"http://www.onem2m.org/xml/protocols\" rn=\"" + rn + "\">");
                } else {
                    xmlContent = xmlContent.replaceFirst(">", " xmlns:m2m=\"http://www.onem2m.org/xml/protocols\">");
                }
            } else {
                xmlContent = XML.toString(content, null); //not resource, maybe error
            }
        } else { //JSONArray - discovery result
            StringBuilder xmlBuilder = new StringBuilder();
            JSONArray arr = (JSONArray) content;

            xmlBuilder.append("<m2m:discovery xmlns:m2m=\"http://www.onem2m.org/xml/protocols\">");
            if (resultContent != null && resultContent.equals(Onem2m.ResultContent.ATTRIBUTES_CHILD_RESOURCE_REFS)) { //build edevice custom xml
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject el = (JSONObject) arr.get(i);
                    if (el.opt("m2m:cnt") != null) {
                        JSONObject cnt = el.getJSONObject("m2m:cnt");
                        String pi = cnt.getString("pi");
                        xmlBuilder.append("<ref ty=\"3\">")
                                .append(pi.substring(0, pi.lastIndexOf("/") + 1))
                                .append(cnt.getString("ri"))
                                .append("</ref>");
                    } else if (el.opt("val") != null) {
                        xmlBuilder.append("<ref ty=\"")
                                .append(el.getInt("typ"))
                                .append("\">")
                                .append(el.getString("val"))
                                .append("</ref>");
                    } else
                        xmlBuilder.append(XML.toString(el));
                }
            } else { //build xml converted discovery result
                for (int i = 0; i < arr.length(); i++) {
                    xmlBuilder.append(XML.toString(arr.get(i)));
                }
            }
            xmlBuilder.append("</m2m:discovery>");
            xmlContent = xmlBuilder.toString();
        }
        //return xml string
        return xmlContent;
    }
}
