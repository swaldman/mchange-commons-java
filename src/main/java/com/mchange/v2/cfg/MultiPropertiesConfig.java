/*
 * Distributed as part of mchange-commons-java 0.2.7
 *
 * Copyright (C) 2014 Machinery For Change, Inc.
 *
 * Author: Steve Waldman <swaldman@mchange.com>
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of EITHER:
 *
 *     1) The GNU Lesser General Public License (LGPL), version 2.1, as 
 *        published by the Free Software Foundation
 *
 * OR
 *
 *     2) The Eclipse Public License (EPL), version 1.0
 *
 * You may choose which license to accept if you wish to redistribute
 * or modify this work. You may offer derivatives of this work
 * under the license you have chosen, or you may provide the same
 * choice of license which you have been offered here.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received copies of both LGPL v2.1 and EPL v1.0
 * along with this software; see the files LICENSE-EPL and LICENSE-LGPL.
 * If not, the text of these licenses are currently available at
 *
 * LGPL v2.1: http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  EPL v1.0: http://www.eclipse.org/org/documents/epl-v10.php 
 * 
 */

package com.mchange.v2.cfg;

import java.util.List;
import java.util.Properties;

/**
 * MultiPropertiesConfig allows applications to accept configuration data
 * from a more than one property file (each of which is to be loaded from
 * a unique path using this class' ClassLoader's resource-loading mechanism),
 * and permits access to property data via the resource path from which the
 * properties were loaded, via the prefix of the property (where hierarchical 
 * property names are presumed to be '.'-separated), and simply by key.
 * In the by-key and by-prefix indices, when two definitions conflict, the
 * key value pairing specified in the MOST RECENT properties file shadows
 * earlier definitions, and files are loaded in the order of the list of
 * resource paths provided a constructor.
 *
 * The resource path "/" is a special case that always refers to System
 * properties. No actual resource will be loaded.
 *
 * If the mchange-hocon-bridge jar file is available, resource paths specified
 * as "hocon:/path/to/resource" will be parsed as 
 * <a href="https://github.com/typesafehub/config/blob/master/HOCON.md">HOCON</a>,
 * whenever values can be interpreted as Strings. 
 *
 * The class manages a special instance called "vmConfig" which is accessable
 * via a static method. It's resource path is list specified by a text-file,
 * itself a ClassLoader managed resource, which may be located at
 * <tt>/com/mchange/v2/cfg/vmConfigResourcePaths.txt</tt> or <tt>/mchange-config-resource-paths.txt</tt>.
 * This file should
 * be one resource path per line, with blank lines ignored and lines beginning
 * with '#' treated as comments.
 *
 * If no text file of resource paths are available, the following resources are
 * checked: "/mchange-commons.properties", "hocon:/reference,/application,/", "/"
 *
 * See {@link com/mchange/v3/hocon/HoconPropertiesConfigSource.java} for information
 * on HOCON identifiers.
 */
public abstract class MultiPropertiesConfig implements PropertiesConfig
{
    /**
     * @deprecated Please use the MConfig facade class to acquire configuration
     */
    public static MultiPropertiesConfig readVmConfig(String[] defaultResources, String[] preemptingResources )
    { return ConfigUtils.readVmConfig( defaultResources, preemptingResources ); }

    /**
     * @deprecated Please use the MConfig facade class to acquire configuration
     */
    public static MultiPropertiesConfig readVmConfig()
    { return ConfigUtils.readVmConfig(); }

    public abstract String[] getPropertiesResourcePaths();

    public abstract Properties getPropertiesByResourcePath(String path);


    /**
     *  The special prefix "" returns all the Properties
     */
    public abstract Properties getPropertiesByPrefix(String pfx);

//    public abstract Properties getProperties( String key );

    public abstract String getProperty( String key );

    public abstract List getDelayedLogItems();
}
