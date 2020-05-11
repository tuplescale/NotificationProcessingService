package com.go.notification.processor.service;

import java.util.List;

import com.go.notification.processor.models.NotificationEventMeta;
import com.ne.commons.notification.vo.NotificationProcessingVO;
import com.ne.commons.notification.vo.NotificationVO;
import com.ne.commons.notification.vo.PushNotificationVO;
import com.ne.commons.notification.vo.User;

public interface NotificationProcessingHelper {
	
	public NotificationEventMeta getEventMetaInfo(NotificationProcessingVO eventObj);
	
	public List<User> getNotificationUserPreferences(NotificationVO notificationObj,String preferenceType);



}
