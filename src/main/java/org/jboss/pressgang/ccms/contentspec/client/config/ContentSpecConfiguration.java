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

package org.jboss.pressgang.ccms.contentspec.client.config;

public class ContentSpecConfiguration {
    private String serverUrl = null;
    private Integer contentSpecId = null;
    private String rootOutputDir = null;
    private String kojiHubUrl = null;
    private String publishCommand = null;

    public Integer getContentSpecId() {
        return contentSpecId;
    }

    public void setContentSpecId(final Integer contentSpecId) {
        this.contentSpecId = contentSpecId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(final String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRootOutputDirectory() {
        return rootOutputDir;
    }

    public void setRootOutputDirectory(final String rootOutputDir) {
        this.rootOutputDir = rootOutputDir;
    }

    public String getKojiHubUrl() {
        return kojiHubUrl;
    }

    public void setKojiHubUrl(final String kojiHubUrl) {
        this.kojiHubUrl = kojiHubUrl;
    }

    public String getPublishCommand() {
        return publishCommand;
    }

    public void setPublishCommand(final String publishCommand) {
        this.publishCommand = publishCommand;
    }
}
