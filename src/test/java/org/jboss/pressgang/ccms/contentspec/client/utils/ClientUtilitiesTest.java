package org.jboss.pressgang.ccms.contentspec.client.utils;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryNumber;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest(FileUtilities.class)
public class ClientUtilitiesTest extends BaseUnitTest {
    private static final String BOOK_TITLE = "Test";
    private static final String EMPTY_FILE_NAME = "empty.txt";

    @Rule public PowerMockRule rule = new PowerMockRule();

    @Arbitrary Integer id;
    @Arbitrary Integer secondId;
    @ArbitraryNumber Integer checksum;
    @Arbitrary String randomString;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock ContentSpec contentSpec;
    @Mock ErrorLoggerManager elm;
    @Mock BaseCommandImpl command;
    @Mock UserWrapper user;
    @Mock CSTransformer csTransformer;
    @Mock ContentSpecWrapper contentSpecWrapper;

    File rootTestDirectory;
    File bookDir;
    File emptyFile;

    @Before
    public void setUp() throws IOException {
        doCallRealMethod().when(command).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());

        // Return the test directory as the root directory
        rootTestDirectory = FileUtils.toFile(ClassLoader.getSystemResource(""));
        when(cspConfig.getRootOutputDirectory()).thenReturn(rootTestDirectory.getAbsolutePath() + File.separator);

        // Make the book title directory
        bookDir = new File(rootTestDirectory, BOOK_TITLE);
        bookDir.mkdir();

