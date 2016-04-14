/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 *  From TS0004: DateTime string of ‘Basic Format’ specified in ISO8601.
 *  Time zone shall be interpreted as UTC timezone.
 *  ISO8601 standard YYYYMMDDTHHMMSS
 */
public class Onem2mDateTime {

    public static final String DEFAULT_EXPIRATION_TIME = "yyyyMMdd'T'HHmmss";
    public static final String FOREVER = "29991231T111111";

    private Onem2mDateTime() {
    }

    public static String getCurrDateTime() {
        DateTime dt = new DateTime(DateTimeZone.UTC);
        //DateTimeFormatter fmt = ISODateTimeFormat.basicDateTimeNoMillis();
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DEFAULT_EXPIRATION_TIME);
        return fmt.print(dt);
    }

    public static String addAgeToCurTime(Integer ageInSeconds) {
        DateTime dt = new DateTime(DateTimeZone.UTC);
        dt = dt.plusSeconds(ageInSeconds);
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DEFAULT_EXPIRATION_TIME);
        return fmt.print(dt);
    }

    private static DateTime stringToDate(String dateTimeString) {

        DateTime dt = null;
        //DateTimeFormatter fmt = ISODateTimeFormat.basicDateTimeNoMillis();
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DEFAULT_EXPIRATION_TIME);
        try {
            dt = fmt.parseDateTime(dateTimeString);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return dt;
    }

    /**
     * check whether the input string is valid time format
     * @param dateTimeString
     * @return boolean
     */
    public static boolean isValidDateTime(String dateTimeString) {

//        DateTimeFormatter fmt = ISODateTimeFormat.basicDateTimeNoMillis();
        DateTimeFormatter fmt = DateTimeFormat.forPattern(DEFAULT_EXPIRATION_TIME);
        try {
            DateTime dt = fmt.parseDateTime(dateTimeString);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    /**
     * check whether the time string is still alive, should be longer then the currentTime
     * @param dateTimeString
     * @return boolean
     */
    public static boolean isAlive(String dateTimeString) {

        if (dateTimeString.contentEquals(FOREVER))
            return true;
        String cur = getCurrDateTime();
        return (dateCompare(dateTimeString, cur) > 0);
    }

    public static int dateCompare(String dateString1, String dateString2) {

        if (dateString1.contentEquals(FOREVER) && dateString2.contentEquals(FOREVER)) {
            return 0;
        }
        if (dateString1.contentEquals(FOREVER)) {
            return 1;
        }
        if (dateString2.contentEquals(FOREVER)) {
            return -1;
        }

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