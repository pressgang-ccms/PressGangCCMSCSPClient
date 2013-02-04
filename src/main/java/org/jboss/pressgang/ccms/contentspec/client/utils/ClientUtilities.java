package org.jboss.pressgang.ccms.contentspec.client.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.internal.Console;
import com.google.code.regexp.NamedMatcher;
import com.google.code.regexp.NamedPattern;
import com.redhat.j2koji.base.KojiConnector;
import com.redhat.j2koji.entities.KojiBuild;
import com.redhat.j2koji.exceptions.KojiException;
import com.redhat.j2koji.rpc.search.KojiBuildSearch;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.builder.utils.DocbookBuildUtilities;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.entities.Spec;
import org.jboss.pressgang.ccms.contentspec.client.entities.SpecList;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecParser;
import org.jboss.pressgang.ccms.contentspec.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.contentspec.provider.RESTUserProvider;
import org.jboss.pressgang.ccms.contentspec.utils.CSTransformer;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.contentspec.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.UserWrapper;
import org.jboss.pressgang.ccms.contentspec.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.utils.common.DocBookUtilities;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;
import org.jboss.pressgang.ccms.utils.constants.CommonConstants;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;

public class ClientUtilities {
    /**
     * Checks if the location for a config location is in the correct format then corrects it if not.
     *
     * @param location The path location to be checked
     * @return The fixed path location string
     */
    public static String validateConfigLocation(final String location) {
        if (location == null || location.isEmpty()) return location;

        String fixedLocation = location;
        if (location.startsWith("~")) {
            fixedLocation = Constants.HOME_LOCATION + location.substring(1);
        } else if (location.startsWith("./") || location.startsWith("../")) {
            try {
                fixedLocation = new File(location).getCanonicalPath();
            } catch (IOException e) {
                // Do nothing
            }
        }
        final File file = new File(fixedLocation);
        if (file.exists() && file.isFile()) {
            return fixedLocation;
        }
        if (!location.endsWith(File.separator) && !location.endsWith(".ini")) {
            fixedLocation += File.separator;
        }
        if (!location.endsWith(".ini")) {
            fixedLocation += Constants.CONFIG_FILENAME;
        }
        return fixedLocation;
    }

    /**
     * Checks if the directory location is in the correct format then corrects it if not.
     *
     * @param location The path location to be checked
     * @return The fixed path location string
     */
    public static String validateDirLocation(final String location) {
        if (location == null || location.isEmpty()) return location;

        String fixedLocation = location;
        if (!location.endsWith(File.separator)) {
            fixedLocation += File.separator;
        }
        if (location.startsWith("~")) {
            fixedLocation = Constants.HOME_LOCATION + fixedLocation.substring(1);
        } else if (location.startsWith("./") || location.startsWith("../")) {
            try {
                fixedLocation = new File(fixedLocation).getCanonicalPath();
            } catch (IOException e) {
                // Do nothing
            }
        }
        return fixedLocation;
    }

