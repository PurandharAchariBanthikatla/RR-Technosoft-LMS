package com.rrtechnosoft.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
@EnableScheduling
public class LmsApplication extends SpringBootServletInitializer {

    // Entry point when run as an executable jar/war (e.g. `java -jar`, IDE run).
    public static void main(String[] args) {
        SpringApplication.run(LmsApplication.class, args);
    }

    // Entry point when deployed to an external servlet container (Tomcat/WildFly),
    // which calls this instead of main() to bootstrap the Spring context.
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(LmsApplication.class);
    }
}
