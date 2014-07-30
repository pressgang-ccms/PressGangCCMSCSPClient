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

package org.jboss.pressgang.ccms.contentspec.client.commands;

import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.powermock.core.classloader.annotations.PrepareForTest;

// TODO Write these tests
@Ignore
@PrepareForTest(ClientUtilities.class)
public class PushTranslationCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    PushTranslationCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        command = new PushTranslationCommand(parser, cspConfig, clientConfig);
    }
}
