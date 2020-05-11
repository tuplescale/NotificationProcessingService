package com.go.notification.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.go.notification.processor.service.KafkaProducer;
import com.ne.commons.notification.vo.NotificationProcessingVO;
import com.ne.commons.notification.vo.User;

@Controller
public class postController {

	@Autowired
	KafkaProducer producer;

	@ResponseBody
	@RequestMapping(value = "/post", method = RequestMethod.GET)
	public String getChannelStats() throws JsonProcessingException {

		ObjectMapper objMap = new ObjectMapper();

		List<User> users = new ArrayList<User>();
		User user = new User();
		user.setEmail("saroj.farhana@netenrich.com");
		user.setUserId(4651);
		user.setUserName("Saroj Farhana");
		users.add(user);

		Map<String, String> eventAttributes = new HashMap<String, String>();
		eventAttributes.put("conversationName", "NotificationTest");
		eventAttributes.put("oldUserName", "Ravindar Veerla");
		eventAttributes.put("newUserName", "Farhana Saroj");

		NotificationProcessingVO obj = new NotificationProcessingVO();
		obj.setEntityId("002df251-0276-45fc-bdc9-02c04cb32dc3");
		obj.setEntityType("conversation");
		obj.setEvent("OWNER_CHANGE");
		obj.setEventId(2);
		obj.setNotificationUsers(users);
		obj.setEventAttributes(eventAttributes);

		producer.send("ne-notification-event-local", objMap.writeValueAsString(obj));

		System.out.println("send to kafka sucessfully");
		return "send to kafka sucessfully";

	}
	
	@ResponseBody
	@RequestMapping(value = "/postBellEvent", method = RequestMethod.POST)
	public String postBellEvent(@RequestBody NotificationProcessingVO obj) throws JsonProcessingException {
		ObjectMapper objMap = new ObjectMapper();
		producer.send("ne-notification-event-local", objMap.writeValueAsString(obj));
		return "Data Posted sucessfully";
		
	}
	
	
	
	
}
