package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        // Si por alguna razón no detecta la variable en local, puedes poner una de respaldo o dejar esta validación
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            throw new IllegalArgumentException("DATABASE_URL environment variable is not set.");
        }

        URI dbUri = new URI(databaseUrl);
        String userInfo = dbUri.getUserInfo();
        String username = userInfo != null ? userInfo.split(":")[0] : null;
        String password = userInfo != null && userInfo.contains(":") ? userInfo.split(":")[1] : null;
        
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();

        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
