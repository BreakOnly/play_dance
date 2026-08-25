package com.dance.studio.mapper;

import com.dance.studio.entity.Goods;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GoodsMapper {
    List<Goods> list(@Param("storeId") Long storeId,
                     @Param("category") String category,
                     @Param("status") Integer status);
    Goods findById(@Param("id") Long id);
    int insert(Goods g);
    int update(Goods g);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);

    /** 抓娃娃机随机池：可抓取 + 上架 + 有库存 */
    List<Goods> clawPool(@Param("storeId") Long storeId);

    int decreaseStock(@Param("id") Long id);
    int increaseStock(@Param("id") Long id);
}
