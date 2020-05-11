package com.go.notification.processor.service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducer {

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	private static Logger log = LoggerFactory.getLogger(KafkaProducer.class);

	public boolean send(String topicName, String event) {
		//System.out.println("inside the send"+topicName);
		boolean resp = false;

		try {
			kafkaTemplate.send(topicName, event).get(5, TimeUnit.SECONDS);
			resp = true;
		} catch (InterruptedException e) {
			log.error("Exception occured when sending events to kafka", e);
		} catch (ExecutionException e) {
			log.error("Exception occured when sending events to kafka", e);
		} catch (TimeoutException e) {
			log.error("Exception occured when sending events to kafka", e);
		}

		//System.out.println(resp);
		return resp;
		

	}

}
