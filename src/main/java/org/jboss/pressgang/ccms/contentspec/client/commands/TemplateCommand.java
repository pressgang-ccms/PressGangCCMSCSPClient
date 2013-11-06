package org.jboss.pressgang.ccms.contentspec.client.commands;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jboss.pressgang.ccms.contentspec.client.commands.base.BaseCommandImpl;
import org.jboss.pressgang.ccms.contentspec.client.config.ClientConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.config.ContentSpecConfiguration;
import org.jboss.pressgang.ccms.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.contentspec.client.converter.FileConverter;
import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.constants.TemplateConstants;
import org.jboss.pressgang.ccms.utils.common.FileUtilities;

@Parameters(resourceBundle = "commands", commandDescriptionKey = "TEMPLATE")
public class TemplateCommand extends BaseCommandImpl {
    @Parameter(names = Constants.COMMENTED_LONG_PARAM, description = "Get the fully commented template")
    private Boolean commented = false;

    @Parameter(names = {Constants.OUTPUT_LONG_PARAM, Constants.OUTPUT_SHORT_PARAM}, descriptionKey = "OUTPUT", metaVar = "<FILE>",
            converter = FileConverter.class)
    private File output;

    public TemplateCommand(final JCommander parser, final ContentSpecConfiguration cspConfig, final ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    @Override
    public String getCommandName() {
        return Constants.TEMPLATE_COMMAND_NAME;
    }

    public Boolean getCommented() {
        return commented;
    }

    public void setCommented(final Boolean commented) {
        this.commented = commented;
    }

    public File getOutput() {
        return output;
    }

    public void setOutput(File output) {
        this.output = output;
    }

    @Override
    public void process() {
        final String template = commented ? TemplateConstants.FULLY_COMMENTED_TEMPLATE : TemplateConstants.EMPTY_TEMPLATE;

        // Save or print the data
        if (output == null) {
            JCommander.getConsole().println(template);
        } else {
            // Make sure the directories exist
            if (output.isDirectory()) {
                // TODO check that the directory was created
                output.mkdirs();
                output = new File(output.getAbsolutePath() + File.separator + "template." + Constants.FILENAME_EXTENSION);
            } else if (output.getParentFile() != null) {
                // TODO check that the directory was created
                output.getParentFile().mkdirs();
            }

            // Good point to check for a shutdown
            allowShutdownToContinueIfRequested();

            // Create and write to the file
            try {
                FileUtilities.saveFile(output, template, Constants.FILE_ENCODING);
                JCommander.getConsole().println(ClientUtilities.getMessage("OUTPUT_SAVED_MSG", output.getAbsolutePath()));
            } catch (IOException e) {
                printErrorAndShutdown(Constants.EXIT_FAILURE, ClientUtilities.getMessage("ERROR_FAILED_SAVING_MSG"), false);
            }
        }
    }

    @Override
    public boolean loadFromCSProcessorCfg() {
        // Doesn't need an ID so no point in loading from csprocessor.cfg
        return false;
    }

    @Override
    public boolean requiresExternalConnection() {
        return false;
    }
}
