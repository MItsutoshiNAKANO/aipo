/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
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

dojo.require("aipo.widget.MemberNormalSelectList");

dojo.provide("aipo.msgboard");



aipo.msgboard.onLoadMsgboardDetail = function(portlet_id){
  aipo.portletReload('whatsnew');
}

aipo.msgboard.onLoadMsgboardDialog = function(portlet_id){
  var obj = dojo.byId("topic_name");
  if(obj){
     obj.focus();
  }
}

aipo.msgboard.onChangeFilter=aipo.msgboard.onChangeSearch=function (baseuri,portlet_id){
	var search = encodeURIComponent(dojo.byId("q").value);
	baseuri+="?template=MsgboardTopicListScreen";
	baseuri+="&filter="+dojo.byId("topic").value;
	baseuri+="&filtertype=category";
	baseuri+="&search="+search;
	aipo.viewPage(baseuri,portlet_id);
}

aipo.msgboard.onLoadCategoryDialog = function(portlet_id){
  var obj = dojo.byId("category_name");
  if(obj){
     obj.focus();
  }

  var mpicker = dijit.byId("membernormalselect");
  if(mpicker){
    var select = dojo.byId('init_memberlist');
    var i;
    var s_o = select.options;
    if (s_o.length == 1 && s_o[0].value == "") return;
    for(i = 0 ; i < s_o.length; i ++ ) {
        mpicker.addOptionSync(s_o[i].value,s_o[i].text,true);
    }
  }
}



aipo.msgboard.showMember = function(button) {
  dojo.byId('Block-GroupMember-Show').style.display="";
  dojo.byId('is_member').value = "TRUE";
}

aipo.msgboard.hideMember = function(button) {
  dojo.byId('Block-GroupMember-Show').style.display="none";
  dojo.byId('member_to').options.length = 0;
  dojo.byId('is_member').value = "FALSE";
}

aipo.msgboard.expandImageWidth = function(img) {
  var class_name = img.className;
  if(! class_name.match(/width_auto/i)) {
    img.className = img.className.replace( /\bwidth_thumbs\b/g, "width_auto");
  } else {
    img.className = img.className.replace( /\bwidth_auto\b/g, "width_thumbs");
  }
}

aipo.msgboard.formSwitchCategoryInput = function(button) {
    if(button.form.is_new_category.value == 'TRUE' || button.form.is_new_category.value == 'true') {
        button.value = aimluck.io.escapeText("msgboard_val_switch1");
        aipo.msgboard.formCategoryInputOff(button.form);
    } else {
        button.value = aimluck.io.escapeText("msgboard_val_switch2");
        aipo.msgboard.formCategoryInputOn(button.form);
    }
}

aipo.msgboard.formCategoryInputOn = function(form) {
    dojo.byId('msgboardCategorySelectField').style.display = "none";
    dojo.byId('msgboardCategoryInputField').style.display = "";

    form.is_new_category.value = 'TRUE';
}

aipo.msgboard.formCategoryInputOff = function(form) {
    dojo.byId('msgboardCategoryInputField').style.display = "none";
    dojo.byId('msgboardCategorySelectField').style.display = "";

    form.is_new_category.value = 'FALSE';
}

aipo.msgboard.onReceiveMessage = function(msg){
    //送信時に作成した場合selectを削除。
	var select=dojo.byId("attachments_select");
	if(typeof select!="undefined"&& select!=null)
		select.parentNode.removeChild(select);
    if(!msg) {
        var arrDialog = dijit.byId("modalDialog");
        if(arrDialog){
            arrDialog.hide();
        }
        aipo.portletReload('msgboard');
        aipo.portletReload('timeline');
    }
    if (dojo.byId('messageDiv')) {
        dojo.byId('messageDiv').innerHTML = msg;
    }
}

aipo.msgboard.onListReceiveMessage = function(msg){
    if(!msg) {
        var arrDialog = dijit.byId("modalDialog");
        if(arrDialog){
            arrDialog.hide();
        }
        aipo.portletReload('msgboard');
    }
    if (dojo.byId('listmessageDiv')) {
        dojo.byId('listmessageDiv').innerHTML = msg;
    }
}

aipo.msgboard.ajaxCheckboxDeleteSubmit = function(button, url, indicator_id, portlet_id, receive) {
  aimluck.io.ajaxVerifyCheckbox( button.form, aipo.msgboard.ajaxMultiDeleteSubmit, button, url, indicator_id, portlet_id, receive );
}

aipo.msgboard.ajaxMultiDeleteSubmit = function(button, url, indicator_id, portlet_id, receive) {
  if(confirm('選択した'+button.form._name.value+'を削除してよろしいですか？なお、カテゴリに含まれるトピックはすべて削除されます。')) {
    aimluck.io.disableForm(button.form, true);
    aimluck.io.setHiddenValue(button);
    button.form.action = url;
    aimluck.io.submit(button.form,indicator_id,portlet_id,receive);
  }
}

aipo.msgboard.ajaxDeleteSubmit = function(button, url, indicator_id, portlet_id, receive) {
  if(confirm('この'+button.form._name.value+'を削除してよろしいですか？なお、カテゴリに含まれるトピックはすべて削除されます。')) {
    aimluck.io.disableForm(button.form, true);
    aimluck.io.setHiddenValue(button);
    button.form.action = url;
    aimluck.io.submit(button.form, indicator_id, portlet_id, receive);
  }
}
