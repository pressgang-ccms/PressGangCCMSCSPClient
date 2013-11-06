package org.jboss.pressgang.ccms.contentspec.client.processor;

import org.jboss.pressgang.ccms.contentspec.client.utils.ClientUtilities;
import org.jboss.pressgang.ccms.contentspec.processor.ContentSpecProcessor;
import org.jboss.pressgang.ccms.contentspec.processor.structures.ProcessingOptions;
import org.jboss.pressgang.ccms.contentspec.utils.logging.ErrorLoggerManager;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;

public class ClientContentSpecProcessor extends ContentSpecProcessor {
    private final RESTProviderFactory factory;

    /**
     * Constructor
     *
     * @param factory           A DBManager object that manages the REST connection and the functions to read/write to the REST Interface.
     * @param loggerManager
     * @param processingOptions The set of options to use when processing.
     */
    public ClientContentSpecProcessor(final RESTProviderFactory factory, final ErrorLoggerManager loggerManager,
            final ProcessingOptions processingOptions) {
        super(factory, loggerManager, processingOptions);
        this.factory = factory;
    }

    @Override
    protected boolean doSecondValidationPass(final ProcessorData processorData) {
        // Attempt to download all the topic data in one request
        ClientUtilities.downloadAllTopics(factory, processorData.getContentSpec(), null);

        return super.doSecondValidationPass(processorData);
    }
}
