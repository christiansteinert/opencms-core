/*
 * File   : $Source: /alkacon/cvs/opencms/test/org/opencms/main/CmsTestEventManager.java,v $
 * Date   : $Date: 2009/06/04 14:35:29 $
 * Version: $Revision: 1.5 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2009 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
 
package org.opencms.main;

/**
 * Simple custom event manager implementation for testing purposes.<p>
 */
public class CmsTestEventManager extends CmsEventManager {

    /**
     * Simple constructor with an output message for testing.<p>
     */
    public CmsTestEventManager() {
    
        super();        
        System.err.println("Initializing " + getClass().getName());
    }
}