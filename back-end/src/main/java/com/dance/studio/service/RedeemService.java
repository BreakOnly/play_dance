package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Goods;
import com.dance.studio.entity.RedeemOrder;
import com.dance.studio.entity.User;
import com.dance.studio.mapper.GoodsMapper;
import com.dance.studio.mapper.RedeemOrderMapper;
import com.dance.studio.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 积分乐园：抓娃娃机(claw 必得) + 指定兑换(redeem)，统一进发放单走审批/物流。 */
@Service
@RequiredArgsConstructor
public class RedeemService {

    private final GoodsMapper goodsMapper;
    private final RedeemOrderMapper redeemMapper;
    private final UserMapper userMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final StudioLock lock;

    /** 指定兑换：兑换款 */
    @Transactional(rollbackFor = Exception.class)
    public RedeemOrder redeem(Long userId, Long goodsId) {
        lock.acquire();
        User stu = userMapper.findById(userId);
        Goods g = goodsMapper.findById(goodsId);
        if (g == null) throw new BizException(ResultCode.GOODS_NOT_FOUND);
        if (g.getStatus() == null || g.getStatus() != 1)
            throw new BizException(ResultCode.GOODS_OFF_SHELF);
        if (!"redeem".equals(g.getCategory()))
            throw new BizException(ResultCode.GOODS_CATEGORY_MISMATCH);
        int cost = g.getPointsRequired();
        if (cost <= 0 || !Long.valueOf(1).equals(g.getStoreId())) throw new BizException("商品信息不正确");
        if (stu.getPointsBalance() < cost) throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
        if (goodsMapper.decreaseStock(goodsId) == 0)
            throw new BizException(ResultCode.GOODS_STOCK_EMPTY);

        accountService.spendPoints(userId, cost, "redeem", goodsId, "积分兑换:" + g.getName());
        return createOrder(stu, g, "redeem", cost);
    }

    private RedeemOrder createOrder(User stu, Goods g, String source, int cost) {
        boolean autoApprove = configService.getBool(stu.getStoreId(), "redeem_auto_approve", false);
        RedeemOrder o = new RedeemOrder();
        o.setStoreId(stu.getStoreId());
        o.setUserId(stu.getId());
        o.setGoodsId(g.getId());
        o.setGoodsName(g.getName());
        o.setSource(source);
        o.setPointsCost(cost);
        o.setPickupCode(
                java.util
                        .UUID
                        .randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase());
        o.setStatus(autoApprove ? "ready" : "pending");
        redeemMapper.insert(o);
        return o;
    }

    // ---------- 后台审批/物流 ----------
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId, Long adminId) {
        lock.acquire();
        RedeemOrder o = mustPending(orderId);
        if (redeemMapper.transition(orderId, "pending", "ready", adminId, null) != 1)
            throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, Long adminId, String reason) {
        lock.acquire();
        RedeemOrder o = mustPending(orderId);
        if (reason == null || reason.isBlank()) throw new BizException("请填写驳回原因");
        if (redeemMapper.transition(orderId, "pending", "rejected", adminId, reason) != 1)
            throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
        // 退积分 + 回库存
        accountService.refundPoints(o.getUserId(), o.getPointsCost(), "redeem", orderId, "兑换驳回退积分");
        goodsMapper.increaseStock(o.getGoodsId());
    }

    public void ship(Long orderId, String company, String no) {
        RedeemOrder o = redeemMapper.findById(orderId);
        if (o == null) throw new BizException(ResultCode.REDEEM_NOT_FOUND);
        if (!"to_ship".equals(o.getStatus()))
            throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
        redeemMapper.updateLogistics(orderId, company, no);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(Long orderId, String pickupCode) {
        lock.acquire();
        RedeemOrder o = redeemMapper.findById(orderId);
        if (o == null) throw new BizException(ResultCode.REDEEM_NOT_FOUND);
        if (pickupCode == null || !pickupCode.trim().equalsIgnoreCase(o.getPickupCode()))
            throw new BizException("领取码不正确，请核对学员出示的兑换记录");
        if ("completed".equals(o.getStatus())) return;
        if (redeemMapper.pickup(orderId, com.dance.studio.common.context.ContextHolder.userId())
                != 1) throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
    }

    private RedeemOrder mustPending(Long orderId) {
        RedeemOrder o = redeemMapper.findById(orderId);
        if (o == null) throw new BizException(ResultCode.REDEEM_NOT_FOUND);
        if (!"pending".equals(o.getStatus()))
            throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
        return o;
    }

    public List<RedeemOrder> adminList(Long storeId, String status, String source) {
        return redeemMapper.list(storeId, status, source);
    }

    public List<RedeemOrder> myOrders(Long userId, String status) {
        return redeemMapper.listByUser(userId, status);
    }

    public void fillAddress(Long orderId, Long userId, String name, String phone, String address) {
        redeemMapper.updateAddress(orderId, userId, name, phone, address);
    }
}
