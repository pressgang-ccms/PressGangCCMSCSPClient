package com.redhat.contentspec.client.entities;

import com.redhat.contentspec.client.constants.Constants;
import org.jboss.pressgang.ccms.rest.v1.constants.RESTv1Constants;
import org.jboss.pressgang.ccms.utils.common.VersionUtilities;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.interception.ClientExecutionContext;
import org.jboss.resteasy.spi.interception.ClientExecutionInterceptor;

public class RESTVersionDecorator implements ClientExecutionInterceptor {
    private final static String version = VersionUtilities.getAPIVersion(Constants.VERSION_PROPERTIES_FILENAME,
            Constants.VERSION_PROPERTY_NAME);

    @Override
    public ClientResponse execute(ClientExecutionContext ctx) throws Exception {
        if (version != null) {
            ctx.getRequest().getHeadersAsObjects().add(RESTv1Constants.X_CSP_VERSION_HEADER, version);
        }
        return ctx.proceed();
    }
}