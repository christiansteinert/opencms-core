/*
* File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/workplace/Attic/CmsAdminPicGalleries.java,v $
* Date   : $Date: 2005/06/27 23:22:07 $
* Version: $Revision: 1.5 $
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

import org.opencms.db.CmsImportFolder;
import org.opencms.file.CmsFile;
import org.opencms.file.CmsFolder;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsResource;
import org.opencms.file.types.CmsResourceTypeFolder;
import org.opencms.file.types.CmsResourceTypeImage;
import org.opencms.main.CmsException;
import org.opencms.main.OpenCms;
import org.opencms.workplace.CmsWorkplace;

import com.opencms.core.I_CmsSession;
import com.opencms.legacy.CmsLegacyException;
import com.opencms.legacy.CmsXmlTemplateLoader;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Template Class for administration of picture galleries
 * <p>
 *
 * @author Mario Stanke
 * @version $Revision: 1.5 $ $Date: 2005/06/27 23:22:07 $
 * @see com.opencms.workplace.CmsXmlWpTemplateFile
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */

public class CmsAdminPicGalleries extends CmsAdminGallery {
    
    /**
     * This method must be implemented by all galleries. 
     * It must return the path to the gallery root folder.<p>
     */ 
    public String getGalleryPath() {
        return CmsWorkplaceDefault.C_VFS_GALLERY_PICS;
    }
    
    /**
     * This method must return the path to the gallery icon.<p>
     * 
     * The gallery image is displayed in the list of available galleries.
     * 
     * @param cms The current CmsObject
     * @return The path to the gallery icon
     * @throws CmsException In case of problem accessing system resources
     */ 
    public String getGalleryIconPath(CmsObject cms) throws CmsException {
        CmsXmlWpConfigFile config = this.getConfigFile(cms);        
        return config.getWpPicturePath() + "ic_file_picgallery.gif";
    }     
    
    /**
     * Default XMLTemplate method called to build the output,
     *
     * @param cms CmsObject for accessing system resources
     * @param templateFile Filename of the template file
     * @param elementName Element name of this template in our parent template
     * @param parameters Hashtable with all template class parameters
     * @param templateSelector template section that should be processed
     * 
     * @return A HTML String converted to bytes that contains the generated output
     */
    public byte[] getContent(CmsObject cms, String templateFile, String elementName,
            Hashtable parameters, String templateSelector) throws CmsException {
                
        I_CmsSession session = CmsXmlTemplateLoader.getSession(cms.getRequestContext(), true);
        CmsXmlWpTemplateFile xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms,
                templateFile, elementName, parameters, templateSelector);
                
        // Get the URL to which we need to return when we're done
        String lasturl = getLastUrl(cms, parameters);    
        
        // Check if this is the inital call to the page
        getInitial(session, parameters)                  ;
                
        // Get the folder for the gallery
        String foldername = getGalleryPath(cms, session, parameters);        
        CmsFolder thefolder = cms.readFolder(foldername);  
        
        // get the file size upload limitation value (value is in kB)
        int maxFileSize = OpenCms.getWorkplaceManager().getFileMaxUploadSize();                          
        // check if current user belongs to Admin group, if so no file upload limit
        if ((maxFileSize <= 0) || cms.userInGroup(cms.getRequestContext().currentUser().getName(), OpenCms.getDefaultUsers().getGroupAdministrators())) {
            maxFileSize = -1;
            xmlTemplateDocument.setData("limitation", "");
        } 
        else {
            xmlTemplateDocument.setData("maxfilesize", "" + maxFileSize);
            try {
                String limitation = xmlTemplateDocument.getProcessedDataValue("filesize_limited");
                xmlTemplateDocument.setData("limitation", limitation);
            } catch (CmsException e) {
                xmlTemplateDocument.setData("limitation", "");
            }
        }
               
