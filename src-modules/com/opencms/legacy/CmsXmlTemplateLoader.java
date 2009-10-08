/*
 * File   : $Source: /alkacon/cvs/opencms/src-modules/com/opencms/legacy/Attic/CmsXmlTemplateLoader.java,v $
 * Date   : $Date: 2006/09/15 10:38:14 $
 * Version: $Revision: 1.16 $
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

package com.opencms.legacy;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsPropertyDefinition;
import org.opencms.file.CmsRequestContext;
import org.opencms.file.CmsResource;
import org.opencms.flex.CmsFlexController;
import org.opencms.importexport.CmsCompatibleCheck;
import org.opencms.jsp.CmsJspTagInclude;
import org.opencms.loader.I_CmsLoaderIncludeExtension;
import org.opencms.loader.I_CmsResourceLoader;
import org.opencms.main.CmsEvent;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.I_CmsEventListener;
import org.opencms.main.OpenCms;
import org.opencms.staticexport.CmsLinkManager;
import org.opencms.workplace.CmsWorkplace;
import org.opencms.workplace.editors.CmsDefaultPageEditor;

import com.opencms.core.*;
import com.opencms.template.*;
import com.opencms.template.cache.*;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.ExtendedProperties;

/**
 * Implementation of the {@link I_CmsResourceLoader} for XMLTemplates.<p>
 * 
 * Parameters supported by this loader:<dl>
 * 
 * <dt>elementcache.enabled</dt>
 * <dd>(Optional) Controls if the legacy XMLTemplate element cache is disabled 
 * (the default) or enabled. Enable the element cache only to support old
 * XMLTemplate based code that depend on specific element cache behaviour.</dd>
 * 
 * <dt>elementcache.uri</dt>
 * <dd>(Optional) Element cache URI size. The default is 10000.</dd>
 * 
 * <dt>elementcache.elements</dt>
 * <dd>(Optional) Element cache element size. The default is 50000.</dd>
 * 
 * <dt>elementcache.variants</dt>
 * <dd>(Optional) Element cache variant size. The default is 100.</dd></dl>
 * 
 * @author  Alexander Kandzior (a.kandzior@alkacon.com)
 *
 * @version $Revision: 1.16 $
 * 
 * @deprecated Will not be supported past the OpenCms 6 release.
 */
public class CmsXmlTemplateLoader implements I_CmsResourceLoader, I_CmsLoaderIncludeExtension, I_CmsEventListener {

    /** URI of the bodyloader XML file in the OpenCms VFS. */
    public static final String C_BODYLOADER_URI = CmsWorkplace.VFS_PATH_SYSTEM + "shared/bodyloader.html";

    /** Magic element replace name. */
    public static final String C_ELEMENT_REPLACE = "_CMS_ELEMENTREPLACE";

    /** The id of this loader. */
    public static final int C_RESOURCE_LOADER_ID = 3;

    /** Flag for debugging output. Set to 9 for maximum verbosity. */
    private static final int DEBUG = 0;

    /** The element cache used for the online project. */
    private static CmsElementCache m_elementCache;

    /** The template cache that holds all cached templates. */
    private static I_CmsTemplateCache m_templateCache;

    /** The variant dependencies for the element cache. */
    private static Hashtable m_variantDeps;

    /** The resource loader configuration. */
    private Map m_configuration;

    /**
     * The constructor of the class is empty and does nothing.
     */
    public CmsXmlTemplateLoader() {

        OpenCms.addCmsEventListener(this);
        m_templateCache = new CmsTemplateCache();
        m_configuration = new TreeMap();
    }

    /**
     * Returns the element cache,
     * or null if the element cache is not initialized.<p>
     * 
     * @return the element cache that belongs to the given cms context
     */
    public static final CmsElementCache getElementCache() {

        return m_elementCache;
    }

    /**
     * Returns the variant dependencies of the online element cache.<p>
     * 
     * @return the variant dependencies of the online element cache
     */
    public static final CmsElementCache getOnlineElementCache() {

        return m_elementCache;
    }

    /**
     * Provides access to the current request through a CmsRequestContext, 
     * required for legacy backward compatibility.<p>
     * 
     * @param context the current request context
     * @return the request, of null if no request is available
     */
    public static I_CmsRequest getRequest(CmsRequestContext context) {

        return (I_CmsRequest)context.getAttribute(I_CmsRequest.C_CMS_REQUEST);
    }

    /**
     * Provides access to the current response through a CmsRequestContext, 
     * required for legacy backward compatibility.<p>
     * 
     * @param context the current request context
     * @return the response, of null if no request is available
     */
    public static I_CmsResponse getResponse(CmsRequestContext context) {

        return (I_CmsResponse)context.getAttribute(I_CmsResponse.C_CMS_RESPONSE);
    }

