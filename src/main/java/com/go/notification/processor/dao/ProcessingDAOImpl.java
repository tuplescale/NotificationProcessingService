package com.go.notification.processor.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.notification.processor.configuration.LRUCache;
import com.go.notification.processor.models.NotificationEventMeta;
import com.ne.commons.notification.vo.NotificationProcessingVO;
import com.ne.commons.notification.vo.NotificationVO;
import com.ne.commons.notification.vo.User;

@Component
public class ProcessingDAOImpl implements ProcessingDAO {

	private static Logger log = LoggerFactory.getLogger(ProcessingDAOImpl.class);

	@Autowired
	private DataSource dataSource;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	static Map<String, String> userEmailPreferenceCache = Collections.synchronizedMap(new LRUCache<String, String>(1000));

	@Override
	public NotificationEventMeta getEventMeta(int eventId) throws SQLException {
		NotificationEventMeta eventMetaObj = new NotificationEventMeta();
		Connection conn = null;
		conn = dataSource.getConnection();
		if (conn == null) {
			throw new SQLException("Unable to get the Connection!");
		}
		String getEventMetaSQL = "select * from ne_notification_events where event_id=?";
		PreparedStatement ps = conn.prepareStatement(getEventMetaSQL);
		ps.setInt(1, eventId);
		ResultSet rs = null;
		try {
			rs = ps.executeQuery();
			while (rs.next()) {
				eventMetaObj.setEventId(rs.getInt("event_id"));
				eventMetaObj.setEventLevel(rs.getString("notification_level"));
				eventMetaObj.setEvent(rs.getString("event"));
				eventMetaObj.setPriority(rs.getInt("priority"));
			}
		} catch (SQLException e) {
			log.error("Error in getting notification event meta details:", e);
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}
			if (ps != null) {
				ps.close();
				ps = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}

		return eventMetaObj;
	}

