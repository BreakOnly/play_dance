package com.dance.studio.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedeemOrder {
    private Long id;
    private Long storeId;
    private Long studentId;
    private Long goodsId;
    private String goodsName;
    /** claw抓取 / redeem兑换 */
    private String source;
    private Integer pointsCost;
    /** pending待审批 to_ship待发货 shipping配送中 completed已完成 rejected已驳回 */
    private String status;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String logisticsCompany;
    private String logisticsNo;
    private Long auditAdminId;
    private LocalDateTime auditAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
