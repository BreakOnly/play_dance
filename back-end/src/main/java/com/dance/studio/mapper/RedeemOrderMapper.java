package com.dance.studio.mapper;

import com.dance.studio.entity.RedeemOrder;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RedeemOrderMapper {
    int insert(RedeemOrder o);
    RedeemOrder findById(@Param("id") Long id);
    List<RedeemOrder> list(@Param("storeId") Long storeId,
                           @Param("status") String status,
                           @Param("source") String source);
    List<RedeemOrder> listByStudent(@Param("studentId") Long studentId,
                                    @Param("status") String status);
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("auditAdminId") Long auditAdminId, @Param("rejectReason") String rejectReason);
    int updateLogistics(@Param("id") Long id, @Param("company") String company, @Param("no") String no);
    int updateAddress(@Param("id") Long id, @Param("studentId") Long studentId,
                      @Param("name") String name, @Param("phone") String phone, @Param("address") String address);
}
