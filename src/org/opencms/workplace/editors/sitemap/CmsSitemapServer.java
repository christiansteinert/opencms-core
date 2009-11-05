/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/workplace/editors/sitemap/Attic/CmsSitemapServer.java,v $
 * Date   : $Date: 2009/11/05 15:03:24 $
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

package org.opencms.workplace.editors.sitemap;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResource;
import org.opencms.file.CmsUser;
import org.opencms.file.CmsVfsResourceNotFoundException;
import org.opencms.file.types.CmsResourceTypeXmlSitemap;
import org.opencms.file.types.I_CmsResourceType;
import org.opencms.flex.CmsFlexController;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.i18n.CmsMessages;
import org.opencms.json.JSONArray;
import org.opencms.json.JSONException;
import org.opencms.json.JSONObject;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.loader.CmsTemplateLoaderFacade;
import org.opencms.main.CmsException;
import org.opencms.main.CmsIllegalArgumentException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.security.CmsPermissionViolationException;
import org.opencms.util.CmsRequestUtil;
import org.opencms.util.CmsStringUtil;
import org.opencms.util.CmsUUID;
import org.opencms.xml.CmsXmlUtils;
import org.opencms.xml.containerpage.CmsADEManager;
import org.opencms.xml.containerpage.CmsXmlContainerPage;
import org.opencms.xml.content.CmsXmlContentProperty;
import org.opencms.xml.sitemap.CmsSiteEntryBean;
import org.opencms.xml.sitemap.CmsSitemapBean;
import org.opencms.xml.sitemap.CmsXmlSitemap;
import org.opencms.xml.sitemap.CmsXmlSitemapFactory;
import org.opencms.xml.types.I_CmsXmlContentValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;

/**
 * Sitemap server used for client/server communication.<p>
 * 
 * see jsp file <tt>/system/workplace/editors/sitemap/server.jsp</tt>.<p>
 * 
 * @author Michael Moossen 
 * 
 * @version $Revision: 1.6 $
 * 
 * @since 7.6
 */
public class CmsSitemapServer extends CmsJspActionElement {

    /** Request parameter action value constants. */
    protected enum Action {
        /** First call to get all the data. */
        ALL,
        /** To retrieve the favorite or recent list. */
        GET,
        /** To save the sitemap. */
        SAVE,
        /** To retrieve the favorite or recent list. */
        SET,
        /** To lock the sitemap. */
        STARTEDIT,
        /** To unlock the sitemap. */
        STOPEDIT;
    }

    /** Json property name constants for request parameters. */
    protected enum JsonRequest {

        /** To get or save the favorites list. */
        FAV("fav"),
        /** To retrieve or save the recent list. */
        REC("rec");

        /** Property name. */
        private String m_name;

