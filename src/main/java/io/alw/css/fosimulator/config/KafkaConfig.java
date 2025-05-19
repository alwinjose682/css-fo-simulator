package io.alw.css.fosimulator.config;

import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean("kafkaTemplateCashMessage")
    public KafkaTemplate<String, FoCashMessageAvro> kafkaTemplate(KafkaProperties kafkaProperties) {
        var producerPropMap = kafkaProperties.buildProducerProperties(null);
        var producerFactory = new DefaultKafkaProducerFactory<String, FoCashMessageAvro>(producerPropMap);
        return new KafkaTemplate<>(producerFactory);
    }
}