        // Check if we must redirect to head_1
        if(foldername.equals(CmsWorkplaceDefault.C_VFS_GALLERY_PICS) && templateFile.endsWith("administration_head_picgalleries2")) {
            // we are in the wrong head - use the first one
            xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, CmsWorkplace.VFS_PATH_WORKPLACE + "administration/picgallery/administration_head_picgalleries1", elementName, parameters, templateSelector);
        }        

        // Check if we must redirect to head_2
        try {
            String parent = CmsResource.getParentFolder(cms.getSitePath(thefolder));
            if(foldername.startsWith(CmsWorkplaceDefault.C_VFS_GALLERY_PICS) && (parent.equals(CmsWorkplaceDefault.C_VFS_GALLERY_PICS)) && templateFile.endsWith("administration_head_picgalleries1")) {
                // we are in the wrong head - use the second one
                xmlTemplateDocument = (CmsXmlWpTemplateFile)getOwnTemplateFile(cms, CmsWorkplace.VFS_PATH_WORKPLACE + "administration/htmlgallery/administration_head_picgalleries2", elementName, parameters, templateSelector);
            }
        }
        catch(Exception e) {}                        
        
        // Now read further parameters        
        String action = (String)parameters.get("action");
        String unzip = (String) parameters.get("unzip");                          
        String newname = (String)parameters.get(CmsWorkplaceDefault.C_PARA_NAME);
        String title = (String)parameters.get("TITLE"); // both for gallery and upload file
        String step = (String)parameters.get("step");
        String imagedescription = (String)parameters.get("DESCRIPTION");
        
        if(foldername == null) {
            foldername = "";
        }
        if("new".equals(action)) {
            String galleryname = (String)parameters.get("NAME");
            String group = (String)parameters.get("GROUP");
            if(galleryname != null && group != null && galleryname != "" && group != "") {
//                boolean read = parameters.get("READ") != null;
//                boolean write = parameters.get("WRITE") != null;
                try {

                    // create the folder

                    // get the path from the workplace.ini
                    String superfolder = getConfigFile(cms).getPicGalleryPath();
                    CmsResource folder = cms.createResource(superfolder + galleryname, CmsResourceTypeFolder.RESOURCE_TYPE_ID);
                    if(title != null) {
                        cms.writeProperty(cms.getSitePath(folder), CmsPropertyDefinition.PROPERTY_TITLE, title);
                    }
//                  TODO: check how to set the appropriate access using acl
                    /*
                    cms.chgrp(cms.readAbsolutePath(folder), group);
                    int flag = folder.getAccessFlags();

                    // set the access rights for 'other' users
                    if(read != ((flag & C_ACCESS_PUBLIC_READ) != 0)) {
                        flag ^= C_ACCESS_PUBLIC_READ;
                    }
                    if(write != ((flag & C_ACCESS_PUBLIC_WRITE) != 0)) {
                        flag ^= C_ACCESS_PUBLIC_WRITE;
                    }
                    if((flag & C_ACCESS_GROUP_READ) == 0){
                        flag ^= C_ACCESS_GROUP_READ;
                    }
                    if((flag & C_ACCESS_GROUP_WRITE) == 0){
                        flag ^= C_ACCESS_GROUP_WRITE;
                    }
                    if((flag & C_ACCESS_GROUP_VISIBLE) == 0){
                        flag ^= C_ACCESS_GROUP_VISIBLE;
                    }
                    if((flag & C_PERMISSION_READ ) == 0){
                        flag ^= C_PERMISSION_READ;
                    }
                    if((flag & C_PERMISSION_WRITE) == 0){
                        flag ^= C_PERMISSION_WRITE;
                    }
                    if((flag & C_PERMISSION_VIEW) == 0){
                        flag ^= C_PERMISSION_VIEW;
                    }
                    if((flag & C_ACCESS_PUBLIC_VISIBLE) == 0){
                        flag ^= C_ACCESS_PUBLIC_VISIBLE;
                    }
                    cms.chmod(cms.readAbsolutePath(folder), flag);
                    */
                    cms.unlockResource(cms.getSitePath(folder));
                }
                catch(CmsException ex) {
                    xmlTemplateDocument.setData("ERRORDETAILS", CmsException.getStackTraceAsString(ex));
                    templateSelector = "error";
                }
            }
            else {
                templateSelector = "datamissing";
            }
        }
        else {
            
            if("upload".equals(action)) {

                // get filename and file content if available
                String filename = null;
                byte[] filecontent = new byte[0];

                // get the filename
                Enumeration files = CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getFileNames();
                while(files.hasMoreElements()) {
                    filename = (String)files.nextElement();
                }
                if(filename != null) {
                    session.putValue(CmsWorkplaceDefault.C_PARA_RESOURCE, filename);
                }
                filename = (String)session.getValue(CmsWorkplaceDefault.C_PARA_RESOURCE);

                // get the filecontent
                if(filename != null) {
                    filecontent = CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getFile(filename);
                }
                if(filecontent != null) {
                    session.putValue(CmsWorkplaceDefault.C_PARA_FILECONTENT, filecontent);
                }
                filecontent = (byte[])session.getValue(CmsWorkplaceDefault.C_PARA_FILECONTENT);
                if("0".equals(step)) {
                    templateSelector = "";
                }
                else {
                    if("1".equals(step)) {

                        // display the select filetype screen
                        if(filename != null) {

                            // check if the file size is 0
                            if(filecontent.length == 0) {
                                templateSelector = "error";
                                xmlTemplateDocument.setData("details", filename);
                            }
                            
                            // check if the file size is larger than the maximum allowed upload size 
                            else if ((maxFileSize > 0) && (filecontent.length > (maxFileSize * 1024))) {
                                templateSelector = "errorfilesize";
                                xmlTemplateDocument.setData("details", filename+": "+(filecontent.length/1024)+" kb, max. "+maxFileSize+" kb.");
                            }
                                             
                            else {
                                if(unzip != null) {
                                    // try to unzip the file here ...
                                    CmsImportFolder zip = new CmsImportFolder(
                                        filecontent, foldername, cms, true);
                                    if( zip.isValidZipFile() ) {

                                    // remove the values form the session
                                    session.removeValue(CmsWorkplaceDefault.C_PARA_RESOURCE);
                                    session.removeValue(CmsWorkplaceDefault.C_PARA_FILECONTENT);
                                    session.removeValue(CmsWorkplaceDefault.C_PARA_NEWTYPE);
                                    session.removeValue("unzip");
                                    // return to the filelist
                                    try {
                                        if((lasturl != null) && (lasturl != "")) {
                                            CmsXmlTemplateLoader.getResponse(cms.getRequestContext()).sendRedirect(lasturl);
                                        }
                                        else {
                                            CmsXmlTemplateLoader.getResponse(cms.getRequestContext()).sendCmsRedirect(
                                                getConfigFile(cms).getWorkplaceActionPath() + CmsWorkplaceAction.getExplorerFileUri(CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getOriginalRequest()));
                                        }
                                    } catch(Exception ex) {
                                        throw new CmsLegacyException(
                                            "Redirect fails :" + getConfigFile(cms).getWorkplaceActionPath()
                                            + CmsWorkplaceAction.getExplorerFileUri(CmsXmlTemplateLoader.getRequest(cms.getRequestContext()).getOriginalRequest()), ex);
                                    }
                                    return null;
                                    }
                                } // else, zip was not valid, so continue ...
                                xmlTemplateDocument.setData("MIME", filename);
                                xmlTemplateDocument.setData("SIZE", "Not yet available");
                                xmlTemplateDocument.setData("FILESIZE",
                                        new Integer(filecontent.length).toString() + " Bytes");
                                xmlTemplateDocument.setData("FILENAME", filename);
                                xmlTemplateDocument.setData("IMAGEDESCRIPTION",imagedescription);
                                templateSelector = "step1";
                            }
                        }
                    }
                    else {
                        if("2".equals(step)) {

                            // check if a new filename is given
                            if(newname != null) {
                                filename = newname;
                            }
                            try {
                                CmsFile file = (CmsFile)cms.createResource(foldername + filename, CmsResourceTypeImage.getStaticTypeId(), filecontent, null);
                                if(title != null) {
                                    String filepath = cms.getSitePath(file);
                                    cms.writeProperty(filepath, CmsPropertyDefinition.PROPERTY_TITLE, title);
                                }
                                if(imagedescription != null) {
                                    String filepath = cms.getSitePath(file);
                                    cms.writeProperty(filepath, CmsPropertyDefinition.PROPERTY_DESCRIPTION, imagedescription);
                                }
                            }
                            catch(CmsException ex) {
                                xmlTemplateDocument.setData("details", CmsException.getStackTraceAsString(ex));
                                templateSelector = "error";
                                xmlTemplateDocument.setData("link_value", foldername);
                                xmlTemplateDocument.setData("lasturl", lasturl);
                                return startProcessing(cms, xmlTemplateDocument, elementName,
                                        parameters, templateSelector);
                            }
                            try {
                                CmsXmlTemplateLoader.getResponse(cms.getRequestContext()).sendRedirect(lasturl);
                            }
                            catch(Exception ex) {
                                throw new CmsLegacyException("Redirect fails :" + getConfigFile(cms).getWorkplaceActionPath() +
                                        lasturl, ex);
                            }
                            return null;
                        }
                    }
                }
            }
        }
        
        xmlTemplateDocument.setData("link_value", foldername);
        xmlTemplateDocument.setData("lasturl", lasturl);
        xmlTemplateDocument.setData("galleryRootFolder", CmsWorkplaceDefault.C_VFS_GALLERY_PICS);        

        // Finally start the processing
        return startProcessing(cms, xmlTemplateDocument, elementName, parameters,
                templateSelector);
    }
}