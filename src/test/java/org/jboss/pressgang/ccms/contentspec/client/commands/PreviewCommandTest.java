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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.contentspec.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest({RESTProviderFactory.class, FileUtilities.class})
public class PreviewCommandTest extends BaseUnitTest {
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

    PreviewCommand command;
    File rootTestDirectory;

    @Before
    public void setUp() {
        bindStdout();
        PowerMockito.mockStatic(RESTProviderFactory.class);
        when(RESTProviderFactory.create(anyString())).thenReturn(providerFactory);
        when(providerFactory.getProvider(ContentSpecProvider.class)).thenReturn(contentSpecProvider);
        command = spy(new PreviewCommand(parser, cspConfig, clientConfig));

        // Only test the preview command and not the build or assemble command content.
        command.setNoAssemble(true);

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
        assertThat(getStdoutLogs(), containsString("No ID was specified by the command line or a csprocessor.cfg file."));
    }

    @Test
    public void shouldShutdownWhenPreviewFormatIsNotValid() {
        // Given a command with an invalid format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("blah");
        // and some id
        command.setIds(Arrays.asList(id.toString()));

        // When the command is processing
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(4));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("\"blah\" is not currently supported as a preview format."));
    }

    @Test
    public void shouldShutdownWhenPreviewFileDoesntExist() {
        final String rootPath = rootTestDirectory.getAbsolutePath();
        // Given a command with ids
        command.setIds(Arrays.asList(id.toString()));
        // and a valid preview format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("html-single");
        // and the find preview file returns an invalid file path
        doReturn(rootPath).when(command).findFileToPreview(any(ContentSpecProvider.class), anyBoolean(), anyString());

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
        assertThat(getStdoutLogs(),
                containsString("Unable to preview the Content Specification because the \"" + rootPath + "\" file couldn't be found"));
    }

    @Test
    public void shouldShutdownWhenUnableToOpenFile() throws Exception {
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        // Given a command with ids
        command.setIds(Arrays.asList(id.toString()));
        // and a valid preview format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("html-single");
        // and the find preview file returns a valid
        doReturn(rootPath + "EmptyFile.txt").when(command).findFileToPreview(any(ContentSpecProvider.class), anyBoolean(), anyString());
        // and the File won't open
        PowerMockito.mockStatic(FileUtilities.class);
        PowerMockito.doThrow(new Exception()).when(FileUtilities.class);
        FileUtilities.openFile(any(File.class));

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        PowerMockito.verifyStatic(times(1));
        FileUtilities.openFile(any(File.class));
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("Unable to open the \"" + rootPath + "EmptyFile.txt\" file."));
    }

    @Test
    public void shouldShutdownWhenContentSpecNotFound() {
        // Given a command called with an an ID
        command.setIds(Arrays.asList(id.toString()));
        // And no matching content spec
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(null);

        // When finding files
        try {
            command.findFileToPreview(contentSpecProvider, false, "html-single");
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("No data was found for the specified ID!"));
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
            command.findFileToPreview(contentSpecProvider, false, "html-single");
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        verify(command, times(1)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertThat(getStdoutLogs(), containsString("The specified file was empty!"));
    }

    @Test
    public void shouldGetPreviewFileWhenLoadingFromCsprocessorCfg() {
        boolean previewFromConfig = true;
        // Given a command with no ids
        command.setIds(new ArrayList<String>());
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and the provider will return a wrapper
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        // and the wrapper returns a title, product, version and id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getVersion()).willReturn("1");
        given(contentSpecWrapper.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);

        // When the command is finding the files
        final String previewHtmlSingleFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html-single");
        final String previewHtmlFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html");
        final String previewPdfFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "pdf");

        // Then the output should be in a CSP Project directory
        assertThat(previewHtmlSingleFile, containsString(BOOK_TITLE + "/assembly/publican/tmp/en-US/html-single/index.html"));
        assertThat(previewHtmlFile, containsString(BOOK_TITLE + "/assembly/publican/tmp/en-US/html/index.html"));
        assertThat(previewPdfFile, containsString(BOOK_TITLE + "/assembly/publican/tmp/en-US/pdf/" + BOOK_TITLE + "-1-" + BOOK_TITLE +
                "-en-US.pdf"));
    }

    @Test
    public void shouldGetPreviewFileWhenFileSpecifiedAndNoOutputPath() {
        boolean previewFromConfig = false;
        // Given a command with a file
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // and the file will be found
        doReturn("").when(command).getContentSpecFromFile(anyString());
        // and the content spec parses
        doReturn(contentSpec).when(command).parseContentSpec(any(RESTProviderFactory.class), anyString(), anyBoolean());
        // and the content spec returns a title, product, version and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getVersion()).willReturn("1");
        given(contentSpec.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);

        // When the command is finding the files
        final String previewHtmlSingleFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html-single");
        final String previewHtmlFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html");
        final String previewPdfFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "pdf");

        // Then the output should be in a the book publican directory
        assertThat(previewHtmlSingleFile, is(BOOK_TITLE + "/tmp/en-US/html-single/index.html"));
        assertThat(previewHtmlFile, is(BOOK_TITLE + "/tmp/en-US/html/index.html"));
        assertThat(previewPdfFile, is(BOOK_TITLE + "/tmp/en-US/pdf/" + BOOK_TITLE + "-1-" + BOOK_TITLE + "-en-US.pdf"));
    }

    @Test
    public void shouldGetPreviewFileWhenFileSpecifiedAndOutputDirectory() {
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        boolean previewFromConfig = false;
        // Given a command with a file
        command.setIds(Arrays.asList(rootPath));
        // and the file will be found
        doReturn("").when(command).getContentSpecFromFile(anyString());
        // and the output path is a directory
        command.setOutputPath(rootPath);
        // and the content spec parses
        doReturn(contentSpec).when(command).parseContentSpec(any(RESTProviderFactory.class), anyString(), anyBoolean());
        // and the content spec returns a title, product, version and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getVersion()).willReturn("1");
        given(contentSpec.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);

        // When the command is finding the files
        final String previewHtmlSingleFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html-single");
        final String previewHtmlFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html");
        final String previewPdfFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "pdf");

        // Then the output should be in a the book publican directory
        assertThat(previewHtmlSingleFile, is(rootPath + BOOK_TITLE + "/tmp/en-US/html-single/index.html"));
        assertThat(previewHtmlFile, is(rootPath + BOOK_TITLE + "/tmp/en-US/html/index.html"));
        assertThat(previewPdfFile, is(rootPath + BOOK_TITLE + "/tmp/en-US/pdf/" + BOOK_TITLE + "-1-" + BOOK_TITLE + "-en-US.pdf"));
    }

    @Test
    public void shouldGetPreviewFileWhenFileSpecifiedAndOutputFile() {
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        boolean previewFromConfig = false;
        // Given a command with a file
        command.setIds(Arrays.asList(rootPath));
        // and the file will be found
        doReturn("").when(command).getContentSpecFromFile(anyString());
        // and the output path is a directory
        command.setOutputPath(rootPath + DUMMY_BUILD_FILE_NAME);
        // and the content spec parses
        doReturn(contentSpec).when(command).parseContentSpec(any(RESTProviderFactory.class), anyString(), anyBoolean());
        // and the content spec returns a title, product, version and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getVersion()).willReturn("1");
        given(contentSpec.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);

        // When the command is finding the files
        final String previewHtmlSingleFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html-single");
        final String previewHtmlFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "html");
        final String previewPdfFile = command.findFileToPreview(contentSpecProvider, previewFromConfig, "pdf");

        // Then the output should be in a the book publican directory
        assertThat(previewHtmlSingleFile, is(rootPath + BOOK_TITLE + "/tmp/en-US/html-single/index.html"));
        assertThat(previewHtmlFile, is(rootPath + BOOK_TITLE + "/tmp/en-US/html/index.html"));
        assertThat(previewPdfFile, is(rootPath + BOOK_TITLE + "/tmp/en-US/pdf/" + BOOK_TITLE + "-1-" + BOOK_TITLE + "-en-US.pdf"));
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