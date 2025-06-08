package io.alw.css.fosimulator.model.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties("app.kafka.topic")
public class KafkaTopicProperties {
    private final String cashflowOutput;

    @ConstructorBinding
    public KafkaTopicProperties(String cashflowOutput) {
        this.cashflowOutput = cashflowOutput;
    }

    public String cashMessageOutputTopic() {
        return cashflowOutput;
    }
}
