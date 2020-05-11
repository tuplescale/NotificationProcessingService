package com.go.notification.processor;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.go.notification.processor.eventbus.events.BellNotificationListener;
import com.go.notification.processor.eventbus.events.EmailNotificationListener;
import com.go.notification.processor.eventbus.events.MblPushNotificationListener;
import com.go.notification.processor.eventbus.events.PushNotificationListener;
import com.google.common.eventbus.EventBus;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootApplication
//@EnableDiscoveryClient
@ComponentScan({ "com.go.notification.processor","com.ne.commons" })
public class NeNotificationProcessingServiceApplication {
	
	@Autowired
	EmailNotificationListener emailNotificationListener;
	
	@Autowired
	BellNotificationListener bellNotificationListener;

	@Autowired
	PushNotificationListener pushNotificationListener;

	@Autowired
	MblPushNotificationListener mblPushNotificationListener;
	
	public static void main(String[] args) {
		SpringApplication.run(NeNotificationProcessingServiceApplication.class, args);
	}
	
	@Bean
	public EventBus eventBus() {
		EventBus eventBus = new EventBus();
		eventBus.register(emailNotificationListener);
		eventBus.register(bellNotificationListener);
		eventBus.register(pushNotificationListener);
		eventBus.register(mblPushNotificationListener);
		return eventBus;
	}
	
	
	@Configuration
	public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {
	    @Override
	    protected void configure(HttpSecurity http) throws Exception {
	        http.authorizeRequests().anyRequest().permitAll()  
	            .and().csrf().disable();
	    }
	}
}
