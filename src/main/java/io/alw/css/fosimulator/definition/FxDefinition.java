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

import static io.alw.css.domain.cashflow.TradeEventAction.*;
import static io.alw.css.domain.cashflow.TradeEventType.*;
import static io.alw.css.fosimulator.model.TradeLinkConstants.tradeLink_counterSide;

public final class FxDefinition extends CashMessageDefinition {
    private long counterSideCashflowId;
    private final static Predicate<FoCashMessage> inclusionCriteria = msg -> msg.tradeEventType() != TradeEventType.CANCEL;

    public FxDefinition(Entity entity, TransactionType transactionType, RefDataService refDataService, DayTicker dayTicker, CashMessageDefinitionProperties cashMessageDefinitionProperties) {
        super(entity, TradeType.FX, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
    }

    @Override
    public List<FoCashMessage> get() {
        // Get cash messages that need to be amended
        final List<FoCashMessage> messagesToBeAmended = getMessagesToBeAmended();

        // Build amended cashMessages and cashMessages for a new FX trade. There are 2 cashMessages for a single FX trade
        List<FoCashMessage> newAndAmendedMsgs = newDefinition()
                .withAmendedMessagesOf(messagesToBeAmended)
                .withRelatedType(this::buildCounterSide)
                .withDefaults()
                .buildWithRelatedDefinition();

        // Select new cash messages for future amendments and add to the message store
        rndmlySelectValidAmendCandidatesAndSave(newAndAmendedMsgs, inclusionCriteria);

        return newAndAmendedMsgs;
    }

    /// Builds the counter side of the given FX message
    private FoCashMessage buildCounterSide(FoCashMessage fx1) {
        String counterpartyCode = getCounterpartyCorrespondingToTransactionTypeOtherThan(fx1.counterpartyCode());
        Entity entity = refDataService.entityOtherThan(rndm, fx1.entityCode());
        String entityCode = entity.entityCode();
        String currCode = entity.currCode();

        FoCashMessageBuilder fx2Bdr = getBuilderFrom(fx1)
                // Values that differ for counter side of the FX deal
                .cashflowID(counterSideCashflowId)
                .counterpartyCode(counterpartyCode)
                .entityCode(entityCode)
                .currCode(currCode)
                .secondaryLedgerAccount(refDataService.counterpartyMappedSecondarySla(entityCode, currCode, counterpartyCode))
                .tradeLinks(List.of(new TradeLink(tradeLink_counterSide, String.valueOf(fx1.cashflowID()))))
                .payOrRecieve(fx1.payOrRecieve() == PayOrRecieve.RECEIVE ? PayOrRecieve.PAY : PayOrRecieve.RECEIVE)
                .amount(BigDecimal.valueOf(rndm.nextDouble(2, 95036))); // TODO and NOTE: The amount of the other side of FX trade is not calculated based on rate. It is just a random number which is incorrect.
        // bookCode and counterBookCode are not changed as they are dummy values as of now

        return fx2Bdr.build();
    }

    @Override
    public BaseDefinition<FoCashMessage> withDefaults() {
        IdProvider idProvider = IdProvider.singleton();
        // Create the builder with base values
        FoCashMessageBuilder bdr = getBuilderWithDefaultValues();
        // Generate cashflowID for the counter side of this FX deal
        counterSideCashflowId = idProvider.nextCashflowId();
        // Set the values specific to FX trade
        bdr
                .valueDate(getRndmValueDate(1000))
                .tradeLinks(List.of(new TradeLink(tradeLink_counterSide, String.valueOf(counterSideCashflowId))))
                .payOrRecieve(rndm.nextBoolean() ? PayOrRecieve.PAY : PayOrRecieve.RECEIVE)
                .amount(BigDecimal.valueOf(rndm.nextDouble(2, 95036)))
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