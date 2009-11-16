/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/CmsXmlComplexTypeSequence.java,v $
 * Date   : $Date: 2009/08/21 15:09:43 $
 * Version: $Revision: 1.7 $
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

package org.opencms.xml;

import org.opencms.xml.types.I_CmsXmlSchemaType;

import java.util.List;

/**
 * Simple data structure to describe a type sequence in a XML schema.<p>
 * 
 * @author Alexander Kandzior 
 * 
 * @version $Revision: 1.7 $ 
 * 
 * @since 6.0.0 
 */
class CmsXmlComplexTypeSequence {

    /** Indicates if this type sequence has a language attribute. */
    private boolean m_hasLanguageAttribute;

    /** The name of the complex type sequence. */
    private String m_name;

    /** The type sequence elements. */
    private List<I_CmsXmlSchemaType> m_sequence;

    /**
     * Creates a new complex type sequence data structure.<p>
     * 
     * @param name the name of the sequence
     * @param sequence the type sequence element list
     * @param hasLanguageAttribute indicates if a "language" attribute is present
     */
    protected CmsXmlComplexTypeSequence(String name, List<I_CmsXmlSchemaType> sequence, boolean hasLanguageAttribute) {

        m_name = name;
        m_sequence = sequence;
        m_hasLanguageAttribute = hasLanguageAttribute;
    }

    /**
     * Returns the name of the sequence.<p>
     *
     * @return the name of the sequence
     */
    public String getName() {

        return m_name;
    }

    /**
     * Returns the type sequence element list.<p>
     *
     * @return the type sequence element list
     */
    public List<I_CmsXmlSchemaType> getSequence() {

        return m_sequence;
    }

    /**
     * Returns <code>true</code> if a "language" attribute is present in this sequence.<p>
     *
     * @return <code>true</code> if a "language" attribute is present in this sequence
     */
    public boolean hasLanguageAttribute() {

        return m_hasLanguageAttribute;
    }
}