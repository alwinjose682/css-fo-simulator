package io.alw.css.fosimulator;

import io.alw.css.domain.cashflow.FoCashMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

// TODO: This class has to publish the messages to CSS cashflow-consumer component
public class FoCashMessageConsumer implements Consumer<List<FoCashMessage>> {
    private final static Logger log = LoggerFactory.getLogger(FoCashMessageConsumer.class);

    @Override
    public void accept(List<FoCashMessage> foCashMessages) {
        StringBuilder sb = new StringBuilder();
        for (FoCashMessage msg : foCashMessages) {
            sb.append(msg).append(System.lineSeparator());
        }
        log.info("Published {} messages: {}", foCashMessages.size(), sb);
    }
}
