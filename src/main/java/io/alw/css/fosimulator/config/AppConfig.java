package io.alw.css.fosimulator.config;

import io.alw.css.fosimulator.FoCashMessageConsumer;
import io.alw.css.fosimulator.VT;
import io.alw.css.fosimulator.cashflowgnrtr.CashflowGeneratorHandler;
import io.alw.css.fosimulator.cashflowgnrtr.DayTicker;
import io.alw.css.fosimulator.model.properties.CashflowGeneratorProperties;
import io.alw.css.fosimulator.model.properties.CashMessageDefinitionProperties;
import io.alw.css.fosimulator.service.RefDataService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public VT vt() {
        return new VT();
    }

    @Bean
    public CashflowGeneratorHandler cashflowGeneratorHandler(CashflowGeneratorProperties cashflowGeneratorProperties, CashMessageDefinitionProperties cashMessageDefinitionProperties, FoCashMessageConsumer foCashMessageConsumer, DayTicker dayTicker, VT vt, RefDataService refDataService) {
        return new CashflowGeneratorHandler(cashflowGeneratorProperties, cashMessageDefinitionProperties, foCashMessageConsumer, refDataService, dayTicker, vt);
    }

    @Bean
    public DayTicker dayTicker(VT vt) {
        return new DayTicker(10, 30, 2, vt);
    }

    @Bean
    public FoCashMessageConsumer foCashMessageConsumer() {
        return new FoCashMessageConsumer();
    }
}
