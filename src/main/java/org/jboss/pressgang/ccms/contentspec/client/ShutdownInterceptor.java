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

package org.jboss.pressgang.ccms.contentspec.client;

import org.jboss.pressgang.ccms.contentspec.interfaces.ShutdownAbleApp;

/**
 * A class that allows a Shutdown able Application to have the shutdown intercepted and then shutdown using the applications shutdown
 * method();
 *
 * @author lnewson
 */
public class ShutdownInterceptor extends Thread {

    private final ShutdownAbleApp app;
    private final long maxWaitTime;

    public ShutdownInterceptor(ShutdownAbleApp app) {
        this.app = app;
        maxWaitTime = 5000;
    }

    public ShutdownInterceptor(ShutdownAbleApp app, long maxWaitTime) {
        this.app = app;
        this.maxWaitTime = maxWaitTime;
    }

    @Override
    public void run() {
        long shutdownTime = System.currentTimeMillis() + maxWaitTime;
        app.shutdown();
        while (!app.isShutdown() && System.currentTimeMillis() <= shutdownTime) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
