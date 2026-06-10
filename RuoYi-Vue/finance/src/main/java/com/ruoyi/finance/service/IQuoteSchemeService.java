package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.QuoteScheme;
import com.ruoyi.finance.domain.QuoteSchemeChannel;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.domain.QuoteSchemeValueFeeRule;
import com.ruoyi.finance.domain.QuoteSchemeWarehouse;

public interface IQuoteSchemeService
{
    List<QuoteScheme> selectQuoteSchemeList(QuoteScheme query);

    QuoteScheme selectQuoteSchemeById(Long schemeId);

    int insertQuoteScheme(QuoteScheme scheme);

    int updateQuoteScheme(Long schemeId, QuoteScheme scheme);

    int updateQuoteSchemeStatus(Long schemeId, String status);

    List<QuoteSchemeWarehouse> selectQuoteSchemeWarehouseList(Long schemeId);

    int saveQuoteSchemeWarehouses(Long schemeId, List<String> warehouseCodes);

    List<QuoteSchemeChannel> selectQuoteSchemeChannelList(Long schemeId);

    int insertQuoteSchemeChannel(Long schemeId, QuoteSchemeChannel channel);

    int updateQuoteSchemeChannel(Long schemeId, Long schemeChannelId, QuoteSchemeChannel channel);

    int deleteQuoteSchemeChannel(Long schemeId, Long schemeChannelId);

    List<QuoteSchemeValueFeeRule> selectQuoteSchemeValueFeeRuleList(Long schemeId);

    int insertQuoteSchemeValueFeeRule(Long schemeId, QuoteSchemeValueFeeRule rule);

    int updateQuoteSchemeValueFeeRule(Long schemeId, Long valueFeeRuleId, QuoteSchemeValueFeeRule rule);

    int deleteQuoteSchemeValueFeeRule(Long schemeId, Long valueFeeRuleId);

    List<QuoteSchemeOption> selectBuyerOptions(String keyword);

    List<QuoteSchemeOption> selectWarehouseOptions(String keyword);

    List<QuoteSchemeOption> selectCustomerChannelOptions(String keyword);

    List<QuoteSchemeOption> selectSystemChannelOptions(String keyword);

    List<QuoteSchemeOption> selectFeePlaceholderOptions(String feeType);
}
