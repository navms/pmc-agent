package io.github.navms.infrastructure.converter;

import io.github.navms.domain.bank.valueobj.AggregateRow;
import io.github.navms.infrastructure.model.AggregateRowDO;
import io.github.navms.utils.enums.EnumConverter;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 聚合行转换器。
 *
 * @author navms
 */
@Mapper(uses = EnumConverter.class)
public interface AggregateRowConverter {

    AggregateRowConverter INSTANCE = Mappers.getMapper(AggregateRowConverter.class);

    /**
     * @param rows 聚合 DO
     * @return 领域行
     */
    List<AggregateRow> toAggregateRowList(List<AggregateRowDO> rows);

}