        /** Constructor.<p> */
        private JsonRequest(String name) {

            m_name = name;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** Json property name constants for responses. */
    protected enum JsonResponse {

        /** The error message. */
        ERROR("error"),
        /** The favorites list. */
        FAVORITES("favorites"),
        /** The recent list. */
        RECENT("recent"),
        /** The sitemap tree. */
        SITEMAP("sitemap"),
        /** The response state. */
        STATE("state");

        /** Property name. */
        private String m_name;

        /** Constructor.<p> */
        private JsonResponse(String name) {

            m_name = name;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** Json property name constants for sitemap entries. */
    protected enum JsonSiteEntry {

        /** The HTML content. */
        CONTENT("content"),
        /** The resource id. */
        ID("id"),
        /** The name. */
        NAME("name"),
        /** The properties. */
        PROPERTIES("properties"),
        /** The sub-entries. */
        SUBENTRIES("subentries"),
        /** The title. */
        TITLE("title");

        /** Property name. */
        private String m_name;

        /** Constructor.<p> */
        private JsonSiteEntry(String name) {

            m_name = name;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** Json property name constants for response status. */
    protected enum JsonState {

        /** An error occurred. */
        ERROR("error"),
        /** The request was executed with any problems. */
        OK("ok");

        /** Property name. */
        private String m_name;

        /** Constructor.<p> */
        private JsonState(String name) {

            m_name = name;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** Request parameter name constants. */
    protected enum ReqParam {

        /** The action of execute. */
        ACTION("action"),
        /** Generic data parameter. */
        DATA("data"),
        /** The current locale. */
        LOCALE("locale"),
        /** The sitemap uri. */
        SITEMAP("sitemap");

        /** Parameter name. */
        private String m_name;

        /** Constructor.<p> */
        private ReqParam(String name) {

            m_name = name;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** Formatters map key constant. */
    public static final Integer KEY_DEFAULT_FORMATTER = new Integer(-100);

    /** Mime type constant. */
    public static final String MIMETYPE_APPLICATION_JSON = "application/json";

    /** User additional info key constant. */
    protected static final String ADDINFO_SITEMAP_FAVORITE_LIST = "SITEMAP_FAVORITE_LIST";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsSitemapServer.class);

    /** The site map entry formatters. */
    private Map<Integer, CmsResource> m_formatters;

    /** The session cache. */
    private CmsSitemapSessionCache m_sessionCache;

    /** Path constant. */
    public static final String PATH_DEFAULT_FORMATTER = "/system/workplace/editors/sitemap/default-formatter.jsp";

    /**
     * Constructor.<p>
     * 
     * @param context the JSP page context object
     * @param req the JSP request 
     * @param res the JSP response 
     */
    public CmsSitemapServer(PageContext context, HttpServletRequest req, HttpServletResponse res) {

        super(context, req, res);
        m_sessionCache = (CmsSitemapSessionCache)req.getSession().getAttribute(
            CmsSitemapSessionCache.SESSION_ATTR_SITEMAP_CACHE);
        if (m_sessionCache == null) {
            m_sessionCache = new CmsSitemapSessionCache(getCmsObject());
            req.getSession().setAttribute(CmsSitemapSessionCache.SESSION_ATTR_SITEMAP_CACHE, m_sessionCache);
        }
        if (m_formatters == null) {
            m_formatters = new HashMap<Integer, CmsResource>();
            try {
                m_formatters.put(KEY_DEFAULT_FORMATTER, getCmsObject().readResource(
                    CmsSitemapServer.PATH_DEFAULT_FORMATTER));
            } catch (CmsException e) {
                // should never happen
                LOG.error(e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Handles all ADE requests.<p>
     * 
     * @return the result
     * 
     * @throws JSONException if there is any problem with JSON
     * @throws CmsException if there is a problem with the cms context
     */
    public JSONObject executeAction() throws CmsException, JSONException {

        JSONObject result = new JSONObject();

        HttpServletRequest request = getRequest();
        CmsObject cms = getCmsObject();

        if (!checkParameters(request, result, ReqParam.ACTION, ReqParam.LOCALE, ReqParam.SITEMAP)) {
            // every request needs to have at least these parameters 
            return result;
        }
        String actionParam = request.getParameter(ReqParam.ACTION.getName());
        Action action = Action.valueOf(actionParam.toUpperCase());
        String localeParam = request.getParameter(ReqParam.LOCALE.getName());
        cms.getRequestContext().setLocale(CmsLocaleManager.getLocale(localeParam));
        JSONObject data = new JSONObject();
        if (checkParameters(request, null, ReqParam.DATA)) {
            String dataParam = request.getParameter(ReqParam.DATA.getName());
            data = new JSONObject(dataParam);
        }
        String sitemapParam = request.getParameter(ReqParam.SITEMAP.getName());

        CmsResource sitemapRes = cms.readResource(sitemapParam);
        CmsXmlSitemap xmlSitemap = CmsXmlSitemapFactory.unmarshal(cms, sitemapRes, request);
        CmsSitemapBean sitemap = xmlSitemap.getSitemap(cms, cms.getRequestContext().getLocale());

        if (action.equals(Action.ALL)) {
            // first load, get everything
            result = getSitemap(sitemapRes, sitemap);
        } else if (action.equals(Action.GET)) {
            if (checkParameters(data, null, JsonRequest.FAV)) {
                // get the favorite list
                result.put(JsonResponse.FAVORITES.getName(), getFavoriteList());
            } else if (checkParameters(data, result, JsonRequest.REC)) {
                // get recent list
                result.put(JsonResponse.RECENT.getName(), m_sessionCache.getRecentList());
            } else {
                return result;
            }
        } else if (action.equals(Action.SET)) {
            if (checkParameters(data, null, JsonRequest.FAV)) {
                // save the favorite list
                JSONArray list = data.optJSONArray(JsonRequest.FAV.getName());
                saveFavoriteList(cms, list);
            } else if (checkParameters(data, result, JsonRequest.REC)) {
                // save the recent list
                JSONArray list = data.optJSONArray(JsonRequest.REC.getName());
                m_sessionCache.setRecentList(list);
            } else {
                return result;
            }
        } else if (action.equals(Action.SAVE)) {
            // save the sitemap
            saveSitemap(sitemapParam, data);
        } else if (action.equals(Action.STARTEDIT)) {
            // lock the sitemap
            try {
                cms.lockResourceTemporary(sitemapParam);
            } catch (CmsException e) {
                result.put(JsonResponse.ERROR.getName(), e.getLocalizedMessage(cms.getRequestContext().getLocale()));
            }
        } else if (action.equals(Action.STOPEDIT)) {
            // lock the sitemap
            try {
                cms.unlockResource(sitemapParam);
            } catch (CmsException e) {
                result.put(JsonResponse.ERROR.getName(), e.getLocalizedMessage(cms.getRequestContext().getLocale()));
            }
        } else {
            result.put(JsonResponse.ERROR.getName(), Messages.get().getBundle().key(
                Messages.ERR_JSON_WRONG_PARAMETER_VALUE_2,
                ReqParam.ACTION.getName(),
                actionParam));
        }
        return result;
    }

    /**
     * Returns the content of an entry when rendered with the given formatter.<p> 
     * 
     * @param entry the site entry bean
     * 
     * @return generated html code
     * 
     * @throws CmsException if an cms related error occurs
     * @throws ServletException if a jsp related error occurs
     * @throws IOException if a jsp related error occurs
     */
    public String getEntryContent(CmsSiteEntryBean entry) throws CmsException, ServletException, IOException {

        CmsObject cms = getCmsObject();
        CmsResource elementRes = cms.readResource(entry.getResourceId());

        CmsResource formatter = m_formatters.get(new Integer(elementRes.getTypeId()));
        if (formatter == null) {
            // get the formatter from the resource type configuration
            I_CmsResourceType type = OpenCms.getResourceManager().getResourceType(elementRes);
            String formatterPath = type.getConfiguration().get(CmsResourceTypeXmlSitemap.PARAM_SITEMAP_FORMATTER);
            if (CmsStringUtil.isNotEmptyOrWhitespaceOnly(formatterPath)) {
                try {
                    formatter = cms.readResource(formatterPath);
                    m_formatters.put(new Integer(elementRes.getTypeId()), formatter);
                } catch (CmsException e) {
                    if (!LOG.isDebugEnabled()) {
                        LOG.warn(e.getLocalizedMessage());
                    }
                    LOG.debug(e.getLocalizedMessage(), e);
                }
            }
        }
        if (formatter == null) {
            // use default
            formatter = m_formatters.get(KEY_DEFAULT_FORMATTER);
            m_formatters.put(new Integer(elementRes.getTypeId()), formatter);
        }

        CmsTemplateLoaderFacade loaderFacade = new CmsTemplateLoaderFacade(OpenCms.getResourceManager().getLoader(
            formatter), elementRes, formatter);

        CmsResource loaderRes = loaderFacade.getLoaderStartResource();

        HttpServletRequest m_req = getRequest();
        HttpServletResponse m_res = getResponse();

        // TODO: is this going to be cached? most likely not! any alternative?
        Object currentElement = m_req.getAttribute(CmsADEManager.ATTR_CURRENT_ELEMENT);
        m_req.setAttribute(CmsADEManager.ATTR_CURRENT_ELEMENT, new CmsSiteEntryBean(
            entry.getResourceId(),
            entry.getName(),
            entry.getTitle(),
            entry.getProperties(),
            null));
        try {
            return new String(loaderFacade.getLoader().dump(
                cms,
                loaderRes,
                null,
                cms.getRequestContext().getLocale(),
                m_req,
                m_res), CmsLocaleManager.getResourceEncoding(cms, elementRes));
        } finally {
            m_req.setAttribute(CmsADEManager.ATTR_CURRENT_ELEMENT, currentElement);
        }
    }

    /**
     * Returns the current user's favorites list.<p>
     * 
     * @return the current user's favorites list
     * 
     * @throws CmsException if something goes wrong 
     */
    public JSONArray getFavoriteList() throws CmsException {

        CmsObject cms = getCmsObject();
        CmsUser user = cms.getRequestContext().currentUser();
        Object obj = user.getAdditionalInfo(ADDINFO_SITEMAP_FAVORITE_LIST);

        JSONArray favList = new JSONArray();
        if (obj instanceof String) {
            try {
                favList = new JSONArray((String)obj);
            } catch (Throwable e) {
                // should never happen, catches json parsing
                if (!LOG.isDebugEnabled()) {
                    LOG.warn(e.getLocalizedMessage());
                }
                LOG.debug(e.getLocalizedMessage(), e);
            }
        } else {
            // save to be better next time
            saveFavoriteList(cms, favList);
        }

        return favList;
    }

    /**
     * Returns the data for the given sitemap.<p>
     * 
     * @param resource the sitemap's resource 
     * @param sitemap the sitemap to use
     * 
     * @return the data for the given sitemap
     * 
     * @throws CmsException if something goes wrong with the cms context
     * @throws JSONException if something goes wrong with the JSON manipulation
     */
    public JSONObject getSitemap(CmsResource resource, CmsSitemapBean sitemap) throws CmsException, JSONException {

        // create empty result object
        JSONObject result = new JSONObject();

        // collect the site map entries
        JSONArray siteEntries = new JSONArray();
        for (CmsSiteEntryBean entry : sitemap.getSiteEntries()) {
            JSONObject siteEntry = jsonifyEntry(entry);
            if (siteEntry != null) {
                siteEntries.put(siteEntry);
            }
        }
        result.put(JsonResponse.SITEMAP.getName(), siteEntries);

        // collect the favorites
        JSONArray resFavorites = getFavoriteList();
        result.put(JsonResponse.FAVORITES.getName(), resFavorites);
        // collect the recent list
        JSONArray resRecent = m_sessionCache.getRecentList();
        result.put(JsonResponse.RECENT.getName(), resRecent);

        return result;
    }

    /**
     * Saves the favorite list, user based.<p>
     * 
     * @param cms the cms context
     * @param favoriteList the element id list
     * 
     * @throws CmsException if something goes wrong 
     */
    public void saveFavoriteList(CmsObject cms, JSONArray favoriteList) throws CmsException {

        CmsUser user = cms.getRequestContext().currentUser();
        user.setAdditionalInfo(ADDINFO_SITEMAP_FAVORITE_LIST, favoriteList.toString());
        cms.writeUser(user);
    }

    /**
     * Saves the new state of the sitemap.<p>
     * 
     * @param uri the uri of the sitemap to save
     * @param sitemap the sitemap data
     * 
     * @throws CmsException if something goes wrong with the cms context
     * @throws JSONException if something goes wrong with the JSON manipulation
     */
    public void saveSitemap(String uri, JSONObject sitemap) throws CmsException, JSONException {

        CmsObject cms = getCmsObject();

        cms.lockResourceTemporary(uri);
        CmsFile sitemapFile = cms.readFile(uri);
        CmsXmlSitemap xmlSitemap = CmsXmlSitemapFactory.unmarshal(cms, sitemapFile);
        Locale locale = cms.getRequestContext().getLocale();
        if (xmlSitemap.hasLocale(locale)) {
            // remove the locale 
            xmlSitemap.removeLocale(locale);
        }
        xmlSitemap.addLocale(cms, locale);

        Map<String, CmsXmlContentProperty> propertiesConf = OpenCms.getADEManager().getElementPropertyConfiguration(
            cms,
            sitemapFile);

        int entryCount = 0;
        List<CmsSiteEntryBean> entries = jsonToEntryList(sitemap.getJSONArray(JsonResponse.SITEMAP.getName()));
        for (CmsSiteEntryBean entry : entries) {
            saveEntry(cms, locale, null, xmlSitemap, entry, entryCount, propertiesConf);
            entryCount++;
        }

        sitemapFile.setContents(xmlSitemap.marshal());
        cms.writeFile(sitemapFile);
    }

    /**
     * Main method that handles all requests.<p>
     * 
     * @throws IOException if there is any problem while writing the result to the response 
     * @throws JSONException if there is any problem with JSON
     */
    public void serve() throws JSONException, IOException {

        // set the mime type to application/json
        CmsFlexController controller = CmsFlexController.getController(getRequest());
        controller.getTopResponse().setContentType(MIMETYPE_APPLICATION_JSON);

        JSONObject result = new JSONObject();
        try {
            result = executeAction();
        } catch (Exception e) {
            // a serious error occurred, should not...
            result.put(JsonResponse.ERROR.getName(), e.getLocalizedMessage() == null ? "NPE" : e.getLocalizedMessage());
            LOG.error(Messages.get().getBundle().key(
                Messages.ERR_SERVER_EXCEPTION_1,
                CmsRequestUtil.appendParameters(
                    getRequest().getRequestURL().toString(),
                    CmsRequestUtil.createParameterMap(getRequest().getQueryString()),
                    false)), e);
        }
        // add state info
        if (result.has(JsonResponse.ERROR.getName())) {
            // add state=error in case an error occurred 
            result.put(JsonResponse.STATE.getName(), JsonState.ERROR.getName());
        } else if (!result.has(JsonResponse.STATE.getName())) {
            // add state=ok i case no error occurred
            result.put(JsonResponse.STATE.getName(), JsonState.OK.getName());
        }
        // write the result
        result.write(getResponse().getWriter());
    }

    /**
     * Checks whether a list of parameters are present as attributes of a request.<p>
     * 
     * If this isn't the case, an error message is written to the JSON result object.
     * 
     * @param request the request which contains the parameters
     * @param result the JSON object which the error message should be written into, can be <code>null</code>
     * @param params the array of parameters which should be checked
     * 
     * @return true if and only if all parameters are present in the request
     * 
     * @throws JSONException if something goes wrong with JSON
     */
    protected boolean checkParameters(HttpServletRequest request, JSONObject result, ReqParam... params)
    throws JSONException {

        for (ReqParam param : params) {
            String value = request.getParameter(param.getName());
            if (value == null) {
                if (result != null) {
                    CmsMessages bundle = Messages.get().getBundle();
                    result.put(JsonResponse.ERROR.getName(), bundle.key(
                        Messages.ERR_JSON_MISSING_PARAMETER_1,
                        param.getName()));
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether a list of parameters are present as attributes of a request.<p>
     * 
     * If this isn't the case, an error message is written to the JSON result object.
     * 
     * @param data the JSONObject data which contains the parameters
     * @param result the JSON object which the error message should be written into, can be <code>null</code>
     * @param params the array of parameters which should be checked
     * 
     * @return <code>true</code> if and only if all parameters are present in the given data
     * 
     * @throws JSONException if something goes wrong with JSON
     */
    protected boolean checkParameters(JSONObject data, JSONObject result, JsonRequest... params) throws JSONException {

        for (JsonRequest param : params) {
            if (!data.has(param.getName())) {
                if (result != null) {
                    result.put(CmsSitemapServer.JsonResponse.ERROR.getName(), Messages.get().getBundle().key(
                        Messages.ERR_JSON_MISSING_PARAMETER_1,
                        param.getName()));
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a site entry bean into a JSON object.<p>
     * 
     * @param entry the entry to convert
     * 
     * @return the JSON representation, can be <code>null</code> in case of not enough permissions
     * 
     * @throws JSONException if something goes wrong
     */
    protected JSONObject jsonifyEntry(CmsSiteEntryBean entry) throws JSONException {

        JSONObject result = new JSONObject();
        result.put(JsonSiteEntry.NAME.getName(), entry.getName());
        result.put(JsonSiteEntry.TITLE.getName(), entry.getTitle());
        result.put(JsonSiteEntry.ID.getName(), entry.getResourceId().toString());

        // properties
        JSONObject props = new JSONObject();
        for (Map.Entry<String, String> prop : entry.getProperties().entrySet()) {
            props.put(prop.getKey(), prop.getValue());
        }
        result.put(JsonSiteEntry.PROPERTIES.getName(), props);

        // content
        try {
            result.put(JsonSiteEntry.CONTENT.getName(), getEntryContent(entry));
        } catch (CmsVfsResourceNotFoundException e) {
            // most likely the user has no read permission on this entry
            LOG.debug(e.getLocalizedMessage(), e);
            return null;
        } catch (CmsPermissionViolationException e) {
            // most likely the user has no read permission on this entry
            LOG.debug(e.getLocalizedMessage(), e);
            return null;
        } catch (Exception e) {
            // should never happen
            LOG.error(e.getLocalizedMessage(), e);
            result.put(JsonSiteEntry.CONTENT.getName(), e.getLocalizedMessage());
        }

        // subentries
        JSONArray subentries = new JSONArray();
        for (CmsSiteEntryBean subentry : entry.getSubEntries()) {
            JSONObject jsonSubEntry = jsonifyEntry(subentry);
            if (jsonSubEntry != null) {
                subentries.put(jsonSubEntry);
            }
        }
        result.put(JsonSiteEntry.SUBENTRIES.getName(), subentries);

        return result;
    }

    /**
     * Returns a list of sitemap entries from a json array.<p>
     * 
     * @param array the json array
     * 
     * @return a list of sitemap entries
     * 
     * @throws JSONException if something goes wrong
     */
    protected List<CmsSiteEntryBean> jsonToEntryList(JSONArray array) throws JSONException {

        List<CmsSiteEntryBean> result = new ArrayList<CmsSiteEntryBean>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            CmsUUID id = null;
            try {
                id = new CmsUUID(json.getString(JsonSiteEntry.ID.getName()));
            } catch (CmsIllegalArgumentException e) {
                if (!LOG.isDebugEnabled()) {
                    LOG.warn(e.getLocalizedMessage());
                }
                LOG.debug(e.getLocalizedMessage(), e);
                continue;
            }
            String name = json.getString(JsonSiteEntry.NAME.getName());
            String title = json.getString(JsonSiteEntry.TITLE.getName());
            Map<String, String> properties = new HashMap<String, String>();
            JSONObject jsonProps = json.getJSONObject(JsonSiteEntry.PROPERTIES.getName());
            Iterator<String> itKeys = jsonProps.keys();
            while (itKeys.hasNext()) {
                String key = itKeys.next();
                String value = jsonProps.getString(key);
                properties.put(key, value);
            }
            JSONArray jsonSub = json.getJSONArray(JsonSiteEntry.SUBENTRIES.getName());
            CmsSiteEntryBean entry = new CmsSiteEntryBean(id, name, title, properties, jsonToEntryList(jsonSub));
            result.add(entry);
        }
        return result;
    }

    /**
     * Saves the given entry in the XML content.<p>
     * 
     * @param cms the CMS context
     * @param locale the current locale
     * @param rootPath the root path for this entry
     * @param xmlSitemap the XML content to save to
     * @param entry the entry to save
     * @param entryCount the entry count
     * @param propertiesConf the properties configuration
     * 
     * @throws CmsException if something goes wrong
     */
    protected void saveEntry(
        CmsObject cms,
        Locale locale,
        String rootPath,
        CmsXmlSitemap xmlSitemap,
        CmsSiteEntryBean entry,
        int entryCount,
        Map<String, CmsXmlContentProperty> propertiesConf) throws CmsException {

        // create the entry node itself
        I_CmsXmlContentValue entryValue = xmlSitemap.getValue(rootPath == null
        ? CmsXmlSitemap.XmlNode.SITEENTRY.getName()
        : CmsXmlUtils.concatXpath(rootPath, CmsXmlSitemap.XmlNode.SITEENTRY.getName()), locale, entryCount);
        if (entryValue == null) {
            entryValue = xmlSitemap.addValue(cms, CmsXmlContainerPage.XmlNode.CONTAINER.getName(), locale, entryCount);
        }

        // entry info
        xmlSitemap.getValue(
            CmsXmlUtils.concatXpath(entryValue.getPath(), CmsXmlSitemap.XmlNode.NAME.getName()),
            locale,
            0).setStringValue(cms, entry.getName());
        xmlSitemap.getValue(
            CmsXmlUtils.concatXpath(entryValue.getPath(), CmsXmlSitemap.XmlNode.TITLE.getName()),
            locale,
            0).setStringValue(cms, entry.getTitle());
        CmsResource res = cms.readResource(entry.getResourceId());
        xmlSitemap.getValue(
            CmsXmlUtils.concatXpath(entryValue.getPath(), CmsXmlSitemap.XmlNode.VFSFILE.getName()),
            locale,
            0).setStringValue(cms, cms.getSitePath(res));

        // the properties
        int j = 0;
        for (Map.Entry<String, String> property : entry.getProperties().entrySet()) {
            if (!propertiesConf.containsKey(property.getKey())) {
                continue;
            }
            // only if the property is configured in the schema we will save it to the sitemap
            I_CmsXmlContentValue propValue = xmlSitemap.addValue(cms, CmsXmlUtils.concatXpath(
                entryValue.getPath(),
                CmsXmlContainerPage.XmlNode.PROPERTIES.getName()), locale, j);
            xmlSitemap.getValue(
                CmsXmlUtils.concatXpath(propValue.getPath(), CmsXmlContainerPage.XmlNode.NAME.getName()),
                locale,
                0).setStringValue(cms, property.getKey());
            I_CmsXmlContentValue valValue = xmlSitemap.addValue(cms, CmsXmlUtils.concatXpath(
                propValue.getPath(),
                CmsXmlContainerPage.XmlNode.VALUE.getName()), locale, 0);

            if (propertiesConf.get(property.getKey()).getPropertyType().equals(CmsXmlContentProperty.T_VFSLIST)) {
                I_CmsXmlContentValue filelistValue = xmlSitemap.addValue(cms, CmsXmlUtils.concatXpath(
                    valValue.getPath(),
                    CmsXmlContainerPage.XmlNode.FILELIST.getName()), locale, 0);
                int index = 0;
                for (String strId : CmsStringUtil.splitAsList(property.getValue(), CmsXmlContainerPage.IDS_SEPARATOR)) {
                    try {
                        CmsResource fileRes = cms.readResource(new CmsUUID(strId));
                        I_CmsXmlContentValue fileValue = xmlSitemap.getValue(CmsXmlUtils.concatXpath(
                            filelistValue.getPath(),
                            CmsXmlContainerPage.XmlNode.URI.getName()), locale, index);
                        if (fileValue == null) {
                            fileValue = xmlSitemap.addValue(cms, CmsXmlUtils.concatXpath(
                                filelistValue.getPath(),
                                CmsXmlContainerPage.XmlNode.URI.getName()), locale, index);
                        }
                        fileValue.setStringValue(cms, cms.getSitePath(fileRes));
                        index++;
                    } catch (CmsException e) {
                        // could happen when the resource are meanwhile deleted
                        LOG.error(e.getLocalizedMessage(), e);
                    }
                }
            } else {
                xmlSitemap.addValue(
                    cms,
                    CmsXmlUtils.concatXpath(valValue.getPath(), CmsXmlContainerPage.XmlNode.STRING.getName()),
                    locale,
                    0).setStringValue(cms, property.getValue());
            }
            j++;
        }

        // the subentries
        int subentryCount = 0;
        for (CmsSiteEntryBean subentry : entry.getSubEntries()) {
            saveEntry(cms, locale, entryValue.getPath(), xmlSitemap, subentry, subentryCount, propertiesConf);
            subentryCount++;
        }
    }
}