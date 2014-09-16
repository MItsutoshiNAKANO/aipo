/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2014 Aimluck,Inc.
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

package com.aimluck.eip.message.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.jetspeed.portal.Portlet;
import org.apache.jetspeed.util.template.BaseJetspeedLink;
import org.apache.jetspeed.util.template.ContentTemplateLink;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTMessage;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRead;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoom;
import com.aimluck.eip.cayenne.om.portlet.EipTMessageRoomMember;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.message.MessageMockPortlet;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SQLTemplate;

/**
 *
 */
public class MessageUtils {

  public static final String MESSAGE_PORTLET_NAME = "Message";

  public static void setupContext(RunData rundata, Context context) {
    Portlet portlet = new MessageMockPortlet();
    context.put("portlet", portlet);
    context.put("jslink", new BaseJetspeedLink(rundata));
    context.put("clink", new ContentTemplateLink(rundata));
  }

  public static EipTMessageRoom getRoom(int roomId) {
    return Database.get(EipTMessageRoom.class, roomId);
  }

  public static EipTMessageRoom getRoom(int userId, int targetUserId) {
    EipTMessageRoomMember model =
      Database.query(EipTMessageRoomMember.class).where(
        Operations.eq(EipTMessageRoomMember.USER_ID_PROPERTY, userId)).where(
        Operations.eq(
          EipTMessageRoomMember.TARGET_USER_ID_PROPERTY,
          targetUserId)).fetchSingle();
    if (model != null) {
      return model.getEipTMessageRoom();
    } else {
      return null;
    }
  }

  public static ResultList<EipTMessage> getMessageList(int roomId, int page,
      int limit) {
    StringBuilder select = new StringBuilder();

    select.append("select");
    select.append(" t1.message_id, ");
    select.append(" t1.room_id,  ");
    select.append(" t1.user_id, ");
    select.append(" t1.message, ");
    select.append(" t1.create_date, ");
    select.append(" t1.member_count, ");
    select.append(" t2.last_name, ");
    select.append(" t2.first_name, ");
    select.append(" t2.has_photo, ");

    select
      .append(" (select count(*) from eip_t_message_read t3 where t3.message_id = t1.message_id and t3.room_id = t1.room_id and t3.is_read = 'F') as unread ");

    StringBuilder count = new StringBuilder();
    count.append("select count(t1.message_id) AS c ");

    StringBuilder body = new StringBuilder();
    body
      .append("  from eip_t_message t1, turbine_user t2 where t1.user_id = t2.user_id and t1.room_id = #bind($room_id) ");

    StringBuilder last = new StringBuilder();

    last.append(" order by t1.create_date desc ");

    SQLTemplate<EipTMessage> countQuery =
      Database
        .sql(EipTMessage.class, count.toString() + body.toString())
        .param("room_id", Integer.valueOf(roomId));

    int countValue = 0;
    if (page > 0 && limit > 0) {
      List<DataRow> fetchCount = countQuery.fetchListAsDataRow();

      for (DataRow row : fetchCount) {
        countValue = ((Long) row.get("c")).intValue();
      }

      int offset = 0;
      if (limit > 0) {
        int num = ((int) (Math.ceil(countValue / (double) limit)));
        if ((num > 0) && (num < page)) {
          page = num;
        }
        offset = limit * (page - 1);
      } else {
        page = 1;
      }

      last.append(" LIMIT ");
      last.append(limit);
      last.append(" OFFSET ");
      last.append(offset);
    }

    SQLTemplate<EipTMessage> query =
      Database.sql(
        EipTMessage.class,
        select.toString() + body.toString() + last.toString()).param(
        "room_id",
        Integer.valueOf(roomId));

    List<DataRow> fetchList = query.fetchListAsDataRow();

    List<EipTMessage> list = new ArrayList<EipTMessage>();
    for (DataRow row : fetchList) {
      Long unread = (Long) row.get("unread");
      String lastName = (String) row.get("last_name");
      String firstName = (String) row.get("first_name");
      String hasPhoto = (String) row.get("has_photo");
      EipTMessage object = Database.objectFromRowData(row, EipTMessage.class);
      object.setUnreadCount(unread.intValue());
      object.setFirstName(firstName);
      object.setLastName(lastName);
      object.setHasPhoto(hasPhoto);
      list.add(object);
    }

    if (page > 0 && limit > 0) {
      return new ResultList<EipTMessage>(list, page, limit, countValue);
    } else {
      return new ResultList<EipTMessage>(list, -1, -1, list.size());
    }
  }

