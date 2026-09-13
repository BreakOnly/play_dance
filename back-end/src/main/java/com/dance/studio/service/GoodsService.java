package com.dance.studio.service;

import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.dto.GoodsSaveReq;
import com.dance.studio.entity.Goods;
import com.dance.studio.mapper.GoodsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsService {
    private final StudioLock lock;

    private final GoodsMapper goodsMapper;
    private final ObjectMapper objectMapper;

    private Long store() {
        return ContextHolder.storeId();
    }

    public List<Goods> list(String category, Integer status) {
        return goodsMapper.list(store(), category, status);
    }

    public Goods detail(Long id) {
        return goodsMapper.findById(id);
    }

    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public Goods save(Long id, GoodsSaveReq req) {
        lock.acquire();
        if (id != null) {
            Goods current = goodsMapper.findById(id);
            if (current == null
                    || !java.util.Objects.equals(current.getVersion(), req.getVersion()))
                throw new com.dance.studio.common.exception.BizException("商品或库存已更新，请刷新后再编辑");
        }
        if (req.getName() == null
                || req.getName().isBlank()
                || req.getName().length() > 100
                || req.getPointsRequired() == null
                || req.getPointsRequired() < 1
                || req.getPointsRequired() > 1000000
                || req.getStock() == null
                || req.getStock() < 0
                || req.getStock() > 100000)
            throw new com.dance.studio.common.exception.BizException("商品名称、积分或库存不正确");
        if (req.getImages() != null && req.getImages().size() > 9)
            throw new com.dance.studio.common.exception.BizException("最多 9 张商品图片");
        req.setCategory("redeem");
        Goods g = new Goods();
        g.setId(id);
        g.setVersion(req.getVersion());
        g.setStoreId(store());
        g.setName(req.getName());
        g.setImages(toJson(req.getImages()));
        g.setDescription(req.getDescription());
        g.setCategory(req.getCategory());
        g.setPointsRequired(req.getPointsRequired());
        g.setStock(req.getStock());
        g.setStatus(req.getStatus());
        g.setSort(req.getSort());
        if (id == null) goodsMapper.insert(g);
        else goodsMapper.update(g);
        return goodsMapper.findById(g.getId());
    }

    public void delete(Long id) {
        goodsMapper.deleteById(id, store());
    }

    private String toJson(List<String> images) {
        if (images == null) return null;
        try {
            return objectMapper.writeValueAsString(images);
        } catch (Exception e) {
            return "[]";
        }
    }
}
