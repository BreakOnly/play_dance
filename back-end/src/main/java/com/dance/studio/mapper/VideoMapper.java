package com.dance.studio.mapper;

import com.dance.studio.entity.Video;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface VideoMapper {
    int insert(Video v);
    Video findById(@Param("id") Long id);
    int deleteById(@Param("id") Long id, @Param("storeId") Long storeId);

    /** 后台列表 */
    List<Video> listAdmin(@Param("storeId") Long storeId,
                          @Param("courseId") Long courseId,
                          @Param("date") LocalDate date);

    /** 学员可见视频 */
    List<Video> listForStudent(@Param("storeId") Long storeId, @Param("studentId") Long studentId);

    /** 学员是否可见某视频 */
    int canAccess(@Param("videoId") Long videoId, @Param("studentId") Long studentId);

    // 自定义可见名单
    int insertVisible(@Param("videoId") Long videoId, @Param("studentId") Long studentId);
    int deleteVisible(@Param("videoId") Long videoId);
}
