package com.dance.studio.mapper;

import org.apache.ibatis.annotations.Param;

public interface LikeRecordMapper {

    /** 插入点赞（唯一键冲突返回0=已赞过） */
    int insert(@Param("studentId") Long studentId,
               @Param("targetType") String targetType,
               @Param("targetId") Long targetId);

    /** 删除点赞（未赞过返回0） */
    int delete(@Param("studentId") Long studentId,
               @Param("targetType") String targetType,
               @Param("targetId") Long targetId);
}
