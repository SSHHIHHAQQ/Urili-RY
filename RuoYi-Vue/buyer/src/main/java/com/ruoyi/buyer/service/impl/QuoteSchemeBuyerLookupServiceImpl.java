package com.ruoyi.buyer.service.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.buyer.domain.Buyer;
import com.ruoyi.buyer.mapper.BuyerMapper;
import com.ruoyi.finance.domain.QuoteSchemeOption;
import com.ruoyi.finance.service.QuoteSchemeBuyerLookupService;
import com.ruoyi.system.service.support.PartnerSupport;

/**
 * Buyer implementation of the finance quote scheme lookup port.
 */
@Service
public class QuoteSchemeBuyerLookupServiceImpl implements QuoteSchemeBuyerLookupService
{
    @Autowired
    private BuyerMapper buyerMapper;

    @Override
    public QuoteSchemeOption selectBuyerOption(Long buyerId)
    {
        if (buyerId == null)
        {
            return null;
        }
        Buyer buyer = buyerMapper.selectBuyerById(buyerId);
        if (buyer == null || !PartnerSupport.STATUS_NORMAL.equals(buyer.getStatus()))
        {
            return null;
        }
        return toOption(buyer);
    }

    @Override
    public List<QuoteSchemeOption> selectBuyerOptions(String keyword)
    {
        Buyer query = new Buyer();
        query.setStatus(PartnerSupport.STATUS_NORMAL);
        String normalizedKeyword = StringUtils.trimToNull(keyword);
        return buyerMapper.selectBuyerList(query).stream()
            .filter(item -> matchesKeyword(item, normalizedKeyword))
            .map(this::toOption)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private boolean matchesKeyword(Buyer buyer, String keyword)
    {
        if (keyword == null)
        {
            return true;
        }
        return StringUtils.containsIgnoreCase(buyer.getBuyerCode(), keyword)
            || StringUtils.containsIgnoreCase(buyer.getBuyerName(), keyword)
            || StringUtils.containsIgnoreCase(buyer.getBuyerShortName(), keyword);
    }

    private QuoteSchemeOption toOption(Buyer buyer)
    {
        QuoteSchemeOption option = new QuoteSchemeOption();
        option.setId(buyer.getBuyerId());
        option.setValue(String.valueOf(buyer.getBuyerId()));
        option.setCode(buyer.getBuyerCode());
        option.setName(buyer.getBuyerName());
        option.setShortName(buyer.getBuyerShortName());
        option.setLabel(buyer.getBuyerName() + " (" + buyer.getBuyerCode() + ")");
        option.setKind(buyer.getBuyerLevel());
        option.setSearchText(String.join(" ",
            StringUtils.defaultString(buyer.getBuyerCode()),
            StringUtils.defaultString(buyer.getBuyerName()),
            StringUtils.defaultString(buyer.getBuyerShortName())));
        return option;
    }
}
