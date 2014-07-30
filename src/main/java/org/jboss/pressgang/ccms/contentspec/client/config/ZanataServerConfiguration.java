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

public class ZanataServerConfiguration {
    private String name;
    private String url;
    private String username;
    private String token;
    private boolean cache;

    public ZanataServerConfiguration() {

    }

    public ZanataServerConfiguration(final String name, final String url) {
        this.name = name;
        this.url = url;
    }

    public ZanataServerConfiguration(final String name, final String url, final String username, final String token) {
        this.name = name;
        this.url = url;
        this.username = username;
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
    }

    public boolean useCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }
}
