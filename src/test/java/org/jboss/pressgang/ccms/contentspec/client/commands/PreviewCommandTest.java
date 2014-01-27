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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import net.sf.ipsedixit.annotation.Arbitrary;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.CSNodeWrapper;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.UpdateableCollectionWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.internal.CheckExitCalled;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({FileUtilities.class, ClientUtilities.class})
public class PreviewCommandTest extends BaseCommandTest {
    private static final String BOOK_TITLE = "Test";
    private static final String DUMMY_BUILD_FILE_NAME = "Test.zip";

    @Rule public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Arbitrary Integer id;
    @Arbitrary String randomString;

    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock ContentSpec contentSpec;
    @Mock UpdateableCollectionWrapper<CSNodeWrapper> contentSpecChildren;

    PreviewCommand command;
    File rootTestDirectory;
    File bookDir;
    File previewFile;
    File htmlSingleDir;

    @Before
    public void setUp() throws IOException {
        bindStdOut();
        command = spy(new PreviewCommand(parser, cspConfig, clientConfig));

        // Only test the preview command and not the build or assemble command content.
        command.setNoAssemble(true);

        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);

        // Make the book title directory
        bookDir = new File(rootTestDirectory, BOOK_TITLE);
        bookDir.mkdir();

        // Make the preview dummy file
        htmlSingleDir = new File(
                bookDir.getAbsolutePath() + File.separator + "tmp" + File.separator + "en-US" + File.separator + "html-single");
        htmlSingleDir.mkdirs();
        previewFile = new File(htmlSingleDir, "index.html");
        previewFile.createNewFile();
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(bookDir);
    }

    @Test
    public void shouldFailWithNoIds() {
        // Given a command with no ids
        command.setIds(new ArrayList<String>());
        // and no csprocessor.cfg data
        given(cspConfig.getContentSpecId()).willReturn(null);
        // and the command has a valid preview format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("html-single");

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
        assertThat(getStdOutLogs(), containsString("\"blah\" is not currently supported as a preview format."));
    }

    @Test
    public void shouldFailWhenNoValidContentSpecs() {
        // Given a command with ids
        command.setIds(Arrays.asList(id.toString()));
        // and a valid preview format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("html");
        // and the provider will return a wrapper, that has no children
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecWrapper.getChildren()).willReturn(contentSpecChildren);
        given(contentSpecChildren.isEmpty()).willReturn(true);

        // When the command is processing
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("No valid version exists on the server, please fix any errors and try again."));
    }

    @Test
    public void shouldShutdownWhenPreviewFileDoesntExist() {
        final String rootPath = rootTestDirectory.getAbsolutePath();
        // Given a command with ids
        command.setIds(Arrays.asList(id.toString()));
        // and a valid preview format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("html");
        // and the output path is set
        command.setOutputPath(rootTestDirectory.getAbsolutePath() + File.separator);
        // and the provider will return a wrapper
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecWrapper.getChildren()).willReturn(contentSpecChildren);
        given(contentSpecChildren.isEmpty()).willReturn(false);
        // and the wrapper returns a title, product, version and id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getVersion()).willReturn("1");
        given(contentSpecWrapper.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);

        // When processing the command
        try {
            command.process();
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString(
                "Unable to preview the Content Specification because the \"" + previewFile.getAbsolutePath().replace("-single",
                        "") + "\" file couldn't be found"));
    }

    @Test
    public void shouldShutdownWhenUnableToOpenFile() throws Exception {
        // Given a command with ids
        command.setIds(Arrays.asList(id.toString()));
        // and a valid preview format
        given(clientConfig.getPublicanPreviewFormat()).willReturn("html-single");
        // and the output path is set
        command.setOutputPath(rootTestDirectory.getAbsolutePath() + File.separator);
        // and the provider will return a wrapper
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecWrapper.getChildren()).willReturn(contentSpecChildren);
        given(contentSpecChildren.size()).willReturn(1);
        // and the wrapper returns a title, product, version and id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getVersion()).willReturn("1");
        given(contentSpecWrapper.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);
        // and the File won't open
        PowerMockito.mockStatic(FileUtilities.class);
        PowerMockito.doThrow(new IOException()).when(FileUtilities.class);
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
        assertThat(getStdOutLogs(), containsString("Unable to open the \"" + previewFile.getAbsolutePath() + "\" file."));
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
        assertThat(getStdOutLogs(), containsString("No data was found for the specified ID!"));
    }

    @Test
    public void shouldShutdownWhenContentSpecFileNotFound() {
        // Given a command called with a file
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // And no file
        PowerMockito.mockStatic(FileUtilities.class);
        when(FileUtilities.readFileContents(any(File.class))).thenReturn("");

        // When it finding files
        try {
            command.findFileToPreview(contentSpecProvider, false, "html-single");
            // Then an error is printed and the program is shut down
            fail(SYSTEM_EXIT_ERROR);
        } catch (CheckExitCalled e) {
            assertThat(e.getStatus(), is(-1));
        }

        // Then the command should be shutdown and an error message printed
        assertThat(getStdOutLogs(), containsString("The specified file was empty!"));
    }

    @Test
    public void shouldGetPreviewFileWhenLoadingFromCsprocessorCfg() {
        boolean previewFromConfig = true;
        // Given a command with no ids
        command.setIds(new ArrayList<String>());
        given(cspConfig.getContentSpecId()).willReturn(id);
        // and the provider will return a wrapper
        given(contentSpecProvider.getContentSpec(anyInt(), anyInt())).willReturn(contentSpecWrapper);
        given(contentSpecWrapper.getChildren()).willReturn(contentSpecChildren);
        given(contentSpecChildren.size()).willReturn(1);
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
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.mockStatic(FileUtilities.class);
        boolean previewFromConfig = false;
        // Given a command with a file
        command.setIds(Arrays.asList(rootTestDirectory.getAbsolutePath()));
        // and the content spec file will be found
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec parses
        when(ClientUtilities.parseContentSpecString(eq(providerFactory), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        // and the content spec returns a title, product, version and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getVersion()).willReturn("1");
        given(contentSpec.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);
        // and the fix file path method returns something
        when(ClientUtilities.fixFilePath(anyString())).thenCallRealMethod();
        when(ClientUtilities.fixDirectoryPath(anyString())).thenCallRealMethod();

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
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.mockStatic(FileUtilities.class);
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        boolean previewFromConfig = false;
        // Given a command with a file
        command.setIds(Arrays.asList(rootPath));
        // and the content spec file will be found
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec parses
        when(ClientUtilities.parseContentSpecString(eq(providerFactory), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        // and the output path is a directory
        command.setOutputPath(rootPath);
        // and the content spec returns a title, product, version and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getVersion()).willReturn("1");
        given(contentSpec.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);
        // and the fix file path method returns something
        when(ClientUtilities.fixFilePath(anyString())).thenCallRealMethod();
        when(ClientUtilities.fixDirectoryPath(anyString())).thenCallRealMethod();

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
        PowerMockito.mockStatic(ClientUtilities.class);
        PowerMockito.mockStatic(FileUtilities.class);
        final String rootPath = rootTestDirectory.getAbsolutePath() + File.separator;
        boolean previewFromConfig = false;
        // Given a command with a file
        command.setIds(Arrays.asList(rootPath));
        // and the content spec file will be found
        when(FileUtilities.readFileContents(any(File.class))).thenReturn(randomString);
        // and the content spec parses
        when(ClientUtilities.parseContentSpecString(eq(providerFactory), any(ErrorLoggerManager.class), anyString(),
                any(ContentSpecParser.ParsingMode.class), anyBoolean())).thenReturn(contentSpec);
        // and the output path is a directory
        command.setOutputPath(rootPath + DUMMY_BUILD_FILE_NAME);
        // and the content spec returns a title, product, version and id
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpec.getVersion()).willReturn("1");
        given(contentSpec.getProduct()).willReturn(BOOK_TITLE);
        given(contentSpec.getId()).willReturn(id);
        // and the fix file path method returns something
        when(ClientUtilities.fixFilePath(anyString())).thenCallRealMethod();
        when(ClientUtilities.fixDirectoryPath(anyString())).thenCallRealMethod();

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

    @Test
    public void shouldReturnRightCommandName() {
        // Given
        // When getting the commands name
        String commandName = command.getCommandName();

        // Then the name should be "preview"
        assertThat(commandName, is("preview"));
    }
}