  public static ResultList<EipTMessageRoom> getRoomList(int userId) {
    return getRoomList(userId, null, -1, -1);
  }

  protected static ResultList<EipTMessageRoom> getRoomList(int userId,
      String keyword, int page, int limit) {
    StringBuilder select = new StringBuilder();

    boolean isMySQL = Database.isJdbcMySQL();
    boolean isSearch = (keyword != null && keyword.length() > 0);

    select.append("select");
    select.append(" t2.room_id, ");
    select.append(" t2.name, ");
    select.append(" t4.last_name, ");
    select.append(" t4.first_name, ");
    select.append(" t2.auto_name, ");
    select.append(" t2.room_type, ");

    select.append(" t2.last_message, ");
    select.append(" last_update_date, ");
    select
      .append(" (select count(*) from eip_t_message_read t3 where t3.room_id = t2.room_id and t3.user_id = #bind($user_id) and t3.is_read ='F') as unread ");

    StringBuilder count = new StringBuilder();
    count.append("select count(t2.room_id) AS c ");

    StringBuilder body = new StringBuilder();
    body
      .append("  from eip_t_message_room_member t1, eip_t_message_room t2, turbine_user t4 where t1.user_id = #bind($user_id) and t1.room_id = t2.room_id and t1.target_user_id = t4.user_id ");
    if (isSearch) {
      if (isMySQL) {
        body
          .append(" and ((t2.room_type='G' and t2.name like #bind($keyword)) or (t2.room_type='O' and CONCAT(t4.last_name,t4.first_name) like #bind($keyword))) ");
      } else {
        body
          .append(" and ((t2.room_type='G' and t2.name like #bind($keyword)) or (t2.room_type='O' and (t4.last_name || t4.first_name) like #bind($keyword))) ");
      }
    }

    StringBuilder last = new StringBuilder();

    last.append(" order by t2.last_update_date desc ");

    SQLTemplate<EipTMessageRoom> countQuery =
      Database
        .sql(EipTMessageRoom.class, count.toString() + body.toString())
        .param("user_id", Integer.valueOf(userId));
    if (isSearch) {
      countQuery.param("keyword", "%" + keyword + "%");
    }

    int countValue = 0;
    if (page > 0 && limit > 0) {
      List<DataRow> fetchCount = countQuery.fetchListAsDataRow();

      for (DataRow row : fetchCount) {
        countValue = ((Long) row.get("c")).intValue();
      }

      int offset = 0;
      if (limit > 0) {
        int num = ((int) (Math.ceil(countValue / (double) limit)));
        if ((num > 0) && (num < page)) {
          page = num;
        }
        offset = limit * (page - 1);
      } else {
        page = 1;
      }

      last.append(" LIMIT ");
      last.append(limit);
      last.append(" OFFSET ");
      last.append(offset);
    }

    SQLTemplate<EipTMessageRoom> query =
      Database.sql(
        EipTMessageRoom.class,
        select.toString() + body.toString() + last.toString()).param(
        "user_id",
        Integer.valueOf(userId));
    if (isSearch) {
      query.param("keyword", "%" + keyword + "%");
    }

    List<DataRow> fetchList = query.fetchListAsDataRow();

    List<EipTMessageRoom> list = new ArrayList<EipTMessageRoom>();
    for (DataRow row : fetchList) {
      Long unread = (Long) row.get("unread");
      String lastName = (String) row.get("last_name");
      String firstName = (String) row.get("first_name");
      EipTMessageRoom object =
        Database.objectFromRowData(row, EipTMessageRoom.class);
      object.setUnreadCount(unread.intValue());
      object.setFirstName(firstName);
      object.setLastName(lastName);
      list.add(object);
    }

    if (page > 0 && limit > 0) {
      return new ResultList<EipTMessageRoom>(list, page, limit, countValue);
    } else {
      return new ResultList<EipTMessageRoom>(list, -1, -1, list.size());
    }
  }

  public static ResultList<TurbineUser> getUserList(String groupName) {
    return getUserList(groupName, null, -1, -1);
  }

