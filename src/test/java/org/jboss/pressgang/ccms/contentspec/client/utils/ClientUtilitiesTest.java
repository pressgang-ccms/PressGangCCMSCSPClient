package org.jboss.pressgang.ccms.contentspec.client.utils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.beust.jcommander.JCommander;
import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.annotation.ArbitraryNumber;
import net.sf.ipsedixit.annotation.ArbitraryString;
import net.sf.ipsedixit.core.StringType;
import org.apache.commons.io.FileUtils;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.client.BaseUnitTest;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.TestUtil;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.entities.Spec;
import org.jboss.pressgang.ccms.contentspec.entities.SpecList;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.ServerSettingsProvider;
import org.jboss.pressgang.ccms.provider.UserProvider;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.PropertyTagInContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerEntitiesWrapper;
import org.jboss.pressgang.ccms.wrapper.ServerSettingsWrapper;
import org.jboss.pressgang.ccms.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
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
    @ArbitraryString(type = StringType.ALPHANUMERIC) String randomAlphanumString;
    @ArbitraryString(type = StringType.ALPHANUMERIC) String username;
    @Mock JCommander parser;
    @Mock ContentSpecConfiguration cspConfig;
    @Mock ClientConfiguration clientConfig;
    @Mock ContentSpec contentSpec;
    @Mock ErrorLoggerManager elm;
    @Mock BaseCommandImpl command;
    @Mock UserWrapper user;
    @Mock CSTransformer csTransformer;
    @Mock ContentSpecWrapper contentSpecWrapper;
    @Mock RESTProviderFactory providerFactory;
    @Mock UserProvider userProvider;
    @Mock ServerSettingsProvider serverSettingsProvider;
    @Mock ServerEntitiesWrapper serverEntities;
    @Mock ServerSettingsWrapper serverSettings;

    File rootTestDirectory;
    File bookDir;
    File emptyFile;

    @Before
    public void setUp() throws IOException {
        bindStdOut();
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

        // Set up the server settings mock
        TestUtil.setUpServerSettings(serverSettings, serverEntities);
    }

    @After
    public void cleanUp() throws IOException {
        FileUtils.deleteDirectory(bookDir);
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
    public void shouldLeaveConfigLocationUnalteredForCorrectPath() {
        // Given a correct config location
        String configLocation = rootTestDirectory.getAbsolutePath() + File.separator + ".config" + File.separator + "csprocessor.ini";

        // When fixing the config location
        String fixedConfigLocation = ClientUtilities.fixConfigLocation(configLocation);

        // Then the two locations should be the same
        assertThat(fixedConfigLocation, is(configLocation));
    }

    @Test
    public void shouldLeaveConfigLocationUnalteredForAnEmptyFileThatExists() {
        // Given a correct config location
        String configLocation = rootTestDirectory.getAbsolutePath() + File.separator + "EmptyFile.txt";

        // When fixing the config location
        String fixedConfigLocation = ClientUtilities.fixConfigLocation(configLocation);

        // Then the two locations should be the same
        assertThat(fixedConfigLocation, is(configLocation));
    }

    @Test
    public void shouldLeaveConfigLocationUnalteredForNonExistentFileThatEndsInINI() {
        // Given a correct config location
        String configLocation = rootTestDirectory.getAbsolutePath() + File.separator + "NonExistentFile.ini";

        // When fixing the config location
        String fixedConfigLocation = ClientUtilities.fixConfigLocation(configLocation);

        // Then the two locations should be the same
        assertThat(fixedConfigLocation, is(configLocation));
    }

    @Test
    public void shouldAddConfigFileNameToConfigDirectoryLocation() {
        // Given a correct config location
        String configLocation = rootTestDirectory.getAbsolutePath();

        // When fixing the config location
        String fixedConfigLocation = ClientUtilities.fixConfigLocation(configLocation);

        // Then the fixed location should have the csprocessor.ini filename added
        assertThat(fixedConfigLocation, is(configLocation + File.separator + "csprocessor.ini"));
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
        ArgumentCaptor <String> errorMessage = ArgumentCaptor.forClass(String.class);
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
    public void shouldLoadIdFromFileOnProcessWhenNoStringIdSupplied() {
        // Given a command called with no ID
        List<String> ids = new ArrayList<String>();
        // And that there is an ID in the CS Processor config
        given(cspConfig.getContentSpecId()).willReturn(id);
        // And we are loading from the csprocessor.cfg
        given(command.loadFromCSProcessorCfg()).willReturn(true);

        // When it is processed
        boolean retValue = ClientUtilities.prepareAndValidateStringIds(command, cspConfig, ids);

        // Then the command should complete without error
        verify(command, times(0)).printErrorAndShutdown(anyInt(), anyString(), anyBoolean());
        assertTrue(retValue);
    }

    @Test
    public void shouldSaveSpecAndConfig() throws IOException {
        // Given the title of the book and an id
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);
        given(contentSpecWrapper.getId()).willReturn(id);

        // When the command is processing
        ClientUtilities.createContentSpecProject(command, cspConfig, bookDir, randomString, contentSpecWrapper, new ZanataDetails());

        // Then check that only the csprocessor.cfg and <TITLE>-post.contentspec exists
        assertArrayEquals(bookDir.list(), new String[]{"csprocessor.cfg", BOOK_TITLE + "-post.contentspec"});
        // and check that the file contents is correct
        assertThat(FileUtils.readFileToString(new File(bookDir, "csprocessor.cfg")), containsString("SPEC_ID=" + id));
        assertThat(FileUtils.readFileToString(new File(bookDir, BOOK_TITLE + "-post.contentspec")), is(randomString));
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
        ArgumentCaptor <String> errorMessage = ArgumentCaptor.forClass(String.class);
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
    public void shouldSaveFileWhenOutputPathIsDirectoryForSaveOutputFile() throws IOException {
        // Given a filename
        String filename = BOOK_TITLE + "-post.contentspec";
        // and a output path that is a directory
        String outputPath = bookDir.getAbsolutePath();

        // When saving the output file
        ClientUtilities.saveOutputFile(command, filename, outputPath, randomString);

        // Then check that the file exists and contains the right data
        File file = new File(bookDir, filename);
        assertTrue(file.exists());
        assertThat(FileUtils.readFileToString(file), is(randomString));
    }

    @Test
    public void shouldSaveFileWhenOutputPathIsFileForSaveOutputFile() throws IOException {
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
        assertThat(FileUtils.readFileToString(file), is(randomString));
    }

    @Test
    public void shouldSaveFileAndCreateBackupIfFileExistsForSaveOutputFile() throws IOException {
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
        assertThat(FileUtils.readFileToString(emptyFile), is(randomString));
        assertThat(FileUtils.readFileToString(backupFile), is(""));
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

    @Test
    public void shouldReadFromCsprocessorCfg() throws IOException {
        // Given a file
        File csprocessorCfg = new File(rootTestDirectory.getAbsolutePath() + File.separator + "csprocessor.cfg");

        // When getting the csprocessor.cfg
        final ContentSpecConfiguration cspConfig = ClientUtilities.readFromCsprocessorCfg(csprocessorCfg);

        // Then check that the server and id is set
        assertNotNull(cspConfig);
        assertThat(cspConfig.getContentSpecId(), is(7210));
        assertThat(cspConfig.getServerUrl(), is("http://www.example.com/"));
    }

    @Test
    public void shouldReadFromCsprocessorCfgWithBlankFile() throws IOException {
        // Given a new empty file
        File csprocessorCfg = TestUtil.createRealFile(bookDir.getAbsolutePath() + File.separator + "csprocessor.cfg", "");

        // When getting the csprocessor.cfg
        final ContentSpecConfiguration cspConfig = ClientUtilities.readFromCsprocessorCfg(csprocessorCfg);

        // Then check that the server and id are still null
        assertNull(cspConfig.getServerUrl());
        assertNull(cspConfig.getContentSpecId());
    }

    @Test
    public void shouldReturnCorrectOutputRootDirectoryWithRootDirectorySet() {
        // Given a config with a root directory
        given(cspConfig.getRootOutputDirectory()).willReturn(rootTestDirectory.getAbsolutePath() + File.separator);

        // When determining the root directory
        String rootDirectory = ClientUtilities.getOutputRootDirectory(cspConfig, BOOK_TITLE);

        // Then the directory should be the root test dir + book title
        assertThat(rootDirectory, is(rootTestDirectory.getAbsolutePath() + File.separator + BOOK_TITLE + File.separator));
    }

    @Test
    public void shouldReturnCorrectOutputRootDirectoryWithNoRootDirectorySet() {
        // Given a config with no root directory
        given(cspConfig.getRootOutputDirectory()).willReturn(null);

        // When determining the root directory
        String rootDirectory = ClientUtilities.getOutputRootDirectory(cspConfig, BOOK_TITLE);

        // Then the directory should be the current working directory or an empty string for short
        assertThat(rootDirectory, is(""));
    }

    @Test
    public void shouldReturnCorrectOutputRootDirectoryWithEmptyRootDirectorySet() {
        // Given a config with no root directory
        given(cspConfig.getRootOutputDirectory()).willReturn("");

        // When determining the root directory
        String rootDirectory = ClientUtilities.getOutputRootDirectory(cspConfig, BOOK_TITLE);

        // Then the directory should be the current working directory or an empty string for short
        assertThat(rootDirectory, is(""));
    }

    @Test
    public void shouldReturnCorrectOutputRootDirectoryFromContentSpec() {
        // Given a config with a root directory
        given(cspConfig.getRootOutputDirectory()).willReturn(rootTestDirectory.getAbsolutePath() + File.separator);
        // and the content spec has a title
        given(contentSpec.getTitle()).willReturn(BOOK_TITLE);

        // When determining the root directory
        String rootDirectory = ClientUtilities.getOutputRootDirectory(cspConfig, contentSpec);

        // Then the directory should be the root test dir + book title
        assertThat(rootDirectory, is(rootTestDirectory.getAbsolutePath() + File.separator + BOOK_TITLE + File.separator));
    }

    @Test
    public void shouldReturnCorrectOutputRootDirectoryFromContentSpecWrapper() {
        // Given a config with a root directory
        given(cspConfig.getRootOutputDirectory()).willReturn(rootTestDirectory.getAbsolutePath() + File.separator);
        // and the content spec wrapper has a title
        given(contentSpecWrapper.getTitle()).willReturn(BOOK_TITLE);

        // When determining the root directory
        String rootDirectory = ClientUtilities.getOutputRootDirectory(providerFactory, cspConfig, contentSpecWrapper);

        // Then the directory should be the root test dir + book title
        assertThat(rootDirectory, is(rootTestDirectory.getAbsolutePath() + File.separator + BOOK_TITLE + File.separator));
    }

    @Test
    public void shouldGenerateEmptyStringForEmptySpecList() {
        // Given a SpecList with no data
        SpecList specList = new SpecList();

        // When generating the spec list response
        String output = ClientUtilities.generateContentSpecList(specList);

        // Then the output should be an empty string
        assertThat(output, is(""));
    }

    @Test
    public void shouldGenerateEmptyStringForNullSpecList() {
        // Given a null SpecList
        SpecList specList = null;

        // When generating the spec list response
        String output = ClientUtilities.generateContentSpecList(specList);

        // Then the output should be an empty string
        assertThat(output, is(""));
    }

    @Test
    public void shouldGenerateStringWithHeaderForSpecListWithOneEntry() {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy");
        // Given a SpecList with one entry
        SpecList specList = new SpecList();
        Date now = new Date();
        specList.addSpec(new Spec(id, randomAlphanumString, randomString, secondId.toString(), username, now));

        // When generating the spec list response
        String output = ClientUtilities.generateContentSpecList(specList);

        // Then the output should have a header and the spec entry
        final String format = "%" + (id.toString().length() + 2) + "s%" + (randomAlphanumString.length() + 2) + "s%" + (randomString
                .length() + 2) + "s%9s%" + (username.length() + 2) + "s%15s";
        assertThat(output, containsString(String.format(format, "ID", "TITLE", "PRODUCT", "VERSION", "CREATED BY", "LAST MODIFIED")));
        assertThat(output, containsString(
                String.format(format, id, randomAlphanumString, randomString, secondId, username, dateFormatter.format(now))));
    }

    @Test
    public void shouldGenerateStringWithHeaderForSpecListWithOneEntryWithoutCreator() {
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy");
        // Given a SpecList with one entry that has no user
        SpecList specList = new SpecList();
        Date now = new Date();
        specList.addSpec(new Spec(id, randomAlphanumString, randomString, secondId.toString(), null, now));

        // When generating the spec list response
        String output = ClientUtilities.generateContentSpecList(specList);

        // Then the output should have a header and the spec entry
        final String format = "%" + (id.toString().length() + 2) + "s%" + (randomAlphanumString.length() + 2) + "s%" + (randomString
                .length() + 2) + "s%9s%12s%15s";
        assertThat(output, containsString(String.format(format, "ID", "TITLE", "PRODUCT", "VERSION", "CREATED BY", "LAST MODIFIED")));
        assertThat(output, containsString(
                String.format(format, id, randomAlphanumString, randomString, secondId, "Unknown", dateFormatter.format(now))));
    }

    @Test
    public void shouldGenerateStringWithHeaderForSpecListWithOneEntryWithoutALastModifiedDate() {
        // Given a SpecList with one entry that has no date
        SpecList specList = new SpecList();
        specList.addSpec(new Spec(id, randomAlphanumString, randomString, secondId.toString(), username, null));

        // When generating the spec list response
        String output = ClientUtilities.generateContentSpecList(specList);

        // Then the output should have a header and the spec entry
        final String format = "%" + (id.toString().length() + 2) + "s%" + (randomAlphanumString.length() + 2) + "s%" + (randomString
                .length() + 2) + "s%9s%" + (username.length() + 2) + "s%15s";
        assertThat(output, containsString(String.format(format, "ID", "TITLE", "PRODUCT", "VERSION", "CREATED BY", "LAST MODIFIED")));
        assertThat(output, containsString(String.format(format, id, randomAlphanumString, randomString, secondId, username, "Unknown")));
    }

    @Test
    public void shouldBuildSpecListObjectFromListOfContentSpecsWithNoCreator() {
        Date now = new Date();
        // Given a list of content specs
        List<ContentSpecWrapper> contentSpecs = Arrays.asList(contentSpecWrapper);
        // and the content spec has some basic properties
        given(contentSpecWrapper.getTitle()).willReturn(randomAlphanumString);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getProduct()).willReturn(randomString);
        given(contentSpecWrapper.getVersion()).willReturn(secondId.toString());
        given(contentSpecWrapper.getLastModified()).willReturn(now);
        // and no creator
        given(contentSpecWrapper.getProperty(anyInt())).willReturn(null);
        // and the provider factory will return a user provider
        given(providerFactory.getProvider(UserProvider.class)).willReturn(userProvider);

        // When building the spec list
        final SpecList list = ClientUtilities.buildSpecList(contentSpecs, providerFactory, serverEntities);

        // Then check that the spec list contains one spec and the details are right
        assertThat(list.getCount(), is(1L));
        assertNotNull(list.getSpecs());
        Spec spec = list.getSpecs().get(0);
        assertThat(spec.getId(), is(id));
        assertThat(spec.getTitle(), is(randomAlphanumString));
        assertThat(spec.getProduct(), is(randomString));
        assertThat(spec.getVersion(), is(secondId.toString()));
        assertThat(spec.getLastModified(), is(now));
    }

    @Test
    public void shouldBuildSpecListObjectFromListOfContentSpecsWithWithInvalidCreator() {
        final PropertyTagInContentSpecWrapper propTag = mock(PropertyTagInContentSpecWrapper.class);
        Date now = new Date();
        // Given a list of content specs
        List<ContentSpecWrapper> contentSpecs = Arrays.asList(contentSpecWrapper);
        // and the content spec has some basic properties
        given(contentSpecWrapper.getTitle()).willReturn(randomAlphanumString);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getProduct()).willReturn(randomString);
        given(contentSpecWrapper.getVersion()).willReturn(secondId.toString());
        given(contentSpecWrapper.getLastModified()).willReturn(now);
        // and a creator
        given(contentSpecWrapper.getProperty(anyInt())).willReturn(propTag);
        given(propTag.getValue()).willReturn(username);
        // and the provider factory will return a user provider
        given(providerFactory.getProvider(UserProvider.class)).willReturn(userProvider);
        // and the user provider returns a user that doesn't exist
        given(userProvider.getUsersByName(anyString())).willReturn(null);

        // When building the spec list
        final SpecList list = ClientUtilities.buildSpecList(contentSpecs, providerFactory, serverEntities);

        // Then check that the spec list contains one spec and the details are right
        assertThat(list.getCount(), is(1L));
        assertNotNull(list.getSpecs());
        Spec spec = list.getSpecs().get(0);
        assertThat(spec.getId(), is(id));
        assertThat(spec.getTitle(), is(randomAlphanumString));
        assertThat(spec.getProduct(), is(randomString));
        assertThat(spec.getVersion(), is(secondId.toString()));
        assertThat(spec.getLastModified(), is(now));
        assertNull(spec.getCreator());
    }

    @Test
    public void shouldBuildSpecListObjectFromListOfContentSpecsWithWithValidCreator() {
        final PropertyTagInContentSpecWrapper propTag = mock(PropertyTagInContentSpecWrapper.class);
        final CollectionWrapper<UserWrapper> users = mock(CollectionWrapper.class);
        Date now = new Date();
        // Given a list of content specs
        List<ContentSpecWrapper> contentSpecs = Arrays.asList(contentSpecWrapper);
        // and the content spec has some basic properties
        given(contentSpecWrapper.getTitle()).willReturn(randomAlphanumString);
        given(contentSpecWrapper.getId()).willReturn(id);
        given(contentSpecWrapper.getProduct()).willReturn(randomString);
        given(contentSpecWrapper.getVersion()).willReturn(secondId.toString());
        given(contentSpecWrapper.getLastModified()).willReturn(now);
        // and a creator
        given(contentSpecWrapper.getProperty(anyInt())).willReturn(propTag);
        given(propTag.getValue()).willReturn(username);
        // and the provider factory will return a user provider
        given(providerFactory.getProvider(UserProvider.class)).willReturn(userProvider);
        // and the user provider finds a valid user
        given(userProvider.getUsersByName(anyString())).willReturn(users);
        given(users.size()).willReturn(1);
        given(users.getItems()).willReturn(Arrays.asList(user));
        given(user.getUsername()).willReturn(username);

        // When building the spec list
        final SpecList list = ClientUtilities.buildSpecList(contentSpecs, providerFactory, serverEntities);

        // Then check that the spec list contains one spec and the details are right
        assertThat(list.getCount(), is(1L));
        assertNotNull(list.getSpecs());
        Spec spec = list.getSpecs().get(0);
        assertThat(spec.getId(), is(id));
        assertThat(spec.getTitle(), is(randomAlphanumString));
        assertThat(spec.getProduct(), is(randomString));
        assertThat(spec.getVersion(), is(secondId.toString()));
        assertThat(spec.getLastModified(), is(now));
        assertThat(spec.getCreator(), is(username));
    }
}
