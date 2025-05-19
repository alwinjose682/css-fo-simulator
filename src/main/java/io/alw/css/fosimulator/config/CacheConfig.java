package io.alw.css.fosimulator.config;

import org.apache.ignite.Ignition;
import org.apache.ignite.client.SslMode;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    @Value("${ignite.hosts}")
    String igniteHosts;

    @Bean
    public ClientConfiguration clientConfiguration() {
        Ignition.setClientMode(true);
        ClientConfiguration cfg = new ClientConfiguration();
        cfg.setSslMode(SslMode.DISABLED);
        cfg.setAddresses(igniteHosts.split(","));
        return cfg;
    }
}
