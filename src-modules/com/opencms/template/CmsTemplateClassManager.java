/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/template/Attic/CmsTemplateClassManager.java,v $
 * Date   : $Date: 2005/05/17 13:47:32 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002  Alkacon Software (http://www.alkacon.com)
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

package com.opencms.template;

import org.opencms.main.CmsException;

import com.opencms.legacy.CmsLegacyException;

/**
 * Class for loading class instances, required by the legagy XmlTemplate mechanism.<p>
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public final class CmsTemplateClassManager {

    /**
     * Hides the public constructor.<p>
     */
    private CmsTemplateClassManager() {
        // empty constructor
    }
    
    /**
     * Creates an instance of the class with the given classname.<p>
     *
     * @param classname name of the class to create
     * @return an instance of the class with the given name
     * @throws CmsException if something goes wrong
     */
    public static Object getClassInstance(String classname) throws CmsException {        
        try {
            return Class.forName(classname).newInstance();
        } catch (Throwable t) {
            throw new CmsLegacyException("Could not create new instance of " + classname, CmsLegacyException.C_CLASSLOADER_ERROR, t);
        }
    }
}