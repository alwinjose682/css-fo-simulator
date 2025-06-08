package io.alw.css.fosimulator.mapper;

import io.alw.css.domain.cashflow.FoCashMessage;
import io.alw.css.domain.cashflow.TradeLink;
import io.alw.css.serialization.cashflow.FoCashMessageAvro;
import io.alw.css.serialization.cashflow.TradeLinkAvro;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper
public interface FoCashMessageAvroMapper {
    //NOTE: MapStruct does correctly map between the two types of enum values such as 'PayOrReceive' and 'TradeEventAction'
    static FoCashMessageAvroMapper instance() {
        return Mappers.getMapper(FoCashMessageAvroMapper.class);
    }

    @Mapping(source = "valueDate", target = "valueDate", qualifiedByName = "mapJavaTimeDateToString")
    @Mapping(source = "tradeLinks", target = "tradeLinks", qualifiedByName = "mapTradeLinksToAvro")
    FoCashMessageAvro domainToAvro(FoCashMessage foCashMessage);

    @Mapping(target = "valueDate", source = "valueDate", qualifiedByName = "mapStringToJavaTimeDate")
    @Mapping(target = "tradeLinks", source = "tradeLinks", qualifiedByName = "mapAvroToTradeLinks")
    @InheritInverseConfiguration
    FoCashMessage avroToDomain(FoCashMessageAvro foCashMessageAvro);

    @Named("mapTradeLinksToAvro")
    static List<TradeLinkAvro> mapTradeLinksToAvro(List<TradeLink> tradeLinks) {
        return tradeLinks == null
                ? null
                : tradeLinks.stream().map(tl -> new TradeLinkAvro(tl.linkType(), tl.relatedReference())).toList();
    }

    @Named("mapAvroToTradeLinks")
    static List<TradeLink> mapAvroToTradeLinks(List<TradeLinkAvro> tradeLinks) {
        return tradeLinks == null
                ? null
                : tradeLinks.stream().map(tl -> new TradeLink(tl.getLinkType(), tl.getRelatedReference())).toList();
    }

    @Named("mapJavaTimeDateToString")
    static String mapJavaTimeDateToString(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ISO_DATE) : null;
    }

    @Named("mapStringToJavaTimeDate")
    static LocalDate mapStringToJavaTimeDate(String strDate) {
        return strDate != null ? LocalDate.parse(strDate, DateTimeFormatter.ISO_DATE) : null;
    }
}
