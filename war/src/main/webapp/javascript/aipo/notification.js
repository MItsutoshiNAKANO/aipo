/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2015 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
dojo.provide("aipo.notification");

aipo.notification.setUp = function() {
    var notificationRequestField = dojo.query(".notificationRequestField");
    var notificationConfirmField = dojo.query(".notificationConfirmField");
    var notificationSuccessField = dojo.query(".notificationSuccessField");
    if(aipo.notification.isDefault()) {
    	dojo.forEach(notificationRequestField,function(item){item.style.display="";});
    }
}

aipo.notification.hasFeature = function() {
	return window.Notification || window.webkitNotifications;
}

aipo.notification.isGranted = function() {
	return (window.Notification && window.Notification.permission == "granted") || (window.webkitNotifications && window.webkitNotifications.checkPermission() == 0);
}

aipo.notification.isDefault = function() {
	return (window.Notification && window.Notification.permission == "default") || (window.webkitNotifications && window.webkitNotifications.checkPermission() == 1);
}

aipo.notification.requestPermission = function(callback) {
	if(window.Notification) {
	    window.Notification.requestPermission(callback);
	} else if(window.webkitNotifications) {
	    window.webkitNotifications.requestPermission(callback);
	}
}

aipo.notification.enableDesktopNotification = function(enabled) {
    var handleJSONResponse = function(obj) {
        if (obj.rc == 200) {
            var data = obj.data;
            if(data){
                aipo.activityDesktopNotifyEnable = data.enable;
            }
        }
    };

    var request = {
    };

    var makeRequestParams = {
       "CONTENT_TYPE" : "JSON",
       "METHOD" : "POST",
       "POST_DATA" : gadgets.json.stringify(request)
    };

    var url = "?template=ActivityNotifyEnableJSONScreen&enable=" + enabled;

    gadgets.io.makeNonProxiedRequest(url,
            handleJSONResponse,
            makeRequestParams,
            "application/javascript");
}

aipo.notification.requestDesktopNotification = function(portletId) {
    var notificationRequestField = dojo.query(".notificationRequestField");
    var notificationConfirmField = dojo.query(".notificationConfirmField");
    var notificationSuccessField = dojo.query(".notificationSuccessField");

    var callback = function() {
    	if(aipo.notification.isGranted()) {
        	dojo.forEach(notificationRequestField,function(item){item.style.display="none";});
        	dojo.forEach(notificationConfirmField,function(item){item.style.display="none";});
        	dojo.forEach(notificationSuccessField,function(item){item.style.display="";});
            aipo.notification.enableDesktopNotification('T');
            if(portletId) {
                dojo.byId("desktopNotification" + portletId + "_T").checked = true;
            }
        	setTimeout(function() { aipo.notification.closeRequest(); }, 3000);
        } else if(aipo.notification.isDefault()) {
        	dojo.forEach(notificationSuccessField,function(item){item.style.display="none";});
        	dojo.forEach(notificationConfirmField,function(item){item.style.display="none";});
        	dojo.forEach(notificationRequestField,function(item){item.style.display="";});
        } else {
        	dojo.forEach(notificationSuccessField,function(item){item.style.display="none";});
        	dojo.forEach(notificationConfirmField,function(item){item.style.display="none";});
        	dojo.forEach(notificationRequestField,function(item){item.style.display="none";});
        	dojo.forEach(dojo.query(".notificationBlockField"),function(item){item.style.display="";});
        }
    };
    aipo.notification.requestPermission(callback);
	dojo.forEach(notificationRequestField,function(item){item.style.display="none";});
	dojo.forEach(notificationSuccessField,function(item){item.style.display="none";});
	dojo.forEach(notificationConfirmField,function(item){item.style.display="";});
}



aipo.notification.closeRequest = function() {
    var notificationField = dojo.byId("notificationField");
    if(notificationField) {
    	notificationField.style.display="none";
    }
}

aipo.notification.openDialog = function() {
	aipo.menu.hideDropdownAll();
	aipo.notification.closeRequest();
	if (window.Notification) {
		aipo.common.showDialog("?template=ActivityNotificationFormScreen&s=1&p=" + window.Notification.permission);
    } else if (window.webkitNotifications) {
    	aipo.common.showDialog("?template=ActivityNotificationFormScreen&s=1&p=" + window.webkitNotifications.checkPermission());
    } else  {
    	aipo.common.showDialog("?template=ActivityNotificationFormScreen&s=0");
    }
}

aipo.notification.onReceiveMessage = function(msg){
    if(!msg) {
        if(window.webkitNotifications || window.Notification) {
        	aipo.activityDesktopNotifyEnable = null;
            aipo.container.gadgetService.requestDesktopNotifyEnable();
        }
        var arrDialog = dijit.byId("modalDialog");
        if(arrDialog){
            arrDialog.hide();
        }
    }
    if (dojo.byId('messageDiv')) {
        dojo.byId('messageDiv').innerHTML = msg;
    }
}
