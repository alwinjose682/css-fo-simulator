package io.alw.css.fosimulator.config;

import io.alw.css.fosimulator.CashMessagePublisher;
import io.alw.css.fosimulator.CssTaskExecutor;
import io.alw.css.fosimulator.cashflowgnrtr.CashflowGeneratorHandler;
import io.alw.css.fosimulator.cashflowgnrtr.DayTicker;
import io.alw.css.fosimulator.model.properties.CashflowGeneratorProperties;
import io.alw.css.fosimulator.model.properties.CashMessageDefinitionProperties;
import io.alw.css.fosimulator.model.properties.KafkaTopicProperties;
import io.alw.css.fosimulator.service.RefDataService;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class AppConfig {

    @Bean
    public CssTaskExecutor cssTaskExecutor() {
        return new CssTaskExecutor();
    }

    @Bean
    public CashflowGeneratorHandler cashflowGeneratorHandler(CashflowGeneratorProperties cashflowGeneratorProperties, CashMessageDefinitionProperties cashMessageDefinitionProperties, CashMessagePublisher cashMessagePublisher, DayTicker dayTicker, CssTaskExecutor cssTaskExecutor, RefDataService refDataService) {
        return new CashflowGeneratorHandler(cashflowGeneratorProperties, cashMessageDefinitionProperties, cashMessagePublisher, refDataService, dayTicker, cssTaskExecutor);
    }

    @Bean
    public DayTicker dayTicker(CssTaskExecutor cssTaskExecutor) {
        return new DayTicker(10, 30, 2, cssTaskExecutor);
    }

    @Bean
    public CashMessagePublisher cashMessagePublisher(KafkaTopicProperties kafkaTopicProperties, KafkaTemplate<String, FoCashMessageAvro> kafkaTemplateCashMessage, CssTaskExecutor cssTaskExecutor) {
        return new CashMessagePublisher(kafkaTopicProperties, kafkaTemplateCashMessage, cssTaskExecutor);
    }
}
