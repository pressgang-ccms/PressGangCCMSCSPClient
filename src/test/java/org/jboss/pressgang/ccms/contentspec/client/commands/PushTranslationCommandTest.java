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
