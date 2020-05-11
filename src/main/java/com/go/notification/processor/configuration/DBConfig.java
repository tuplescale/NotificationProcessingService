package com.go.notification.processor.configuration;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DBConfig {

	@Bean
	@Primary
	public DataSource dataSource(@Value("${spring.datasource.driver-class-name}") String DB_DRIVER,
			@Value("${spring.datasource.password}") String DB_PASSWORD,
			@Value("${spring.datasource.url}") String DB_URL,
			@Value("${spring.datasource.username}") String DB_USERNAME) {

		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(DB_DRIVER);
		dataSource.setUrl(DB_URL);
		dataSource.setUsername(DB_USERNAME);
		dataSource.setPassword(DB_PASSWORD);
		return dataSource;

	}
}
