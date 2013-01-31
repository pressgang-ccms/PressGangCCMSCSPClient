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