        // Make a empty file in that directory
        emptyFile = new File(bookDir, EMPTY_FILE_NAME);
        emptyFile.createNewFile();
    }

    @Test
    public void shouldAppendTrailingSlashToDirectoryLocation() {
        // Given a string that represents a directory
        String dir = "/example/dir";

        // When validating the directory location
        String fixedDir = ClientUtilities.fixDirectoryPath(dir);

        // Then verify that the directory ends with a slash
        assertTrue(fixedDir.endsWith(File.separator));
    }

    @Test
    public void shouldReplaceTildeInFilePath() {
        // Given a string that represents a directory
        String dir = "~/example/dir/";

        // When fixing the file path
        String fixedDir = ClientUtilities.fixFilePath(dir);

        // Then verify that the paths tilde is replaced by the users home directory
        assertThat(fixedDir, is(System.getProperty("user.home") + "/example/dir/"));
    }

    @Test
    public void shouldFixRelativePathInFilePath() {
        final File currentDirectory = new File(System.getProperty("user.dir"));
        // Given a string that represents a directory
        String dir = "./example/dir/";
        String secondDir = "../example/dir/";

        // When fixing the file path
        String fixedDir = ClientUtilities.fixFilePath(dir);
        String secondFixedDir = ClientUtilities.fixFilePath(secondDir);

        // Then verify that the file path has the relative url's replaced with absolute paths
        assertThat(fixedDir, is(currentDirectory.getAbsolutePath() + "/example/dir/"));
        final String currentParentDirectory = currentDirectory.getParentFile().getAbsolutePath();
        assertThat(secondFixedDir, is(currentParentDirectory + "/example/dir/"));
    }

    @Test
    public void shouldAppendTrailingSlashToHostURL() {
        // Given a string that represents a URL with no trailing slash
        String url = "https://www.example.com";

        // When fixing a host url
        String fixedUrl = ClientUtilities.fixHostURL(url);

        // Then verify that the url now ends with a slash
        assertThat(fixedUrl, is("https://www.example.com/"));
    }

    @Test
    public void shouldPrefixHttpProtocolToHostURL() {
        // Given a string that represents a URL with no HTTP protocol
        String url = "www.example.com";

        // When fixing a host url
        String fixedUrl = ClientUtilities.fixHostURL(url);

        // Then verify that the url now has the protocol included
        assertThat(fixedUrl, is("http://www.example.com/"));
    }

    @Test
    public void shouldPrintErrorAndShutdownOnProcessWhenNoId() {
        // Given a command called with no ID
        List<Integer> ids = Collections.EMPTY_LIST;
        // And that there is no ID in the CS Processor config
        given(cspConfig.getContentSpecId()).willReturn(null);
        // And we aren't loading from the csprocessor.cfg
        given(command.loadFromCSProcessorCfg()).willReturn(false);

        // When it is processed
        ArgumentCaptor<String> errorMessage = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> exitStatus = ArgumentCaptor.forClass(Integer.class);
        boolean retValue = ClientUtilities.prepareAndValidateIds(command, cspConfig, ids);

        // Then an error is printed and the program is shut down
        verify(command, times(1)).printErrorAndShutdown(exitStatus.capture(), errorMessage.capture(), anyBoolean());
        assertThat(errorMessage.getValue(), is("No ID was specified by the command line or a csprocessor.cfg file."));
        assertThat(exitStatus.getValue(), is(5));
        assertFalse(retValue);
    }

    @Test
    public void shouldPrintErrorAndShutdownOnProcessWhenMultipleIds() {
        // Given a command called with multiple ID's
        List<Integer> ids = Arrays.asList(id, secondId);
        // And that there is no ID in the CS Processor config
        given(cspConfig.getContentSpecId()).willReturn(null);
        // And we aren't loading from the csprocessor.cfg
        given(command.loadFromCSProcessorCfg()).willReturn(false);

        // When it is processed
        ArgumentCaptor<String> errorMessage = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> exitStatus = ArgumentCaptor.forClass(Integer.class);
        boolean retValue = ClientUtilities.prepareAndValidateIds(command, cspConfig, ids);

        // Then an error is printed and the program is shut down
        verify(command, times(1)).printErrorAndShutdown(exitStatus.capture(), errorMessage.capture(), anyBoolean());
        assertThat(errorMessage.getValue(), is("Multiple ID's specified. Please only specify one ID."));
        assertThat(exitStatus.getValue(), is(5));
        assertFalse(retValue);
    }

    @Test
    public void shouldLoadIdFromFileOnProcessWhenNoIdSupplied() {
        // Given a command called with no ID
        List<Integer> ids = new ArrayList<Integer>();
        // And that there is an ID in the CS Processor config
        given(cspConfig.getContentSpecId()).willReturn(id);
        // And we are loading from the csprocessor.cfg
        given(command.loadFromCSProcessorCfg()).willReturn(true);

        // When it is processed
        boolean retValue = ClientUtilities.prepareAndValidateIds(command, cspConfig, ids);

        // Then the command should complete without error
        verify(command, times(0)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertTrue(retValue);
    }

    @Test
    public void shouldReturnRightChecksum() {
        // Given the ContentSpec.toString() method will return a string with a checksum in it
        given(contentSpec.toString()).willReturn("CHECKSUM = " + checksum + "\nTitle = Test Spec\n");

        // When the method is invoked
        String contentSpecChecksumString = ContentSpecUtilities.getContentSpecChecksum(contentSpec);

        // Then the output of the getContentSpecChecksum command should match the checksum
        assertThat(contentSpecChecksumString, is(checksum.toString()));
    }

    @Test
    public void shouldSaveSpecAndConfig() {
        // Given the title of the book and an id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);

        // When the command is processing
        ClientUtilities.createContentSpecProject(command, cspConfig, bookDir, randomString, contentSpecWrapper, new ZanataDetails());

        // Then check that only the csprocessor.cfg and <TITLE>-post.contentspec exists
        assertArrayEquals(bookDir.list(), new String[]{"csprocessor.cfg", BOOK_TITLE + "-post.contentspec"});
        // and check that the file contents is correct
        assertThat(FileUtilities.readFileContents(new File(bookDir, "csprocessor.cfg")), containsString("SPEC_ID=" + id));
        assertThat(FileUtilities.readFileContents(new File(bookDir, BOOK_TITLE + "-post.contentspec")), Matchers.is(randomString));
    }

    @Test
    public void shouldShutdownWhenSaveFailsForCreateProjectDir() throws IOException {
        // Given the title of the book and an id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);

        // and the saving should fail
        PowerMockito.mockStatic(FileUtilities.class);
        PowerMockito.doThrow(new IOException()).when(FileUtilities.class);
        FileUtilities.saveFile(any(File.class), anyString(), anyString());

        // When it is processed
        ArgumentCaptor<String> errorMessage = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> exitStatus = ArgumentCaptor.forClass(Integer.class);
        ClientUtilities.createContentSpecProject(command, cspConfig, bookDir, randomString, contentSpecWrapper, new ZanataDetails());

        // Then the command should be shutdown and an error message printed
        verify(command, times(2)).printError(errorMessage.capture(), anyBoolean());
        verify(command, times(1)).shutdown(exitStatus.capture());
        assertThat(exitStatus.getValue(), is(-1));
        assertThat(errorMessage.getAllValues().get(0),
                containsString("An error occurred while trying to save " + bookDir.getAbsolutePath() + File.separator + "csprocessor.cfg" +
                        "."));
        assertThat(errorMessage.getAllValues().get(1),
                containsString("An error occurred while trying to save " + bookDir.getAbsolutePath() + File.separator + BOOK_TITLE +
                        "-post.contentspec"));
    }

    @Test
    public void shouldSaveFileWhenOutputPathIsDirectoryForSaveOutputFile() {
        // Given a filename
        String filename = BOOK_TITLE + "-post.contentspec";
        // and a output path that is a directory
        String outputPath = bookDir.getAbsolutePath();

        // When saving the output file
        ClientUtilities.saveOutputFile(command, filename, outputPath, randomString);

        // Then check that the file exists and contains the right data
        File file = new File(bookDir, filename);
        assertTrue(file.exists());
        assertThat(FileUtilities.readFileContents(file), Matchers.is(randomString));
    }

    @Test
    public void shouldSaveFileWhenOutputPathIsFileForSaveOutputFile() {
        // Given a filename
        String filename = BOOK_TITLE + "-post.contentspec";
        // and a output path that is a directory
        String outputPath = bookDir.getAbsolutePath() + File.separator + BOOK_TITLE + ".txt";

        // When saving the output file
        ClientUtilities.saveOutputFile(command, filename, outputPath, randomString);

        // Then check that the file exists and contains the right data
        File file = new File(bookDir, BOOK_TITLE + ".txt");
        File incorrectFile = new File(bookDir, filename);
        assertTrue(file.exists());
        assertFalse(incorrectFile.exists());
        assertThat(FileUtilities.readFileContents(file), Matchers.is(randomString));
    }

    @Test
    public void shouldSaveFileAndCreateBackupIfFileExistsForSaveOutputFile() {
        // Given a filename
        String filename = BOOK_TITLE + "-post.contentspec";
        // and a output path that is a directory
        String outputPath = emptyFile.getAbsolutePath();

        // When saving the output file
        ClientUtilities.saveOutputFile(command, filename, outputPath, randomString);

        // Then check that the file exists, a backup has been created and contains the right data
        File backupFile = new File(emptyFile.getAbsolutePath() + ".backup");
        assertTrue(emptyFile.exists());
        assertTrue(backupFile.exists());
        assertThat(FileUtilities.readFileContents(emptyFile), Matchers.is(randomString));
        assertThat(FileUtilities.readFileContents(backupFile), Matchers.is(""));
    }

    @Test
    public void shouldValidateServerUrlWhenValid() {
        // Given a valid URL, that also contains a redirect
        String url = "http://www.example.com/";

        // When checking if the URL is valid and exists
        boolean result = ClientUtilities.validateServerExists(url);

        // Then
        assertTrue(result);
    }

    @Test
    public void shouldNotValidateServerUrlWhenInvalid() {
        // Given a null URL
        String url = null;

        // When checking if the URL is valid and exists
        boolean result = ClientUtilities.validateServerExists(url);

        // Then the result should be false
        assertFalse(result);
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(bookDir);
    }
}
