package com.go.notification.processor.eventbus.events;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.notification.processor.constants.EventLevels;
import com.go.notification.processor.constants.NotificationTypes;
import com.go.notification.processor.dao.ProcessingDAO;
import com.go.notification.processor.models.NotificationEventMeta;
import com.go.notification.processor.service.JedisService;
import com.go.notification.processor.service.KafkaProducer;
import com.go.notification.processor.service.NotificationProcessingHelper;
import com.google.common.eventbus.Subscribe;
import com.ne.commons.notification.vo.BellNotificationVO;
import com.ne.commons.notification.vo.User;

@Component
public class BellNotificationListener {

	private static final Logger log = LoggerFactory.getLogger(BellNotificationListener.class);

	@Value("${kafka.topic.notification-bell}")
	private String bellNotificationTopic;

	@Autowired
	KafkaProducer producer;

	@Autowired
	JedisService jedisService;
	
	@Autowired
	NotificationProcessingHelper processingHelper;

	@Autowired
	ProcessingDAO processingDAO;

	@Subscribe
	public void bellNotificationListener(NotificationEventMeta eventObj) {
		log.info("Subscribed Email Event from Event Bus");

		BellNotificationVO bellObj = null;
		ObjectMapper mapper = new ObjectMapper();
		String eventData = null;
		List<String> eventChannels = eventObj.getEventChannels();
		if (eventChannels.contains(NotificationTypes.BELL.toString())) {
			try {

				if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.FORCED.toString())) {
					bellObj = constructBellNotificationVO(eventObj);
					eventData = mapper.writeValueAsString(bellObj);
					// insert notification users
					processingDAO.insertNotificationUsers(bellObj);
					producer.send(bellNotificationTopic, eventData);
					log.info("Bell Event Sent Sucessfully to Notification Service");
				} else if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.PREFERENCE.toString())) {
					bellObj = constructBellNotificationVO(eventObj);
					List<User> updatedUserList = processingHelper.getNotificationUserPreferences(bellObj,"BELL-NOTIFICATION");
					bellObj.setUser(updatedUserList);
					if (bellObj.getUsers().size() > 0) {
						eventData = mapper.writeValueAsString(bellObj);
						// insert notification users
						processingDAO.insertNotificationUsers(bellObj);
						producer.send(bellNotificationTopic, eventData);
						log.info("Bell Event Sent Sucessfully to Notification Service");
					} else {
						log.info("No users with preferences found to send the Bell Email");
					}

				} else {
					log.error("Unknow Notification EventType to process:", eventObj.getEventLevel());
					return;
				}
			} catch (Exception e) {
				log.error("Exception in sending Email Notification", e);
			}
		} else {
			log.info("Bell channel not found for event with name:" + eventObj.getEvent());
		}

	}

	private BellNotificationVO constructBellNotificationVO(NotificationEventMeta eventObj) {

		BellNotificationVO bellObj = new BellNotificationVO();
		bellObj.setNotificationType(NotificationTypes.BELL.toString());
		bellObj.setNotificationId(eventObj.getNotificationId());
		bellObj.setUser(eventObj.getNotificationUsers());
		try {
			eventObj.getCommonEventContent().getUser().setOnline_status(jedisService.getUserStatusFromRedis(eventObj.getCommonEventContent().getUser().getId()));
		} catch (SQLException e) {
			log.error("error while setting the online status");
	    }
		bellObj.setCommonEventContent(eventObj.getCommonEventContent());
		bellObj.setEntityId(eventObj.getEntityId());
		bellObj.setEntityType(eventObj.getEntityType());
		bellObj.setEvent(eventObj.getEvent());
		bellObj.setEventTime(new Timestamp(System.currentTimeMillis()).getTime());
		bellObj.setNotificationLevel(eventObj.getEventLevel());
		bellObj.setAction(eventObj.getAction());
		return bellObj;
	}

}
