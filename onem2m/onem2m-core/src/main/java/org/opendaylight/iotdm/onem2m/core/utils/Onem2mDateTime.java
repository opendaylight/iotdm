/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *  From TS0004: DateTime string of ‘Basic Format’ specified in ISO8601.
 *  Time zone shall be interpreted as UTC timezone.
 *  ISO8601 standard YYYYMMDDTHHMMSSZ
 */
public class Onem2mDateTime {

    private Onem2mDateTime() {
    }

    public static String getCurrDateTime() {
        DateTime dt = new DateTime(DateTimeZone.UTC);
        DateTimeFormatter fmt = ISODateTimeFormat.basicDateTimeNoMillis();
        return fmt.print(dt);
    }

    private static DateTime stringToDate(String dateTimeString) {

        DateTime dt = null;
        DateTimeFormatter fmt = ISODateTimeFormat.basicDateTimeNoMillis();
        try {
            dt = fmt.parseDateTime(dateTimeString);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return dt;
    }

    public static boolean isValidDateTime(String dateTimeString) {

        DateTimeFormatter fmt = ISODateTimeFormat.basicDateTimeNoMillis();
        try {
            DateTime dt = fmt.parseDateTime(dateTimeString);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public static int dateCompare(String dateString1, String dateString2) {

        DateTime dt1 = stringToDate(dateString1);
        DateTime dt2 = stringToDate(dateString2);
        if (dt1 == null || dt2 == null) {
            return 0;
        }
        if (dt1.isAfter(dt2)) {
            return 1;
        }
        if (dt1.isBefore(dt2)) {
            return -1;
        }
        return 0;
    }
}