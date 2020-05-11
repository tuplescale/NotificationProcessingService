package com.go.notification.processor.constants;

import java.util.HashMap;

public class EventTemplates {
	private static HashMap<Integer, String> map = new HashMap<>();

	public static String getTemplate(int eventId) {
		return map.get(eventId);
	}

	static {
		map.put(204, "conversationMovedQueue.ftl");
		map.put(205, "queue-notification.ftl");
		map.put(103, "OwnerChange.ftl");
		map.put(109, "InviteUser.ftl");
		map.put(108, "ConvStatusChange.ftl");
		map.put(114, "warroominvitation.ftl");
		map.put(115, "warroomend.ftl");
		map.put(104, "OwnerAssigned.ftl");
		map.put(102, "ConvStatusChange.ftl");
		map.put(200, "unreadMsgNotification.ftl");
		map.put(201, "unreadMsgNotification.ftl");
		map.put(117, "ConvTaskStatusChange.ftl");
		map.put(123, "ConvTaskStatusChange.ftl");
		map.put(207, "Atmention.ftl");
		map.put(208, "Atmention.ftl");
		map.put(209, "Atmention.ftl");
		map.put(322, "AssessmentDataCollectionFilled.ftl");
		map.put(323, "MigrationOVFLink.ftl");
		map.put(501,"scheduled_activity.ftl");
		map.put(502,"scheduled_activity.ftl");
		map.put(324, "PendingApprovalTask.ftl");
		map.put(325, "TriggeredActionStatusNotification.ftl");
		map.put(404, "ProvisionHoldRelease.ftl");
		map.put(405, "MailToKeepIT.ftl");
		map.put(406,"VeritasStrike.ftl");
		map.put(407,"VeritasStrike2.ftl");
		map.put(139, "PostFeedback.ftl");
		map.put(119, "TaskOwnerAssigned.ftl");
		map.put(120, "TaskOwnerChange.ftl");
		map.put(140, "TaskGroupAssign.ftl");
	}
}
