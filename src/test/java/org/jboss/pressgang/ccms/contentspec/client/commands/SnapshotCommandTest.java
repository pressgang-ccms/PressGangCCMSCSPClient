package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.FutureTask;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.SnapshotProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.structures.SnapshotOptions;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.TextContentSpecProvider;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.UserProvider;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.LogMessageWrapper;
import org.jboss.pressgang.ccms.wrapper.TextCSProcessingOptionsWrapper;
import org.jboss.pressgang.ccms.wrapper.TextContentSpecWrapper;
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
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, CSTransformer.class, ClientUtilities.class})
public class SnapshotCommandTest extends BaseUnitTest {
    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary String randomString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String checksum;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock TextContentSpecProvider textContentSpecProvider;
    @Mock TopicProvider topicProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock TextContentSpecWrapper textContentSpecWrapper;
    @Mock TextCSProcessingOptionsWrapper textCSProcessingOptionsWrapper;
    @Mock UserProvider userProvider;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock ContentSpec contentSpec;
    @Mock ContentSpecProcessor processor;
    @Mock SnapshotProcessor snapshotProcessor;

    SnapshotCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(TextContentSpecProvider.class)).thenReturn(textContentSpecProvider);
        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);

        command = spy(new SnapshotCommand(parser, cspConfig, clientConfig));

        when(textContentSpecProvider.newTextContentSpec()).thenReturn(textContentSpecWrapper);
        when(textContentSpecProvider.newTextProcessingOptions()).thenReturn(textCSProcessingOptionsWrapper);
        when(textContentSpecProvider.updateTextContentSpec(any(TextContentSpecWrapper.class), any(TextCSProcessingOptionsWrapper.class),
                any(LogMessageWrapper.class))).thenReturn(textContentSpecWrapper);

        // Authentication is tested in the base implementation so assume all users are valid
        TestUtil.setUpAuthorisedUser(command, userProvider, users, user, username);

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
    public void shouldPrintErrorAndShutdownWhenContentSpecRevisionNotFound() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and the revision is set and we are creating a new spec
        command.setRevision(revision);
        command.setCreateNew(true);
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(id, revision)).willReturn(null);

        // When it is processed
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID and revision!"));
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
    public void shouldPrintErrorAndShutdownWhenRevisionIsUsedWithoutNew() {
        // Given a command with a revision
        command.setRevision(revision);

        // When processing
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(5));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString(
                "You cannot turn an existing Content Specification into a snapshot from a revision. Please create use the --new option to" +
                        " create a new Content Specification instead."));
    }

    @Test
    public void shouldShutdownWhenCreateNewContentSpecFails() {
        final ContentSpec spec = new ContentSpec();
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and the new flag is set
        command.setCreateNew(true);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(eq(id), anyInt())).willReturn(contentSpecWrapper);
        // and the transform works successfully
        PowerMockito.mockStatic(CSTransformer.class);
        when(CSTransformer.transform(any(ContentSpecWrapper.class), eq(providerFactory), anyBoolean())).thenReturn(spec);
        // and the content spec has some data
        spec.setId(id);
        spec.setChecksum(checksum);
        spec.setTitle(randomString);
        // and we don't want to create the content spec snapshot
        given(command.getProcessor()).willReturn(snapshotProcessor);
        doNothing().when(snapshotProcessor).processContentSpec(eq(spec), any(SnapshotOptions.class));
        // and the processing fails
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.saveContentSpec(eq(command), any(FutureTask.class))).willReturn(textContentSpecWrapper);
        given(textContentSpecWrapper.getFailed()).willReturn("");
        // and the helper method to get the content spec works
        when(ClientUtilities.getContentSpecEntity(eq(contentSpecProvider), anyInt(), anyInt())).thenCallRealMethod();
        when(ClientUtilities.getContentSpecAsString(eq(contentSpecProvider), anyInt(), anyInt())).thenCallRealMethod();

        // When setting the content spec topic revisions
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then the command should shutdown and an error message should be printed
        assertNull(spec.getId());
        assertNull(spec.getChecksum());
        assertThat(getStdOutLogs(),
                containsString("The revision of the Content Specification is invalid and as such the snapshot couldn't be saved."));
    }

    @Test
    public void shouldShutdownWhenSaveEditedContentSpecFails() {
        // Given a command with an id
        command.setIds(Arrays.asList(id));
        // and the new flag is set
        command.setCreateNew(false);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(eq(id), anyInt())).willReturn(contentSpecWrapper);
        // and the transform works successfully
        PowerMockito.mockStatic(CSTransformer.class);
        when(CSTransformer.transform(any(ContentSpecWrapper.class), eq(providerFactory), anyBoolean())).thenReturn(contentSpec);
        // and the processing fails
        // and we don't want to create the content spec snapshot
        given(command.getProcessor()).willReturn(snapshotProcessor);
        doNothing().when(snapshotProcessor).processContentSpec(eq(contentSpec), any(SnapshotOptions.class));
        // and the processing fails
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.saveContentSpec(eq(command), any(FutureTask.class))).willReturn(textContentSpecWrapper);
        given(textContentSpecWrapper.getFailed()).willReturn("");
        // and the helper method to get the content spec works
        when(ClientUtilities.getContentSpecEntity(eq(contentSpecProvider), anyInt(), anyInt())).thenCallRealMethod();
        when(ClientUtilities.getContentSpecAsString(eq(contentSpecProvider), anyInt(), anyInt())).thenCallRealMethod();

        // When setting the content spec topic revisions
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then the command should shutdown and an error message should be printed
        assertThat(getStdOutLogs(),
                containsString("The revision of the Content Specification is invalid and as such the snapshot couldn't be saved."));
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
        // and the transform works successfully
        PowerMockito.mockStatic(CSTransformer.class);
        when(CSTransformer.transform(any(ContentSpecWrapper.class), eq(providerFactory), anyBoolean())).thenReturn(contentSpec);
        // and the content spec has some data
        given(contentSpec.getId()).willReturn(id);
        given(contentSpec.getChecksum()).willReturn(checksum);
        given(contentSpec.getTitle()).willReturn(randomString);
        // and we don't want to create the content spec snapshot
        given(command.getProcessor()).willReturn(snapshotProcessor);
        doNothing().when(snapshotProcessor).processContentSpec(eq(contentSpec), any(SnapshotOptions.class));
        // and the processing succeeds
        PowerMockito.mockStatic(ClientUtilities.class);
        given(ClientUtilities.saveContentSpec(eq(command), any(FutureTask.class))).willReturn(textContentSpecWrapper);
        given(textContentSpecWrapper.getId()).willReturn(id);
        given(textContentSpecWrapper.getRevision()).willReturn(revision);
        // and the helper method to get the content spec works
        when(ClientUtilities.getContentSpecEntity(eq(contentSpecProvider), anyInt(), anyInt())).thenCallRealMethod();
        when(ClientUtilities.getContentSpecAsString(eq(contentSpecProvider), anyInt(), anyInt())).thenCallRealMethod();

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
