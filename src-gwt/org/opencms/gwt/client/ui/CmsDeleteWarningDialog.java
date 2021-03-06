/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH (http://www.alkacon.com)
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

package org.opencms.gwt.client.ui;

import org.opencms.gwt.client.CmsCoreProvider;
import org.opencms.gwt.client.Messages;
import org.opencms.gwt.client.rpc.CmsRpcAction;
import org.opencms.gwt.shared.CmsDeleteResourceBean;

import com.google.gwt.user.client.Command;

/**
 * A dialog which informs the user that deleting a resource will break links
 * from other resources.<p>
 * 
 * @since 8.0.0
 */
public class CmsDeleteWarningDialog extends CmsConfirmDialog {

    private static final String TM_DIALOG_LIST = "dialogList";

    /** The callback command. */
    protected Command m_cmd;

    /** The content of the dialog. */
    protected CmsLinkWarningPanel m_content = new CmsLinkWarningPanel();

    /** The default dialog handler. */
    private I_CmsConfirmDialogHandler m_handler = new I_CmsConfirmDialogHandler() {

        /**
         * @see org.opencms.gwt.client.ui.I_CmsCloseDialogHandler#onClose()
         */
        public void onClose() {

            // do nothing 
        }

        /**
         * @see org.opencms.gwt.client.ui.I_CmsConfirmDialogHandler#onOk()
         */
        public void onOk() {

            deleteResource();

            // execute the callback if present
            if (m_cmd != null) {
                m_cmd.execute();
            }
        }
    };

    /** The site path of the resource to delete. */
    private String m_sitePath;

    /**
     * Constructor.<p>
     * 
     * @param sitePath the site-path of the resource going to be deleted
     */
    public CmsDeleteWarningDialog(String sitePath) {

        super(Messages.get().key(Messages.GUI_DIALOG_DELETE_TITLE_0));
        m_sitePath = sitePath;
        setWarningMessage(Messages.get().key(Messages.GUI_DIALOG_DELETE_TEXT_0));
        setOkText(Messages.get().key(Messages.GUI_DELETE_0));
        setCloseText(Messages.get().key(Messages.GUI_CANCEL_0));
        setHandler(m_handler);
    }

    /**
     * Loads and shows the delete dialog.<p>
     * 
     * @param callback the callback that is executed when the resource was deleted (can be <code>null</code>)
     */
    public void loadAndShow(Command callback) {

        m_cmd = callback;
        checkBrokenLinks();
    }

    /**
     * Sets the site path.<p>
     * 
     * @param sitePath the site path to set
     */
    public void setSitePath(String sitePath) {

        m_sitePath = sitePath;
    }

    /**
     * Deletes a resource from the vfs.<p>
     */
    protected void deleteResource() {

        final String sitePath = m_sitePath;

        CmsRpcAction<Void> action = new CmsRpcAction<Void>() {

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
             */
            @Override
            public void execute() {

                CmsCoreProvider.getVfsService().deleteResource(sitePath, this);

            }

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
             */
            @Override
            protected void onResponse(Void result) {

                onAfterDeletion();
            }
        };
        action.execute();
    }

    /**
     * Method which should be called after the deletion has been performed.<p>
     */
    protected void onAfterDeletion() {

        // do nothing by default 
    }

    /**
     * Checks for broken links, ask for confirmation and finally deletes the given resource.<p>
     */
    private void checkBrokenLinks() {

        final String sitePath = m_sitePath;

        CmsRpcAction<CmsDeleteResourceBean> action = new CmsRpcAction<CmsDeleteResourceBean>() {

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#execute()
             */
            @Override
            public void execute() {

                start(0, true);

                CmsCoreProvider.getVfsService().getBrokenLinks(sitePath, this);
            }

            /**
             * @see org.opencms.gwt.client.rpc.CmsRpcAction#onResponse(java.lang.Object)
             */
            @Override
            protected void onResponse(CmsDeleteResourceBean result) {

                stop(false);
                CmsListItemWidget widget = new CmsListItemWidget(result.getPageInfo());
                widget.truncate(TM_DIALOG_LIST, 370);
                addTopWidget(widget);
                if (result.getBrokenLinks().size() > 0) {
                    m_content.fill(result.getBrokenLinks());
                    addBottomWidget(m_content);
                    setWidth(600);
                }
                center();
            }
        };
        action.execute();
    }

}
