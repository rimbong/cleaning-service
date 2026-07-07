package com.boot.cleanhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(
    value = {"classpath:etc.properties","classpath:db.properties"}
)
public class PropertySourceConfig {
}
