package io.alw.css.fosimulator.service;

import io.alw.css.fosimulator.model.Entity;
import io.alw.css.fosimulator.model.SlaMapping;
import jakarta.annotation.PostConstruct;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.random.RandomGenerator;

@Service
public class RefDataService {
    private final ClientConfiguration clientConfiguration;

    private List<String> externalCounterparties;
    private List<String> internalCounterparties;
    private List<Entity> entities;
    private Map<String, SlaMapping> slaMappings;

    public RefDataService(ClientConfiguration clientConfiguration) {
        this.clientConfiguration = clientConfiguration;
    }

    @PostConstruct
    private void createLocalCache() {
        try (var client = Ignition.startClient(clientConfiguration)) {
            entities = getEntity(client);
            slaMappings = getSlaMapping(client);
            externalCounterparties = getCounterparty(client, false);
            internalCounterparties = getCounterparty(client, true);
        }
    }

    private Map<String, SlaMapping> getSlaMapping(IgniteClient client) {
        Map<String, SlaMapping> slaMappings = new HashMap<>();
        var query = """
                select a.entityCode, a.currCode, b.counterpartyCode, b.secondaryLedgerAccount
                FROM
                entity a JOIN counterpartySlaMapping b ON a.entityCode = b.entityCode and a.currCode = b.currCode
                JOIN nostro c ON c.secondaryLedgerAccount = b.secondaryLedgerAccount
                where c.isPrimary is ?
                and a.active is ? and b.active is ? and c.active is ?
                """;
        try (var cursor = client.query(new SqlFieldsQuery(query).setArgs(false, true, true, true))) {
            for (List<?> rs : cursor) {
                var entityCode = (String) rs.get(0);
                var currCode = (String) rs.get(1);
                var counterpartyCode = (String) rs.get(2);
                var secondaryLedgerAccount = (String) rs.get(3);
                SlaMapping slaMapping = new SlaMapping(entityCode, currCode, counterpartyCode, secondaryLedgerAccount);
                slaMappings.put(getSlaMapKey(slaMapping), slaMapping);
            }
        }

        return Collections.unmodifiableMap(slaMappings);
    }

    private List<Entity> getEntity(IgniteClient client) {
        return client
                .query(new SqlFieldsQuery("""
                        select entityCode, currCode, countryCode, bicCode FROM entity where active is ?
                        """).setArgs(true))
                .getAll()
                .stream().map(rs -> {
                    var entityCode = (String) rs.get(0);
                    var currCode = (String) rs.get(1);
                    var countryCode = (String) rs.get(2);
                    var bicCode = (String) rs.get(3);
                    return new Entity(entityCode, currCode, countryCode, bicCode);
                })
                .toList();
    }

    private List<String> getCounterparty(IgniteClient client, boolean internal) {
        List<String> counterparties = new ArrayList<>();
        var query = "select counterpartyCode from counterparty where internal is ? and active is ?";
        try (FieldsQueryCursor<List<?>> cursor = client.query(new SqlFieldsQuery(query).setArgs(internal, true))) {
            for (List<?> rs : cursor) {
                counterparties.add((String) rs.get(0));
            }
        }

        return Collections.unmodifiableList(counterparties);
    }

    public String dummyBookCode() {
        return "DUMY";
    }

    public String dummyCounterBookCode() {
        return "YMUD";
    }

    /// Returns the secondary nostro if there is one mapped to the counterparty in the reference data repository
    /// If there is no secondary nostro mapped, returns null
    public String counterpartyMappedSecondarySla(String entityCode, String currCode, String counterpartyCode) {
        SlaMapping sms = slaMappings.get(getSlaMapKey(entityCode, currCode, counterpartyCode));
        return sms == null ? null : sms.secondaryLedgerAccount();
    }

    private String getSlaMapKey(SlaMapping sm) {
        return getSlaMapKey(sm.entityCode(), sm.currCode(), sm.counterpartyCode());
    }

    private String getSlaMapKey(String entityCode, String currCode, String counterpartyCode) {
        return entityCode + "-" + currCode + "-" + counterpartyCode;
    }

    public String externalCounterparty(RandomGenerator rndm) {
        return externalCounterparties.get(rndm.nextInt(0, externalCounterparties.size()));
    }

    public String internalCounterparty(RandomGenerator rndm) {
        return internalCounterparties.get(rndm.nextInt(0, internalCounterparties.size()));
    }

    public List<Entity> entities() {
        return entities;
    }

    public Entity entityOtherThan(RandomGenerator rndm, String entityCodeToAvoid) {
        final int idx = rndm.nextInt(0, entities.size());
        final Entity entity = entities.get(idx);
        if (entity.entityCode().equalsIgnoreCase(entityCodeToAvoid)) {
            int nextIdx = getNextIdx(idx, entities);
            return entities.get(nextIdx);
        } else {
            return entity;
        }
    }

    public String externalCounterpartyOtherThan(RandomGenerator rndm, String counterpartyCodeToAvoid) {
        final int idx = rndm.nextInt(0, externalCounterparties.size());
        final String cpty = externalCounterparties.get(idx);
        if (cpty.equalsIgnoreCase(counterpartyCodeToAvoid)) {
            int nextIdx = getNextIdx(idx, externalCounterparties);
            return externalCounterparties.get(nextIdx);
        } else {
            return cpty;
        }
    }

    public String internalCounterpartyOtherThan(RandomGenerator rndm, String counterpartyCodeToAvoid) {
        final int idx = rndm.nextInt(0, internalCounterparties.size());
        final String cpty = internalCounterparties.get(idx);
        if (cpty.equalsIgnoreCase(counterpartyCodeToAvoid)) {
            int nextIdx = getNextIdx(idx, internalCounterparties);
            return internalCounterparties.get(nextIdx);
        } else {
            return cpty;
        }
    }

    private int getNextIdx(int currIdx, List<?> list) {
        return currIdx == list.size() - 1 ? 0 : currIdx + 1;
    }
}
