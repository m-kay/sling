/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.core;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.SlingLaunchpadServer;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.ServerCore;

public abstract class ServerUtil {
    
    public static Repository getDefaultRepository(IProject project) {
        IServer server = getDefaultServer(project);
        try {
            return getRepository(server, new NullProgressMonitor());
        } catch (CoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static IServer getDefaultServer(IProject project) {
        IModule module = org.eclipse.wst.server.core.ServerUtil.getModule(project);
        if (module==null) {
            // if there's no module for a project then there's no IServer for sure - which 
            // is what we need to create a RepositoryInfo
            return null;
        }
        IServer server = ServerCore.getDefaultServer(module);
        if (server!=null) {
            return server;
        }
        // then we cannot create a repository
        IServer[] allServers = ServerCore.getServers();
        out: for (int i = 0; i < allServers.length; i++) {
            IServer aServer = allServers[i];
            IModule[] allModules = aServer.getModules();
            for (int j = 0; j < allModules.length; j++) {
                IModule aMoudle = allModules[j];
                if (aMoudle.equals(module)) {
                    server = aServer;
                    break out;
                }
            }
        }
        return server;
    }

    public static Repository getRepository(IServer server, IProgressMonitor monitor) throws CoreException {
        RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
        try {
            RepositoryInfo repositoryInfo = getRepositoryInfo(server, monitor);
            return repository.getRepository(repositoryInfo);
        } catch (URISyntaxException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        } catch (RuntimeException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        } catch (RepositoryException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    public static void stopRepository(IServer server, IProgressMonitor monitor) throws CoreException {
        RepositoryFactory repository = Activator.getDefault().getRepositoryFactory();
        try {
            RepositoryInfo repositoryInfo = getRepositoryInfo(server, monitor);
            repository.stopRepository(repositoryInfo);
        } catch (URISyntaxException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        } catch (RuntimeException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
    }
    
    
    public static RepositoryInfo getRepositoryInfo(IServer server, IProgressMonitor monitor) throws URISyntaxException {

        ISlingLaunchpadServer launchpadServer = (ISlingLaunchpadServer) server.loadAdapter(SlingLaunchpadServer.class,
                monitor);

        ISlingLaunchpadConfiguration configuration = launchpadServer.getConfiguration();

        // TODO configurable scheme?
        URI uri = new URI("http", null, server.getHost(), configuration.getPort(), configuration.getContextPath(),
                null, null);
        return new RepositoryInfo(configuration.getUsername(),
                configuration.getPassword(), uri.toString());
    }

    private ServerUtil() {

    }
}