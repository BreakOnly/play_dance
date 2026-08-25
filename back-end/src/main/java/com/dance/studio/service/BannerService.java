package com.dance.studio.service;

import com.dance.studio.entity.Banner;
import com.dance.studio.mapper.BannerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页 Banner 管理（后台配置，小程序首页顶部轮播展示）。
 */
@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerMapper bannerMapper;

    /** 后台列表（含下架） */
    public List<Banner> list(Long storeId) {
        return bannerMapper.listByStore(storeId, false);
    }

    /** 小程序端上架列表（按 sort 排序） */
    public List<Banner> listEnabled(Long storeId) {
        return bannerMapper.listByStore(storeId, true);
    }

    public Banner save(Long id, Banner b, Long storeId) {
        b.setStoreId(storeId);
        if (id == null) {
            bannerMapper.insert(b);
        } else {
            b.setId(id);
            bannerMapper.update(b);
        }
        return bannerMapper.findById(b.getId());
    }

    public void delete(Long id, Long storeId) {
        bannerMapper.deleteById(id, storeId);
    }
}
