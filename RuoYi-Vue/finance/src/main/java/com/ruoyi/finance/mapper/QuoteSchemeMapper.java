package com.ruoyi.finance.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.finance.domain.QuoteScheme;
import com.ruoyi.finance.domain.QuoteSchemeChannel;
import com.ruoyi.finance.domain.QuoteSchemeScope;
import com.ruoyi.finance.domain.QuoteSchemeValueFeeRule;
import com.ruoyi.finance.domain.QuoteSchemeWarehouse;

public interface QuoteSchemeMapper
{
    List<QuoteScheme> selectQuoteSchemeList(QuoteScheme query);

    QuoteScheme selectQuoteSchemeById(Long schemeId);

    QuoteScheme selectQuoteSchemeByCode(@Param("schemeCode") String schemeCode);

    int insertQuoteScheme(QuoteScheme scheme);

    int updateQuoteScheme(QuoteScheme scheme);

    int updateQuoteSchemeStatus(@Param("schemeId") Long schemeId, @Param("status") String status,
        @Param("updateBy") String updateBy);

    int updateQuoteSchemeWarehouseScopeMode(@Param("schemeId") Long schemeId,
        @Param("warehouseScopeMode") String warehouseScopeMode, @Param("updateBy") String updateBy);

    List<QuoteSchemeScope> selectQuoteSchemeScopeList(@Param("schemeId") Long schemeId);

    int deleteQuoteSchemeScopes(@Param("schemeId") Long schemeId);

    int insertQuoteSchemeScope(QuoteSchemeScope scope);

    List<QuoteSchemeWarehouse> selectQuoteSchemeWarehouseList(@Param("schemeId") Long schemeId);

    int deleteQuoteSchemeWarehouses(@Param("schemeId") Long schemeId);

    int insertQuoteSchemeWarehouse(QuoteSchemeWarehouse warehouse);

    List<QuoteSchemeChannel> selectQuoteSchemeChannelList(@Param("schemeId") Long schemeId);

    QuoteSchemeChannel selectQuoteSchemeChannelById(@Param("schemeId") Long schemeId,
        @Param("schemeChannelId") Long schemeChannelId);

    QuoteSchemeChannel selectQuoteSchemeChannelByCustomerChannelCode(@Param("schemeId") Long schemeId,
        @Param("customerChannelCode") String customerChannelCode);

    Integer selectMaxChannelDisplayOrder(@Param("schemeId") Long schemeId);

    int insertQuoteSchemeChannel(QuoteSchemeChannel channel);

    int updateQuoteSchemeChannel(QuoteSchemeChannel channel);

    int deleteQuoteSchemeChannel(@Param("schemeId") Long schemeId, @Param("schemeChannelId") Long schemeChannelId);

    List<QuoteSchemeValueFeeRule> selectQuoteSchemeValueFeeRuleList(@Param("schemeId") Long schemeId);

    QuoteSchemeValueFeeRule selectQuoteSchemeValueFeeRuleById(@Param("schemeId") Long schemeId,
        @Param("valueFeeRuleId") Long valueFeeRuleId);

    QuoteSchemeValueFeeRule selectQuoteSchemeValueFeeRuleByChannelAndTrigger(@Param("schemeId") Long schemeId,
        @Param("logisticsChannelCode") String logisticsChannelCode, @Param("triggerCode") String triggerCode);

    Integer selectMaxValueFeeDisplayOrder(@Param("schemeId") Long schemeId);

    int insertQuoteSchemeValueFeeRule(QuoteSchemeValueFeeRule rule);

    int updateQuoteSchemeValueFeeRule(QuoteSchemeValueFeeRule rule);

    int deleteQuoteSchemeValueFeeRule(@Param("schemeId") Long schemeId,
        @Param("valueFeeRuleId") Long valueFeeRuleId);
}