	@Override
	public NotificationEventMeta getEventChannelDetails(NotificationEventMeta eventMetaObj) throws SQLException {
		List<String> eventChannels = new ArrayList<String>();
		Connection conn = null;
		conn = dataSource.getConnection();
		if (conn == null) {
			throw new SQLException("Unable to get the Connection!");
		}
		String getEventChannelsSQL = "select event_id,notification_type"
				+ " from ne_notification_event_types  where event_id=?";

		PreparedStatement ps = conn.prepareStatement(getEventChannelsSQL);
		ps.setInt(1, eventMetaObj.getEventId());
		ResultSet rs = null;
		try {
			rs = ps.executeQuery();
			while (rs.next()) {
				eventChannels.add(rs.getString("notification_type"));
			}
			eventMetaObj.setEventChannels(eventChannels);
		} catch (SQLException e) {
			log.error("Error in getting notification event meta details:", e);
		} finally {
			if (rs != null) {
				rs.close();
				rs = null;
			}
			if (ps != null) {
				ps.close();
				ps = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}

		return eventMetaObj;
	}

	@Override
	public String getUserEmailPreference(User user,String userPreference) throws SQLException {
		String preference = null;
		Connection conn = null;

		// getting preference from cache
		/*try {
			preference = userEmailPreferenceCache.get(user.getUserId()+"-"+userPreference);
		} catch (Exception e) {
			log.debug("Preference not found in cache:");
		}*/
		if (preference == null) {
			conn = dataSource.getConnection();
			if (conn == null) {
				throw new SQLException("Unable to get the Connection!");
			}
			String getEventChannelsSQL = " SELECT userpreference FROM `ne_preferences` WHERE userid=? AND preferencetype=? ;";
			PreparedStatement ps = conn.prepareStatement(getEventChannelsSQL);
			ps.setLong(1, user.getUserId());
			ps.setString(2,userPreference);
			ResultSet rs = null;
			try {
				rs = ps.executeQuery();
				while (rs.next()) {
					preference = rs.getString("userpreference");
					//userEmailPreferenceCache.put(user.getUserId()+"-"+userPreference, preference);
				}
			} catch (SQLException e) {
				log.error("Error in getting user notification preference:", e);
			} finally {
				if (rs != null) {
					rs.close();
					rs = null;
				}
				if (ps != null) {
					ps.close();
					ps = null;
				}
				if (conn != null) {
					conn.close();
					conn = null;
				}
			}
		}

		return preference;
	}

	@Override
	public String insertNotificationRecord(NotificationProcessingVO processObj) throws SQLException {
		String notificationId = UUID.randomUUID().toString();
		Connection conn = null;
		int count = 0;
		conn = dataSource.getConnection();
		if (conn == null) {
			throw new SQLException("Unable to get the Connection!");
		}
		String notificationInsertQuery = "INSERT INTO ne_notifications(notification_id,entity_id,entity_type,action,notification_data)"
				+ " VALUES(?,?,?,?,?) ";
		ObjectMapper mapper = new ObjectMapper();
		PreparedStatement ps = conn.prepareStatement(notificationInsertQuery);

		log.info("Query : " +ps.toString());
		try {
			String data = mapper.writeValueAsString(processObj);
			ps.setString(1, notificationId);
			ps.setString(2, processObj.getEntityId());
			ps.setString(3, processObj.getEntityType());
			ps.setString(4, processObj.getAction());
			ps.setObject(5, data);
			count = ps.executeUpdate();
			if (count > 0) {
				log.info("Notification Inserted successfully");
			}
		} catch (Exception e) {
			log.error("Exception in inserting notification Record to DB:", e);
		} finally {

			if (ps != null) {
				ps.close();
				ps = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}
		return notificationId;
	}

	@Override
	public boolean insertNotificationUsers(NotificationVO notificationObj) throws SQLException {
		boolean flag = false;
		Connection conn = null;
		int count = 0;
		conn = dataSource.getConnection();
		if (conn == null) {
			throw new SQLException("Unable to get the Connection!");
		}
		String notificationInsertQuery = "INSERT INTO ne_notification_users(notification_id,user_id,status,last_modified,notification_type)"
				+ " VALUES(?,?,?,?,?) ";
		PreparedStatement stm = conn.prepareStatement(notificationInsertQuery);

		try {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			for (User user : notificationObj.getUsers()) {
				if (user != null) {

					stm.setString(1, notificationObj.getNotificationId());
					stm.setLong(2, user.getUserId());
					stm.setInt(3, 0);
					stm.setTimestamp(4, timestamp);
					stm.setString(5, notificationObj.getNotificationType());
					stm.addBatch();
				}
			}
			stm.executeBatch();
			if (count > 0) {
				log.info("Notification Users Inserted successfully");
			}
		} catch (Exception e) {
			log.error("Exception in inserting notification Users Record to DB:", e);
		} finally {

			if (stm != null) {
				stm.close();
				stm = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}
		return flag;
	}

	@Override
	public void unSubscribePushNotification(long userId) throws SQLException {
		Connection conn = null;
		int count = 0;
		conn = dataSource.getConnection();
		if (conn == null) {
			throw new SQLException("Unable to get the Connection!");
		}
		String unSubscribe = "UPDATE ne_push_notification set status=0 where user_id=? AND push_type = 'BROWSER'";
		PreparedStatement stm = conn.prepareStatement(unSubscribe);

		try {
			stm = conn.prepareStatement(unSubscribe);
			stm.setLong(1, userId);
			count = stm.executeUpdate();
			if(count > 0) {
				log.info("Unsubscribe push notification success for User Id:"+userId);
			}
		} catch (Exception e) {
			log.error("Exception in inserting notification Users Record to DB:", e);
		} finally {

			if (stm != null) {
				stm.close();
				stm = null;
			}
			if (conn != null) {
				conn.close();
				conn = null;
			}
		}
	}

}
