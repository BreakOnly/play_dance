package com.dance.studio.mapper;

import com.dance.studio.entity.RedeemOrder;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RedeemOrderMapper {
    @org.apache.ibatis.annotations.Update(
            "UPDATE redeem_order SET"
                + " status=#{next},audit_admin_id=#{adminId},audit_at=NOW(),reject_reason=#{reason}"
                + " WHERE id=#{id} AND store_id=1 AND status=#{expected}")
    int transition(
            @Param("id") Long id,
            @Param("expected") String expected,
            @Param("next") String next,
            @Param("adminId") Long adminId,
            @Param("reason") String reason);

    @org.apache.ibatis.annotations.Update(
            "UPDATE redeem_order SET"
                + " status='completed',pickup_admin_id=#{adminId},picked_up_at=NOW() WHERE id=#{id}"
                + " AND store_id=1 AND status='ready'")
    int pickup(@Param("id") Long id, @Param("adminId") Long adminId);

    int insert(RedeemOrder o);

    RedeemOrder findById(@Param("id") Long id);

    List<RedeemOrder> list(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            @Param("source") String source);

    List<RedeemOrder> listByUser(@Param("userId") Long userId, @Param("status") String status);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("auditAdminId") Long auditAdminId,
            @Param("rejectReason") String rejectReason);

    int updateLogistics(
            @Param("id") Long id, @Param("company") String company, @Param("no") String no);

    int updateAddress(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("phone") String phone,
            @Param("address") String address);
}
