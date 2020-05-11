package com.go.notification.processor.subscribers;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.notification.processor.constants.EventLevels;
import com.go.notification.processor.dao.ProcessingDAO;
import com.go.notification.processor.models.NotificationEventMeta;
import com.go.notification.processor.service.NotificationProcessingHelper;
import com.google.common.eventbus.EventBus;
import com.ne.commons.notification.vo.NotificationProcessingVO;

@Component
public class EventSubscriber {

	private static final Logger log = LoggerFactory.getLogger(EventSubscriber.class);

	@Autowired
	NotificationProcessingHelper processingService;

	@Autowired
	ProcessingDAO processingDAO;

	@Autowired
	EventBus eventBus;

	@KafkaListener(topics = "${kafka.topic.notification-event}")
	public <T> void processNotification(List<String> content) {
		for (String string : content) {
			log.info("Received Notification Event");
			ObjectMapper mapper = new ObjectMapper();
			NotificationEventMeta eventMetaObj = null;
			String notificationId = null;
			try {
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				NotificationProcessingVO processObj = mapper.readValue(string, NotificationProcessingVO.class);
				eventMetaObj = processingService.getEventMetaInfo(processObj);
				log.info("eventsList"+eventMetaObj.getEventChannels().toString());
				processObj.setEvent(eventMetaObj.getEvent());

				// creating notification record
				try {
					notificationId = processingDAO.insertNotificationRecord(processObj);
					System.out.println(notificationId);
				} catch (Exception e) {
					log.error("Exception in inserting notification to the Database:", e);
				}
				if (eventMetaObj.getEventLevel().equalsIgnoreCase(EventLevels.DISABLE.toString())) {
					log.info("Following Event is Disabled:", eventMetaObj.getEvent());
					return;
				}
				eventMetaObj.setEventAttributes(processObj.getEventAttributes());
				eventMetaObj.setNotificationUsers(processObj.getNotificationUsers());
				eventMetaObj.setCommonEventContent(processObj.getCommonEventContent());
				eventMetaObj.setNotificationId(notificationId);
				eventMetaObj.setEntityId(processObj.getEntityId());
				eventMetaObj.setEntityType(processObj.getEntityType());
				eventMetaObj.setEventId(processObj.getEventId());
				eventMetaObj.setAction(processObj.getAction());
				eventBus.post(eventMetaObj);

			} catch (Exception e) {
				log.error("Exception in subscribing notification event:", e);
			}
		}
	}

	@KafkaListener(topics = "${kafka.topic.msg-notification}")
	public <T> void processMsgNotification(List<String> content) {
		for (String string : content) {
			log.info("Received Notification Event" +string);
			ObjectMapper mapper = new ObjectMapper();
			NotificationEventMeta eventMetaObj = null;
			String notificationId = null;
			try {
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				NotificationProcessingVO processObj = mapper.readValue(string, NotificationProcessingVO.class);
				eventMetaObj = processingService.getEventMetaInfo(processObj);
				processObj.setEvent(eventMetaObj.getEvent());

				// creating notification record
				try {
					notificationId = processingDAO.insertNotificationRecord(processObj);
					System.out.println(notificationId);
				} catch (Exception e) {
					log.error("Exception in inserting notification to the Database:", e);
				}
				if (eventMetaObj.getEventLevel().equalsIgnoreCase(EventLevels.DISABLE.toString())) {
					log.info("Following Event is Disabled:", eventMetaObj.getEvent());
					return;
				}
				eventMetaObj.setEventAttributes(processObj.getEventAttributes());
				eventMetaObj.setNotificationUsers(processObj.getNotificationUsers());
				eventMetaObj.setCommonEventContent(processObj.getCommonEventContent());
				eventMetaObj.setNotificationId(notificationId);
				eventMetaObj.setEntityId(processObj.getEntityId());
				eventMetaObj.setEntityType(processObj.getEntityType());
				eventMetaObj.setEventId(processObj.getEventId());
				eventMetaObj.setAction(processObj.getAction());
				eventBus.post(eventMetaObj);

			} catch (Exception e) {
				log.error("Exception in subscribing notification event:", e);
			}
		}
	}
}
