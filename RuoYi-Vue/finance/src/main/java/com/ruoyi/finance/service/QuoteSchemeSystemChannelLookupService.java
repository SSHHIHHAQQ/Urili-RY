package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.QuoteSchemeOption;

/**
 * Logistics-owned system channel lookup port used by finance quote schemes.
 */
public interface QuoteSchemeSystemChannelLookupService
{
    QuoteSchemeOption selectSystemChannelOption(String systemChannelCode);

    List<QuoteSchemeOption> selectSystemChannelOptions(String keyword);
}