  public static ResultList<TurbineUser> getUserList(String groupName,
      String keyword, int page, int limit) {

    StringBuilder select = new StringBuilder();

    boolean isMySQL = Database.isJdbcMySQL();
    boolean isSearch = (keyword != null && keyword.length() > 0);

    select
      .append("select distinct t2.user_id, t2.login_name, t2.last_name, t2.first_name, t2.last_name_kana, t2.first_name_kana, t2.has_photo, t2.photo_modified, (t2.last_name_kana = '') ");

    StringBuilder count = new StringBuilder();
    count.append("select count(distinct t2.user_id) AS c ");

    StringBuilder body = new StringBuilder();
    body
      .append(" from turbine_user_group_role t1, turbine_user t2, turbine_group t3 where t1.user_id = t2.user_id and t1.group_id = t3.group_id and t2.user_id > 3 and t2.disabled = 'F' and t3.group_name = #bind($group_name)");
    if (isSearch) {
      if (isMySQL) {
        body
          .append(" and ( (CONCAT(t2.last_name,t2.first_name) like #bind($keyword)) or (CONCAT(t2.last_name_kana,t2.first_name_kana) like #bind($keyword)) ) ");
      } else {
        body
          .append(" and ( ((t2.last_name || t2.first_name)    like #bind($keyword)) or ((t2.last_name_kana || t2.first_name_kana)    like #bind($keyword)) ) ");
      }
    }

    StringBuilder last = new StringBuilder();

    last
      .append(" order by (t2.last_name_kana = ''), t2.last_name_kana, t2.first_name_kana ");

    SQLTemplate<TurbineUser> countQuery =
      Database
        .sql(TurbineUser.class, count.toString() + body.toString())
        .param("group_name", groupName);
    if (isSearch) {
      countQuery.param("keyword", "%" + keyword + "%");
    }

    int countValue = 0;
    if (page > 0 && limit > 0) {
      List<DataRow> fetchCount = countQuery.fetchListAsDataRow();

      for (DataRow row : fetchCount) {
        countValue = ((Long) row.get("c")).intValue();
      }

      int offset = 0;
      if (limit > 0) {
        int num = ((int) (Math.ceil(countValue / (double) limit)));
        if ((num > 0) && (num < page)) {
          page = num;
        }
        offset = limit * (page - 1);
      } else {
        page = 1;
      }

      last.append(" limit ");
      last.append(limit);
      last.append(" offset ");
      last.append(offset);
    }

    SQLTemplate<TurbineUser> query =
      Database.sql(
        TurbineUser.class,
        select.toString() + body.toString() + last.toString()).param(
        "group_name",
        groupName);
    if (isSearch) {
      query.param("keyword", "%" + keyword + "%");
    }

    List<TurbineUser> list = query.fetchList();

    if (page > 0 && limit > 0) {
      return new ResultList<TurbineUser>(list, page, limit, countValue);
    } else {
      return new ResultList<TurbineUser>(list, -1, -1, list.size());
    }
  }

  public static void read(int roomId, int userId, int lastMessageId) {
    SQLTemplate<EipTMessageRead> countQuery =
      Database
        .sql(
          EipTMessageRead.class,
          "select count(*) as c from eip_t_message_read where room_id = #bind($room_id) and user_id = #bind($user_id) and is_read = 'F' and message_id <= #bind($message_id)")
        .param("room_id", Integer.valueOf(roomId))
        .param("user_id", Integer.valueOf(userId))
        .param("message_id", Integer.valueOf(lastMessageId));

    int countValue = 0;
    List<DataRow> fetchCount = countQuery.fetchListAsDataRow();

    for (DataRow row : fetchCount) {
      countValue = ((Long) row.get("c")).intValue();
    }
    if (countValue > 0) {
      String sql =
        "update eip_t_message_read set is_read = 'T' where room_id = #bind($room_id) and user_id = #bind($user_id) and is_read = 'F' and message_id <= #bind($message_id)";
      Database
        .sql(EipTMessageRead.class, sql)
        .param("room_id", Integer.valueOf(roomId))
        .param("user_id", Integer.valueOf(userId))
        .param("message_id", Integer.valueOf(lastMessageId))
        .execute();
    }
  }
}
