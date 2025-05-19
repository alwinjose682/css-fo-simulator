package io.alw.css.fosimulator.model.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.Collections;
import java.util.Map;

@ConfigurationProperties("cashflow.generator")
public class CashflowGeneratorProperties {
    private final long frequencySecondsDefault;
    private final Map<String, Long> frequencySeconds;

    @ConstructorBinding
    public CashflowGeneratorProperties(long frequencySecondsDefault, Map<String, Long> frequencySeconds) {
        this.frequencySecondsDefault = frequencySecondsDefault;
        this.frequencySeconds = Collections.unmodifiableMap(frequencySeconds);
    }

    public long frequencySecondsDefault() {
        return frequencySecondsDefault;
    }

    public Map<String, Long> frequencySeconds() {
        return frequencySeconds;
    }
}
