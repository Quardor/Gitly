package ru.quard0r.gitly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableJpaRepositories
public class GitlyApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitlyApplication.class, args);
    }
}
