package org.airsonic.player.service;

import com.google.common.io.MoreFiles;

import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApacheCommonsConfigurationService {
    
    @org.springframework.context.annotation.Configuration
    public static class ApacheCommonsConfigurationServiceConfiguration {
        @Bean
        public ApacheCommonsConfigurationService apacheCommonsConfigurationService() {
            return ApacheCommonsConfigurationService.getInstance();
        };
    }

    private static final Logger LOG = LoggerFactory.getLogger(ApacheCommonsConfigurationService.class);

    private final FileBasedConfigurationBuilder<FileBasedConfiguration> builder;

    private final Configuration config;

    public static final String HEADER_COMMENT = "Airsonic preferences.  NOTE: This file is automatically generated."
                           + " Do not modify while application is running";
    
    private static ApacheCommonsConfigurationService instance = new ApacheCommonsConfigurationService();
    
    public static ApacheCommonsConfigurationService getInstance() {
        return instance;
    }
    
    public static void reset() {
        instance = new ApacheCommonsConfigurationService();
    }

    private ApacheCommonsConfigurationService() {
        Path propertyFile = SettingsService.getPropertyFile();
        if (!Files.exists(propertyFile)) {
            try {
                MoreFiles.touch(propertyFile);
            } catch (IOException e) {
                throw new RuntimeException("Could not create new property file", e);
            }
        }
        Parameters params = new Parameters();
        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        layout.setHeaderComment(HEADER_COMMENT);
        layout.setGlobalSeparator("=");
        builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class).configure(
                params.properties()
                      .setFile(propertyFile.toFile())
                      .setSynchronizer(new ReadWriteSynchronizer())
                      .setLayout(layout));
        try {
            config = builder.getConfiguration();
        } catch (ConfigurationException e) {
            throw new RuntimeException("Could not load property file at " + propertyFile, e);
        }
    }

    public void save() {
        try {
            builder.save();
        } catch (ConfigurationException e) {
            LOG.error("Unable to write to property file.", e);
        }
    }

    public Object getProperty(String key) {
        return config.getProperty(key);
    }

    public boolean containsKey(String key) {
        return config.containsKey(key);
    }

    public void clearProperty(String key) {
        config.clearProperty(key);
    }

    public String getString(String key, String defaultValue) {
        return config.getString(key, defaultValue);
    }

    public void setProperty(String key, Object value) {
        config.setProperty(key, value);
    }

    public long getLong(String key, long defaultValue) {
        return config.getLong(key, defaultValue);
    }

    public int getInteger(String key, int defaultValue) {
        return config.getInteger(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return config.getBoolean(key, defaultValue);
    }

    public Configuration getConfiguration() {
        return config;
    }
}
