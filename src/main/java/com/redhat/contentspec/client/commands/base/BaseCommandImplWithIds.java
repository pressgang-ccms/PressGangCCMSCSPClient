package com.redhat.contentspec.client.commands.base;

import java.util.List;

import com.beust.jcommander.JCommander;
import com.redhat.contentspec.client.config.ClientConfiguration;
import com.redhat.contentspec.client.config.ContentSpecConfiguration;
import com.redhat.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.utils.common.CollectionUtilities;

public abstract class BaseCommandImplWithIds extends BaseCommandImpl {

    public BaseCommandImplWithIds(JCommander parser, ContentSpecConfiguration cspConfig, ClientConfiguration clientConfig) {
        super(parser, cspConfig, clientConfig);
    }

    public abstract List<Integer> getIds();
    public abstract void setIds(List<Integer> ids);

    /**
     * Prepare the command for processing.
     */
    protected void prepare() {
        // If there are no ids then use the csprocessor.cfg file
        if (loadFromCSProcessorCfg()) {
            // Check that the config details are valid
            if (getCspConfig() != null && getCspConfig().getContentSpecId() != null) {
                setIds(CollectionUtilities.toArrayList(getCspConfig().getContentSpecId()));
            }
        }

        // Check that one and only one ID exists
        if (getIds().size() == 0) {
            printError(Constants.ERROR_NO_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        } else if (getIds().size() > 1) {
            printError(Constants.ERROR_MULTIPLE_ID_MSG, false);
            shutdown(Constants.EXIT_ARGUMENT_ERROR);
        }
    }
}
