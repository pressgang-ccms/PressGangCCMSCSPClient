package org.jboss.pressgang.ccms.contentspec.client.commands;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.internal.Console;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.common.ZipUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, FileUtilities.class, ZipUtilities.class, ClientUtilities.class})
public class AssembleCommandTest extends BaseUnitTest {
    private static final String BOOK_TITLE = "Test";
    private static final String DUMMY_BUILD_FILE_NAME = "Test.zip";

    @Rule public PowerMockRule rule = new PowerMockRule();
    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock RESTProviderFactory providerFactory;
    @Mock ContentSpecProvider contentSpecProvider;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;

    AssembleCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdOut();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        command = spy(new AssembleCommand(parser, cspConfig, clientConfig));

        // Only test the assemble command and not the build command content.
        command.setNoBuild(true);

        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);
    }

    @Test
    public void shouldFailWithNoIds() {
        // Given a command with no ids
        command.setIds(new ArrayList<String>());
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
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
    }

    @Test
    public void shouldShutdownWhenBuildFileDoesntExist() {
        // Given a command with ids
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // and the build file information has been set
        command.setBuildFileDirectory(rootTestDirectory.getAbsolutePath());
        command.setBuildFileName(DUMMY_BUILD_FILE_NAME);
        doNothing().when(command).findBuildDirectoryAndFiles(any(ContentSpecProvider.class), anyBoolean());

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("Unable to assemble the Content Specification because the \"" + DUMMY_BUILD_FILE_NAME +
                "\" file couldn't be found."));
    }

    @Test
    public void shouldShutdownWhenUnableToUnzipBuild() {
        // Given a command with ids
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // and the build file information has been set
        command.setBuildFileDirectory(rootTestDirectory.getAbsolutePath());
        command.setBuildFileName("EmptyFile.txt");
        command.setOutputDirectory(rootTestDirectory.getAbsolutePath() + File.separator + BOOK_TITLE);
        doNothing().when(command).findBuildDirectoryAndFiles(any(ContentSpecProvider.class), anyBoolean());
        // and the unzip fails
        PowerMockito.mockStatic(ZipUtilities.class);
        when(ZipUtilities.unzipFileIntoDirectory(any(File.class), anyString())).thenReturn(false);

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("The content specification failed to be assembled."));
    }

    @Test
    public void shouldOnlyPrintSuccessWhenUnzipBuildAndNoPublicanBuild() {
        final String rootPath = rootTestDirectory.getAbsolutePath();
        // Given a command with ids
        command.setIds(Arrays.asList(rootPath));
        // and the build file information has been set
        command.setBuildFileDirectory(rootPath);
        command.setBuildFileName("EmptyFile.txt");
        command.setOutputDirectory(rootPath + File.separator + BOOK_TITLE);
        doNothing().when(command).findBuildDirectoryAndFiles(any(ContentSpecProvider.class), anyBoolean());
        // and no publican build
        command.setNoPublicanBuild(true);
        // and the unzip succeeds
        PowerMockito.mockStatic(ZipUtilities.class);
        when(ZipUtilities.unzipFileIntoDirectory(any(File.class), anyString())).thenReturn(true);

        // When processing the command
        command.process();

        // Then the command printed a success message and the runPublican method wasn't executed
        verify(command, times(0)).runPublican(any(File.class));
        assertThat(getStdOutLogs(), containsString("Content Specification build unzipped to " + rootPath + File.separator + BOOK_TITLE));
    }

    @Test
    public void shouldPrintSuccessAndRunPublican() {
        final String rootPath = rootTestDirectory.getAbsolutePath();
        // Given a command with ids
        command.setIds(Arrays.asList(rootPath));
        // and the build file information has been set
        command.setBuildFileDirectory(rootPath);
        command.setBuildFileName("EmptyFile.txt");
        command.setOutputDirectory(rootPath + File.separator + BOOK_TITLE);
        doNothing().when(command).findBuildDirectoryAndFiles(any(ContentSpecProvider.class), anyBoolean());
        // and the unzip succeeds
        PowerMockito.mockStatic(ZipUtilities.class);
        when(ZipUtilities.unzipFileIntoDirectory(any(File.class), anyString())).thenReturn(true);
        // and don't actually run the runPublican method
        doNothing().when(command).runPublican(any(File.class));

        // When processing the command
        command.process();

        // Then the command printed a success message and the runPublican method wasn't executed
        verify(command, times(1)).runPublican(any(File.class));
        assertThat(getStdOutLogs(), containsString("Content Specification build unzipped to " + rootPath + File.separator + BOOK_TITLE));
        assertThat(getStdOutLogs(),
                containsString("Content Specification successfully assembled at " + rootPath + File.separator + BOOK_TITLE));
    }

    @Test
    public void shouldShutdownWhenContentSpecNotFound() {
        // Given a command called with an an ID
        command.setIds(Arrays.asList(id.toString()));
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(null);

        // When finding files
        try {
            command.findBuildDirectoryAndFiles(contentSpecProvider, false);
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldShutdownWhenContentSpecFileNotFound() {
        // Given a command called with a file
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // And no file
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(null);

        // When it finding files
        try {
            command.findBuildDirectoryAndFiles(contentSpecProvider, false);
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("The specified file was empty!"));
    }

    @Test
    public void shouldSetBuildFileLocationsWhenLoadingFromCsprocessorCfg() {
        // Given a command with no ids
        command.setIds(new ArrayList<String>());
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and the provider will return a wrapper
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        // and the wrapper returns a title and id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);

        // When the command is finding the files
        command.findBuildDirectoryAndFiles(contentSpecProvider, true);

        // Then the command should have the build information set
        assertThat(command.getBuildFileDirectory(), containsString("assembly" + File.separator));
        assertThat(command.getOutputDirectory(), containsString("assembly" + File.separator + "publican" + File.separator));
        assertThat(command.getBuildFileName(), containsString(BOOK_TITLE + "-publican.zip"));
    }

    @Test
    public void shouldSetBuildFileLocationsWhenFileSpecifiedAndNoOutputPath() {
        // Given a command with a file
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // and the file will be found
        doReturn("").when(command).getContentSpecFromFile(anyString());
        // and the content spec parses
        doReturn(contentSpec).when(command).parseContentSpec(any(RESTProviderFactory.class), anyString(), anyBoolean());
        // and the content spec returns a title and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);

        // When the command is finding the files
        command.findBuildDirectoryAndFiles(contentSpecProvider, false);

        // Then the command should have the build information set
        assertThat(command.getBuildFileDirectory(), is(""));
        assertThat(command.getOutputDirectory(), is(BOOK_TITLE));
        assertThat(command.getBuildFileName(), containsString(BOOK_TITLE + ".zip"));
    }

    @Test
    public void shouldSetBuildFileLocationsWhenFileSpecifiedAndOutputDirectory() {
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        // Given a command with ids
        command.setIds(Arrays.asList(rootPath));
        // and the file will be found
        doReturn("").when(command).getContentSpecFromFile(anyString());
        // and the output path is a directory
        command.setOutputPath(rootPath);
        // and the content spec parses
        doReturn(contentSpec).when(command).parseContentSpec(any(RESTProviderFactory.class), anyString(), anyBoolean());
        // and the content spec returns a title and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);

        // When the command is finding the files
        command.findBuildDirectoryAndFiles(contentSpecProvider, false);

        // Then the command should have the build information set
        assertThat(command.getBuildFileDirectory(), is(rootPath));
        assertThat(command.getOutputDirectory(), is(rootPath + BOOK_TITLE));
        assertThat(command.getBuildFileName(), containsString(BOOK_TITLE + ".zip"));
    }

    @Test
    public void shouldSetBuildFileLocationsWhenFileSpecifiedAndOutputFile() {
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        // Given a command with ids
        command.setIds(Arrays.asList(rootPath));
        // and the file will be found
        doReturn("").when(command).getContentSpecFromFile(anyString());
        // and the output path is a directory
        command.setOutputPath(rootPath + DUMMY_BUILD_FILE_NAME);
        // and the content spec parses
        doReturn(contentSpec).when(command).parseContentSpec(any(RESTProviderFactory.class), anyString(), anyBoolean());
        // and the content spec returns a title and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);

        // When the command is finding the files
        command.findBuildDirectoryAndFiles(contentSpecProvider, false);

        // Then the command should have the build information set
        assertThat(command.getBuildFileDirectory(), is(""));
        assertThat(command.getOutputDirectory(), is(rootPath + BOOK_TITLE));
        assertThat(command.getBuildFileName(), containsString(BOOK_TITLE + ".zip"));
    }

    @Test
    public void shouldShutdownWhenRunPublicanFails() throws IOException {
        int exitCode = 1;
        // Given publican build options
        clientConfig.setPublicanBuildOptions("--formats html-single --langs en-US");
        // and the publican command fails with an exit code other than 0
        PowerMockito.mockStatic(ClientUtilities.class);
        when(ClientUtilities.runCommand(anyString(), any(File.class), any(Console.class), anyBoolean(), anyBoolean())).thenReturn(exitCode);

        // When running publican
        try {
            command.runPublican(rootTestDirectory);
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should shutdown and an error be printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdOutLogs(), containsString("Unable to assemble the Content Specification because an error " +
                "occurred while running Publican. (exit code: " + exitCode + ")"));
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
        command.setIds(Arrays.asList(id.toString()));

        // When invoking the method
        boolean result = command.loadFromCSProcessorCfg();

        // Then the result should be false
        assertFalse(result);
    }
}
