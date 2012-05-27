package com.redhat.contentspec.client.utils;

import java.io.PrintStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class LoggingUtilities
{
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
