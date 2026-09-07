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
import java.util.concurrent.ThreadLocalRandom;

/**
 * 积分乐园：抓娃娃机(claw 必得) + 指定兑换(redeem)，统一进发放单走审批/物流。
 */
@Service
@RequiredArgsConstructor
public class RedeemService {

    private final GoodsMapper goodsMapper;
    private final RedeemOrderMapper redeemMapper;
    private final UserMapper userMapper;
    private final AccountService accountService;
    private final ConfigService configService;

    /** 指定兑换：兑换款 */
    @Transactional(rollbackFor = Exception.class)
    public RedeemOrder redeem(Long userId, Long goodsId) {
        User stu = userMapper.findById(userId);
        Goods g = goodsMapper.findById(goodsId);
        if (g == null) throw new BizException(ResultCode.GOODS_NOT_FOUND);
        if (g.getStatus() == null || g.getStatus() != 1) throw new BizException(ResultCode.GOODS_OFF_SHELF);
        if (!"redeem".equals(g.getCategory())) throw new BizException(ResultCode.GOODS_CATEGORY_MISMATCH);
        int cost = g.getPointsRequired();
        if (stu.getPointsBalance() < cost) throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
        if (goodsMapper.decreaseStock(goodsId) == 0) throw new BizException(ResultCode.GOODS_STOCK_EMPTY);

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
        o.setStatus(autoApprove ? "to_ship" : "pending");
        redeemMapper.insert(o);
        return o;
    }

    // ---------- 后台审批/物流 ----------
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long orderId, Long adminId) {
        RedeemOrder o = mustPending(orderId);
        redeemMapper.updateStatus(orderId, "to_ship", adminId, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, Long adminId, String reason) {
        RedeemOrder o = mustPending(orderId);
        redeemMapper.updateStatus(orderId, "rejected", adminId, reason);
        // 退积分 + 回库存
        accountService.refundPoints(o.getUserId(), o.getPointsCost(), "redeem", orderId, "兑换驳回退积分");
        goodsMapper.increaseStock(o.getGoodsId());
    }

    public void ship(Long orderId, String company, String no) {
        RedeemOrder o = redeemMapper.findById(orderId);
        if (o == null) throw new BizException(ResultCode.REDEEM_NOT_FOUND);
        if (!"to_ship".equals(o.getStatus())) throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
        redeemMapper.updateLogistics(orderId, company, no);
    }

    public void complete(Long orderId) {
        RedeemOrder o = redeemMapper.findById(orderId);
        if (o == null) throw new BizException(ResultCode.REDEEM_NOT_FOUND);
        if (!"shipping".equals(o.getStatus())) throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
        redeemMapper.updateStatus(orderId, "completed", o.getAuditAdminId(), null);
    }

    private RedeemOrder mustPending(Long orderId) {
        RedeemOrder o = redeemMapper.findById(orderId);
        if (o == null) throw new BizException(ResultCode.REDEEM_NOT_FOUND);
        if (!"pending".equals(o.getStatus())) throw new BizException(ResultCode.REDEEM_STATUS_INVALID);
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
