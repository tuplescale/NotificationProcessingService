package com.go.notification.processor.eventbus.events;

import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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
import com.go.notification.processor.service.HtmlParser;
import com.go.notification.processor.service.KafkaProducer;
import com.go.notification.processor.service.NotificationProcessingHelper;
import com.go.notification.processor.service.PushHelperService;
import com.google.common.eventbus.Subscribe;
import com.ne.commons.constants.EventConstants;
import com.ne.commons.notification.vo.PushNotificationVO;
import com.ne.commons.notification.vo.User;

@Component
public class MblPushNotificationListener {
	

	private static final Logger log = LoggerFactory.getLogger(PushNotificationListener.class);


	@Value("${kafka.topic.notification-push}")
	private String pushNotificationTopic;

	@Autowired
	KafkaProducer producer;

	@Autowired
	NotificationProcessingHelper processingHelper;

	@Autowired
	ProcessingDAO processingDAO;

	@Autowired
	HtmlParser htmlParser;
	
	@Autowired
	PushHelperService pushHelperService;

	@Value("${com.diva.app_url}")
	private String diva_url;

    /**
     * NotificationEventMeta listener
     * Process the push notification and send to 
     * @param eventObj
     */
	@Subscribe
	public void mblPushNotificationListener(NotificationEventMeta eventObj) {
		log.info("Event action from the content is : "+eventObj.getCommonEventContent().getAction());
		if(eventObj.getCommonEventContent().getAction().equalsIgnoreCase("media")) {
			log.info("Media file upload event received. Just ignore new message notification");
			return;
		}
		PushNotificationVO pushObj = null;
		ObjectMapper mapper = new ObjectMapper();
		String eventData = null;
		List<String> eventChannels = eventObj.getEventChannels();
		log.info("eventChannels Object"+eventChannels.toString());
		if (eventChannels.contains(NotificationTypes.MBPUSH.toString())) {
			log.info("inside the MobilePushNotificationEvent");
			try {

				if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.FORCED.toString())) {
					pushObj = constructPushNotificationVO(eventObj);
					if(pushObj != null) {
						eventData = mapper.writeValueAsString(pushObj);
						// insert notification users
						processingDAO.insertNotificationUsers(pushObj);
						producer.send(pushNotificationTopic, eventData);
						log.info("Push Event Sent to Notification Service");
					} else {
						log.info("No user are onlie to send push notification");
					}
				} else if (eventObj.getEventLevel().equalsIgnoreCase(EventLevels.PREFERENCE.toString())) {
					pushObj = constructPushNotificationVO(eventObj);
					if(pushObj != null) {
						List<User> updatedUserList = processingHelper.getNotificationUserPreferences(pushObj,"CONVERSATION");
						pushObj.setUser(updatedUserList);
						if (pushObj.getUsers().size() > 0) {
							eventData = mapper.writeValueAsString(pushObj);
							// insert notification users
							processingDAO.insertNotificationUsers(pushObj);
							
							producer.send(pushNotificationTopic, eventData);
							log.info("Push Event Sent Sucessfully to Notification Service");
						} else {
							log.info("No users with preferences found to send push notification");
						}
					}
				} else {
					log.error("Unknow Notification EventType to process:", eventObj.getEventLevel());
					return;
				}
			} catch (Exception e) {
				log.error("Exception in sending Email Notification", e);
			}
		} else {
			log.info("Push channel not found for event with name:" + eventObj.getEvent());
		}

	}

	/**
	 * It constructs notification vo
	 * @param eventObj
	 * @return pushNotificationVO
	 */
	private PushNotificationVO constructPushNotificationVO(NotificationEventMeta eventObj) {
		log.info("inside the mbl push NotificationEvent");
		List<User> notificationUsers = eventObj.getNotificationUsers();
		if(!notificationUsers.isEmpty()) {
			PushNotificationVO pushObj = new PushNotificationVO();
			pushObj.setNotificationType(NotificationTypes.MBPUSH.toString());
			pushObj.setNotificationId(eventObj.getNotificationId());
			pushObj.setUser(eventObj.getNotificationUsers());
			pushObj.setBaseEvent(eventObj.getCommonEventContent());
			pushObj.setEntityId(eventObj.getEntityId());
			pushObj.setEntityType(eventObj.getEntityType());
			pushObj.setEvent(eventObj.getEvent());
			pushObj.setEventTime(new Timestamp(System.currentTimeMillis()).getTime());
			pushObj.setNotificationLevel(eventObj.getEventLevel());
			pushObj.setAction(eventObj.getAction());
			String convUrl = eventObj.getEventAttributes().get("conv_url");

			pushObj.setConvUrl(convUrl);
		
			if(eventObj.getEventAttributes() != null && !eventObj.getEventAttributes().isEmpty()) {
				if(eventObj.getAction().equalsIgnoreCase(EventConstants.NEW_DM_MESSAGE.name())) {
					log.info("New DM Message :"+eventObj.getEventAttributes().get("message"));
					String message = eventObj.getEventAttributes().get("message");
					if(eventObj.getCommonEventContent().getActionType().equalsIgnoreCase("direct")) {
						if (message.length() > 60) {
							pushObj.setConvTitle("DM:"+StringUtils.substring(message, 0, 60)+"...");
						} else {
							pushObj.setConvTitle("DM:"+message);
						}
					} else {
						if (message.length() > 60) {
							pushObj.setConvTitle("DMG:"+StringUtils.substring(message, 0, 60)+"...");
						} else {
							pushObj.setConvTitle("DMG:"+message);
						}
					}
				} else if(eventObj.getAction().equalsIgnoreCase(EventConstants.DM_FILE_ATTACHED.name())){
					if(eventObj.getCommonEventContent().getActionType().equalsIgnoreCase("dm")) {
						pushObj.setConvTitle("DM");
					} else {
						String fileName = eventObj.getCommonEventContent().getEntityName();
						if(!org.springframework.util.StringUtils.isEmpty(fileName)) {
							if(fileName.length() > 60) {
								pushObj.setConvTitle("DMG:"+StringUtils.substring(fileName, 0, 60)+"...");
							} else {
								pushObj.setConvTitle("DMG:"+fileName);
							}
						} else {
							pushObj.setConvTitle("DMG");
						}
					}
				} else {
					if(eventObj.getEventAttributes().get("conversation_title") != null) {
						pushObj.setConvTitle(eventObj.getEventAttributes().get("conversation_title"));
					} else {
						log.info("Entity name received is : "+eventObj.getCommonEventContent().getEntityName());
						pushObj.setConvTitle(htmlParser.convertHtmlToPlainText(eventObj.getCommonEventContent().getEntityName()));
					}
				}
			} else if(eventObj.getAction().equalsIgnoreCase(EventConstants.DM_FILE_ATTACHED.name())){
				if(eventObj.getCommonEventContent().getActionType().equalsIgnoreCase("dm")) {
					pushObj.setConvTitle("DM");
				} else {
					String fileName = eventObj.getCommonEventContent().getEntityName();
					if(!org.springframework.util.StringUtils.isEmpty(fileName)) {
						if(fileName.length() > 60) {
							pushObj.setConvTitle("DMG:"+StringUtils.substring(fileName, 0, 60)+"...");
						} else {
							pushObj.setConvTitle("DMG:"+fileName);
						}
					} else {
						pushObj.setConvTitle("DMG");
					}
				}
			} else {
				log.info("Entity name received is : "+eventObj.getCommonEventContent().getEntityName());
				pushObj.setConvTitle(htmlParser.convertHtmlToPlainText(eventObj.getCommonEventContent().getEntityName()));
			}
			log.info("Conversation title : "+pushObj.getConvTitle());
			return pushObj;
		}
		return null;
	}



}
