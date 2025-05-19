package io.alw.css.fosimulator.definition;

import io.alw.css.domain.cashflow.*;
import io.alw.css.fosimulator.cashflowgnrtr.DayTicker;
import io.alw.css.fosimulator.model.Entity;
import io.alw.css.fosimulator.model.TradeEventAndAction;
import io.alw.css.fosimulator.model.properties.CashMessageDefinitionProperties;
import io.alw.css.fosimulator.service.RefDataService;
import io.alw.css.fosimulator.store.CashMessageStore;
import io.alw.css.fosimulator.store.InMemoryCashMessageStore;
import io.alw.datagen.definition.BaseDefinition;
import io.alw.datagen.definition.CountAware;
import io.alw.datagen.provider.CyclicStringDataProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

import static io.alw.css.fosimulator.model.AmendableFoCashMessageFields.*;
import static io.alw.css.fosimulator.model.TradeLinkConstants.*;

/// This class is not concurrent safe / thread safe.
///
/// [CashMessageDefinition] instances are both a trade type definition and a supplier of the build output of the definition
/// Each instance of this class is supposed to be exclusive for a single thread
public sealed abstract class CashMessageDefinition
        extends BaseDefinition<FoCashMessage>
        implements CountAware, Supplier<List<FoCashMessage>>
        permits FxDefinition, TemporaryGenericDefinition {

    // Values that change for each build. These also remain un-modified for each build
    /// After each build of the definition, the existing [FoCashMessageBuilder] (`bdr`) is just replaced with a new one.
    private FoCashMessageBuilder bdr;
    private long dayForBuild;

    // Message Store and Related
    protected final CashMessageStore messageStore;
    private long lastMessageRetrievalDay;
    private static final int maxAmendmentGenerationDelayInDays = 20; // NOTE: Increasing this value will result in retaining the messages requiring amendment for a longer period in the messageStore. Hence, will also result in increased size of the messageStore
    private static final int maxAmendmentGenerationDelayInDays_relatedVal = 5;

    // Fixed values for each instance of CashMessageDefinition
    private final String entityCode;
    private final String currCode;
    private final TradeType tradeType;
    private final TransactionType transactionType;

    // Spring Beans
    protected final CashMessageDefinitionProperties cashMessageDefinitionProperties;
    protected final RefDataService refDataService;
    protected final DayTicker dayTicker;
    protected final RandomGenerator rndm;
    private long counter;

    // Others
    private final CyclicStringDataProvider cyclicAmendableFieldsProvider;

    public CashMessageDefinition(Entity entity, TradeType tradeType, TransactionType transactionType, RefDataService refDataService, DayTicker dayTicker, CashMessageDefinitionProperties cashMessageDefinitionProperties) {
        this(null, entity, tradeType, transactionType, refDataService, dayTicker, cashMessageDefinitionProperties);
    }

    private CashMessageDefinition(FoCashMessage parent, Entity entity, TradeType tradeType, TransactionType transactionType, RefDataService refDataService, DayTicker dayTicker, CashMessageDefinitionProperties cashMessageDefinitionProperties) {
        super(parent);
        this.entityCode = entity.entityCode();
        this.currCode = entity.currCode();
        this.tradeType = tradeType;
        this.transactionType = transactionType;
        this.cashMessageDefinitionProperties = cashMessageDefinitionProperties;
        this.refDataService = refDataService;
        this.dayTicker = dayTicker;
        this.rndm = RandomGenerator.getDefault();
        this.counter = 0L;
        this.messageStore = new InMemoryCashMessageStore();
        this.lastMessageRetrievalDay = dayTicker.firstDay();
        this.cyclicAmendableFieldsProvider = new CyclicStringDataProvider(List.of(VALUE_DATE, AMOUNT, COUNTERPARTY_CODE));
    }

    protected abstract TradeEventAndAction rndmlyGetNextValidEventAndActionFor(TradeEventType amendMsgEvt, TradeEventAction amendMsgAct);

    // Amendment Related - START

    protected CashMessageDefinition withAmendedMessagesOf(List<FoCashMessage> messagesToBeAmended) {
        for (FoCashMessage msg : messagesToBeAmended) {
            switch (cyclicAmendableFieldsProvider.next()) {
                case VALUE_DATE -> this.withRelatedType(this::buildAmendedMessageForValueDate, msg);
                case AMOUNT -> this.withRelatedType(this::buildAmendedMessageForAmount, msg);
                case COUNTERPARTY_CODE -> this.withRelatedType(this::buildAmendedMessageForCounterparty, msg);
            }
        }
        return this;
    }

    private FoCashMessage buildAmendedMessageForCounterparty(FoCashMessage msg) {
        // NOTE: Here, it is required to get a counterpartyCode that is not used by 1) the current cashMessage being amended and 2) the counter side cashMessage of the current cashMessage
        // But, counterpartyCode of point 2 above is not available handy and hence there is a risk that the counterpartyCode used by counter side cashMessage may be re-used.
        String newCounterpartyCode = getCounterpartyCorrespondingToTransactionTypeOtherThan(msg.counterpartyCode());
        return getBuilderWithDefaultAmdntBaseFrom(msg)
                .counterpartyCode(newCounterpartyCode)
                .secondaryLedgerAccount(refDataService.counterpartyMappedSecondarySla(msg.entityCode(), msg.currCode(), newCounterpartyCode))
                .build();
    }

    private FoCashMessage buildAmendedMessageForAmount(FoCashMessage msg) {
        return getBuilderWithDefaultAmdntBaseFrom(msg)
                .amount(BigDecimal.valueOf(rndm.nextDouble(2, 75036)))
                .build();
    }

    private FoCashMessage buildAmendedMessageForValueDate(FoCashMessage msg) {
        return getBuilderWithDefaultAmdntBaseFrom(msg)
                .valueDate(getRndmValueDate())
                .build();
    }

    /// If NOT rebooked, then, increments the cashflow version and randomly chooses to increment the trade version
    ///
    /// If rebooked, then:
    ///
    /// 1) create a new trade with a new cashflow. The trade event is 'TradeEventType.REBOOK' and not 'TradeEventType.NEW_TRADE'
    /// 2) create cashflow to cancel the original cashflow. The trade event is 'TradeEventType.REBOOK'
    private FoCashMessageBuilder getBuilderWithDefaultAmdntBaseFrom(FoCashMessage msg) {
        TradeEventAndAction nextEventAndAction = rndmlyGetNextValidEventAndActionFor(msg.tradeEventType(), msg.tradeEventAction());
        FoCashMessageBuilder amndBdr = getBuilderFrom(msg);

        // If NOT rebooked
        if (nextEventAndAction.event() != TradeEventType.REBOOK) {
            boolean incrementTradeVersion = rndm.nextInt(0, 100) > 70;
            amndBdr
                    // Id Version
                    .tradeVersion(incrementTradeVersion ? msg.tradeVersion() + 1 : msg.tradeVersion())
                    .cashflowVersion(msg.cashflowVersion() + 1);
        }
        // If rebooked
        else {
            // 1. Create new trade and cashflow IDs
            IdProvider idProvider = IdProvider.singleton();
            final long newTradeID = idProvider.nextTradeId();
            final long newCashflowID = idProvider.nextCashflowId();

            // 2. Create cancellation for the original cashflow and register in the BaseDefinition
            this.withRelatedType(origMsg -> {
                        List<TradeLink> newTradeLinks = msg.tradeLinks() != null && !msg.tradeLinks().isEmpty() ? new ArrayList<>(msg.tradeLinks()) : new ArrayList<>();
                        newTradeLinks.add(new TradeLink(tradeLink_childTrade, String.valueOf(newTradeID)));
                        newTradeLinks.add(new TradeLink(tradeLink_childCashflow, String.valueOf(newCashflowID)));
                        return getBuilderFrom(origMsg)
                                // Id Version
                                .tradeVersion(origMsg.tradeVersion())
                                .cashflowVersion(origMsg.cashflowVersion() + 1)
                                // Trade Event and Action
                                .tradeEventType(TradeEventType.CANCEL)
                                .tradeEventAction(TradeEventAction.ADD)
                                .tradeLinks(Collections.unmodifiableList(newTradeLinks))
                                .build();
                    }
                    , msg);

            // 3. Create the new trade and cashflow
            List<TradeLink> newTradeLinks = msg.tradeLinks() != null && !msg.tradeLinks().isEmpty() ? new ArrayList<>(msg.tradeLinks()) : new ArrayList<>();
            newTradeLinks.add(new TradeLink(tradeLink_parentTrade, String.valueOf(msg.tradeID())));
            newTradeLinks.add(new TradeLink(tradeLink_parentCashflow, String.valueOf(msg.cashflowID())));
            amndBdr
                    // Id Version
                    .tradeID(newTradeID)
                    .tradeVersion(1)
                    .cashflowID(newCashflowID)
                    .cashflowVersion(1)
                    .tradeLinks(Collections.unmodifiableList(newTradeLinks));
        }

        return amndBdr
                // Trade Event and Action
                .tradeEventType(nextEventAndAction.event())
                .tradeEventAction(nextEventAndAction.action());
    }

    // Amendment Related - END

    // Message Store Related - START
    protected List<FoCashMessage> getMessagesToBeAmended() {
        final long currentDay = dayForBuild;
        List<FoCashMessage> msgsToBeAmended = new ArrayList<>();
        for (; lastMessageRetrievalDay <= currentDay; ++lastMessageRetrievalDay) {
            List<FoCashMessage> msgs = messageStore.remove(lastMessageRetrievalDay);
            if (msgs != null) {
                msgsToBeAmended.addAll(msgs);
            }
        }
        return msgsToBeAmended;
    }

    /// Randomly select valid amend candidates and save in [CashMessageStore] with a random retrieval day. Random retrieval day depends on [CashMessageDefinition#maxAmendmentGenerationDelayInDays]
    protected void rndmlySelectValidAmendCandidatesAndSave(List<FoCashMessage> msgs, Predicate<FoCashMessage> inclusionCriteria) {
        long[] amendmentDelayDay = new long[1];
        Predicate<FoCashMessage> finalInclusionCriteria = msg -> inclusionCriteria
                .and(m -> m.cashflowVersion() + m.tradeVersion() <= cashMessageDefinitionProperties.maxNumOfAmendments()).test(msg)
                && (amendmentDelayDay[0] = rndm.nextInt(0, maxAmendmentGenerationDelayInDays)) > maxAmendmentGenerationDelayInDays_relatedVal;

        msgs.stream()
                .filter(finalInclusionCriteria)
                .forEach(msg -> messageStore.add(amendmentDelayDay[0], msg));
    }
    // Message Store Related - END

    /// TODO: Need to refactor such that this is the only method which can be invoked from implementations of [CashMessageDefinition]. This is to ensure that this method is indeed invoked FIRST.
    /// This method ensures that the same day is used at all points of building the definition.
    protected CashMessageDefinition newDefinition() {
        dayForBuild = dayTicker.day();
        return this;
    }

    /// NOTE: New [CashMessageDefinition] instances are not created by this method.
    /// Instead, the existing [FoCashMessageBuilder] (`bdr`) is just replaced with a new one and then new values are assigned.
    protected FoCashMessageBuilder getBuilderWithDefaultValues() {
        bdr = newFoCashMessageBuilder();
        IdProvider idProvider = IdProvider.singleton();
        final String counterpartyCode = getCounterpartyCorrespondingToTransactionType();
        bdr
                // Fixed value for this definition
                .entityCode(this.entityCode)
                .currCode(this.currCode)
                .tradeType(tradeType)
                .transactionType(transactionType)
                // Always a new trade
                .tradeEventType(TradeEventType.NEW_TRADE)
                .tradeEventAction(TradeEventAction.ADD)
                // Id values
                .tradeID(idProvider.nextTradeId())
                .tradeVersion(1)
                .cashflowID(idProvider.nextCashflowId())
                .cashflowVersion(1)
                // Entity dependent fields. Book codes are dummy for now
                .bookCode(refDataService.dummyBookCode())
                .counterBookCode(isInterbookTransaction() ? refDataService.dummyCounterBookCode() : null) // Also a TransactionType dependent
                // Counterparty driven fields
                .secondaryLedgerAccount(refDataService.counterpartyMappedSecondarySla(this.entityCode, this.currCode, counterpartyCode)) // This field accepts null if there is no sla
                // TransactionType dependent fields
                .counterpartyCode(counterpartyCode)
                // Others
                .rate(new BigDecimal("1.2154754")) // rate is just a constant. No rate dependent calculation is done in CSS
        ;

        return bdr;
    }


    // Utility methods - START

    /// Check the documentation for [CashMessageDefinition#getRndmValueDate()]
    ///
    /// `numOfDefinitionCreationsAfterWhichARandomNumberIsToBeAdded` - determines the first N number of definitions for which a random number should not be added to the current [CashMessageDefinition#dayForBuild]
    protected LocalDate getRndmValueDate(long numOfDefinitionCreationsAfterWhichARandomNumberIsToBeAdded) {
        if (counter() < numOfDefinitionCreationsAfterWhichARandomNumberIsToBeAdded) {
            long daysToAdd = dayForBuild;
            return LocalDate.now().plusDays(daysToAdd);
        } else {
            return getRndmValueDate();
        }
    }

    /// Returns the value date which can randomly range from [CashMessageDefinitionProperties#vdBackwardDays] to [CashMessageDefinitionProperties#vdForwardDays] with respect to the current [CashMessageDefinition#dayForBuild].
    /// This means this method can return back valued date as well, but the percentage of back valued cashMessages is configured to be very less.
    protected LocalDate getRndmValueDate() {
        final long daysToAdd;
        if (isAnNthDefinition(cashMessageDefinitionProperties.numOfCfsForABackVdCf())) {
            daysToAdd = rndm.nextInt(Math.negateExact(cashMessageDefinitionProperties.vdBackwardDays()), -1);
        } else {
            daysToAdd = dayForBuild + rndm.nextInt(0, cashMessageDefinitionProperties.vdForwardDays());
        }
        return LocalDate.now().plusDays(daysToAdd);
    }

    protected String getCounterpartyCorrespondingToTransactionType() {
        return isInternalTransaction()
                ? refDataService.internalCounterparty(rndm)
                : refDataService.externalCounterparty(rndm);
    }

    protected String getCounterpartyCorrespondingToTransactionTypeOtherThan(String counterpartyCodeToAvoid) {
        return isInternalTransaction()
                ? refDataService.internalCounterpartyOtherThan(rndm, counterpartyCodeToAvoid)
                : refDataService.externalCounterpartyOtherThan(rndm, counterpartyCodeToAvoid);
    }

    protected boolean isInterbookTransaction() {
        return transactionType == TransactionType.INTER_BOOK;
    }

    protected boolean isInternalTransaction() {
        return transactionType == TransactionType.INTER_BOOK || transactionType == TransactionType.INTER_BRANCH || transactionType == TransactionType.INTER_COMPANY;
    }
    // Utility methods - END

    private FoCashMessageBuilder newFoCashMessageBuilder() {
        incrementCounter();
        return FoCashMessageBuilder.builder();
    }

    /// NOTE: The [CashMessageDefinition#counter] is not incremented by this method
    protected FoCashMessageBuilder getBuilderFrom(FoCashMessage cashMsg) {
        return FoCashMessageBuilder.builder(cashMsg);
    }

    @Override
    public long counter() {
        return this.counter;
    }

    @Override
    public void incrementCounter() {
        ++counter;
    }

    @Override
    public FoCashMessage buildDefinition() {
        return bdr.build();
    }

    @Override
    protected BaseDefinition<FoCashMessage> childDefinition(FoCashMessage parent) {
        throw new RuntimeException("This method is not supported for CashMessageDefinition");
    }
}
