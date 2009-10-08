/*
* File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/workplace/Attic/CmsXmlLanguageFileContent.java,v $
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

import org.opencms.file.CmsFile;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.workplace.CmsWorkplace;

import com.opencms.template.A_CmsXmlContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides backward compatibility with pre 5.0 XML-style localization.<p>
 * 
 * The use of this class is <em>deprecated</em> and it is provided only to make old modules
 * work without modifications. It is suggested that you modify you modules 
 * to use the 5.0 style ResourceBundle approach, because of performance issues.<p>
 * 
 * Support for XML-style locales will be removed in a future release.<p> 
 *
 * @author Alexander Lucas
 * @version $Revision: 1.3 $ $Date: 2005/06/27 23:22:07 $
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public class CmsXmlLanguageFileContent extends A_CmsXmlContent {

    /**
     * Constructor for creating a new language file object containing the content
     * of the corresponding system language file for the actual user.<p<
     * 
     * The position of the language file will be looked up in workplace.ini.
     * The selected language of the current user can be searched in the user object.
     *
     * @param cms CmsObject object for accessing system resources.
     * @param locale name of the locale to initialize
     */
    public CmsXmlLanguageFileContent(CmsObject cms, String locale) throws CmsException {
        super();
        try {
            mergeLanguageFiles(cms, locale);
        }
        catch(Exception e) {
            throwException("Error while merging language files in folder " + CmsWorkplace.VFS_PATH_LOCALES + locale + "/.");
        }
    }

    /**
     * Gets a description of this content type.
     * @return Content type description.
     */
    public String getContentDescription() {
        return "Language definition file";
    }

    /**
     * Gets the language value vor the requested tag.
     * @param tag requested tag.
     * @return Language value.
     */
    public String getLanguageValue(String tag) throws CmsException {
        String result = null;
        if(hasData(tag)) {
            result = getDataValue(tag);
        }
        return result;
    }

    /**
     * Gets the expected tagname for the XML documents of this content type
     * @return Expected XML tagname.
     */
    public String getXmlDocumentTagName() {
        return "LANGUAGE";
    }

    /**
     * Checks if there exists a language value vor the requested tag.
     * @param tag requested tag.
     * @return Language value.
     */
    public boolean hasLanguageValue(String tag) {
        return hasData(tag);
    }

    /**
     * Merges all language files available for current language settings.
     * Language files have to be stored in a folder like
     * "system/workplace/config/language/[language shortname]/"
     *
     * @author Matthias Schreiber
     * @param cms CmsObject object for accessing system resources.
     * @param language Current language
     */
    private void mergeLanguageFiles(CmsObject cms, String language) throws CmsException {
        List langFiles = (List) new ArrayList();

        // Make sure old "uk" stuff still works        
        if ("uk".equals(language)) language = "en";
        
        try {
            langFiles = cms.getFilesInFolder(CmsWorkplace.VFS_PATH_LOCALES + language + "/");
        } catch (CmsException e) {
            // noop
        }

        // get all modules-language Files
        List modules = (List) new ArrayList();
        modules = cms.getSubFolders(CmsWorkplace.VFS_PATH_MODULES);
        String lang = CmsWorkplaceDefault.C_VFS_DIR_LOCALES + language + "/";
        // make sure old modules language files still work
        String oldLang = "language/" + language + "/";
        for(int i = 0;i < modules.size();i++) {
            List moduleLangFiles = (List) new ArrayList();
            try {
                moduleLangFiles = cms.getFilesInFolder(cms.getSitePath((CmsFolder)modules.get(i)) + lang);
            } catch (CmsException e) {
                // try read from old module locales path
                try {
                    moduleLangFiles = cms.getFilesInFolder(cms.getSitePath((CmsFolder)modules.get(i)) + oldLang);
                    if(CmsLog.getLog(this).isWarnEnabled() ) {
                        CmsLog.getLog(this).warn("Old module 'locales' path used: " + cms.getSitePath((CmsFolder)modules.get(i)) + oldLang);
                    }                    
                } catch (CmsException ex) {
                    // no language files found, we can live with that, probably the module just has none                      
                }
            }
            for(int j = 0;j < moduleLangFiles.size();j++) {
                langFiles.add(moduleLangFiles.get(j));
            }
        }
        CmsFile file = null;
        for(int i = 0;i < langFiles.size();i++) {
            file = (CmsFile)langFiles.get(i);
            if(! file.getName().toLowerCase().endsWith(".txt") && file.getState() != CmsResource.STATE_DELETED) {
                try {
                    init(cms, cms.getSitePath(file));
                } catch(Exception exc) {
                    if(CmsLog.getLog(this).isErrorEnabled() ) {
                        CmsLog.getLog(this).error("Error merging language file: " + cms.getSitePath(file), exc);
                    }
                }
            }
        }
    }
}