    /**
     * Provides access to the current session through a CmsRequestContext, 
     * required for legacy backward compatibility.<p>
     * 
     * @param context the current request context
     * @param value if true, try to create a session if none exist, if false, do not create a session
     * @return the response, of null if no request is available
     */
    public static I_CmsSession getSession(CmsRequestContext context, boolean value) {

        I_CmsRequest req = (I_CmsRequest)context.getAttribute(I_CmsRequest.C_CMS_REQUEST);
        HttpSession session = req.getOriginalRequest().getSession(value);
        if (session != null) {
            return new CmsSession(session);
        } else {
            return null;
        }
    }

    /**
     * Returns the hashtable with the variant dependencies used for the elementcache.<p>
     * 
     * @return the hashtable with the variant dependencies used for the elementcache
     */
    public static final Hashtable getVariantDependencies() {

        return m_variantDeps;
    }

    /**
     * Initializes the current request with the legacy Cms request and response wrappers.<p>
     * 
     * @param cms the current cms context
     * @param req the request to wrap
     * @param res the response to wrap
     * @throws CmsException if something goes wrong
     */
    public static void initLegacyRequest(CmsObject cms, HttpServletRequest req, HttpServletResponse res)
    throws CmsException {

        if (cms.getRequestContext().getAttribute(I_CmsRequest.C_CMS_REQUEST) != null) {
            return;
        }
        try {
            CmsRequestHttpServlet cmsReq = new CmsRequestHttpServlet(req, cms.getRequestContext().getFileTranslator());
            CmsResponseHttpServlet cmsRes = new CmsResponseHttpServlet(req, res);
            cms.getRequestContext().setAttribute(I_CmsRequest.C_CMS_REQUEST, cmsReq);
            cms.getRequestContext().setAttribute(I_CmsResponse.C_CMS_RESPONSE, cmsRes);
        } catch (IOException e) {
            throw new CmsLegacyException("Trouble setting up legacy request / response", e);
        }
    }

    /**
     * Returns true if the element cache is enabled.<p>
     * 
     * @return true if the element cache is enabled
     */
    public static final boolean isElementCacheEnabled() {

        return m_elementCache != null;
    }

    /**
     * Compatibility method to ensure the legacy cache command line parameters
     * are still supported.<p>
     * 
     * @param clearFiles if true, A_CmsXmlContent cache is cleared
     * @param clearTemplates if true, internal template cache is cleared
     */
    private static void clearLoaderCache(boolean clearFiles, boolean clearTemplates) {

        if (clearFiles) {
            A_CmsXmlContent.clearFileCache();
        }
        if (clearTemplates) {
            m_templateCache.clearCache();
        }
    }

    /**
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#addConfigurationParameter(java.lang.String, java.lang.String)
     */
    public void addConfigurationParameter(String paramName, String paramValue) {

        m_configuration.put(paramName, paramValue);
    }

    /**
     * Implements the CmsEvent interface,
     * the static export properties uses the events to clear 
     * the list of cached keys in case a project is published.<p>
     *
     * @param event CmsEvent that has occurred
     */
    public synchronized void cmsEvent(CmsEvent event) {

        switch (event.getType()) {

            case I_CmsEventListener.EVENT_FLEX_CACHE_CLEAR:
                clearLoaderCache(true, true);
                break;
            case I_CmsEventListener.EVENT_CLEAR_ONLINE_CACHES:
                clearLoaderCache(true, true);
                break;
            case I_CmsEventListener.EVENT_CLEAR_OFFLINE_CACHES:
                clearLoaderCache(true, true);
                break;
            case I_CmsEventListener.EVENT_CLEAR_CACHES:
                clearLoaderCache(true, true);
                break;
            default:
        // no operation
        }
    }

