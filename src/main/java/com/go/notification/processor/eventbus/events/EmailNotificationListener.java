package com.go.notification.processor.eventbus.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.notification.processor.constants.EventLevels;
import com.go.notification.processor.constants.EventTemplates;
import com.go.notification.processor.constants.NotificationTypes;
import com.go.notification.processor.dao.ProcessingDAO;
import com.go.notification.processor.models.NotificationEventMeta;
import com.go.notification.processor.service.JedisService;
import com.go.notification.processor.service.KafkaProducer;
import com.go.notification.processor.service.NotificationProcessingHelper;
import com.google.common.eventbus.Subscribe;
import com.ne.commons.constants.EventConstants;
import com.ne.commons.notification.vo.EmailNotificationVO;
import com.ne.commons.notification.vo.User;

@Component
public class EmailNotificationListener {

	@Autowired
	KafkaProducer producer;

	@Autowired
	NotificationProcessingHelper processingHelper;

	@Autowired
	ProcessingDAO processingDAO;
	
	@Autowired
	JedisService jedisService;

	@Value("${kafka.topic.notification-email}")
	private String emailNotificationTopic;

	private static final Logger log = LoggerFactory.getLogger(EmailNotificationListener.class);

	@Subscribe
	public void emailChannelListener(NotificationEventMeta eventObj) {
		if(eventObj.getAction().equalsIgnoreCase(EventConstants.NEW_DM_MESSAGE.name()) ||
				eventObj.getAction().equalsIgnoreCase(EventConstants.NEW_CONVERSATION_MESSAGE.name())) {
			List<User> usersList = getAwayOfflineUsers(eventObj.getNotificationUsers(), eventObj.getEntityId());
			if(usersList != null && !usersList.isEmpty()) {
				eventObj.setNotificationUsers(usersList);
				sendEmail(eventObj);
			}
		} else {
			sendEmail(eventObj);
		}

		/*log.info("Subscribed Email Event from Event Bus");

		EmailNotificationVO emailObj = null;
		ObjectMapper mapper = new ObjectMapper();
		String eventData = null;
		List<String> eventChannels = eventObj.getEventChannels();
		if (eventChannels.contains(NotificationTypes.EMAIL.toString())) {
			try {
				if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.FORCED.toString())) {
					emailObj = constructEmailNotificationVO(eventObj);
					eventData = mapper.writeValueAsString(emailObj);
					// insert notification users
					processingDAO.insertNotificationUsers(emailObj);
					producer.send(emailNotificationTopic, eventData);
					log.info("Email Event Sent Sucessfully to Notification Service");
				} else if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.PREFERENCE.toString())) {
					if (eventObj.getPriority() == 1) {
						emailObj = constructEmailNotificationVO(eventObj);
						List<User> updatedUserList = processingHelper.getNotificationUserPreferences(emailObj);
						emailObj.setUser(updatedUserList);
						if (emailObj.getUsers().size() > 0) {
							eventData = mapper.writeValueAsString(emailObj);
							// insert notification users
							processingDAO.insertNotificationUsers(emailObj);
							producer.send(emailNotificationTopic, eventData);
							log.info("Email Event Sent Sucessfully to Notification Service");
						} else {
							log.info("No users with preferences found to send the Event Email");
						}
					}

				} else {
					log.error("Unknow Notification EventType to process:", eventObj.getEventLevel());
					return;
				}
			} catch (Exception e) {
				log.error("Exception in sending Email Notifiaction", e);
			}
		} else {
			log.info("Email Notification channel not found for event with name:" + eventObj.getEvent());
		}*/

	}
	
	private List<User> getAwayOfflineUsers(List<User> usersList, String entityId){
		
		List<User> newUsers = new ArrayList<>();
		String userStatus = null;
		int ttlValue = 0;
		try {
			for (User user : usersList) {
				userStatus = jedisService.getUserStatusFromRedis(user.getUserId());
				if (userStatus.equalsIgnoreCase("online")) {
					// remove all notification keys for that user
					jedisService.removeNotificationKeys(entityId, user.getUserId());
				} else if(userStatus.equalsIgnoreCase("offline")) {
					if (jedisService.getNotificationUserConv(entityId, user.getUserId()) == null) {
						// check ttl key exists or not not exist create ttl key notificaiton key and add
						// user to notification list
						// else check key value if <8 double increment it
						ttlValue = jedisService.getUserConvTTL(entityId, user.getUserId());
						if (ttlValue == 0) {
							log.info("IN 0 TTL Conditions");
							ttlValue = 1;
							jedisService.createNotificationKeys(entityId, user.getUserId(), ttlValue);
							jedisService.createTTLKey(entityId, user.getUserId(), ttlValue);
							newUsers.add(user);
						} else if (ttlValue > 0 && ttlValue < 8) {
							log.info("Below 8 hours TTL conditions");
							ttlValue = ttlValue + ttlValue;
							jedisService.createNotificationKeys(entityId, user.getUserId(), ttlValue);
							jedisService.createTTLKey(entityId, user.getUserId(), ttlValue);
							newUsers.add(user);
						} else if (ttlValue == 8) {
							log.info("After 8 hours TTL conditions");
							ttlValue = -1;
							jedisService.createNotificationKeys(entityId, user.getUserId(), ttlValue);
							jedisService.createTTLKey(entityId, user.getUserId(), ttlValue);
							newUsers.add(user);
						} else if (ttlValue < 0) {
							log.info("TTL value Exceeded 8 hours no need to send notification");
						}

					}

				}
			}
		} catch (Exception e) {

		}
		return newUsers;
	}
	
