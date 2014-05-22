package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.FutureTask;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.rest.RESTManager;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextContentSpecV1;
import org.jboss.pressgang.ccms.rest.v1.jaxrsinterfaces.RESTInterfaceV1;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TextCSProcessingOptionsWrapper;
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

@PrepareForTest({CSTransformer.class, ClientUtilities.class})
public class SnapshotCommandTest extends BaseCommandTest {
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary String randomString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String checksum;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock RESTTextContentSpecV1 textContentSpec;
    @Mock TextCSProcessingOptionsWrapper textCSProcessingOptionsWrapper;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock RESTManager restManager;
    @Mock RESTInterfaceV1 restClient;

    SnapshotCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        command = spy(new SnapshotCommand(parser, cspConfig, clientConfig));

        // Authentication is tested in the base implementation so assume all users are valid
        TestUtil.setUpAuthorisedUser(command, userProvider, users, user, username);

        when(providerFactory.getRESTManager()).thenReturn(restManager);
        when(restManager.getRESTClient()).thenReturn(restClient);
        when(restClient.freezeJSONTextContentSpec(anyInt(), anyString(), anyBoolean(), anyInt(), anyBoolean(),
                anyString(), anyInt(), anyString())).thenReturn(textContentSpec);

        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);
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
        assertThat(getStdOutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecNotFound() {
        // Given a command called with an ID
        command.setIds(Arrays.asList(id));
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldPrintErrorAndShutdownWhenContentSpecHasErrors() {
        // Given a command called with an ID
        command.setIds(Arrays.asList(id));
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(eq(id), anyInt())).willReturn(contentSpecWrapper);
        // and the content spec has errors
        given(contentSpecWrapper.getFailed()).willReturn(randomString);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("The Content Specification has validation errors, please fix any errors and try again."));
    }

    @Test
    public void shouldShutdownWhenCreateNewContentSpecFails() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and the new flag is set
        command.setCreateNew(true);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(eq(id), anyInt())).willReturn(contentSpecWrapper);
        // and the processing fails
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.runLongRunningRequest(eq(command), any(FutureTask.class))).willReturn(textContentSpec);
        when(textContentSpec.getId()).thenReturn(id);
        given(textContentSpec.getFailedContentSpec()).willReturn("");
        // and the helper method to get the content spec works
        TestUtil.setUpContentSpecHelper(contentSpecProvider);
        // and getting error messages works
        TestUtil.setUpMessages();

        // When setting the content spec topic revisions
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then the command should shutdown and a message should be printed
        assertThat(getStdOutLogs(),
                containsString("Content Specification ID"));
    }

    @Test
    public void shouldShutdownWhenSaveEditedContentSpecFails() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and the new flag is set
        command.setCreateNew(false);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(eq(id), anyInt())).willReturn(contentSpecWrapper);
        // and the processing fails
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.runLongRunningRequest(eq(command), any(FutureTask.class))).willReturn(textContentSpec);
        given(textContentSpec.getFailedContentSpec()).willReturn("");
        // and the helper method to get the content spec works
        TestUtil.setUpContentSpecHelper(contentSpecProvider);
        // and getting error messages works
        TestUtil.setUpMessages();

        // When setting the content spec topic revisions
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then the command should shutdown and a message should be printed
        assertThat(getStdOutLogs(),
                containsString("Content Specification ID"));
    }

    @Test
    public void shouldPrintSuccessWhenSnapshotCreated() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(eq(id), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpec(eq(id))).willReturn(contentSpecWrapper);
        // and the wrapper will return an id and revision
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getRevision()).willReturn(revision);
        // and the processing succeeds
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.runLongRunningRequest(eq(command), any(FutureTask.class))).willReturn(textContentSpec);
        given(textContentSpec.getId()).willReturn(id);
        given(textContentSpec.getRevision()).willReturn(revision);
        // and the helper method to get the content spec works
        TestUtil.setUpContentSpecHelper(contentSpecProvider);
        // and getting error messages works
        TestUtil.setUpMessages();

        // When setting the content spec topic revisions
        command.process();

        // Then the command should shutdown and an error message should be printed
        assertThat(getStdOutLogs(), containsString("Snapshot successfully saved."));
        assertThat(getStdOutLogs(), containsString(String.format("Content Specification ID: " + id + "\nRevision: " + revision)));
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

        // Then the name should be "snapshot"
        assertThat(commandName, is("snapshot"));
    }
}
