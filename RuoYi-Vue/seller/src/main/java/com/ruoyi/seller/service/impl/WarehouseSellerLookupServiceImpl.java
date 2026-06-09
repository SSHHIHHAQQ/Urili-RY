package com.ruoyi.seller.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.seller.mapper.SellerMapper;
import com.ruoyi.warehouse.domain.WarehouseSellerProfile;
import com.ruoyi.warehouse.service.WarehouseSellerLookupService;

/**
 * 仓库模块读取卖家快照的 seller 端实现。
 */
@Service
public class WarehouseSellerLookupServiceImpl implements WarehouseSellerLookupService
{
    @Autowired
    private SellerMapper sellerMapper;

    @Override
    public boolean isNormalSeller(Long sellerId)
    {
        return sellerId != null && sellerMapper.countNormalSellerById(sellerId) > 0;
    }

    @Override
    public WarehouseSellerProfile selectSellerProfile(Long sellerId)
    {
        if (sellerId == null)
        {
            return null;
        }
        return sellerMapper.selectWarehouseSellerProfileById(sellerId);
    }

    @Override
    public List<WarehouseSellerProfile> selectSellerProfilesByIds(Collection<Long> sellerIds)
    {
        Set<Long> normalizedIds = normalizeSellerIds(sellerIds);
        if (normalizedIds.isEmpty())
        {
            return Collections.emptyList();
        }
        return sellerMapper.selectWarehouseSellerProfilesByIds(normalizedIds);
    }

    @Override
    public List<WarehouseSellerProfile> selectSellerProfilesByKeyword(String keyword)
    {
        String normalizedKeyword = StringUtils.trimToNull(keyword);
        if (normalizedKeyword == null)
        {
            return Collections.emptyList();
        }
        return sellerMapper.selectWarehouseSellerProfilesByKeyword(normalizedKeyword);
    }

    @Override
    public List<WarehouseSellerProfile> selectNormalSellerOptions(String keyword)
    {
        return sellerMapper.selectNormalWarehouseSellerOptions(StringUtils.trimToNull(keyword));
    }

    private Set<Long> normalizeSellerIds(Collection<Long> sellerIds)
    {
        if (sellerIds == null || sellerIds.isEmpty())
        {
            return Collections.emptySet();
        }
        List<Long> normalizedIds = new ArrayList<>();
        for (Long sellerId : sellerIds)
        {
            if (sellerId != null)
            {
                normalizedIds.add(sellerId);
            }
        }
        return normalizedIds.stream()
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
