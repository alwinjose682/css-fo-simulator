package io.alw.css.fosimulator;

import io.alw.css.domain.cashflow.FoCashMessage;
import io.alw.css.fosimulator.mapper.FoCashMessageAvroMapper;
import io.alw.css.fosimulator.model.properties.KafkaTopicProperties;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.function.Consumer;

// TODO: This class has to publish the messages to CSS cashflow-consumer component
public class CashMessagePublisher implements Consumer<List<FoCashMessage>> {
    private final static Logger log = LoggerFactory.getLogger(CashMessagePublisher.class);
    private final KafkaTopicProperties kafkaTopicProperties;
    private final KafkaTemplate<String, FoCashMessageAvro> kafkaTemplateCashMessage;
    private final CssTaskExecutor cssTaskExecutor;

    public CashMessagePublisher(KafkaTopicProperties kafkaTopicProperties, KafkaTemplate<String, FoCashMessageAvro> kafkaTemplateCashMessage, CssTaskExecutor cssTaskExecutor) {
        this.kafkaTopicProperties = kafkaTopicProperties;
        this.kafkaTemplateCashMessage = kafkaTemplateCashMessage;
        this.cssTaskExecutor = cssTaskExecutor;
    }

    @Override
    public void accept(List<FoCashMessage> foCashMessages) {
        foCashMessages.forEach(this::publish);
    }

    public void publish(FoCashMessage cashMessage) {
        String outputTopic = kafkaTopicProperties.cashMessageOutputTopic();
        FoCashMessageAvro avroMsg = FoCashMessageAvroMapper.instance().domainToAvro(cashMessage);
        String key = avroMsg.getCashflowID() + "-" + avroMsg.getCashflowVersion();
        log.trace("Sending cash message: {} to topic: {}", key, outputTopic);

        kafkaTemplateCashMessage
                .send(outputTopic, key, avroMsg)
                .whenCompleteAsync((result, e) -> {
                    if (e == null) {
                        RecordMetadata recordMetadata = result.getRecordMetadata();
                        log.info("Published cash message[{}] to topic: {}, partition: {}, offset: {}", key, outputTopic, recordMetadata.partition(), recordMetadata.offset());
                    } else {
                        log.error("An error occurred when publishing cash message[{}] to kafka topic: {}. Message: {}", key, outputTopic, avroMsg);
                    }
                }, cssTaskExecutor.executor());
    }
}
