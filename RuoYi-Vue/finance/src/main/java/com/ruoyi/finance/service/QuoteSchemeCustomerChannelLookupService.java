package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.QuoteSchemeOption;

/**
 * Logistics-owned customer channel lookup port used by finance quote schemes.
 */
public interface QuoteSchemeCustomerChannelLookupService
{
    QuoteSchemeOption selectCustomerChannelOption(String customerChannelCode);

    List<QuoteSchemeOption> selectCustomerChannelOptions(String keyword);
}
