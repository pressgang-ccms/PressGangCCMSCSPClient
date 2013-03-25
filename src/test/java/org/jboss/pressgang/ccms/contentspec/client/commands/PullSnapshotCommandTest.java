package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

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
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecProcessor;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.UserProvider;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, ClientUtilities.class, CSTransformer.class})
public class PullSnapshotCommandTest extends BaseUnitTest {
    private static final String CONTENTSPEC_TITLE = "ContentSpec";

    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary String randomString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock TopicProvider topicProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock UserProvider userProvider;
    @Mock CollectionWrapper<UserWrapper> users;
    @Mock UserWrapper user;
    @Mock ContentSpec contentSpec;
    @Mock ContentSpecProcessor processor;

    PullSnapshotCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        when(providerFactory.getProvider(TopicProvider.class)).thenReturn(topicProvider);
        when(providerFactory.getProvider(UserProvider.class)).thenReturn(userProvider);
        command = spy(new PullSnapshotCommand(parser, cspConfig, clientConfig));

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
        // Given a command called without an ID
        command.setIds(new ArrayList<Integer>());
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
        // and the revision is set
        command.setRevision(revision);
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
    public void shouldGenerateRightFilenameAndPathForContentSpec() {
        final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        final Date date = new Date();
        // Given a command called with an ID
        command.setIds(Arrays.asList(id));
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(contentSpecWrapper);
        // and the content spec title/id/last modified is set
        given(contentSpecWrapper.getTitle()).willReturn(CONTENTSPEC_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getLastModified()).willReturn(date);
        // and a output file is specified
        command.setOutputPath(rootTestDirectory.getAbsolutePath());
        // and assume the processing worked
        given(command.getProcessor()).willReturn(processor);
        given(processor.processContentSpec(any(ContentSpec.class), any(UserWrapper.class),
                any(ContentSpecParser.ParsingMode.class))).willReturn(true);
        // And we don't actually want to save anything
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doNothing().when(ClientUtilities.class);
        ClientUtilities.saveOutputFile(eq(command), anyString(), anyString(), anyString());
        // and the transformer returns a content spec
        PowerMockito.mockStatic(CSTransformer.class);
        when(CSTransformer.transform(any(ContentSpecWrapper.class), eq(providerFactory))).thenReturn(contentSpec);
        // and the content spec will return some string
        given(contentSpec.toString()).willReturn(randomString);

        // When processing the command
        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> outputPath = ArgumentCaptor.forClass(String.class);
        command.process();

        // Then verify that the filename and path are valid
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.saveOutputFile(eq(command), fileName.capture(), outputPath.capture(), anyString());
        assertThat(fileName.getValue(), is(CONTENTSPEC_TITLE + "-snapshot-" + dateFormatter.format(date) + ".contentspec"));
        assertThat(outputPath.getValue(), is(rootTestDirectory.getAbsolutePath()));
    }

    @Test
    public void shouldGenerateRightFilenameAndPathForContentSpecPullingFromConfig() {
        final DateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
        final Date date = new Date();
        // Given a command called without an ID
        command.setIds(new ArrayList<Integer>());
        // and the cspconfig has an id
        given(cspConfig.getContentSpecId()).willReturn(id);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(contentSpecWrapper);
        // and the content spec title/id/last modified is set
        given(contentSpecWrapper.getTitle()).willReturn(CONTENTSPEC_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getLastModified()).willReturn(date);
        // and a output file is specified
        command.setOutputPath(rootTestDirectory.getAbsolutePath());
        // and assume the processing worked
        given(command.getProcessor()).willReturn(processor);
        given(processor.processContentSpec(any(ContentSpec.class), any(UserWrapper.class),
                any(ContentSpecParser.ParsingMode.class))).willReturn(true);
        // And we don't actually want to save anything
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doNothing().when(ClientUtilities.class);
        ClientUtilities.saveOutputFile(eq(command), anyString(), anyString(), anyString());
        // and the transformer returns a content spec
        PowerMockito.mockStatic(CSTransformer.class);
        when(CSTransformer.transform(any(ContentSpecWrapper.class), eq(providerFactory))).thenReturn(contentSpec);
        // and the content spec will return some string
        given(contentSpec.toString()).willReturn(randomString);
        // and the command should call some real methods in ClientUtilities
        when(ClientUtilities.prepareAndValidateIds(eq(command), eq(cspConfig), anyList())).thenCallRealMethod();
        when(ClientUtilities.prepareIds(eq(command), eq(cspConfig), anyList())).thenCallRealMethod();
        // and the prepare getOutputRootDirectory returns the rootDirectory path
        when(ClientUtilities.getOutputRootDirectory(eq(cspConfig), eq(contentSpecWrapper))).thenReturn(
                rootTestDirectory.getAbsolutePath() + File.separator + CONTENTSPEC_TITLE + File.separator);

        // When processing the command
        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> outputPath = ArgumentCaptor.forClass(String.class);
        command.process();

        // Then verify that the filename and path are valid
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.saveOutputFile(eq(command), fileName.capture(), outputPath.capture(), anyString());
        assertThat(fileName.getValue(), is(CONTENTSPEC_TITLE + "-snapshot-" + dateFormatter.format(date) + ".contentspec"));
        assertThat(outputPath.getValue(), is(rootTestDirectory.getAbsolutePath() + File.separator + CONTENTSPEC_TITLE + File.separator +
                "snapshots" + File.separator));
    }

    @Test
    public void shouldShutdownWhenSetContentSpecRevisionsFails() {
        // Given processing the spec will fail
        given(command.getProcessor()).willReturn(processor);
        given(processor.processContentSpec(any(ContentSpec.class), any(UserWrapper.class),
                any(ContentSpecParser.ParsingMode.class))).willReturn(false);

        // When setting the content spec topic revisions
        try {
            command.setRevisionsForContentSpec(contentSpec, user);
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(8));
        }

        // Then the command should shutdown and an error message should be printed
        assertThat(getStdOutLogs(),
                containsString("The revision of the Content Specification is invalid and as such the snapshot couldn't be pulled."));
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

        // Then the name should be "pull-snapshot"
        assertThat(commandName, is("pull-snapshot"));
    }
}
