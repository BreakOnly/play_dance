package com.dance.studio.mapper;

import com.dance.studio.entity.CourseHourBatch;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CourseHourBatchMapper {
    @org.apache.ibatis.annotations.Select(
            "SELECT * FROM course_hour_batch WHERE user_id=#{userId} ORDER BY (expire_at IS"
                    + " NULL),expire_at,id FOR UPDATE")
    List<CourseHourBatch> lockAll(Long userId);

    @org.apache.ibatis.annotations.Update(
            "UPDATE course_hour_batch SET remaining=remaining+#{qty} WHERE id=#{id} AND remaining+#{qty}<=total")
    int restore(@Param("id") Long id, @Param("qty") int qty);

    @org.apache.ibatis.annotations.Select(
            "SELECT DISTINCT user_id FROM course_hour_batch WHERE expire_at<=NOW() AND"
                    + " remaining>0 ORDER BY user_id")
    List<Long> expiredUsers();

    int insert(CourseHourBatch batch);

    /** 未过期且有剩余的批次，按到期时间 FIFO（NULL 永久排最后） */
    List<CourseHourBatch> listAvailable(@Param("userId") Long userId);

    /** 最早到期的有效批次时间（会员有效期展示；NULL=永久/无课时） */
    LocalDateTime earliestExpire(@Param("userId") Long userId);

    /** 扣减指定批次剩余（条件保证不超扣） */
    int decrease(@Param("id") Long id, @Param("qty") int qty);

    /** 已过期且仍有剩余的批次（定时销账用，跨学员） */
    List<CourseHourBatch> listExpiredWithRemaining(@Param("now") LocalDateTime now);

    /** 批次剩余清零（过期销账），返回影响行数 */
    int zeroRemaining(@Param("id") Long id);

    /** 学员已过期但尚未销账的批次剩余合计（约课时校验"未过期"用） */
    int sumExpiredRemaining(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
