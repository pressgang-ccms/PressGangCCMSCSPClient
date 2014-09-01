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

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.junit.Test;

public class LoggingUtilitiesTest {

    @Test
    public void testLoggingUtilities() {
        final StringWriter writer = new StringWriter();
        final Logger logger = Logger.getLogger(LoggingUtilitiesTest.class);
        logger.addAppender(new WriterAppender(new SimpleLayout(), writer));
        logger.setLevel(Level.ALL);
        LoggingUtilities.tieSystemOutAndErrToLog(logger);

        System.out.println("Test stdout message.");
        System.err.println("Test stderr message.");

        assertEquals(writer.toString(), "INFO - Test stdout message.\nERROR - Test stderr message.\n");
    }
}
