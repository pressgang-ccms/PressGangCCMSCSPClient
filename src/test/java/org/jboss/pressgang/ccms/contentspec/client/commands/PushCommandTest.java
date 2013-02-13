package org.jboss.pressgang.ccms.contentspec.client.commands;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.Level;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.enums.LevelType;
import org.jboss.pressgang.ccms.contentspec.provider.*;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.junit.*;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;
import java.util.Arrays;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.createRealFile;
import static org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil.setValidFileProperties;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@PrepareForTest({RESTProviderFactory.class, DocBookUtilities.class, FileUtilities.class, ClientUtilities.class})
public class PushCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String contentSpecString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphanumString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String filename;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;
    @Mock TopicProvider topicProvider;
    @Mock UserProvider userProvider;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock File file;
    @Mock File file2;
    private File realFile;

    private PushCommand command;

    @Before
    public void setUp() throws Exception {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        this.command = new PushCommand(parser, cspConfig, clientConfig);
    }

    @Test
    public void shouldFailIfUserUnauthorised() {
        // Given a user who is unauthorised as they have no name
        command.setUsername("");

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(2));
        }

        // Then it should fail and the program should print an error and exit
        assertThat(getStdOutLogs(), containsString("No username was specified for the server."));
    }

    @Test
    public void shouldFailIfNoFileOrIdInCspConfig() {
        // Given no files are specified
        // And there is no id specified in CSP config
        given(cspConfig.getContentSpecId()).willReturn(null);
        // And an authorised user
        setUpAuthorisedUser();

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldFailIfFileInvalid() {
        // Given multiple files to push, which is an invalid case
        command.setFiles(Arrays.asList(file, file2));
        // And an authorised user
        setUpAuthorisedUser();

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString("No file was found for the specified file name!"));
    }

    @Test
    public void shouldFailIfFileHasNoContent() {
        // Given a valid file to push that has no content
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        PowerMockito.mockStatic(FileUtilities.class);
        given(FileUtilities.readFileContents(file)).willReturn("");
        // And an authorised user
        setUpAuthorisedUser();

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then it should fail with an error message and exit
        assertThat(getStdOutLogs(), containsString("The specified file was empty!"));
    }

    @Test
    public void shouldPrintErrorLogsAndExitIfSpecStringParsingFails() {
        // Given a valid file with a content spec string that produces a null result when parsed
        command.setFiles(Arrays.asList(file));
        setValidFileProperties(file);
        PowerMockito.mockStatic(FileUtilities.class);
        given(FileUtilities.readFileContents(file)).willReturn(contentSpecString);
        // And an authorised user
        setUpAuthorisedUser();

        // When the PushCommand is processed
        try {
            command.process();
            // If we get here then the test failed
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then error messages are printed, INVALID is written to the console and the program exits
        assertThat(getStdOutLogs(), containsString("Invalid Content Specification! Incorrect file format."));
    }

    private void setUpAuthorisedUser() {
        command.setUsername(username);
        given(userProvider.getUsersByName(username)).willReturn(users);
        given(users.size()).willReturn(1);
        given(users.getItems()).willReturn(Arrays.asList(user));
        given(user.getUsername()).willReturn(username);
    }

}
