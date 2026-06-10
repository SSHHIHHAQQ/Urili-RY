package com.ruoyi.finance.service;

import java.util.List;
import com.ruoyi.finance.domain.QuoteSchemeOption;

/**
 * Buyer-owned lookup port used by finance quote schemes.
 */
public interface QuoteSchemeBuyerLookupService
{
    QuoteSchemeOption selectBuyerOption(Long buyerId);

    List<QuoteSchemeOption> selectBuyerOptions(String keyword);
}
