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

import java.util.HashMap;
import java.util.Map;

import org.jboss.pressgang.ccms.contentspec.client.entities.ConfigDefaults;

public class ClientConfiguration {
    private String rootDirectory = "";
    private String publicanBuildOptions = null;
    private String publicanPreviewFormat = null;
    private String publicanCommonContentDirectory = null;

    private String jDocbookBuildOptions = null;
    private String jDocbookPreviewFormat = null;

    private String kojiHubUrl = null;
    private String publishCommand = null;

    private String defaultZanataProject = null;
    private String defaultZanataVersion = null;

    private String editorCommand = null;
    private Boolean editorRequiresTerminal = false;

    private ConfigDefaults defaults =new ConfigDefaults();

    private Map<String, ServerConfiguration> servers = new HashMap<String, ServerConfiguration>();
    private Map<String, ZanataServerConfiguration> zanataServers = new HashMap<String, ZanataServerConfiguration>();

    private String installPath = null;

    public Map<String, ServerConfiguration> getServers() {
        return servers;
    }

    public void setServers(final Map<String, ServerConfiguration> servers) {
        this.servers = servers;
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(final String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public String getPublicanBuildOptions() {
        return publicanBuildOptions;
    }

    public void setPublicanBuildOptions(final String publicanBuildOptions) {
        this.publicanBuildOptions = publicanBuildOptions;
    }

    public String getPublicanPreviewFormat() {
        return publicanPreviewFormat;
    }

    public void setPublicanPreviewFormat(final String publicanPreviewFormat) {
        this.publicanPreviewFormat = publicanPreviewFormat;
    }

    public Map<String, ZanataServerConfiguration> getZanataServers() {
        return zanataServers;
    }

    public void setZanataServers(Map<String, ZanataServerConfiguration> zanataServers) {
        this.zanataServers = zanataServers;
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

    public String getDefaultZanataVersion() {
        return defaultZanataVersion;
    }

    public void setDefaultZanataVersion(final String defaultZanataVersion) {
        this.defaultZanataVersion = defaultZanataVersion;
    }

    public String getDefaultZanataProject() {
        return defaultZanataProject;
    }

    public void setDefaultZanataProject(final String defaultZanataProject) {
        this.defaultZanataProject = defaultZanataProject;
    }

    public String getPublicanCommonContentDirectory() {
        return publicanCommonContentDirectory;
    }

    public void setPublicanCommonContentDirectory(final String publicanCommonContentDirectory) {
        this.publicanCommonContentDirectory = publicanCommonContentDirectory;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public ConfigDefaults getDefaults() {
        return defaults;
    }

    public void setDefaults(ConfigDefaults defaults) {
        this.defaults = defaults;
    }

    public String getjDocbookBuildOptions() {
        return jDocbookBuildOptions;
    }

    public void setjDocbookBuildOptions(String jDocbookBuildOptions) {
        this.jDocbookBuildOptions = jDocbookBuildOptions;
    }

    public String getjDocbookPreviewFormat() {
        return jDocbookPreviewFormat;
    }

    public void setjDocbookPreviewFormat(String jDocbookPreviewFormat) {
        this.jDocbookPreviewFormat = jDocbookPreviewFormat;
    }

    public String getEditorCommand() {
        return editorCommand;
    }

    public void setEditorCommand(String editorCommand) {
        this.editorCommand = editorCommand;
    }

    public Boolean getEditorRequiresTerminal() {
        return editorRequiresTerminal;
    }

    public void setEditorRequiresTerminal(Boolean editorRequiresTerminal) {
        this.editorRequiresTerminal = editorRequiresTerminal;
    }
}
