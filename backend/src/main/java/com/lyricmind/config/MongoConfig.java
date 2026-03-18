package com.lyricmind.config;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClientSettingsBuilderCustomizer tlsCustomizer() {
        return builder -> {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                builder.applyToSslSettings(ssl ->
                        ssl.enabled(true).context(sslContext)
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure TLS for MongoDB", e);
            }
        };
    }
}


