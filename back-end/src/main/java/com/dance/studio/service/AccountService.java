package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.dto.RechargeReq;
import com.dance.studio.entity.*;
import com.dance.studio.mapper.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/** 余额等于各批次剩余课时；预约直接扣课，取消按原批次退款。门店事务锁保证多进程写入有序。 */
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AccountService {
    private final UserMapper userMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final CourseHourBatchMapper batchMapper;
    private final HourLogMapper hourLogMapper;
    private final PointLogMapper pointLogMapper;
    private final ConfigService configService;
    private final HourAllocationMapper allocationMapper;
    private final StudioLock lock;

    private User user(Long id) {
        User u = userMapper.findById(id);
        if (u == null || !Objects.equals(u.getStoreId(), 1L))
            throw new BizException(ResultCode.NOT_FOUND);
        return u;
    }

    private void changed(int rows) {
        if (rows != 1) throw new BizException("账目校验失败，操作已撤销，请联系管理员核对");
    }

    private void positive(int amount) {
        if (amount <= 0 || amount > 100000) throw new BizException("课时必须在 1 到 100000 之间");
    }

    public void recharge(Long userId, RechargeReq req, Long operatorId) {
        lock.acquire();
        expireUser(userId);
        int gift = req.getGiftHours() == null ? 0 : req.getGiftHours();
        int total = Math.addExact(req.getHours(), gift);
        positive(total);
        if (gift < 0 || req.getAmount() == null || req.getAmount().signum() < 0)
            throw new BizException("充值数值不正确");
        int days =
                req.getValidityDays() == null
                        ? configService.getInt(1L, "course_validity_days", 365)
                        : req.getValidityDays();
        if (days < 0 || days > 36500) throw new BizException("有效期不正确");
        int points =
                req.getAmount()
                        .multiply(
                                java.math.BigDecimal.valueOf(
                                        configService.getInt(1L, "points_per_yuan", 1)))
                        .setScale(0, RoundingMode.DOWN)
                        .intValueExact();
        RechargeOrder order = new RechargeOrder();
        order.setStoreId(1L);
        order.setUserId(userId);
        order.setHours(req.getHours());
        order.setGiftHours(gift);
        order.setAmount(req.getAmount());
        order.setPointsGain(points);
        order.setValidityDays(days);
        order.setOperatorId(operatorId);
        order.setRemark(req.getRemark());
        rechargeOrderMapper.insert(order);
        CourseHourBatch b = new CourseHourBatch();
        b.setUserId(userId);
        b.setTotal(total);
        b.setRemaining(total);
        b.setSource("recharge");
        b.setOrderId(order.getId());
        b.setExpireAt(days == 0 ? null : LocalDateTime.now().plusDays(days));
        batchMapper.insert(b);
        changed(userMapper.addCourseBalance(userId, total));
        writeHourLog(
                userId,
                "recharge",
                total,
                "recharge",
                order.getId(),
                operatorId,
                "充值含赠送 " + gift + " 课时");
        if (points > 0)
            changePoints(
                    userId, points, "recharge", "recharge", order.getId(), operatorId, "充值赠送积分");
        assertBalanced(userId);
    }

    public void adjustPoints(Long userId, int delta, String reason, Long operatorId) {
        lock.acquire();
        if (delta == 0 || Math.abs((long) delta) > 1000000 || reason == null || reason.isBlank())
            throw new BizException("请填写有效积分变动值和原因");
        changePoints(userId, delta, "manual", null, null, operatorId, reason);
    }

    private void changePoints(
            Long userId,
            int delta,
            String type,
            String refType,
            Long refId,
            Long operatorId,
            String remark) {
        user(userId);
        if (delta == 0) return;
        if (userMapper.addPoints(userId, delta) != 1)
            throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
        PointLog p = new PointLog();
        p.setUserId(userId);
        p.setType(type);
        p.setAmount(delta);
        p.setBalanceAfter(user(userId).getPointsBalance());
        p.setRefType(refType);
        p.setRefId(refId);
        p.setOperatorId(operatorId);
        p.setRemark(remark);
        pointLogMapper.insert(p);
    }

    public void deductForBooking(Long userId, int cost, Long bookingId) {
        lock.acquire();
        positive(cost);
        expireUser(userId);
        if (user(userId).getCourseBalance() < cost)
            throw new BizException(ResultCode.COURSE_HOUR_NOT_ENOUGH);
        if (!allocationMapper.charged(bookingId).isEmpty()) throw new BizException("预约已扣课时");
        int left = cost;
        LocalDateTime now = LocalDateTime.now();
        for (CourseHourBatch b : batchMapper.lockAll(userId)) {
            if (left == 0) break;
            if (b.getExpireAt() != null && !b.getExpireAt().isAfter(now)) continue;
            int take = Math.min(left, b.getRemaining());
            if (take <= 0) continue;
            changed(batchMapper.decrease(b.getId(), take));
            HourAllocation a = new HourAllocation();
            a.setBookingId(bookingId);
            a.setBatchId(b.getId());
            a.setHours(take);
            allocationMapper.insert(a);
            left -= take;
        }
        if (left != 0) throw new BizException("可用批次不足，操作已撤销，请核对课时");
        changed(userMapper.addCourseBalance(userId, -cost));
        writeHourLog(userId, "booking", -cost, "booking", bookingId, null, "预约扣课时");
        assertBalanced(userId);
    }

    public void refundBooking(Long userId, int cost, Long bookingId) {
        settle(userId, cost, bookingId, true, "refund");
    }

    public void confirmCheckin(Long userId, int cost, Long bookingId) {
        settle(userId, cost, bookingId, false, "deduct");
    }

    public void confirmNoShow(Long userId, int cost, Long bookingId) {
        settle(userId, cost, bookingId, false, "noshow");
    }

    private void settle(Long userId, int cost, Long bookingId, boolean refund, String type) {
        lock.acquire();
        positive(cost);
        expireUser(userId);
        List<HourAllocation> allocations = allocationMapper.charged(bookingId);
        if (allocations.stream().mapToInt(HourAllocation::getHours).sum() != cost)
            throw new BizException("预约扣课记录不匹配，操作已撤销");
        int returned = 0;
        LocalDateTime now = LocalDateTime.now();
        for (HourAllocation a : allocations) {
            boolean valid = a.getExpireAt() == null || a.getExpireAt().isAfter(now);
            boolean release = refund && valid;
            if (release) changed(batchMapper.restore(a.getBatchId(), a.getHours()));
            changed(
                    allocationMapper.finish(
                            bookingId,
                            a.getBatchId(),
                            release ? "released" : refund ? "expired" : "consumed"));
            if (release) returned += a.getHours();
        }
        if (returned > 0) changed(userMapper.addCourseBalance(userId, returned));
        writeHourLog(
                userId,
                type,
                returned,
                "booking",
                bookingId,
                null,
                refund
                        ? "取消退回 " + returned + " 课时；已到期部分 " + (cost - returned) + " 课时不恢复"
                        : type.equals("noshow") ? "爽约确认，预约时已扣课时" : "签到确认，预约时已扣课时");
        assertBalanced(userId);
    }

    public int expireBatches() {
        lock.acquire();
        int count = 0;
        for (Long id : batchMapper.expiredUsers()) {
            expireUser(id);
            count++;
        }
        return count;
    }

    public void expireUser(Long userId) {
        lock.acquire();
        user(userId);
        int expired = 0;
        LocalDateTime now = LocalDateTime.now();
        for (CourseHourBatch b : batchMapper.lockAll(userId)) {
            if (b.getExpireAt() != null
                    && !b.getExpireAt().isAfter(now)
                    && b.getRemaining() > 0) {
                expired += b.getRemaining();
                changed(batchMapper.zeroRemaining(b.getId()));
            }
        }
        if (expired > 0) {
            changed(userMapper.addCourseBalance(userId, -expired));
            writeHourLog(userId, "expire", -expired, null, null, null, "剩余课时到期");
        }
        assertBalanced(userId);
    }

    public Map<String, Integer> balances(Long userId) {
        lock.acquire();
        expireUser(userId);
        User u = user(userId);
        int days = configService.getInt(1L, "expiry_reminder_days", 30);
        int expiring =
                batchMapper.lockAll(userId).stream()
                        .filter(
                                b ->
                                        b.getExpireAt() != null
                                                && b.getExpireAt().isAfter(LocalDateTime.now())
                                                && !b.getExpireAt()
                                                        .isAfter(
                                                                LocalDateTime.now().plusDays(days)))
                        .mapToInt(b -> b.getRemaining())
                        .sum();
        return Map.of(
                "available",
                u.getCourseBalance(),
                "points",
                u.getPointsBalance(),
                "expiring",
                expiring);
    }

    public void assertBalanced(Long userId) {
        User u = user(userId);
        List<CourseHourBatch> bs = batchMapper.lockAll(userId);
        int remaining = bs.stream().mapToInt(CourseHourBatch::getRemaining).sum();
        if (remaining != u.getCourseBalance()
                || bs.stream().anyMatch(b -> b.getRemaining() < 0))
            throw new BizException("课时对账不一致，操作已撤销，请联系管理员核对历史记录");
    }

    public void earnPoints(
            Long id, int delta, String type, String refType, Long refId, String remark) {
        lock.acquire();
        if (delta < 0) throw new BizException("积分奖励不能为负");
        changePoints(id, delta, type, refType, refId, null, remark);
    }

    public void spendPoints(Long id, int cost, String refType, Long refId, String remark) {
        lock.acquire();
        if (cost <= 0) throw new BizException("积分消费必须大于零");
        changePoints(id, -cost, "redeem", refType, refId, null, remark);
    }

    public void refundPoints(Long id, int cost, String refType, Long refId, String remark) {
        lock.acquire();
        if (cost <= 0) throw new BizException("积分退回必须大于零");
        changePoints(id, cost, "refund", refType, refId, null, remark);
    }

    private void writeHourLog(
            Long id,
            String type,
            int amount,
            String refType,
            Long refId,
            Long operatorId,
            String remark) {
        HourLog l = new HourLog();
        l.setUserId(id);
        l.setType(type);
        l.setAmount(amount);
        l.setBalanceAfter(user(id).getCourseBalance());
        l.setRefType(refType);
        l.setRefId(refId);
        l.setOperatorId(operatorId);
        l.setRemark(remark);
        hourLogMapper.insert(l);
    }

    public List<HourLog> hourLogs(Long id) {
        return hourLogMapper.listByUser(id);
    }

    public List<PointLog> pointLogs(Long id) {
        return pointLogMapper.listByUser(id);
    }
}
