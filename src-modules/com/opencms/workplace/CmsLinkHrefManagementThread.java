/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/workplace/Attic/CmsLinkHrefManagementThread.java,v $
 * Date   : $Date: 2005/06/27 23:22:07 $
 * Version: $Revision: 1.3 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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

package com.opencms.workplace;

import org.opencms.file.CmsObject;
import org.opencms.report.A_CmsReportThread;

/**
 * Checks the validity of internal href tags (i.e. html links) on a set of resources.<p>
 * 
 * @author Alexander Kandzior 
 * 
 * @version $Revision: 1.3 $
 * @since 5.1.10
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public class CmsLinkHrefManagementThread extends A_CmsReportThread {

    /**
     * Checks the validity of internal href tags (i.e. html links) on a set of resources.<p>
     * 
     * @param cms the current OpenCms context object
     */
    public CmsLinkHrefManagementThread(CmsObject cms) {
        super(cms, "OpenCms: Href link management");
        // NYI
    }

    /**
     * Returns true if broken links where found during the link check.<p>
     *  
     * @return true if broken links where found during the link check
     */
    public boolean brokenLinksFound() {
        // NYI
        return false;
    }

    /**
     * @see org.opencms.report.A_CmsReportThread#getReportUpdate()
     */
    public String getReportUpdate() {
        // NYI
        return "";
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        // NYI
    }
}