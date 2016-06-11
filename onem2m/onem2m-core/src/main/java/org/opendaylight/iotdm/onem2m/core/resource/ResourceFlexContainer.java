/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

//import org.hamcrest.CoreMatchers;
import org.opendaylight.iotdm.onem2m.core.Onem2m;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.modules.module.configuration.Onem2mCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

import org.w3c.dom.*;

import javax.xml.crypto.dsig.spec.XSLTTransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

public class ResourceFlexContainer {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceFlexContainer.class);

    public static String DEVICE_INSTANCE = "";
    public static String MANUFACTURER = "";
    public static String MODEL_NUMBER = "";
    public static String SERIAL_NUMBER = "";
    public static String FIRMWARE_VERSION = "";
    public static String REBOOT = "";
    public static String FACTORY_RESET = "";
    public static String AVAILABLE_POWER_SOURCES = "";
    public static String POWER_SOURCE_CURRENT = "";
    public static String BATTERY_LEVEL = "";
    public static String MEMORY_FREE = "";
    public static String ERROR_CODE = "";
    public static String RESET_ERROR_CODE = "";
    public static String CURRENT_TIME = "";
    public static String UTC_OFFSET = "";
    public static String TIMEZONE = "";
    public static String SUPPORTED_BINDING_AND_MODES = "";
    public static String xsdurl = "onem2m-core/src/main/config/device.xsd";
    public static String xmlurl = "onem2m-core/src/main/config/device.xml";

    private void ResourceFlexContainer() {

    }

    /*
     * Attributes of Flex Container
     */
    public static final String CONTAINER_DEFINITION = "cnd";
    public static final String CREATOR = "creator";
    public static final String ONTOLOGY_REF = "ontologyRef";

    private static void parseJsonCreateUpdateContent(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        Iterator<?> keys = resourceContent.getInJsonContent().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();

            resourceContent.jsonCreateKeys.add(key); // this line is new

            Object o = resourceContent.getInJsonContent().get(key);

            if (key.equals(CONTAINER_DEFINITION)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;

                    }
                }
            } else if (key.equals(CREATOR)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;

                    }
                }
            } else if (key.equals(ONTOLOGY_REF)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(DEVICE_INSTANCE)) {

                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;

                    }
                }

            }

            else if (key.equals(SERIAL_NUMBER)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;

                    }
                }

            }

            else if (key.equals(FIRMWARE_VERSION)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            }

            else if (key.equals(REBOOT)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            }

            else if (key.equals(FACTORY_RESET)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            }

            else if (key.equals(AVAILABLE_POWER_SOURCES)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            }

            else if (key.equals(POWER_SOURCE_CURRENT)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(BATTERY_LEVEL)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(MEMORY_FREE)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(ERROR_CODE)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(RESET_ERROR_CODE)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(CURRENT_TIME)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(MANUFACTURER)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(MODEL_NUMBER)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(UTC_OFFSET)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(TIMEZONE)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(SUPPORTED_BINDING_AND_MODES)) {
                if (!resourceContent.getInJsonContent().isNull(key)) {
                    if (!(o instanceof String)) {
                        onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                                + RequestPrimitive.CONTENT + ") string expected for json key: " + key);
                        return;
                    }
                }
            } else if (key.equals(ResourceContent.LABELS)) {
            } else if (key.equals(ResourceContent.EXPIRATION_TIME)) {
                if (!ResourceContent.parseJsonCommonCreateUpdateContent(key, resourceContent, onem2mResponse)) {
                }
            } else {

                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.CONTENTS_UNACCEPTABLE, "CONTENT("
                        + RequestPrimitive.CONTENT + ") attribute not recognized: " + key);
            }
            // todo: will need to add "announceTo" "announceAttribute" later,
            // currently we do not support that
        }
    }

    public static void processCreateUpdateAttributes(RequestPrimitive onem2mRequest, ResponsePrimitive onem2mResponse) {

        if (onem2mRequest.isCreate) {
            String parentResourceType = onem2mRequest.getOnem2mResource().getResourceType();
            if (parentResourceType == null || !parentResourceType.contentEquals(Onem2m.ResourceType.NODE)) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.OPERATION_NOT_ALLOWED,
                        "Cannot create FLEX CONTAINER under this resource type: " + parentResourceType);
                return;
            }
        }
        // verify this resource can be created under the target resource
        ResourceContent resourceContent = onem2mRequest.getResourceContent();

        /**
         * Check the mandotory attribtue's value
         */
        String cd = resourceContent.getInJsonContent().optString(CONTAINER_DEFINITION, null);

        switch (cd) {
        case Onem2m.Resource_Flex_Container.DEVICE:

            if (onem2mRequest.isCreate && DEVICE_INSTANCE == null) {
                onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "PARAMETER missing ");

                return;
            }
            break;

        default:

            onem2mResponse.setRSC(Onem2m.ResponseStatusCode.BAD_REQUEST, "Invalid cd");
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

        boolean val = validateDeviceXSD(xsdurl, xmlurl);
        if (val == true) {
            parseDeviceXML();
        } else {
            System.out.println("Device XML Validation failed !");

        }

        ResourceContent resourceContent = onem2mRequest.getResourceContent();
        /**
         * Need to add a new resource in the "Onem2m.ResourceTypeString";
         */
        resourceContent.parse(Onem2m.ResourceTypeString.FLEX_CONTAINER, onem2mRequest, onem2mResponse);
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
        ResourceFlexContainer.processCreateUpdateAttributes(onem2mRequest, onem2mResponse);

    }

    /*
     * Validate Device XSD
     */
    public static boolean validateDeviceXSD(String xsdPath, String xmlPath) {

        try {
            String filePath = new File("").getAbsolutePath();
            String[] s = filePath.split("onem2m-karaf");
            filePath = s[0];
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Source schemaSource = new StreamSource(new FileInputStream(filePath.concat(xsdPath)));
            Schema schema = factory.newSchema(schemaSource);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new FileInputStream(filePath.concat(xmlPath))));
        } catch (IOException | SAXException e) {
            System.out.println("Exception: " + e.getMessage());
            return false;
        }

        return true;

    }

    /*
     * Parse Device XML
     */
    public static void parseDeviceXML() {

        try {

            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            String filePath = new File("").getAbsolutePath();
            String[] s = filePath.split("onem2m-karaf");
            filePath = s[0];
            Document doc = docBuilder.parse(new FileInputStream(filePath.concat(xmlurl)));

            // normalize text representation
            doc.getDocumentElement().normalize();

            NodeList deviceList = doc.getElementsByTagName("device");
            NodeList device = deviceList.item(0).getChildNodes();

            Node deviceInstanceNode = device.item(1);
            DEVICE_INSTANCE = deviceInstanceNode.getNodeName();

            Node manufacturerNode = device.item(3);
            MANUFACTURER = manufacturerNode.getNodeName();

            Node modelNumberNode = device.item(5);
            MODEL_NUMBER = modelNumberNode.getNodeName();

            Node serialNumberNode = device.item(7);
            SERIAL_NUMBER = serialNumberNode.getNodeName();

            Node firmwareVersionNode = device.item(9);
            FIRMWARE_VERSION = firmwareVersionNode.getNodeName();

            Node rebootNode = device.item(11);
            REBOOT = rebootNode.getNodeName();

            Node factoryResetNode = device.item(13);
            FACTORY_RESET = factoryResetNode.getNodeName();

            Node available_power_sources = device.item(15);
            AVAILABLE_POWER_SOURCES = available_power_sources.getNodeName();

            Node power_source_current = device.item(17);
            POWER_SOURCE_CURRENT = power_source_current.getNodeName();

            Node battery_level = device.item(19);
            BATTERY_LEVEL = battery_level.getNodeName();

            Node memory_free = device.item(21);
            MEMORY_FREE = memory_free.getNodeName();

            Node error_code = device.item(23);
            ERROR_CODE = error_code.getNodeName();

            Node reset_error_code = device.item(25);
            RESET_ERROR_CODE = reset_error_code.getNodeName();

            Node current_time = device.item(27);
            CURRENT_TIME = current_time.getNodeName();

            Node utc_offset = device.item(29);
            UTC_OFFSET = utc_offset.getNodeName();

            Node timezone = device.item(31);
            TIMEZONE = timezone.getNodeName();

            Node supported_binding_and_modes = device.item(33);
            SUPPORTED_BINDING_AND_MODES = supported_binding_and_modes.getNodeName();

        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());

        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
