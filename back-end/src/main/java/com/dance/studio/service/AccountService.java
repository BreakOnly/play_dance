package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.dto.RechargeReq;
import com.dance.studio.entity.*;
import com.dance.studio.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 账户核心服务：课时充值/扣减、积分增减。
 * 约定：所有"流水(*_log) + 缓存(student.*)"在同一事务内更新。
 *
 * 课时模型：
 *   course_balance = 当前拥有的总剩余课时(= sum(batch.remaining))
 *   frozen_hours   = 已被"待上课"约课锁定的课时
 *   可用 available = course_balance - frozen_hours （前端展示用，VO 层计算）
 * 本阶段(P2)实现：充值 / 积分手动增减。
 * lock/refund/deduct/noshow 在 P4(约课+签到) 实现。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final StudentMapper studentMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    private final CourseHourBatchMapper batchMapper;
    private final HourLogMapper hourLogMapper;
    private final PointLogMapper pointLogMapper;
    private final ConfigService configService;

    /** 课时充值：建订单 + 批次 + 流水 + 余额；并按充值金额送积分 */
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long studentId, RechargeReq req, Long operatorId) {
        Student student = studentMapper.findById(studentId);
        if (student == null) throw new BizException(ResultCode.NOT_FOUND);

        int hours = req.getHours();
        int gift = req.getGiftHours() == null ? 0 : req.getGiftHours();
        int totalHours = hours + gift;

        // 有效期
        Integer days = req.getValidityDays();
        if (days == null) {
            days = configService.getInt(student.getStoreId(), "course_validity_days", 365);
        }
        LocalDateTime expireAt = (days == null || days <= 0) ? null : LocalDateTime.now().plusDays(days);

        // 充值金额送积分
        int pointsPerYuan = configService.getInt(student.getStoreId(), "points_per_yuan", 1);
        int pointsGain = req.getAmount() == null ? 0 : req.getAmount().intValue() * pointsPerYuan;

        // 1. 订单
        RechargeOrder order = new RechargeOrder();
        order.setStoreId(student.getStoreId());
        order.setStudentId(studentId);
        order.setHours(hours);
        order.setGiftHours(gift);
        order.setAmount(req.getAmount());
        order.setPointsGain(pointsGain);
        order.setValidityDays(days);
        order.setOperatorId(operatorId);
        order.setRemark(req.getRemark());
        rechargeOrderMapper.insert(order);

        // 2. 批次（合并为一个批次，便于 FIFO）
        CourseHourBatch batch = new CourseHourBatch();
        batch.setStudentId(studentId);
        batch.setTotal(totalHours);
        batch.setRemaining(totalHours);
        batch.setSource("recharge");
        batch.setOrderId(order.getId());
        batch.setExpireAt(expireAt);
        batchMapper.insert(batch);

        // 3. 余额
        studentMapper.addCourseBalance(studentId, totalHours);
        int balAfter = student.getCourseBalance() + totalHours;

        // 4. 课时流水（充值 + 赠送分开记）
        writeHourLog(studentId, "recharge", hours, student.getCourseBalance() + hours, "recharge", order.getId(), operatorId, "课时充值");
        if (gift > 0) {
            writeHourLog(studentId, "gift", gift, balAfter, "recharge", order.getId(), operatorId, "赠送课时");
        }

        // 5. 积分（充值得分）
        if (pointsGain > 0) {
            addPointsInternal(student, pointsGain, "recharge", "recharge", order.getId(), operatorId, "充值赠送积分");
        }
    }

    /** 管理员手动调整积分（正加负减），不可使余额为负 */
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long studentId, int delta, String reason, Long operatorId) {
        Student student = studentMapper.findById(studentId);
        if (student == null) throw new BizException(ResultCode.NOT_FOUND);
        addPointsInternal(student, delta, "manual", null, null, operatorId, reason);
    }

    /** 内部积分变更：更新余额(带不为负保护) + 流水 */
    private void addPointsInternal(Student student, int delta, String type, String refType, Long refId, Long operatorId, String remark) {
        int rows = studentMapper.addPoints(student.getId(), delta);
        if (rows == 0) {
            throw new BizException(ResultCode.POINTS_NOT_ENOUGH);
        }
        int after = student.getPointsBalance() + delta;
        PointLog log = new PointLog();
        log.setStudentId(student.getId());
        log.setType(type);
        log.setAmount(delta);
        log.setBalanceAfter(after);
        log.setRefType(refType);
        log.setRefId(refId);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        pointLogMapper.insert(log);
    }

    // ============ 约课/签到 课时变更（在调用方事务内执行） ============

    /** 约课锁定：可用课时 -cost、冻结 +cost。可用不足或已过期抛异常 */
    public void lockHours(Long studentId, int cost, Long bookingId) {
        Student s = studentMapper.findById(studentId);
        if (s == null) throw new BizException(ResultCode.NOT_FOUND);
        // 原型 §3.1 约课校验"未过期"：剔除已过期(尚未销账)批次对应的课时再判断
        int expired = batchMapper.sumExpiredRemaining(studentId, LocalDateTime.now());
        int frozen = s.getFrozenHours() == null ? 0 : s.getFrozenHours();
        int validAvail = s.getCourseBalance() - Math.max(0, expired - frozen);
        if (validAvail < cost) {
            throw new BizException(s.getCourseBalance() >= cost
                    ? ResultCode.COURSE_HOUR_EXPIRED : ResultCode.COURSE_HOUR_NOT_ENOUGH);
        }
        int rows = studentMapper.addCourseBalance(studentId, -cost);
        if (rows == 0) throw new BizException(ResultCode.COURSE_HOUR_NOT_ENOUGH);
        studentMapper.addFrozen(studentId, cost);
        writeHourLog(studentId, "lock", -cost, s.getCourseBalance() - cost, "booking", bookingId, null, "约课锁定");
    }

    /** lock_on_booking 关闭时：签到直接从可用课时扣减（未经锁定），不足抛异常 */
    public void consumeDirect(Long studentId, int cost, Long bookingId) {
        Student s = studentMapper.findById(studentId);
        int rows = studentMapper.addCourseBalance(studentId, -cost);
        if (rows == 0) throw new BizException(ResultCode.COURSE_HOUR_NOT_ENOUGH);
        consumeBatches(studentId, cost);
        writeHourLog(studentId, "deduct", -cost, s.getCourseBalance() - cost, "booking", bookingId, null, "签到扣减课时");
    }

    /** 取消(免费)退回：可用 +cost、冻结 -cost */
    public void refundLock(Long studentId, int cost, Long bookingId) {
        Student s = studentMapper.findById(studentId);
        studentMapper.addCourseBalance(studentId, cost);
        studentMapper.addFrozen(studentId, -cost);
        writeHourLog(studentId, "refund", cost, s.getCourseBalance() + cost, "booking", bookingId, null, "取消退回课时");
    }

    /** 签到核销：冻结 -cost，并按 FIFO 消耗批次（可用课时已在约课时扣除，故此处不变） */
    public void consumeOnCheckin(Long studentId, int cost, Long bookingId) {
        Student s = studentMapper.findById(studentId);
        studentMapper.addFrozen(studentId, -cost);
        consumeBatches(studentId, cost);
        writeHourLog(studentId, "deduct", 0, s.getCourseBalance(), "booking", bookingId, null, "签到核销(课时已于约课时锁定)");
    }

    /** 爽约/逾期取消：冻结 -cost、消耗批次，课时不退 */
    public void forfeitOnNoShow(Long studentId, int cost, Long bookingId, boolean noshow) {
        Student s = studentMapper.findById(studentId);
        studentMapper.addFrozen(studentId, -cost);
        consumeBatches(studentId, cost);
        writeHourLog(studentId, noshow ? "noshow" : "deduct", 0, s.getCourseBalance(), "booking", bookingId, null,
                noshow ? "爽约核销(课时不退)" : "逾期取消核销(课时不退)");
    }

    /** 按到期 FIFO 消耗课时批次 */
    private void consumeBatches(Long studentId, int qty) {
        int left = qty;
        for (CourseHourBatch b : batchMapper.listAvailable(studentId)) {
            if (left <= 0) break;
            int take = Math.min(b.getRemaining(), left);
            if (take > 0 && batchMapper.decrease(b.getId(), take) > 0) {
                left -= take;
            }
        }
    }

    /**
     * 课时过期销账（定时任务调用，对应原型"有效期365天"）：
     * 已过期批次剩余清零，并按学员扣减可用课时。
     * 销账口径：销账额 = min( max(0, 过期批次剩余合计 - 冻结课时), 可用余额)。
     * 冻结课时不回退——已约课次仍可正常上/按取消规则处理；
     * 已知偏差：若冻结课时落在已过期批次上，批次合计与余额会有少量漂移，随约课完结收敛。
     * @return 被销账的学员数
     */
    @Transactional(rollbackFor = Exception.class)
    public int expireBatches() {
        List<CourseHourBatch> expired = batchMapper.listExpiredWithRemaining(LocalDateTime.now());
        if (expired.isEmpty()) return 0;
        // 按学员归集过期课时合计
        Map<Long, Integer> sumByStudent = new LinkedHashMap<>();
        for (CourseHourBatch b : expired) {
            sumByStudent.merge(b.getStudentId(), b.getRemaining(), Integer::sum);
            batchMapper.zeroRemaining(b.getId());
        }
        int students = 0;
        for (Map.Entry<Long, Integer> e : sumByStudent.entrySet()) {
            Long studentId = e.getKey();
            int expiredHours = e.getValue();
            Student s = studentMapper.findById(studentId);
            if (s == null) continue;
            int frozen = s.getFrozenHours() == null ? 0 : s.getFrozenHours();
            int balance = s.getCourseBalance() == null ? 0 : s.getCourseBalance();
            int writeOff = Math.min(Math.max(0, expiredHours - frozen), balance);
            if (writeOff <= 0) continue;
            studentMapper.addCourseBalance(studentId, -writeOff);
            writeHourLog(studentId, "expire", -writeOff, balance - writeOff, null, null, null, "课时过期销账");
            students++;
        }
        return students;
    }

    // ============ 积分（供签到/兑换调用） ============

    /** 赚取积分（签到等） */
    public void earnPoints(Long studentId, int delta, String type, String refType, Long refId, String remark) {
        Student s = studentMapper.findById(studentId);
        if (delta == 0) return;
        addPointsInternal(s, delta, type, refType, refId, null, remark);
    }

    /** 消费积分（兑换/抓取），不足抛 1103 */
    public void spendPoints(Long studentId, int cost, String refType, Long refId, String remark) {
        Student s = studentMapper.findById(studentId);
        addPointsInternal(s, -cost, "redeem", refType, refId, null, remark);
    }

    /** 退回积分（兑换驳回） */
    public void refundPoints(Long studentId, int cost, String refType, Long refId, String remark) {
        Student s = studentMapper.findById(studentId);
        addPointsInternal(s, cost, "redeem", refType, refId, null, remark);
    }

    private void writeHourLog(Long studentId, String type, int amount, int balanceAfter,
                              String refType, Long refId, Long operatorId, String remark) {
        HourLog log = new HourLog();
        log.setStudentId(studentId);
        log.setType(type);
        log.setAmount(amount);
        log.setBalanceAfter(balanceAfter);
        log.setRefType(refType);
        log.setRefId(refId);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        hourLogMapper.insert(log);
    }

    public List<HourLog> hourLogs(Long studentId) {
        return hourLogMapper.listByStudent(studentId);
    }

    public List<PointLog> pointLogs(Long studentId) {
        return pointLogMapper.listByStudent(studentId);
    }
}
