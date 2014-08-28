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

package org.jboss.pressgang.ccms.contentspec.client.constants;

public class ConfigConstants {
    public static final String DEFAULT_CONFIG_FILE = "[servers]\n" +
            // Create the Default server in the config file
            "# Uncomment one of the default servers below based on the server you wish to connect to.\n" +
            "#" + Constants.DEFAULT_SERVER_NAME + "=production\n" +
            "#" + Constants.DEFAULT_SERVER_NAME + "=test\n\n" +

            // Create the default.username attribute
            "#If you use one username for all servers then uncomment and set-up the below value instead of each servers username\n" +
            "#default.username=\n\n" +

            // Create the Production server in the config file
            "# Production Server settings\n" +
            Constants.PRODUCTION_SERVER_NAME + ".url=" + Constants.DEFAULT_PROD_SERVER + "\n" +
            Constants.PRODUCTION_SERVER_NAME + ".username=\n\n" +

            // Create the Test server in the config file
            "# Test Server settings\n" +
            Constants.TEST_SERVER_NAME + ".url=" + Constants.DEFAULT_TEST_SERVER + "\n" +
            Constants.TEST_SERVER_NAME + ".username=\n\n" +

            // Create the Root Directory
            "[directory]\n" +
            "root=\n\n" +

            // Create the publican options
            "[publican]\n" +
            "build.parameters=" + Constants.DEFAULT_PUBLICAN_OPTIONS + "\n" +
            "preview.format=" + Constants.DEFAULT_PUBLICAN_FORMAT + "\n\n" +

            // Create the default translation options
            "[zanata]\n" +
            Constants.DEFAULT_ZANATA_SERVER_NAME + ".url=" + Constants.DEFAULT_ZANATA_SERVER + "\n" +
            Constants.DEFAULT_ZANATA_SERVER_NAME + ".username=\n" +
            Constants.DEFAULT_ZANATA_SERVER_NAME + ".key=\n\n" +

            // Create the default translation options
            "[publish]\n" +
            "koji.huburl=" + Constants.DEFAULT_KOJIHUB_URL + "\n" +
            "command=" + Constants.DEFAULT_PUBLISH_COMMAND + "\n\n";
}
