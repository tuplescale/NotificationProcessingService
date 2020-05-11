package com.go.notification.processor.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.go.notification.processor.configuration.LRUCache;
import com.go.notification.processor.dao.ProcessingDAO;
import com.go.notification.processor.models.NotificationEventMeta;
import com.ne.commons.notification.vo.NotificationProcessingVO;
import com.ne.commons.notification.vo.NotificationVO;
import com.ne.commons.notification.vo.User;

@Component
public class NotificationProcessingHelperImpl implements NotificationProcessingHelper {

	private static final Logger log = LoggerFactory.getLogger(NotificationProcessingHelper.class);

	static List<String> notifyPreference = new ArrayList<String>();
	static List<String> notNotifyPreference = new ArrayList<String>();

	@PostConstruct
	public void init() {
		// notifyPreference.add("EMAIL-15m");
		// notifyPreference.add("EMAIL-1h");
		notifyPreference.add("EMAIL-ALL");
		// notifyPreference.add("DESKTOP-ALL");
		notifyPreference.add("BELL-ALL");
		notifyPreference.add("BELL-CUSTOM");
		notifyPreference.add("P1");
		notifyPreference.add("P2");
		notifyPreference.add("P3");

		// notNotifyPreference.add("DESKTOP-OFF");
		// notNotifyPreference.add("BELL-OFF");

	}

	@Autowired
	ProcessingDAO processingDAO;

	static Map<Integer, NotificationEventMeta> eventMetaCache = Collections
			.synchronizedMap(new LRUCache<Integer, NotificationEventMeta>(100));

	@Override
	public NotificationEventMeta getEventMetaInfo(NotificationProcessingVO eventObj) {
		NotificationEventMeta eventMetaObj = null;
		try {
			eventMetaObj = eventMetaCache.get(eventObj.getEventId());
			if (eventMetaObj == null) {
				eventMetaObj = processingDAO.getEventMeta(eventObj.getEventId());
				eventMetaObj = processingDAO.getEventChannelDetails(eventMetaObj);
				eventMetaCache.put(eventObj.getEventId(), eventMetaObj);

			}

		} catch (Exception e) {
			log.error("Exception in getting notification Event Meta Detail :", e);
		}
		return eventMetaObj;
	}

	@Override
	public List<User> getNotificationUserPreferences(NotificationVO notificationObj, String preferenceType) {
		List<User> newUserList = new ArrayList<User>();
		try {
			String preference = null;
			for (User user : notificationObj.getUsers()) {
				preference = processingDAO.getUserEmailPreference(user, preferenceType);
				log.info("user notification  preference : " + preference + ":::" + user.getUserName());

				if (notifyPreference.contains(preference)) {
					newUserList.add(user);
					continue;
				} else {
					log.info("user opted not to notify");
				}

			}
		} catch (Exception e) {
			log.error("Exception in getting user preferences to the notification event:", e);
		}
		return newUserList;
	}

}