    /** 
     * Destroy this ResourceLoder, this is a NOOP so far.  
     */
    public void destroy() {

        // NOOP
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#dump(org.opencms.file.CmsObject, org.opencms.file.CmsResource, java.lang.String, java.util.Locale, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public byte[] dump(
        CmsObject cms,
        CmsResource file,
        String element,
        Locale locale,
        HttpServletRequest req,
        HttpServletResponse res) throws CmsException {

        initLegacyRequest(cms, req, res);
        String absolutePath = cms.getSitePath(file);
        // this will work for the "default" template class com.opencms.template.CmsXmlTemplate only
        CmsXmlTemplate template = new CmsXmlTemplate();
        // get the appropriate content and convert it to bytes
        return template.getContent(cms, absolutePath, element, null);
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#export(org.opencms.file.CmsObject, org.opencms.file.CmsResource, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public byte[] export(CmsObject cms, CmsResource resource, HttpServletRequest req, HttpServletResponse res)
    throws IOException, CmsException {

        CmsFile file = CmsFile.upgrade(resource, cms);
        initLegacyRequest(cms, req, res);
        CmsRequestHttpServlet cmsReq = new CmsRequestHttpServlet(req, cms.getRequestContext().getFileTranslator());
        return generateOutput(cms, file, cmsReq);
    }

    /**
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#getConfiguration()
     */
    public Map getConfiguration() {

        // return the configuration in an immutable form
        return Collections.unmodifiableMap(m_configuration);
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#getLoaderId()
     */
    public int getLoaderId() {

        return C_RESOURCE_LOADER_ID;
    }

    /**
     * Return a String describing the ResourceLoader,
     * which is <code>"The OpenCms default resource loader for XMLTemplates"</code>.<p>
     * 
     * @return a describing String for the ResourceLoader 
     */
    public String getResourceLoaderInfo() {

        return "The OpenCms default resource loader for XMLTemplates";
    }

    /**
     * @see org.opencms.loader.I_CmsLoaderIncludeExtension#includeExtension(java.lang.String, java.lang.String, boolean, java.util.Map, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public String includeExtension(
        String target,
        String element,
        boolean editable,
        Map parameterMap,
        ServletRequest req,
        ServletResponse res) throws CmsException {

        // the Flex controller provides access to the interal OpenCms structures
        CmsFlexController controller = CmsFlexController.getController(req);
        // simple sanity check, controller should never be null here
        if (controller == null) {
            return target;
        }
        // special code to handle XmlTemplate based file includes        
        if (element != null) {
            if (!("body".equals(element) || "(default)".equals(element))) {
                // add template selector for multiple body XML files
                CmsJspTagInclude.addParameter(parameterMap, CmsXmlTemplate.C_FRAME_SELECTOR, element, true);
            }
        }
        boolean isPageTarget;
        try {
            // check if the target does exist in the OpenCms VFS
            CmsResource targetResource = controller.getCmsObject().readResource(target);
            isPageTarget = ((CmsResourceTypePage.getStaticTypeId() == targetResource.getTypeId()));
        } catch (CmsException e) {
            controller.setThrowable(e, target);
            throw new CmsLegacyException("File not found: " + target, e);
        }
        String bodyAttribute = (String)controller.getCmsObject().getRequestContext().getAttribute(
            CmsDefaultPageEditor.XML_BODY_ELEMENT);
        if (bodyAttribute == null) {
            // no body attribute is set: this is NOT a sub-element in a XML mastertemplate
            if (isPageTarget) {
                // add body file path to target 
                if (!target.startsWith(CmsCompatibleCheck.VFS_PATH_BODIES)) {
                    target = CmsCompatibleCheck.VFS_PATH_BODIES + target.substring(1);
                }
                // save target as "element replace" parameter for body loader
                CmsJspTagInclude.addParameter(
                    parameterMap,
                    CmsXmlTemplateLoader.C_ELEMENT_REPLACE,
                    "body:" + target,
                    true);
                target = C_BODYLOADER_URI;
            }
        } else {
            // body attribute is set: this is a sub-element in a XML mastertemplate
            if (target.equals(controller.getCmsObject().getRequestContext().getUri())) {
                // target can be ignored, set body attribute as "element replace" parameter  
                CmsJspTagInclude.addParameter(parameterMap, CmsXmlTemplateLoader.C_ELEMENT_REPLACE, "body:"
                    + bodyAttribute, true);
                // redirect target to body loader
                target = C_BODYLOADER_URI;
            } else {
                if (isPageTarget) {
                    // add body file path to target 
                    if (isPageTarget && !target.startsWith(CmsCompatibleCheck.VFS_PATH_BODIES)) {
                        target = CmsCompatibleCheck.VFS_PATH_BODIES + target.substring(1);
                    }
                    // save target as "element replace" parameter  
                    CmsJspTagInclude.addParameter(parameterMap, CmsXmlTemplateLoader.C_ELEMENT_REPLACE, "body:"
                        + target, true);
                    target = C_BODYLOADER_URI;
                }
            }
        }

        return target;
    }

    /**
     * @see org.opencms.configuration.I_CmsConfigurationParameterHandler#initConfiguration()
     */
    public void initConfiguration() {

        ExtendedProperties config = new ExtendedProperties();
        config.putAll(m_configuration);

        // check if the element cache is enabled
        boolean elementCacheEnabled = config.getBoolean("elementcache.enabled", false);
        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(". Loader init          : XMLTemplate element cache "
                + (elementCacheEnabled ? "enabled" : "disabled"));
        }
        if (elementCacheEnabled) {
            try {
                m_elementCache = new CmsElementCache(config.getInteger("elementcache.uri", 10000), config.getInteger(
                    "elementcache.elements",
                    50000), config.getInteger("elementcache.variants", 100));
            } catch (Exception e) {
                if (CmsLog.INIT.isWarnEnabled()) {
                    CmsLog.INIT.warn(". Loader init          : XMLTemplate element cache non-critical error "
                        + e.toString());
                }
            }
            m_variantDeps = new Hashtable();
            m_elementCache.getElementLocator().setExternDependencies(m_variantDeps);
        } else {
            m_elementCache = null;
        }

        if (CmsLog.INIT.isInfoEnabled()) {
            CmsLog.INIT.info(". Loader init          : " + this.getClass().getName() + " initialized");
        }
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isStaticExportEnabled()
     */
    public boolean isStaticExportEnabled() {

        return true;
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isStaticExportProcessable()
     */
    public boolean isStaticExportProcessable() {

        return true;
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isUsableForTemplates()
     */
    public boolean isUsableForTemplates() {

        return true;
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#isUsingUriWhenLoadingTemplate()
     */
    public boolean isUsingUriWhenLoadingTemplate() {

        return true;
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#load(org.opencms.file.CmsObject, org.opencms.file.CmsResource, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public void load(CmsObject cms, CmsResource resource, HttpServletRequest req, HttpServletResponse res)
    throws CmsException {

        initLegacyRequest(cms, req, res);
        processXmlTemplate(cms, CmsFile.upgrade(resource, cms));
    }

    /**
     * @see org.opencms.loader.I_CmsResourceLoader#service(org.opencms.file.CmsObject, org.opencms.file.CmsResource, javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void service(CmsObject cms, CmsResource file, ServletRequest req, ServletResponse res)
    throws CmsException, IOException {

        long timer1 = 0;
        if (DEBUG > 0) {
            timer1 = System.currentTimeMillis();
            System.err.println("============ CmsXmlTemplateLoader loading: " + cms.getSitePath(file));
            System.err.println("CmsXmlTemplateLoader.service() cms uri is: " + cms.getRequestContext().getUri());
        }
        // save the original context settings
        String rnc = cms.getRequestContext().getEncoding().trim();
        // String oldUri = cms.getRequestContext().getUri();

        initLegacyRequest(cms, (HttpServletRequest)req, (HttpServletResponse)res);
        I_CmsRequest cms_req = CmsXmlTemplateLoader.getRequest(cms.getRequestContext());
        HttpServletRequest originalreq = cms_req.getOriginalRequest();

        try {
            // get the CmsRequest
            byte[] result = null;
            org.opencms.file.CmsFile fx = cms.readFile(cms.getSitePath(file));
            // care about encoding issues
            String dnc = OpenCms.getSystemInfo().getDefaultEncoding().trim();
            String enc = cms.readPropertyObject(
                cms.getSitePath(fx),
                CmsPropertyDefinition.PROPERTY_CONTENT_ENCODING,
                true).getValue(dnc).trim();
            // fake the called URI (otherwise XMLTemplate / ElementCache would not work)            
            cms_req.setOriginalRequest((HttpServletRequest)req);
            cms.getRequestContext().setEncoding(enc);
            if (DEBUG > 1) {
                System.err.println("CmsXmlTemplateLoader.service(): Encodig set to "
                    + cms.getRequestContext().getEncoding());
                System.err.println("CmsXmlTemplateLoader.service(): Uri set to " + cms.getRequestContext().getUri());
            }
            // process the included XMLTemplate
            result = generateOutput(cms, fx, cms_req);
            // append the result to the output stream
            if (result != null) {
                if (DEBUG > 1) {
                    System.err.println("CmsXmlTemplateLoader.service(): encoding="
                        + enc
                        + " requestEncoding="
                        + rnc
                        + " defaultEncoding="
                        + dnc);
                }
                res.getOutputStream().write(result);
            }
        } finally {
            // restore the context settings
            cms_req.setOriginalRequest(originalreq);
            cms.getRequestContext().setEncoding(rnc);
            // cms.getRequestContext().setUri(oldUri);
            if (DEBUG > 1) {
                System.err.println("CmsXmlTemplateLoader.service(): Encodig reset to "
                    + cms.getRequestContext().getEncoding());
                System.err.println("CmsXmlTemplateLoader.service(): Uri reset to " + cms.getRequestContext().getUri());
            }
        }

        if (DEBUG > 0) {
            long timer2 = System.currentTimeMillis() - timer1;
            System.err.println("============ CmsXmlTemplateLoader time delivering XmlTemplate for "
                + cms.getSitePath(file)
                + ": "
                + timer2
                + "ms");
        }
    }

    /**
     * Starts generating the output.
     * Calls the canonical root with the appropriate template class.
     *
     * @param cms CmsObject Object for accessing system resources
     * @param file CmsFile Object with the selected resource to be shown
     * @param req the CmsRequest
     * @return the generated output for the file
     * @throws CmsException if something goes wrong
     */
    protected byte[] generateOutput(CmsObject cms, CmsFile file, I_CmsRequest req) throws CmsException {

        byte[] output = null;

        // hashtable for collecting all parameters.
        Hashtable newParameters = new Hashtable();
        String uri = cms.getRequestContext().getUri();

        // collect xml template information
        String absolutePath = cms.getSitePath(file);
        if (CmsLog.getLog(this).isDebugEnabled()) {
            CmsLog.getLog(this).debug("absolutePath=" + absolutePath);
        }
        String templateProp = cms.readPropertyObject(absolutePath, CmsPropertyDefinition.PROPERTY_TEMPLATE, false).getValue();
        if (CmsLog.getLog(this).isDebugEnabled()) {
            CmsLog.getLog(this).debug("templateProp=" + templateProp);
        }
        String templateClassProp = cms.readPropertyObject(
            absolutePath,
            org.opencms.file.CmsPropertyDefinition.PROPERTY_BODY_CLASS,
            false).getValue(org.opencms.importexport.CmsCompatibleCheck.XML_CONTROL_DEFAULT_CLASS);
        if (CmsLog.getLog(this).isDebugEnabled()) {
            CmsLog.getLog(this).debug("templateClassProp=" + templateClassProp);
        }

        // ladies and gentelman: and now for something completly different 
        String xmlTemplateContent = null;
        if (templateProp != null) {
            // i got a black magic template,
            StringBuffer buf = new StringBuffer(256);
            buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
            buf.append("<PAGE>\n<class>");
            buf.append(org.opencms.importexport.CmsCompatibleCheck.XML_CONTROL_DEFAULT_CLASS);
            buf.append("</class>\n<masterTemplate>");
            // i got a black magic template,
            buf.append(templateProp);
            buf.append("</masterTemplate>\n<ELEMENTDEF name=\"body\">\n<CLASS>");
            buf.append(templateClassProp);
            buf.append("</CLASS>\n<TEMPLATE>");
            // i got a black magic template got me so blind I can't see,
            buf.append(uri);
            buf.append("</TEMPLATE>\n</ELEMENTDEF>\n</PAGE>\n");
            // i got a black magic template it's try'in to make a devil out of me...
            xmlTemplateContent = buf.toString();
            uri += com.opencms.core.I_CmsConstants.C_XML_CONTROL_FILE_SUFFIX;
        }

        // Parameters used for element cache
        boolean elementCacheEnabled = CmsXmlTemplateLoader.isElementCacheEnabled();
        CmsElementCache elementCache = null;
        CmsUriDescriptor uriDesc = null;
        CmsUri cmsUri = null;

        String templateClass = null;
        String templateName = null;
        CmsXmlControlFile doc = null;

        if (elementCacheEnabled) {
            // Get the global element cache object
            elementCache = CmsXmlTemplateLoader.getElementCache();

            // Prepare URI Locator
            uriDesc = new CmsUriDescriptor(uri);
            cmsUri = elementCache.getUriLocator().get(uriDesc);
            // check if cached
            if (CmsLog.getLog(this).isDebugEnabled()) {
                CmsLog.getLog(this).debug("found cmsUri=" + cmsUri);
            }
            if ((cmsUri != null) && !cmsUri.getElementDescriptor().getTemplateName().equalsIgnoreCase(templateProp)) {
                if (CmsLog.getLog(this).isDebugEnabled()) {
                    CmsLog.getLog(this).debug(
                        "cmsUri has different template: "
                            + cmsUri.getElementDescriptor().getTemplateName()
                            + " than current template: "
                            + templateProp
                            + ", not using cmsUri from cache");
                }
                cmsUri = null;
            }
        }

        // check if printversion is requested
        String replace = req.getParameter(C_ELEMENT_REPLACE);
        boolean elementreplace = false;
        CmsElementDefinition replaceDef = null;
        if (replace != null) {
            int index = replace.indexOf(":");
            if (index != -1) {
                elementreplace = true;
                cmsUri = null;
                String elementName = replace.substring(0, index);
                String replaceUri = replace.substring(index + 1);
                replaceDef = new CmsElementDefinition(
                    elementName,
                    org.opencms.importexport.CmsCompatibleCheck.XML_CONTROL_DEFAULT_CLASS,
                    replaceUri,
                    null,
                    new Hashtable());
                newParameters.put(C_ELEMENT_REPLACE + "_VFS_" + elementName, cms.getRequestContext().addSiteRoot(
                    replaceUri));
            }
        }

        if ((cmsUri == null) || !elementCacheEnabled) {
            // Entry point to page file analysis.
            // For performance reasons this should only be done if the element
            // cache is not activated or if it's activated but no URI object could be found.

            // Parse the page file
            try {
                if (xmlTemplateContent == null) {
                    doc = new CmsXmlControlFile(cms, file);
                } else {
                    doc = new CmsXmlControlFile(cms, uri, xmlTemplateContent);
                }
            } catch (Exception e) {
                // there was an error while parsing the document.
                // No chance to go on here.
                handleException(cms, e, "There was an error while parsing XML page file " + cms.getSitePath(file));
                return "".getBytes();
            }

            if (!elementCacheEnabled && (replaceDef != null)) {
                // Required to enable element replacement if element cache is disabled
                doc.setElementClass(replaceDef.getName(), replaceDef.getClassName());
                doc.setElementTemplate(replaceDef.getName(), replaceDef.getTemplateName());
            }

            // Get the names of the master template and the template class from
            // the parsed page file. Fall back to default value, if template class
            // is not defined
            templateClass = doc.getTemplateClass();
            if (templateClass == null || "".equals(templateClass)) {
                templateClass = this.getClass().getName();
            }
            if (templateClass == null || "".equals(templateClass)) {
                templateClass = org.opencms.importexport.CmsCompatibleCheck.XML_CONTROL_DEFAULT_CLASS;
            }
            templateName = doc.getMasterTemplate();
            if (templateName != null && !"".equals(templateName)) {
                templateName = CmsLinkManager.getAbsoluteUri(templateName, cms.getSitePath(file));
            }

            // Previously, the template class was loaded here.
            // We avoid doing this so early, since in element cache mode the template
            // class is not needed here.

            // Now look for parameters in the page file...
            // ... first the params of the master template...
            Enumeration masterTemplateParams = doc.getParameterNames();
            while (masterTemplateParams.hasMoreElements()) {
                String paramName = (String)masterTemplateParams.nextElement();
                String paramValue = doc.getParameter(paramName);
                newParameters.put(com.opencms.core.I_CmsConstants.C_ROOT_TEMPLATE_NAME + "." + paramName, paramValue);
            }

            // ... and now the params of all subtemplates
            Enumeration elementDefinitions = doc.getElementDefinitions();
            while (elementDefinitions.hasMoreElements()) {
                String elementName = (String)elementDefinitions.nextElement();
                if (doc.isElementClassDefined(elementName)) {
                    newParameters.put(elementName + "._CLASS_", doc.getElementClass(elementName));
                }
                if (doc.isElementTemplateDefined(elementName)) {
                    // need to check for the body template here so that non-XMLTemplate templates 
                    // like JSPs know where to find the body defined in the XMLTemplate
                    String template = doc.getElementTemplate(elementName);
                    if (xmlTemplateContent == null) {
                        template = doc.validateBodyPath(cms, template, file);
                    }
                    if (CmsDefaultPageEditor.XML_BODY_ELEMENT.equalsIgnoreCase(elementName)) {
                        // found body element
                        if (template != null) {
                            cms.getRequestContext().setAttribute(CmsDefaultPageEditor.XML_BODY_ELEMENT, template);
                        }
                    }
                    newParameters.put(elementName + "._TEMPLATE_", template);
                }
                if (doc.isElementTemplSelectorDefined(elementName)) {
                    newParameters.put(elementName + "._TEMPLATESELECTOR_", doc.getElementTemplSelector(elementName));
                }
                Enumeration parameters = doc.getElementParameterNames(elementName);
                while (parameters.hasMoreElements()) {
                    String paramName = (String)parameters.nextElement();
                    String paramValue = doc.getElementParameter(elementName, paramName);
                    if (paramValue != null) {
                        newParameters.put(elementName + "." + paramName, paramValue);
                    } else {
                        if (CmsLog.getLog(this).isInfoEnabled()) {
                            CmsLog.getLog(this).info("Empty parameter \"" + paramName + "\" found.");
                        }
                    }
                }
            }
        }

        // URL parameters ary really dynamic.
        // We cannot store them in an element cache.
        // Therefore these parameters must be collected in ANY case!

        String datafor = req.getParameter("datafor");
        if (datafor == null) {
            datafor = "";
        } else {
            if (!"".equals(datafor)) {
                datafor = datafor + ".";
            }
        }
        Enumeration urlParameterNames = req.getParameterNames();
        while (urlParameterNames.hasMoreElements()) {
            String pname = (String)urlParameterNames.nextElement();
            String paramValue = req.getParameter(pname);
            if (paramValue != null) {
                if ((!"datafor".equals(pname)) && (!"_clearcache".equals(pname))) {
                    newParameters.put(datafor + pname, paramValue);
                }
            } else {
                if (CmsLog.getLog(this).isInfoEnabled()) {
                    CmsLog.getLog(this).info("Empty URL parameter \"" + pname + "\" found.");
                }
            }
        }

        if (elementCacheEnabled && (cmsUri == null)) {
            // ---- element cache stuff --------
            // No URI could be found in cache.
            // So create a new URI object with a start element and store it using the UriLocator
            CmsElementDescriptor elemDesc = new CmsElementDescriptor(templateClass, templateName);
            CmsElementDefinitionCollection eldefs = doc.getElementDefinitionCollection();
            if (elementreplace) {
                // we cant cach this
                eldefs.add(replaceDef);
                cmsUri = new CmsUri(elemDesc, eldefs);
            } else {
                cmsUri = new CmsUri(elemDesc, eldefs);
                elementCache.getUriLocator().put(uriDesc, cmsUri);
            }
        }

        if (elementCacheEnabled) {
            // TODO: Make cache more efficient
            clearLoaderCache(true, true);

            // now lets get the output
            if (elementreplace) {
                output = cmsUri.callCanonicalRoot(elementCache, cms, newParameters);
            } else {
                output = elementCache.callCanonicalRoot(cms, newParameters, uri);
            }
        } else {
            // ----- traditional stuff ------
            // Element cache is deactivated. So let's go on as usual.
            try {
                CmsFile masterTemplate = loadMasterTemplateFile(cms, templateName, doc);
                I_CmsTemplate tmpl = getTemplateClass(templateClass);
                if (!(tmpl instanceof I_CmsXmlTemplate)) {
                    String errorMessage = "Error in "
                        + cms.getSitePath(file)
                        + ": "
                        + templateClass
                        + " is not a XML template class.";
                    if (CmsLog.getLog(this).isErrorEnabled()) {
                        CmsLog.getLog(this).error(errorMessage);
                    }
                    throw new CmsLegacyException(errorMessage, CmsLegacyException.C_XML_WRONG_TEMPLATE_CLASS);
                }
                // TODO: Make cache more efficient
                clearLoaderCache(true, true);
                output = callCanonicalRoot(cms, tmpl, masterTemplate, newParameters);
            } catch (CmsException e) {
                if (CmsLog.getLog(this).isWarnEnabled()) {
                    CmsLog.getLog(this);
                }
                doc.removeFromFileCache();
                throw e;
            }
        }
        return output;
    }

    /**
     * Utility method used by the loader implementation to give control
     * to the CanonicalRoot.<p>
     * 
     * The CanonicalRoot will call the master template and return a byte array of the
     * generated output.<p>
     *
     * @param cms the cms context object
     * @param templateClass to generate the output of the master template
     * @param masterTemplate masterTemplate for the output
     * @param parameters contains all parameters for the template class
     * @return the generated output or null if there were errors
     * @throws CmsException if something goes wrong
     */
    private byte[] callCanonicalRoot(
        CmsObject cms,
        I_CmsTemplate templateClass,
        CmsFile masterTemplate,
        Hashtable parameters) throws CmsException {

        try {
            CmsRootTemplate root = new CmsRootTemplate();
            return root.getMasterTemplate(cms, templateClass, masterTemplate, m_templateCache, parameters);
        } catch (Exception e) {
            // no document we could show...
            handleException(cms, e, "Received error while calling canonical root for requested file "
                + masterTemplate.getName()
                + ". ");
        }
        return null;
    }

    /**
     * Calls the CmsClassManager to get an instance of the given template class.<p>
     * 
     * The returned object is checked to be an implementing class of the interface
     * I_CmsTemplate.
     * If the template cache of the template class is not yet set up, 
     * this will be done, too.<p>
     * 
     * @param classname name of the requested template class
     * @return instance of the template class
     * @throws CmsException if something goes wrong
     */
    private I_CmsTemplate getTemplateClass(String classname) throws CmsException {

        if (CmsLog.getLog(this).isDebugEnabled()) {
            CmsLog.getLog(this).debug("Getting start template class: " + classname);
        }
        Object o = CmsTemplateClassManager.getClassInstance(classname);

        // Check, if the loaded class really is a OpenCms template class.

        // This is done be checking the implemented interface.
        if (!(o instanceof I_CmsTemplate)) {
            String errorMessage = "Class " + classname + " is not an OpenCms template class.";
            if (CmsLog.getLog(this).isErrorEnabled()) {
                CmsLog.getLog(this).error(errorMessage);
            }
            throw new CmsLegacyException(errorMessage, CmsLegacyException.C_XML_NO_TEMPLATE_CLASS);
        }
        I_CmsTemplate cmsTemplate = (I_CmsTemplate)o;
        if (!cmsTemplate.isTemplateCacheSet()) {
            cmsTemplate.setTemplateCache(m_templateCache);
        }
        return cmsTemplate;
    }

    /**
     * Utility method to handle any occurence of an execption.<p>
     * 
     * If the Exception is NO CmsException (i.e. it was not detected previously)
     * it will be written to the logfile.<p>
     * 
     * If the current user is the anonymous user, no further exception will
     * be thrown, but a server error will be sent
     * (we want to prevent the user from seeing any exeptions).
     * Otherwise a new Exception will be thrown.
     * This will trigger the OpenCms error message box.<p>
     *
     * @param cms the cms context object
     * @param e Exception that should be handled
     * @param errorText error message that should be shown
     * @throws CmsException if 
     */
    private void handleException(CmsObject cms, Exception e, String errorText) throws CmsException {

        // log the error if it is no CmsException
        if (!(e instanceof CmsException)) {
            if (CmsLog.getLog(this).isErrorEnabled()) {
                CmsLog.getLog(this).error(errorText, e);
            }
        }

        // if the user is a guest (and it's not a login exception) we send an servlet error,
        // otherwise we try to throw an exception.
        CmsRequestContext reqContext = cms.getRequestContext();
        if ((DEBUG == 0)
            && reqContext.currentUser().isGuestUser()
            && (!(e instanceof CmsLegacyException && ((e instanceof CmsLegacyException) && (((CmsLegacyException)e).getType() == CmsLegacyException.C_NO_USER))))) {
            throw new CmsLegacyException(errorText, CmsLegacyException.C_SERVICE_UNAVAILABLE, e);
        } else {
            if (e instanceof CmsException) {
                throw (CmsException)e;
            } else {
                throw new CmsLegacyException(errorText, CmsLegacyException.C_LOADER_GENERIC_ERROR, e);
            }
        }
    }

    /**
     * Internal utility method for checking and loading a given template file.
     * @param cms CmsObject for accessing system resources.
     * @param templateName Name of the requestet template file.
     * @param doc CmsXmlControlFile object containig the parsed body file.
     * @return CmsFile object of the requested template file.
     * @throws CmsException if something goes wrong
     */
    private CmsFile loadMasterTemplateFile(
        CmsObject cms,
        String templateName,
        com.opencms.template.CmsXmlControlFile doc) throws CmsException {

        CmsFile masterTemplate = null;
        try {
            masterTemplate = cms.readFile(templateName);
        } catch (Exception e) {
            handleException(cms, e, "Cannot load master template " + templateName + ". ");
            doc.removeFromFileCache();
        }
        return masterTemplate;
    }

    /**
     * Processes the XmlTemplates and writes the result to 
     * the apropriate output stream, which is obtained from the request 
     * context of the cms object.<p>
     *
     * @param cms the cms context object
     * @param file the selected resource to be shown
     * @throws CmsException if something goes wrong
     */
    private void processXmlTemplate(CmsObject cms, CmsFile file) throws CmsException {

        // first some debugging output.
        if ((DEBUG > 0) && CmsLog.getLog(this).isDebugEnabled()) {
            CmsLog.getLog(this).debug("Loader started for " + file.getName());
        }

        // check all values to be valid
        String errorMessage = null;
        if (file == null) {
            errorMessage = "CmsFile missing";
        }
        if (cms == null) {
            errorMessage = "CmsObject missing";
        }
        if (errorMessage != null) {
            if (CmsLog.getLog(this).isErrorEnabled()) {
                CmsLog.getLog(this).error(errorMessage);
            }
            throw new CmsLegacyException(errorMessage, CmsLegacyException.C_LOADER_GENERIC_ERROR);
        }

        // Check the clearcache parameter
        String clearcache = getRequest(cms.getRequestContext()).getParameter("_clearcache");

        // Clear loader caches if this is required
        clearLoaderCache(
            ((clearcache != null) && ("all".equals(clearcache) || "file".equals(clearcache))),
            ((clearcache != null) && ("all".equals(clearcache) || "template".equals(clearcache))));

        // get the CmsRequest
        I_CmsRequest req = getRequest(cms.getRequestContext());
        byte[] result = generateOutput(cms, file, req);
        if (result != null) {
            writeBytesToResponse(cms, result);
        }
    }

    /**
     * Writes a given byte array to the HttpServletRespose output stream.<p>
     * 
     * @param cms an initialized CmsObject
     * @param result byte array that should be written.
     * @throws CmsException if something goes wrong
     */
    private void writeBytesToResponse(CmsObject cms, byte[] result) throws CmsException {

        try {
            I_CmsResponse resp = getResponse(cms.getRequestContext());
            if ((result != null) && !resp.isRedirected()) {
                // Only write any output to the response output stream if
                // the current request is neither redirected nor streamed.
                OutputStream out = resp.getOutputStream();

                resp.setContentLength(result.length);
                resp.setHeader("Connection", "keep-alive");
                out.write(result);
                out.close();
            }
        } catch (IOException ioe) {
            if (CmsLog.getLog(this).isDebugEnabled()) {
                CmsLog.getLog(this).debug(
                    "IO error while writing to response stream for " + cms.getRequestContext().getUri(),
                    ioe);
            }
        } catch (Exception e) {
            String errorMessage = "Cannot write output to HTTP response stream";
            handleException(cms, e, errorMessage);
        }
    }
}