    /**
     * Checks if the host address is in the correct format then corrects it if not.
     *
     * @param host The host address of the server to be checked
     * @return The fixed host address string
     */
    public static String validateHost(final String host) {
        if (host == null || host.isEmpty()) return host;

        String fixedHost = host;
        if (!host.endsWith("/")) {
            fixedHost += "/";
        }
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            fixedHost = "http://" + fixedHost;
        }
        return fixedHost;
    }

    /**
     * Checks that a server exists at the specified URL by sending a request to get the headers from the URL.
     *
     * @param serverUrl The URL of the server.
     * @return True if the server exists and got a successful response otherwise false.
     */
    public static boolean validateServerExists(final String serverUrl) {
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            HttpURLConnection.setFollowRedirects(true);
            int response = connection.getResponseCode();
            if (response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP) {
                return validateServerExists(connection.getHeaderField("Location"));
            } else {
                return response == HttpURLConnection.HTTP_OK || response == HttpURLConnection.HTTP_BAD_METHOD;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks if the file path is in the correct format then corrects it if not.
     *
     * @param filePath The path location to be checked
     * @return The fixed path location string
     */
    public static String validateFilePath(final String filePath) {
        if (filePath == null) return null;
        String fixedPath = filePath;
        if (filePath.startsWith("~")) {
            fixedPath = Constants.HOME_LOCATION + fixedPath.substring(1);
        } else if (filePath.startsWith("./") || filePath.startsWith("../")) {
            try {
                fixedPath = new File(fixedPath).getCanonicalPath();
            } catch (IOException e) {
                // Do nothing
            }
        }
        return fixedPath;
    }

    /**
     * Read from a csprocessor.cfg file and intitialise the variables into a configuration object.
     *
     * @param csprocessorcfg The csprocessor.cfg file.
     * @param cspConfig      The content spec configuration object to load the settings into
     * @throws FileNotFoundException The csprocessor.cfg couldn't be found
     * @throws IOException
     */
    public static void readFromCsprocessorCfg(final File csprocessorcfg, final ContentSpecConfiguration cspConfig) throws IOException {
        final Properties prop = new Properties();
        prop.load(new FileInputStream(csprocessorcfg));
        cspConfig.setContentSpecId(Integer.parseInt(prop.getProperty("SPEC_ID")));
        cspConfig.setServerUrl(ClientUtilities.validateHost(prop.getProperty("SERVER_URL")));
        cspConfig.getZanataDetails().setServer(ClientUtilities.validateHost(prop.getProperty("ZANATA_URL")));
        cspConfig.getZanataDetails().setProject(prop.getProperty("ZANATA_PROJECT_NAME"));
        cspConfig.getZanataDetails().setVersion(prop.getProperty("ZANATA_PROJECT_VERSION"));
        cspConfig.setKojiHubUrl(validateHost(prop.getProperty("KOJI_HUB_URL")));
        cspConfig.setPublishCommand(prop.getProperty("PUBLISH_COMMAND"));
    }

    /**
     * Generates the contents of a csprocessor.cfg file from the passed arguments.
     *
     * @param contentSpec The content specification object the csprocessor.cfg will be used for.
     * @param serverUrl   The server URL that the content specification exists on.
     * @return The generated contents of the csprocessor.cfg file.
     */
    public static String generateCsprocessorCfg(final ContentSpecWrapper contentSpec, final String serverUrl,
            final ZanataDetails zanataDetails) {
        final StringBuilder output = new StringBuilder();
        output.append("# SPEC_TITLE=").append(DocBookUtilities.escapeTitle(contentSpec.getTitle())).append("\n");
        output.append("SPEC_ID=").append(contentSpec.getId()).append("\n");
        output.append("SERVER_URL=").append(serverUrl).append("\n");
        output.append("ZANATA_URL=").append(zanataDetails.getServer() == null ? "" : zanataDetails.getServer()).append("\n");
        output.append("ZANATA_PROJECT_NAME=").append(zanataDetails.getProject() == null ? "" : zanataDetails.getProject()).append("\n");
        output.append("ZANATA_PROJECT_VERSION=").append(zanataDetails.getVersion() == null ? "" : zanataDetails.getVersion()).append("\n");
        output.append("KOJI_HUB_URL=\n");
        output.append("PUBLISH_COMMAND=\n");
        return output.toString();
    }

    /**
     * Runs a command from a specified directory
     *
     * @param command       The command to be run.
     * @param dir           The directory to run the command from.
     * @param console       The console to print the output to.
     * @param displayOutput Whether the output should be displayed or not.
     * @param allowInput    Whether the process should allow input form stdin.
     * @return The exit value of the command
     * @throws IOException
     */
    public static Integer runCommand(final String command, final File dir, final Console console, boolean displayOutput,
            boolean allowInput) throws IOException {
        return runCommand(command, null, dir, console, displayOutput, allowInput);
    }

    /**
     * Runs a command from a specified directory
     *
     * @param command       The command to be run.
     * @param envVariables  An array of environment variables to be used.
     * @param dir           The directory to run the command from.
     * @param console       The console to print the output to.
     * @param displayOutput Whether the output should be displayed or not.
     * @param allowInput    Whether the process should allow input form stdin.
     * @return The exit value of the command
     * @throws IOException
     */
    public static Integer runCommand(final String command, final String[] envVariables, final File dir, final Console console,
            boolean displayOutput, boolean allowInput) throws IOException {
        if (!dir.isDirectory()) throw new IOException();

        try {
            String[] fixedEnvVariables = envVariables == null ? null : envVariables.clone();
            final Map<String, String> env = System.getenv();
            final List<String> envVars = new ArrayList<String>();
            for (final Entry<String, String> entry : env.entrySet()) {
                final String key = entry.getKey();
                if (!key.equals("XML_CATALOG_FILES")) envVars.add(key + "=" + entry.getValue());
            }
            if (envVariables != null) {
                Collections.addAll(envVars, envVariables);
            }
            fixedEnvVariables = envVars.toArray(new String[envVars.size()]);

            final Process p = Runtime.getRuntime().exec(command, fixedEnvVariables, dir);

            // Create a separate thread to read the stderr stream
            final InputStreamHandler stdErr = new InputStreamHandler(p.getErrorStream(), console);
            final InputStreamHandler stdInPipe = new InputStreamHandler(System.in, p.getOutputStream());

            // Pipe stdin to the process
            if (allowInput) {
                stdInPipe.start();
            }

            // Get the output of the command
            if (displayOutput) {
                stdErr.start();

                final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line;
                try {
                    while ((line = br.readLine()) != null) {
                        synchronized (console) {
                            console.println(line);
                        }
                    }
                } catch (Exception e) {
                    // Do nothing
                    JCommander.getConsole().println(e.getMessage());
                    e.printStackTrace();
                }
            }

            // Wait for the process to finish
            p.waitFor();

            // Ensure that the stdin reader gets shutdown
            stdInPipe.shutdown();

            // Wait for the output to be printed
            while (stdErr.isAlive()) {
                Thread.sleep(100);
            }

            return p.exitValue();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Builds a Content Specification list for a list of content specifications.
     */
    public static SpecList buildSpecList(final List<ContentSpecWrapper> specList, final DataProviderFactory providerFactory) {
        final List<Spec> specs = new ArrayList<Spec>();
        for (final ContentSpecWrapper cs : specList) {
            UserWrapper creator = null;
            if (cs.getProperty(CSConstants.ADDED_BY_PROPERTY_TAG_ID) != null) {
                final CollectionWrapper<UserWrapper> users = providerFactory.getProvider(RESTUserProvider.class).getUsersByName(
                        cs.getProperty(CSConstants.ADDED_BY_PROPERTY_TAG_ID).getValue());
                if (users.size() == 1) {
                    creator = users.getItems().get(0);
                }
            }
            specs.add(new Spec(cs.getId(), cs.getTitle(), cs.getProduct(), cs.getVersion(), creator != null ? creator.getUsername() : null,
                    cs.getLastModified()));
        }
        return new SpecList(specs, specs.size());
    }

    /**
     * Generates the response output for a list of content specifications
     *
     * @param contentSpecs The SpecList that contains the processed Content Specifications
     * @return The generated response ouput.
     */
    public static String generateContentSpecListResponse(final SpecList contentSpecs) {
        final LinkedHashMap<String, Integer> sizes = new LinkedHashMap<String, Integer>();
        // Create the initial sizes incase they never increase
        sizes.put("ID", 2);
        sizes.put("SPEC ID", 7);
        sizes.put("TITLE", 5);
        sizes.put("SPEC TITLE", 10);
        sizes.put("PRODUCT", 7);
        sizes.put("VERSION", 7);
        sizes.put("CREATED BY", 10);
        sizes.put("LAST MODIFIED", 13);
        if (contentSpecs != null && contentSpecs.getSpecs() != null && !contentSpecs.getSpecs().isEmpty()) {
            for (final Spec spec : contentSpecs.getSpecs()) {
                if (spec.getId().toString().length() > sizes.get("ID")) {
                    sizes.put("ID", spec.getId().toString().length());
                }

                if (spec.getProduct() != null && spec.getProduct().length() > sizes.get("PRODUCT")) {
                    sizes.put("PRODUCT", spec.getProduct().length());
                }

                if (spec.getTitle().length() > sizes.get("TITLE")) {
                    sizes.put("TITLE", spec.getTitle().length());
                }

                if (spec.getVersion() != null && spec.getVersion().length() > sizes.get("VERSION")) {
                    sizes.put("VERSION", spec.getVersion().length());
                }

                if (spec.getCreator() != null && spec.getCreator().length() > sizes.get("CREATED BY")) {
                    sizes.put("CREATED BY", spec.getCreator().length());
                }
            }

            final String format = "%" + (sizes.get("ID") + 2) + "s%" + (sizes.get("TITLE") + 2) + "s%" + (sizes.get(
                    "PRODUCT") + 2) + "s%" + (sizes.get("VERSION") + 2) + "s%" + (sizes.get("CREATED BY") + 2) + "s%" + (sizes.get(
                    "LAST MODIFIED") + 2) + "s";

            final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy");
            final StringBuilder output = new StringBuilder(
                    String.format(format, "ID", "TITLE", "PRODUCT", "VERSION", "CREATED BY", "LAST MODIFIED") + "\n");
            for (final Spec spec : contentSpecs.getSpecs()) {
                output.append(String.format(format, spec.getId().toString(), spec.getTitle(), spec.getProduct(), spec.getVersion(),
                        spec.getCreator(),
                        spec.getLastModified() == null ? "Unknown" : dateFormatter.format(spec.getLastModified())) + "\n");
            }
            return output.toString();
        }
        return "";
    }

    /**
     * Get the next pubsnumber from koji for the content spec that will be built.
     *
     * @param contentSpec The contentspec to be built.
     * @param kojiHubUrl  The URL of the Koji Hub server to connect to.
     * @return The next valid pubsnumber for a build to koji.
     * @throws KojiException         Thrown if an error occurs when searching koji for the builds.
     * @throws MalformedURLException Thrown if the passed Koji Hub URL isn't a valid URL.
     */
    public static Integer getPubsnumberFromKoji(final ContentSpec contentSpec,
            final String kojiHubUrl) throws KojiException, MalformedURLException {
        assert contentSpec != null;

        if (kojiHubUrl == null) {
            throw new MalformedURLException();
        }

        final String product = DocBookUtilities.escapeTitle(contentSpec.getProduct());
        final String version = contentSpec.getVersion();
        final String bookTitle = DocBookUtilities.escapeTitle(contentSpec.getTitle());
        final String locale = contentSpec.getLocale() == null ? CommonConstants.DEFAULT_LOCALE : contentSpec.getLocale();
        final String bookVersion = DocbookBuildUtilities.generateRevision(contentSpec);

        // Connect to the koji hub
        final KojiConnector connector = new KojiConnector();
        connector.connectTo(validateHost(kojiHubUrl));

        // Perform the search using the info from the content spec
        final String packageName = product + "-" + bookTitle + "-" + version + "-web-" + locale + "-" + bookVersion + "-";
        final KojiBuildSearch buildSearch = new KojiBuildSearch(packageName + "*");
        connector.executeMethod(buildSearch);

        // Search through each result to find the pubsnumber
        final List<KojiBuild> builds = buildSearch.getResults();

        // Check to see if we found any results
        if (builds.size() > 0) {
            Integer pubsnumber = 0;
            for (final KojiBuild build : builds) {
                final String buildName = build.getName();
                final String matchString = buildName.replace(packageName, "");
                final NamedPattern pattern = NamedPattern.compile("(?<Pubsnumber>[0-9]+).*");
                final NamedMatcher matcher = pattern.matcher(matchString);

                while (matcher.find()) {
                    final Integer buildPubsnumber = Integer.parseInt(matcher.group("Pubsnumber"));
                    if (buildPubsnumber > pubsnumber) pubsnumber = buildPubsnumber;
                    break;
                }
            }

            return pubsnumber + 1;
        } else {
            return null;
        }
    }

    /**
     * Transforms a Content Specification wrapped entity into a POJO Content Spec object,
     *
     * @param contentSpec The Content Spec entity to be transformed.
     * @return The POJO ContentSpec object created from the Content Spec entity.
     */
    public static ContentSpec transformContentSpec(final ContentSpecWrapper contentSpec) {
        final CSTransformer transformer = new CSTransformer();
        return transformer.transform(contentSpec);
    }

    /**
     * @param providerFactory   The Entity Provider Factory to create Providers to get Entities from a Datasource.
     * @param loggerManager     The Logging manager that keeps tracks of error logs.
     * @param contentSpecString The Content Spec String representation to be parsed.
     * @return The parsed content spec if no errors occurred otherwise null.
     * @throws Exception Thrown if a fatal error occurs while parsing.
     */
    public static ContentSpec parseContentSpecString(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager,
            final String contentSpecString) throws Exception {
        return parseContentSpecString(providerFactory, loggerManager, contentSpecString, ContentSpecParser.ParsingMode.EITHER);
    }

    /**
     * @param providerFactory   The Entity Provider Factory to create Providers to get Entities from a Datasource.
     * @param loggerManager     The Logging manager that keeps tracks of error logs.
     * @param contentSpecString The Content Spec String representation to be parsed.
     * @param parsingMode       The mode that the content spec should be parsed as.
     * @return The parsed content spec if no errors occurred otherwise null.
     * @throws Exception Thrown if a fatal error occurs while parsing.
     */
    public static ContentSpec parseContentSpecString(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager,
            final String contentSpecString, final ContentSpecParser.ParsingMode parsingMode) throws Exception {
        return parseContentSpecString(providerFactory, loggerManager, contentSpecString, parsingMode, false);
    }

    /**
     * @param providerFactory   The Entity Provider Factory to create Providers to get Entities from a Datasource.
     * @param loggerManager     The Logging manager that keeps tracks of error logs.
     * @param contentSpecString The Content Spec String representation to be parsed.
     * @param parsingMode       The mode that the content spec should be parsed as.
     * @param processProcesses  If processes should be processed to setup their relationships (makes external calls)
     * @return The parsed content spec if no errors occurred otherwise null.
     * @throws Exception Thrown if a fatal error occurs while parsing.
     */
    public static ContentSpec parseContentSpecString(final DataProviderFactory providerFactory, final ErrorLoggerManager loggerManager,
            final String contentSpecString, final ContentSpecParser.ParsingMode parsingMode, boolean processProcesses) throws Exception {
        final ContentSpecParser csp = new ContentSpecParser(providerFactory, loggerManager);
        if (csp.parse(contentSpecString, parsingMode, processProcesses)) {
            return csp.getContentSpec();
        } else {
            return null;
        }
    }

    /**
     * Gets the output root directory based on the configuration files and ContentSpec object.
     *
     * @param cspConfig   The content spec configuration settings.
     * @param contentSpec The content spec object to get details from for the output directory.
     * @return A string that represents where the root folder is for content to be saved.
     */
    public static String getOutputRootDirectory(final ContentSpecConfiguration cspConfig, final ContentSpec contentSpec) {
        return getOutputRootDirectory(cspConfig, contentSpec.getTitle());
    }

    /**
     * Gets the output root directory based on the configuration files and ContentSpec entity object.
     *
     * @param cspConfig   The content spec configuration settings.
     * @param contentSpec The content spec object to get details from for the output directory.
     * @return A string that represents where the root folder is for content to be saved.
     */
    public static String getOutputRootDirectory(final ContentSpecConfiguration cspConfig, final ContentSpecWrapper contentSpec) {
        return getOutputRootDirectory(cspConfig, contentSpec.getTitle());
    }

    /**
     * Gets the output root directory based on the configuration files and ContentSpec entity object.
     *
     * @param cspConfig        The content spec configuration settings.
     * @param contentSpecTitle The title of the content specification for the output directory.
     * @return A string that represents where the root folder is for content to be saved.
     */
    private static String getOutputRootDirectory(final ContentSpecConfiguration cspConfig, final String contentSpecTitle) {
        assert contentSpecTitle != null;
        assert cspConfig != null;

        final String fileName = DocBookUtilities.escapeTitle(contentSpecTitle);
        return (cspConfig.getRootOutputDirectory() == null || cspConfig.getRootOutputDirectory().equals(
                "") ? "" : (cspConfig.getRootOutputDirectory() + fileName + File.separator));
    }

    /**
     * TODO
     *
     * @param command
     * @param cspConfig
     * @param ids
     */
    public static boolean prepareAndValidateIds(final BaseCommandImpl command, final ContentSpecConfiguration cspConfig,
            final List<Integer> ids) {
        boolean isLoadingFromConfig = prepareIds(command, cspConfig, ids);
        validateIdsOrFiles(command, ids, true);
        return isLoadingFromConfig;
    }

    /**
     * TODO
     *
     * @param command
     * @param cspConfig
     * @param ids
     */
    public static boolean prepareIds(final BaseCommandImpl command, final ContentSpecConfiguration cspConfig, final List<Integer> ids) {
        boolean isLoadingFromConfig = false;
        // If there are no ids then use the csprocessor.cfg file
        if (command.loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (cspConfig != null && cspConfig.getContentSpecId() != null) {
                ids.clear();
                ids.add(cspConfig.getContentSpecId());
                isLoadingFromConfig = true;
            }
        }

        return isLoadingFromConfig;
    }

    /**
     * TODO
     *
     * @param command
     * @param cspConfig
     * @param ids
     */
    public static boolean prepareAndValidateStringIds(final BaseCommandImpl command, final ContentSpecConfiguration cspConfig,
            final List<String> ids) {
        boolean isLoadingFromConfig = prepareStringIds(command, cspConfig, ids);
        validateIdsOrFiles(command, ids, true);
        return isLoadingFromConfig;
    }

    /**
     * TODO
     *
     * @param command
     * @param cspConfig
     * @param ids
     */
    public static boolean prepareStringIds(final BaseCommandImpl command, final ContentSpecConfiguration cspConfig,
            final List<String> ids) {
        boolean isLoadingFromConfig = false;
        // If there are no ids then use the csprocessor.cfg file
        if (command.loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (cspConfig != null && cspConfig.getContentSpecId() != null) {
                ids.clear();
                ids.add(cspConfig.getContentSpecId().toString());
                isLoadingFromConfig = true;
            }
        }

        return isLoadingFromConfig;
    }

    /**
     * TODO
     *
     * @param command
     * @param ids
     * @param canLoadFromCsprocessorCfg
     */
    public static void validateIdsOrFiles(BaseCommandImpl command, final List<?> ids, boolean canLoadFromCsprocessorCfg) {
        // Check that one and only one ID exists
        if (ids.size() == 0) {
            if (canLoadFromCsprocessorCfg) {
                command.printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.ERROR_NO_ID_MSG, false);
            } else {
                command.printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.ERROR_NO_ID_CMD_LINE_MSG, false);
            }
        } else if (ids.size() > 1) {
            command.printErrorAndShutdown(Constants.EXIT_ARGUMENT_ERROR, Constants.ERROR_MULTIPLE_ID_MSG, false);
        }

    }

    /**
     * Creates the Content Spec Project directory and adds the csprocessor.cfg and Content Spec file to the directory.
     *
     * @param command           The command the details are being created from.
     * @param cspConfig         The csprocessor.cfg configuration settings.
     * @param directory         The project directory.
     * @param contentSpecString The content spec string representation.
     * @param contentSpec       The content spec object from a datasource.
     * @param zanataDetails     The Connection details for zanata.
     */
    public static void createContentSpecProject(final BaseCommandImpl command, final ContentSpecConfiguration cspConfig,
            final File directory, final String contentSpecString, final ContentSpecWrapper contentSpec, ZanataDetails zanataDetails) {
        // If the output directory exists and force is enabled delete the directory contents
        if (directory.exists() && directory.isDirectory()) {
            // TODO Check that the directory was successfully deleted
            FileUtilities.deleteDir(directory);
        }

        boolean error = false;

        // Save the csprocessor.cfg and post spec to file if the create was successful
        final String escapedTitle = DocBookUtilities.escapeTitle(contentSpec.getTitle());
        final File outputSpec = new File(
                cspConfig.getRootOutputDirectory() + escapedTitle + File.separator + escapedTitle + "-post." + Constants
                        .FILENAME_EXTENSION);
        final File outputConfig = new File(cspConfig.getRootOutputDirectory() + escapedTitle + File.separator + "csprocessor.cfg");
        final String config = generateCsprocessorCfg(contentSpec, cspConfig.getServerUrl(), zanataDetails);

        // Create the directory
        if (outputConfig.getParentFile() != null && !outputConfig.getParentFile().exists()) {
            // TODO Check that the directory was successfully created
            outputConfig.getParentFile().mkdirs();
        }

        // Save the csprocessor.cfg
        try {
            FileUtilities.saveFile(outputConfig, config, Constants.FILE_ENCODING);
            JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputConfig.getAbsolutePath()));
        } catch (IOException e) {
            command.printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputConfig.getAbsolutePath()), false);
            error = true;
        }

        // Save the Post Processed spec
        try {
            FileUtilities.saveFile(outputSpec, contentSpecString, Constants.FILE_ENCODING);
            JCommander.getConsole().println(String.format(Constants.OUTPUT_SAVED_MSG, outputSpec.getAbsolutePath()));
        } catch (IOException e) {
            command.printError(String.format(Constants.ERROR_FAILED_SAVING_FILE, outputSpec.getAbsolutePath()), false);
            error = true;
        }

        if (error) {
            command.shutdown(Constants.EXIT_FAILURE);
        }
    }
}

class InputStreamHandler extends Thread implements ShutdownAbleApp {
    private final InputStream stream;
    private final StringBuffer buffer;
    private final Console console;
    private final OutputStream outStream;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    public InputStreamHandler(final InputStream stream, final StringBuffer buffer) {
        this.stream = stream;
        this.buffer = buffer;
        this.console = null;
        this.outStream = null;
    }

    public InputStreamHandler(final InputStream stream, final Console console) {
        this.stream = stream;
        this.buffer = null;
        this.console = console;
        this.outStream = null;
    }

    public InputStreamHandler(final InputStream stream, final OutputStream outStream) {
        this.stream = stream;
        this.buffer = null;
        this.console = null;
        this.outStream = outStream;
    }

    @Override
    public void run() {
        int nextChar;
        try {
            while ((nextChar = stream.read()) != -1 && !isShuttingDown.get()) {
                final char c = (char) nextChar;
                if (buffer != null) {
                    buffer.append(c);
                } else if (outStream != null) {
                    outStream.write(c);
                    outStream.flush();
                } else {
                    synchronized (console) {
                        console.print(c + "");
                    }
                }
            }
        } catch (Exception e) {
            // Do nothing
            JCommander.getConsole().println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        this.isShuttingDown.set(true);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }
}