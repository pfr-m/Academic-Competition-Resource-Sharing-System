package com.competition.hub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.competition.hub.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(exposeProxy = true)
public class CompetitionHubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CompetitionHubBackendApplication.class, args);
	}

}
