package io.alw.css.fosimulator.definition;

import io.alw.css.domain.cashflow.*;
import io.alw.css.fosimulator.cashflowgnrtr.DayTicker;
import io.alw.css.fosimulator.model.Entity;
import io.alw.css.fosimulator.model.TradeEventAndAction;
import io.alw.css.fosimulator.model.properties.CashMessageDefinitionProperties;
import io.alw.css.fosimulator.service.RefDataService;
import io.alw.datagen.definition.BaseDefinition;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

import static io.alw.css.domain.cashflow.TradeEventAction.ADD;
import static io.alw.css.domain.cashflow.TradeEventAction.MODIFY;
import static io.alw.css.domain.cashflow.TradeEventAction.REMOVE;
import static io.alw.css.domain.cashflow.TradeEventType.*;
import static io.alw.css.domain.cashflow.TradeEventType.REBOOK;

/// Note: This is only a temporary definition that is used only till the [TradeType] specific definitions are written.
/// Currently, only FX trade has a proper definition: [FxDefinition]
public final class TemporaryGenericDefinition extends CashMessageDefinition {
    private final static Predicate<FoCashMessage> inclusionCriteria = msg -> msg.tradeEventType() != TradeEventType.CANCEL;

    public TemporaryGenericDefinition(Entity entity, TradeType tradeType, TransactionType transactionType, RefDataService refDataService, DayTicker dayTicker, CashMessageDefinitionProperties cashMessageDefinitionProperties) {
        super(entity, tradeType, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
    }

    @Override
    public List<FoCashMessage> get() {
        // Get cash messages that need to be amended
        final List<FoCashMessage> messagesToBeAmended = getMessagesToBeAmended();

        // Build amended cashMessages and cashMessages for a new FX trade. There are 2 cashMessages for a single FX trade
        List<FoCashMessage> newAndAmendedMsgs = newDefinition()
                .withAmendedMessagesOf(messagesToBeAmended)
                .withDefaults()
                .buildWithRelatedDefinition();

        // Select new cash messages for future amendments and add to the message store
        rndmlySelectValidAmendCandidatesAndSave(newAndAmendedMsgs, inclusionCriteria);

        return newAndAmendedMsgs;
    }

    @Override
    public BaseDefinition<FoCashMessage> withDefaults() {
        // Create the builder with base values
        FoCashMessageBuilder bdr = getBuilderWithDefaultValues();
        bdr
                .valueDate(getRndmValueDate(1000))
                .tradeLinks(null)
                .payOrRecieve(rndm.nextBoolean() ? PayOrRecieve.PAY : PayOrRecieve.RECEIVE)
                .amount(BigDecimal.valueOf(rndm.nextDouble(2, 52458)))
        ;

        return this;
    }

    @Override
    protected TradeEventAndAction rndmlyGetNextValidEventAndActionFor(TradeEventType amendMsgEvt, TradeEventAction amendMsgAct) {
        int rndmNum = rndm.nextInt(1, 100);
        return switch (amendMsgEvt) {
            case NEW_TRADE -> {
                if (rndmNum > 40) yield new TradeEventAndAction(AMEND, ADD);
                else if (rndmNum > 10) yield new TradeEventAndAction(CANCEL, ADD);
                else yield new TradeEventAndAction(REBOOK, ADD);
            }
            case REBOOK -> {
                if (rndmNum > 10) yield new TradeEventAndAction(AMEND, ADD);
                else yield new TradeEventAndAction(CANCEL, ADD);
            }
            case AMEND -> {
                if (amendMsgAct == REMOVE) yield new TradeEventAndAction(AMEND, ADD);
                else if (rndmNum > 30) {
                    if (amendMsgAct == ADD) yield new TradeEventAndAction(AMEND, MODIFY);
                    else if (amendMsgAct == MODIFY) {
                        if (rndmNum > 60) yield new TradeEventAndAction(AMEND, MODIFY);
                        else yield new TradeEventAndAction(AMEND, REMOVE);
                    } else /*if (amendMsgAct == REMOVE)*/ yield new TradeEventAndAction(AMEND, ADD);
                } else if (rndmNum > 20) yield new TradeEventAndAction(CANCEL, ADD);
                else yield new TradeEventAndAction(REBOOK, ADD);
            }
            case CANCEL -> throw new RuntimeException("Attempt to amend a cancelled cashflow is invalid");

            default -> throw new IllegalStateException("Unexpected value: " + amendMsgEvt);
        };
    }
}