	private void sendEmail(NotificationEventMeta eventObj ) {
		//TODO temporary solution for the project task at mention mail restriction
		if(eventObj.getAction().equalsIgnoreCase(EventConstants.TASK_AT_MENTION.name().toString())
				&& StringUtils.isEmpty(eventObj.getEntityId())) {
			log.info("Project task at mention restricted");
			return;
		}
		EmailNotificationVO emailObj = null;
		ObjectMapper mapper = new ObjectMapper();
		String eventData = null;
		List<String> eventChannels = eventObj.getEventChannels();
		if (eventChannels.contains(NotificationTypes.EMAIL.toString())) {
			try {
				if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.FORCED.toString())) {
					emailObj = constructEmailNotificationVO(eventObj);
					eventData = mapper.writeValueAsString(emailObj);
					// insert notification users
					processingDAO.insertNotificationUsers(emailObj);
					producer.send(emailNotificationTopic, eventData);
					log.info("Email Event Sent Sucessfully to Notification Service");
				} else if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.PREFERENCE.toString())) {
					if (eventObj.getPriority() == 1) {
						emailObj = constructEmailNotificationVO(eventObj);
						List<User> updatedUserList = processingHelper.getNotificationUserPreferences(emailObj,"EMAIL-NOTIFICATION");
						emailObj.setUser(updatedUserList);
						if (emailObj.getUsers().size() > 0) {
							eventData = mapper.writeValueAsString(emailObj);
							// insert notification users
							processingDAO.insertNotificationUsers(emailObj);
							producer.send(emailNotificationTopic, eventData);
							log.info("Email Event Sent Sucessfully to Notification Service");
						} else {
							log.info("No users with preferences found to send the Event Email");
						}
					}

				} else {
					log.error("Unknow Notification EventType to process:", eventObj.getEventLevel());
					return;
				}
			} catch (Exception e) {
				log.error("Exception in sending Email Notifiaction", e);
			}
		} else {
			log.info("Email Notification channel not found for event with name:" + eventObj.getEvent());
		}
	}

	private EmailNotificationVO constructEmailNotificationVO(NotificationEventMeta eventObj) {

		EmailNotificationVO emailObj = new EmailNotificationVO();
		String subject = null;
		Map<String, String> attrs = eventObj.getEventAttributes();
		if (eventObj.getAction().equalsIgnoreCase(EventConstants.CONVERSATION_WARROOM_STARTED.name().toString())
				|| eventObj.getAction().equalsIgnoreCase(EventConstants.CONVERSATION_WARROOM_ENDED.name().toString())) {
			try {
				String event = eventObj.getEvent();
				event = event + " in "+attrs.get("channel_id");
				emailObj.setEmailSubject(event);

			} catch (Exception e) {
				emailObj.setEmailSubject(eventObj.getEvent());
			}
		} else if (eventObj.getAction().equalsIgnoreCase(EventConstants.NEW_CONVERSATION_MESSAGE.name().toString())
				|| eventObj.getAction().equalsIgnoreCase(EventConstants.NEW_DM_MESSAGE.name().toString())) {
			if (eventObj.getAction().equals(EventConstants.NEW_CONVERSATION_MESSAGE.toString())) {
				subject = attrs.get("subject") + ' ' + attrs.get("channelName");
				emailObj.setEmailSubject(subject);
			} else {
				subject = attrs.get("subject") + ' ' + attrs.get("conversationName");
				emailObj.setEmailSubject(subject);
			}
		} else if(EventConstants.SCHEDULED_TICKET_IN_MINS.toString().equalsIgnoreCase(eventObj.getAction())){
				if("SCHEDULED_30".equalsIgnoreCase(eventObj.getCommonEventContent().getEntityType())){
					subject = new StringBuilder("30 mins left for scheduled activity for ticket ").append(eventObj.getCommonEventContent().getEventId())
							.append(" in conversation: ").append(eventObj.getEventAttributes().get("EmailSubject")).toString();
					emailObj.setEmailSubject(subject);
				}else if("SCHEDULED_15".equalsIgnoreCase(eventObj.getCommonEventContent().getEntityType())){
					subject = new StringBuilder("15 mins left for scheduled activity for ticket ").append(eventObj.getCommonEventContent().getEventId())
							.append(" in conversation: ").append(eventObj.getEventAttributes().get("EmailSubject")).toString();
					emailObj.setEmailSubject(subject);
				}
		} else if(EventConstants.SCHEDULED_TICKET_STARTED.toString().equalsIgnoreCase(eventObj.getAction())){
			subject = new StringBuilder("Scheduled activity has started for ticket ").append(eventObj.getCommonEventContent().getEventId())
					.append(" in conversation: ").append(eventObj.getEventAttributes().get("EmailSubject")).toString();
			emailObj.setEmailSubject(subject);
		}else {
			emailObj.setEmailSubject(attrs.get("subject"));
		}

		log.info("**************EMAIL SUBJECT********************"+attrs.get("subject")+"**************"+attrs.get("action"));
		emailObj.setNotificationType(NotificationTypes.EMAIL.toString());
		emailObj.setNotificationId(eventObj.getNotificationId());
		emailObj.setTemplateAttributes(eventObj.getEventAttributes());
		emailObj.setTemplatePath(EventTemplates.getTemplate(eventObj.getEventId()));
		emailObj.setUser(eventObj.getNotificationUsers());
		return emailObj;
	}

}
