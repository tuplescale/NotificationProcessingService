package com.go.notification.processor.dao;

import java.sql.SQLException;

import com.go.notification.processor.models.NotificationEventMeta;
import com.ne.commons.notification.vo.NotificationProcessingVO;
import com.ne.commons.notification.vo.NotificationVO;
import com.ne.commons.notification.vo.User;

public interface ProcessingDAO {

	public NotificationEventMeta getEventMeta(int eventId) throws SQLException;

	public NotificationEventMeta getEventChannelDetails(NotificationEventMeta eventMetaObj) throws SQLException;

	public String getUserEmailPreference(User user,String userPreference) throws SQLException;

	public String insertNotificationRecord(NotificationProcessingVO processObj) throws SQLException;

	public boolean insertNotificationUsers(NotificationVO notificationObj) throws SQLException;
	
	void unSubscribePushNotification(long userId) throws SQLException;

}
