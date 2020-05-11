package com.go.notification.processor.models;

import java.util.List;
import java.util.Map;

import com.ne.commons.notification.vo.NotificationProcessingVO;

public class NotificationEventMeta extends NotificationProcessingVO {

	private String event;
	private String notificationId;
	private String eventLevel;
	private int priority;
	private List<String> eventChannels;
	private String templateName;

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getEventLevel() {
		return eventLevel;
	}

	public void setEventLevel(String eventLevel) {
		this.eventLevel = eventLevel;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public List<String> getEventChannels() {
		return eventChannels;
	}

	public void setEventChannels(List<String> eventChannels) {
		this.eventChannels = eventChannels;
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getNotificationId() {
		return notificationId;
	}

	public void setNotificationId(String notificationId) {
		this.notificationId = notificationId;
	}

}
