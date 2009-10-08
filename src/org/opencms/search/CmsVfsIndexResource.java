/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/Attic/CmsVfsIndexResource.java,v $
 * Date   : $Date: 2005/07/28 15:53:10 $
 * Version: $Revision: 1.20 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (c) 2005 Alkacon Software GmbH (http://www.alkacon.com)
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
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.search;

import org.opencms.file.CmsResource;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.loader.CmsResourceManager;
import org.opencms.main.OpenCms;
import org.opencms.search.documents.A_CmsVfsDocument;

/**
 * Contains the data of a VFS Cms resource specified by a Lucene 
 * search result document.<p>
 * 
 * @author Carsten Weinholz 
 * @author Thomas Weckert  
 * 
 * @version $Revision: 1.20 $ 
 * 
 * @since 6.0.0 
 */
public class CmsVfsIndexResource extends A_CmsIndexResource {

    /** 
     * Creates a new instance to wrap the given <code>{@link CmsResource}</code>.<p>
     * 
     * @param res the data object
     */
    public CmsVfsIndexResource(CmsResource res) {

        m_data = res;
        m_id = res.getStructureId();
        m_name = res.getName();

        try {
            I_CmsResourceType resourceType = OpenCms.getResourceManager().getResourceType(res.getTypeId());
            m_type = resourceType.getTypeId();
        } catch (Exception exc) {
            m_type = res.getTypeId();
        }

        // default to plain text mime type if no matching file extension id found
        m_mimeType = OpenCms.getResourceManager().getMimeType(res.getName(), null, CmsResourceManager.MIMETYPE_TEXT);
        m_path = res.getRootPath();
    }

    /**
     * @see org.opencms.search.A_CmsIndexResource#getDocumentKey(boolean)
     */
    public String getDocumentKey(boolean withMimeType) {

        StringBuffer result = new StringBuffer(32);
        result.append(A_CmsVfsDocument.VFS_DOCUMENT_KEY_PREFIX);
        result.append(getType());
        if (withMimeType) {
            result.append(":");
            result.append(getMimetype());
        }
        return result.toString();
    }
}