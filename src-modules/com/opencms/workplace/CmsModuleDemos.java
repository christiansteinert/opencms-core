/*
* File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/workplace/Attic/CmsModuleDemos.java,v $
* Date   : $Date: 2005/06/27 23:22:07 $
* Version: $Revision: 1.3 $
*
* This library is part of OpenCms -
* the Open Source Content Mananagement System
*
* Copyright (C) 2001  The OpenCms Group
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* For further information about OpenCms, please see the
* OpenCms Website: http://www.opencms.org 
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package com.opencms.workplace;

import org.opencms.file.CmsObject;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsWorkplace;

import com.opencms.legacy.CmsXmlTemplateLoader;
import com.opencms.template.CmsXmlTemplateFile;

import java.util.Hashtable;
import java.util.Iterator;

/**
 * Template class for displaying OpenCms workplace administration module
 *
 * Creation date: (30.08.00 17:59:38)
 * @author Hanjo Riege
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public class CmsModuleDemos extends CmsWorkplaceDefault {

    /**
     * Gets the content of a defined section in a given template file and its subtemplates
     * with the given parameters.
     *
     * @see #getContent(CmsObject, String, String, Hashtable, String)
     * @param cms CmsObject Object for accessing system resources.
     * @param templateFile Filename of the template file.
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) throws CmsException {
        if(CmsLog.getLog(this).isDebugEnabled() && C_DEBUG) {
            CmsLog.getLog(this).debug("Getting content of element " + ((elementName==null)?"<root>":elementName));
            CmsLog.getLog(this).debug("Template file is: " + templateFile);
            CmsLog.getLog(this).debug("Selected template section is: " + ((templateSelector==null)?"<default>":templateSelector));
        }
        CmsXmlTemplateFile templateDocument = getOwnTemplateFile(cms, templateFile, elementName, parameters, templateSelector);
        String currentname;
        Iterator modules = OpenCms.getModuleManager().getModuleNames().iterator();
        String completeList = "";
        String servPath = CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getServletUrl();
        while(modules.hasNext()) {
            String name = (String)modules.next();
            String nicename = OpenCms.getModuleManager().getModule(name).getNiceName();
            if(nicename == null || nicename.equals("")) {
                currentname = name;
            }
            else {
                currentname = nicename;
            }
            templateDocument.setData("name", currentname);
            templateDocument.setData("namelink", servPath + "/moduledemos/" + name + "/index.html");
            completeList += templateDocument.getProcessedDataValue("list_entry");
            templateDocument.setData("doc_name", currentname);
            templateDocument.setData("doc_link", servPath + CmsWorkplace.VFS_PATH_MODULES + name + "/doc/index.html");
            completeList += templateDocument.getProcessedDataValue("doc_entry");
        }
        templateDocument.setData("list", completeList);

        // Now load the template file and start the processing
        return startProcessing(cms, templateDocument, elementName, parameters, templateSelector);
    }

    /**
     * Indicates if the results of this class are cacheable.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template.
     * @param parameters Hashtable with all template class parameters.
     * @param templateSelector template section that should be processed.
     * @return <EM>true</EM> if cacheable, <EM>false</EM> otherwise.
     */
    public boolean isCacheable(CmsObject cms, String templateFile, String elementName, Hashtable parameters, String templateSelector) {
        return false;
    }
}