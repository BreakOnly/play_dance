package com.dance.studio.common;

import lombok.Getter;

/**
 * 业务错误码
 * 0 成功；1000+ 通用；11xx 账户/课时；12xx 约课；13xx 签到；14xx 手办/兑换；15xx 视频
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),

    // 通用
    PARAM_ERROR(1001, "参数错误"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限"),
    NOT_FOUND(1004, "资源不存在"),
    SYSTEM_ERROR(1005, "系统异常"),
    LOGIN_FAIL(1006, "账号或密码错误"),

    // 账户 / 课时 / 积分
    COURSE_HOUR_NOT_ENOUGH(1101, "课时不足，请先充值"),
    COURSE_HOUR_EXPIRED(1102, "课时已过期"),
    POINTS_NOT_ENOUGH(1103, "积分不足"),

    // 约课
    BOOK_WINDOW_INVALID(1201, "不在可约课时间范围内"),
    BOOK_DUPLICATE(1202, "已约该课程，请勿重复约课"),
    BOOK_FULL_WAITLISTED(1203, "课程已满，已加入候补"),
    BOOK_WAITLIST_FULL(1204, "候补已满"),
    BOOK_NOT_FOUND(1205, "约课记录不存在"),
    CANCEL_NOT_ALLOWED(1206, "当前状态不可取消"),
    SCHEDULE_CONFLICT(1207, "排课时间冲突(教师/教室)"),

    // 签到
    CHECKIN_NO_BOOKING(1301, "未找到可签到的约课"),
    CHECKIN_DUPLICATE(1302, "请勿重复签到"),
    CHECKIN_WINDOW_INVALID(1303, "不在签到时段"),
    CHECKIN_QR_INVALID(1304, "签到二维码无效"),

    // 手办 / 兑换
    GOODS_NOT_FOUND(1401, "手办不存在"),
    GOODS_OFF_SHELF(1402, "手办已下架"),
    GOODS_STOCK_EMPTY(1403, "库存不足"),
    GOODS_CATEGORY_MISMATCH(1404, "手办类别不匹配"),
    CLAW_POOL_EMPTY(1405, "暂无可抓取的手办"),
    REDEEM_NOT_FOUND(1406, "兑换/发放单不存在"),
    REDEEM_STATUS_INVALID(1407, "发放单状态不允许此操作"),

    // 视频
    VIDEO_NO_PERMISSION(1501, "无权限观看该视频"),

    // 问答 / 投稿
    QA_NOT_FOUND(1601, "问题不存在或已删除"),
    ANSWER_NOT_FOUND(1602, "回答不存在或已删除"),
    USER_VIDEO_NOT_FOUND(1603, "投稿视频不存在"),
    USER_VIDEO_STATUS_INVALID(1604, "该投稿已处理过"),
    USER_VIDEO_NOT_APPROVED(1605, "视频未过审或已下架");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
