package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;

import java.util.ArrayList;
import java.util.Arrays;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

// TODO Finish writing unit tests
@PrepareForTest({ClientUtilities.class})
public class EditCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @ArbitraryString String lang;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;

    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;

    EditCommand command;

    @Before
    public void setUp() {
        bindStdOut();
        command = new EditCommand(parser, cspConfig, clientConfig);

        // Authentication is tested in the base implementation so assume all users are valid
        TestUtil.setUpAuthorisedUser(command, userProvider, users, user, username);
    }

    @Test
    public void shouldShutdownWithNoIds() {
        // Given a command with no ids
        command.setIds(new ArrayList<Integer>());
        // and no csprocessor.cfg data
        given(cspConfig.getContentSpecId()).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("No ID was specified by the command line."));
    }

    @Test
    public void shouldPrintErrorWhenContentSpecAndTopicUsed() {
        // Given a command with --content-spec and --topic specified
        command.setContentSpec(true);
        command.setTopic(true);
        // and an id was specified
        command.setIds(Arrays.asList(id));

        // When it's processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(),
                containsString("Cannot edit a Content Spec and Topic simultaneously. Please ensure only one option is specified."));
    }

    @Test
    public void shouldPrintErrorWhenContentSpecAndLocaleUsed() {
        // Given a command with --content-spec and --lang specified
        command.setContentSpec(true);
        command.setLocale(lang);
        // and an id was specified
        command.setIds(Arrays.asList(id));

        // When it's processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString(
                "Translated Content Specs cannot be edited from PressGang. Please use the Zanata instance associated with the project " +
                        "instead."));
    }

    @Test
    public void shouldPrintErrorWhenTopicAndLocaleUsed() {
        // Given a command with a --lang specified
        // NOTE: Topic should be the default so don't set it
        command.setLocale(lang);
        // and an id was specified
        command.setIds(Arrays.asList(id));

        // When it's processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString(
                "Translated Topics cannot be edited from PressGang. Please use the Zanata instance associated with the project instead."));
    }

    @Test
    public void shouldPrintErrorWhenTopicAndRevHistoryUsed() {
        // Given a command with --rev-history specified
        command.setTopic(true);
        command.setRevHistory(true);
        // and an id was specified
        command.setIds(Arrays.asList(id));

        // When it's processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("The --rev-history option cannot be used with the specified options."));
    }

    @Test
    public void shouldFailWithInvalidLanguage() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and a language is set
        command.setLocale("blah");
        // and the validation will fail
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doReturn(false).when(ClientUtilities.class);
        ClientUtilities.validateLanguage(eq(command), any(ServerSettingsWrapper.class), anyString());

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }
    }

    @Test
    public void shouldPrintErrorWhenNoCommandConfigured() {
        // Given a command with valid options
        command.setContentSpec(true);
        // and an id was specified
        command.setIds(Arrays.asList(id));
        // and no command
        given(clientConfig.getEditorCommand()).willReturn(null);

        // When it's processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(4));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("The csprocessor.ini configuration file doesn't have an editor command specified."));
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

        // Then the name should be "edit"
        assertThat(commandName, is("edit"));
    }
}
