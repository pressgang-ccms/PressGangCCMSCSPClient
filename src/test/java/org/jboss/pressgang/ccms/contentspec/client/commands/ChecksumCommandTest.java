package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;

import java.util.ArrayList;
import java.util.Arrays;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;

/**
 * @author kamiller@redhat.com (Katie Miller)
 */
public class ChecksumCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer randomNumber;

    @Mock ContentSpecWrapper contentSpec;
    @Mock UserWrapper user;

    ChecksumCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        command = new ChecksumCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecNotFound() {
        // Given a ChecksumCommand called without an ID
        command.setIds(new ArrayList<Integer>());
        // And no matching content spec
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn(null);

        // When it is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldPrintChecksumToConsole() {
        // Given a ChecksumCommand called without an ID
        command.setIds(new ArrayList<Integer>());
        // And no matching content spec
        given(contentSpecProvider.getContentSpecAsString(anyInt(), anyInt())).willReturn("CHECKSUM = " + randomNumber + "\n");

        // When it is processed
        command.process();

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("CHECKSUM=" + randomNumber));
    }

    @Test
    public void shouldRequireAnExternalConnection() {
        // Given an already initialised command

        // When invoking the method
        boolean result = command.requiresExternalConnection();

        // Then the answer should be true
        assertTrue(result);
    }

    @Test
    public void shouldNotLoadFromCsprocessorCfgWhenIdsSpecified() {
        // Given a command with ids specified
        command.setIds(Arrays.asList(id));

        // When invoking the method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be false
        assertFalse(result);
    }

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "checksum"
        assertThat(commandName, is("checksum"));
    }
}
