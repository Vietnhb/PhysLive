package com.example.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/** Enable after provider settings and approved topic schemas are installed. */
@Configuration
@ConditionalOnProperty(name = "physlive.staff.preview-enabled", havingValue = "true")
@EnableConfigurationProperties({AIProperties.class, JevProperties.class, UploadProperties.class})
@PropertySource("classpath:staff-preview.properties")
public class StaffPreviewConfiguration { }
