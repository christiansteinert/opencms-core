/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/publish/CmsPublishException.java,v $
 * Date   : $Date: 2009/08/20 11:31:52 $
 * Version: $Revision: 1.6 $
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

package org.opencms.publish;

import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsException;

/**
 * Used to signal publish issues.<p>
 * 
 * @author Michael Moossen 
 * 
 * @version $Revision: 1.6 $
 * 
 * @since 6.5.5
 */
public class CmsPublishException extends CmsException {

    /** Serial version UID required for safe serialization. */
    private static final long serialVersionUID = 3473723181669539061L;

    /**
     * Creates a new localized Exception.<p>
     * 
     * @param container the localized message container to use
     */
    public CmsPublishException(CmsMessageContainer container) {

        super(container);
    }

    /**
     * Creates a new localized Exception that also containes a root cause.<p>
     * 
     * @param container the localized message container to use
     * @param cause the Exception root cause
     */
    public CmsPublishException(CmsMessageContainer container, Throwable cause) {

        super(container, cause);
    }

    /**
     * @see org.opencms.main.CmsException#createException(org.opencms.i18n.CmsMessageContainer, java.lang.Throwable)
     */
    @Override
    public CmsException createException(CmsMessageContainer container, Throwable cause) {

        return new CmsPublishException(container, cause);
    }
}