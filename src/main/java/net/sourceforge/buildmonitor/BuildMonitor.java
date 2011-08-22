/*
 * Copyright 2007 Sebastien Brunot (sbrunot@gmail.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sourceforge.buildmonitor;

import java.awt.Image;
import java.util.List;

public interface BuildMonitor
{
	static final String MESSAGEKEY_TRAYICON_INITIAL_TOOLTIP = "trayIcon.tooltip.initialMessage";
	static final String MESSAGEKEY_TRAYICON_MENU_EXIT = "trayIcon.menu.exit";
	static final String MESSAGEKEY_TRAYICON_MENU_SORT = "trayIcon.menu.sort";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_EXIT = "trayIcon.menuItem.exit";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_SORT_BY_NAME = "trayIcon.menuItem.sortByName";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_SORT_BY_AGE = "trayIcon.menuItem.sortByAge";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_UPDATE_STATUS_NOW = "trayIcon.menuItem.update";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_BUILD_SERVER_HOME_PAGE_SUFFIX = "trayIcon.menuItem.buildServerHomePageSuffix";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_ABOUT = "trayIcon.menuItem.about";
	static final String MESSAGEKEY_TRAYICON_MENUITEM_OPTIONS = "trayIcon.menuItem.options";
	static final String MESSAGEKEY_ERROR_DIALOG_TITLE = "errorDialog.title";
	static final String MESSAGEKEY_UNEXPECTED_ERROR_MESSAGE = "unexpectedError.message";
	static final String MESSAGEKEY_ERROR_SYSTEMTRAY_NOT_SUPPORTED = "error.systemTray.not.supported";
	static final String MESSAGEKEY_ABOUT_TITLE = "about.title";
	static final String MESSAGEKEY_ABOUT_MESSAGE = "about.message";

	/**
	 * The method to call when an unrecoverable error occurs in the application. It displays
	 * an error message to the end user and then exit.
	 * @param theUnexpectedProblem the Throwable that was unexpected.
	 */
	void panic(Throwable theUnexpectedProblem);

	/**
	 * The method to call when an unrecoverable error occurs in the application and you want
	 * to display your own message (instead of a generic one).It displays your error message
	 * to the end user and then exit.
	 * @param theErrorMessage the error message to display to the end user.
	 */
	void panic(String theErrorMessage);

	/**
	 * Get a message identified by its key in the application resource bundle.
	 * The available messages key for the application all begins with prefix MESSAGEKEY_.
	 * @param theMessageKey the message key
	 * @return the message
	 */
	String getMessage(String theMessageKey);
	
	/**
	 * Returns the default icon to be displayed by dialogs opened by monitors
	 * @return the default icon to be displayed by dialogs opened by monitors
	 */
	Image getDialogsDefaultIcon();
	
	/**
	 * Update the build status (this method is called by build monitors to update the GUI when
	 * the build status has changed).
	 * 
	 * @param theBuildsStatus a List that contains the each build report to use to update the
	 * global status.
	 */
	void updateBuildStatus(List<BuildReport> theBuildsStatus);
	
	/**
	 * Report a monitoring exception (this method is called by build monitors to notify that
	 * a monitoring exception occured)
	 * 
	 * @param theMonitoringException the monitoring exception to display to the end user
	 */
	void reportMonitoringException(MonitoringException theMonitoringException);
	
	/**
	 * Report an update of the monitor configuration to be taken into account immediately
	 */
	void reportConfigurationUpdatedToBeTakenIntoAccountImmediately();
}
