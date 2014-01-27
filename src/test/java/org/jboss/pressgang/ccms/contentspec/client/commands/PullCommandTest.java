package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.base.BaseContentSpecWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest(ClientUtilities.class)
public class PullCommandTest extends BaseCommandTest {
    private static final String CONTENTSPEC_TITLE = "ContentSpec";
    private static final String TOPIC_TITLE = "Topic";

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary Integer revision;
    @Arbitrary String randomString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock TopicWrapper topicWrapper;

    PullCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        command = new PullCommand(parser, cspConfig, clientConfig);

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
    public void shouldPrintErrorAndShutdownWhenTopicNotFound() {
        // Given a command called with an id
        command.setIds(Arrays.asList(id));
        // and we are looking up a topic
        command.setTopic(true);
        // And no matching topic
        given(topicProvider.getTopic(anyInt(), anyInt())).willReturn(null);

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
    public void shouldGenerateRightFilenameAndPathForContentSpec() {
        PowerMockito.mockStatic(ClientUtilities.class);
        // Given a command called with an ID
        command.setIds(Arrays.asList(id));
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(id, null)).willReturn(randomString);
        // and the content spec title/id is set
        given(contentSpecWrapper.getTitle()).willReturn(CONTENTSPEC_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);
        // and a output file is specified
        command.setOutputPath(rootTestDirectory.getAbsolutePath());
        // And we don't actually want to save anything
        PowerMockito.doNothing().when(ClientUtilities.class);
        ClientUtilities.saveOutputFile(eq(command), anyString(), anyString(), anyString());
        // and the helper method to get the content spec works
        TestUtil.setUpContentSpecHelper(contentSpecProvider);
        when(ClientUtilities.getEscapedContentSpecTitle(eq(providerFactory), any(BaseContentSpecWrapper.class))).thenCallRealMethod();

        // When processing the command
        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> outputPath = ArgumentCaptor.forClass(String.class);
        command.process();

        // Then verify that the filename and path are valid
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.saveOutputFile(eq(command), fileName.capture(), outputPath.capture(), anyString());
        assertThat(fileName.getValue(), is(CONTENTSPEC_TITLE + "-post.contentspec"));
        assertThat(outputPath.getValue(), is(rootTestDirectory.getAbsolutePath()));
    }

    @Test
    public void shouldGenerateRightFilenameAndPathForContentSpecPullingFromConfig() {
        PowerMockito.mockStatic(ClientUtilities.class);
        // Given a command called without an ID
        command.setIds(new ArrayList<Integer>());
        // and the cspconfig has an id
        given(cspConfig.getContentSpecId()).willReturn(id);
        // And a matching content spec
        given(contentSpecProvider.getContentSpec(id, null)).willReturn(contentSpecWrapper);
        given(contentSpecProvider.getContentSpecAsString(id, null)).willReturn(randomString);
        // and the content spec title/id is set
        given(contentSpecWrapper.getTitle()).willReturn(CONTENTSPEC_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);
        // And we don't actually want to save anything
        PowerMockito.doNothing().when(ClientUtilities.class);
        ClientUtilities.saveOutputFile(eq(command), anyString(), anyString(), anyString());
        // and call some real methods
        when(ClientUtilities.prepareAndValidateIds(any(BaseCommandImpl.class), eq(cspConfig), anyList())).thenCallRealMethod();
        when(ClientUtilities.prepareIds(any(BaseCommandImpl.class), eq(cspConfig), anyList())).thenCallRealMethod();
        when(ClientUtilities.getOutputRootDirectory(eq(providerFactory), eq(cspConfig), eq(contentSpecWrapper))).thenCallRealMethod();
        when(ClientUtilities.getOutputRootDirectoryFromEscapedTitle(eq(cspConfig), anyString())).thenCallRealMethod();
        // and the helper method to get the content spec works
        TestUtil.setUpContentSpecHelper(contentSpecProvider);
        when(ClientUtilities.getEscapedContentSpecTitle(eq(providerFactory), any(BaseContentSpecWrapper.class))).thenCallRealMethod();

        // When processing the command
        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> outputPath = ArgumentCaptor.forClass(String.class);
        command.process();

        // Then verify that the filename and path are valid
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.saveOutputFile(eq(command), fileName.capture(), outputPath.capture(), anyString());
        assertThat(fileName.getValue(), is(CONTENTSPEC_TITLE + "-post.contentspec"));
        assertThat(outputPath.getValue(), is(rootTestDirectory.getAbsolutePath() + File.separator + CONTENTSPEC_TITLE + File.separator));
    }

    @Test
    public void shouldGenerateRightFilenameAndPathForTopicXML() {
        // Given a command called with an ID
        command.setIds(Arrays.asList(id));
        // and pull topic xml
        command.setTopic(true);
        // And a topic exists
        given(topicProvider.getTopic(anyInt(), anyInt())).willReturn(topicWrapper);
        // and the topic title/id/xml is set
        given(topicWrapper.getTitle()).willReturn(TOPIC_TITLE);
        given(topicWrapper.getId()).willReturn(id);
        given(topicWrapper.getXml()).willReturn(randomString);
        // and a output file is specified
        command.setOutputPath(rootTestDirectory.getAbsolutePath());
        // And we don't actually want to save anything
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.doNothing().when(ClientUtilities.class);
        ClientUtilities.saveOutputFile(eq(command), anyString(), anyString(), anyString());
        when(ClientUtilities.getTopicEntity(eq(topicProvider), anyInt(), anyInt())).thenCallRealMethod();

        // When processing the command
        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> outputPath = ArgumentCaptor.forClass(String.class);
        command.process();

        // Then verify that the filename and path are valid
        PowerMockito.verifyStatic(times(1));
        ClientUtilities.saveOutputFile(eq(command), fileName.capture(), outputPath.capture(), anyString());
        assertThat(fileName.getValue(), is(TOPIC_TITLE + ".xml"));
        assertThat(outputPath.getValue(), is(rootTestDirectory.getAbsolutePath()));
    }

    @Test
    public void shouldReturnFalseWhenPullingContentSpecAndTopic() {
        // Given a command with invalid arguments
        command.setContentSpec(true);
        command.setTopic(true);

        // When the command is validating
        boolean valid = command.isValid();

        // Then the command shouldn't be valid
        assertFalse(valid);
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

        // Then the name should be "pull"
        assertThat(commandName, is("pull"));
    }
}
