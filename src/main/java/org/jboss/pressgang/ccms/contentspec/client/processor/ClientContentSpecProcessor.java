/*
 * Copyright 2011-2014 Red Hat, Inc.
 *
 * This file is part of PressGang CCMS.
 *
 * PressGang CCMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PressGang CCMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PressGang CCMS. If not, see <http://www.gnu.org/licenses/>.
 */

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
