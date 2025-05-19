package io.alw.css.fosimulator;

import io.alw.css.domain.cashflow.FoCashMessage;
import io.alw.css.domain.cashflow.TradeLink;
import org.apache.avro.Schema;
import org.apache.avro.reflect.ReflectData;
import org.junit.jupiter.api.Test;

/// NOTE: Schema generated like this is further modified manually where required
public class avro {
    @Test
    void getAvroSchema_notATest() {
        Schema schema = ReflectData.get()
//                .getSchema(TradeLink.class)
                .getSchema(FoCashMessage.class);

        System.out.println(schema);
    }
}
