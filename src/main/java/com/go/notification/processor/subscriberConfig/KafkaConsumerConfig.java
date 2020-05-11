package com.go.notification.processor.subscriberConfig;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
	
	private final static String CONSUMER_ID_PREFIX = "go-notification-processing-" ;
	private final static int MIN_THREADS_COUNT = 8;

	@Value("${spring.kafka.bootstrap-servers}")
	private String clusterAddress;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;
	
    @Value("${server.port}")
    private String serverPort;
    
    @Value("${spring.kafka.consumer.concurrency:4}")
    private int threadsCount;
    
	
	@Autowired
	Environment environment;
	
	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, clusterAddress);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
		String serverAddress = InetAddress.getLoopbackAddress().getHostAddress();
		String kafka_client_id = CONSUMER_ID_PREFIX + serverAddress + ":" + serverPort;
		props.put(ConsumerConfig.CLIENT_ID_CONFIG, kafka_client_id);
		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	@ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		factory.setConcurrency(threadsCount);
        factory.getContainerProperties().setPollTimeout(1000);
		factory.setBatchListener(true);
		return factory;
	}

	
}
