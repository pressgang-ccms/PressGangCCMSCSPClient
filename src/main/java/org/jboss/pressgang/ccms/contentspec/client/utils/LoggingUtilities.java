/*
 * Copyright 2011-2014 Red Hat, Inc.
 *
 * This file is part of PressGang CCMS.
 *
 * PressGang CCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PressGang CCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PressGang CCMS. If not, see <http://www.gnu.org/licenses/>.
 */

package org.jboss.pressgang.ccms.contentspec.client.utils;

import java.io.PrintStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class LoggingUtilities {
    public static void tieSystemOutAndErrToLog(final Logger logger) {
        tieSystemOutToLog(logger);
        tieSystemErrToLog(logger);
    }

    public static void tieSystemErrToLog(final Logger logger) {
        System.setErr(createLoggingProxy(logger, System.err, Level.ERROR));
    }

    public static void tieSystemOutToLog(final Logger logger) {
        System.setOut(createLoggingProxy(logger, System.out, Level.INFO));
    }

    public static PrintStream createLoggingProxy(final Logger logger, final PrintStream realPrintStream, final Priority priority) {
        return new PrintStream(realPrintStream) {
            @Override
            public void print(final String string) {
                logger.log(priority, string);
            }

            @Override
            public void println(final String string) {
                logger.log(priority, string);
            }
        };
    }
}
