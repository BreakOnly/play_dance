package com.dance.studio.mapper;

import com.dance.studio.entity.UserVideo;
import com.dance.studio.vo.UserVideoVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserVideoMapper {

    int insert(UserVideo v);

    UserVideo findById(@Param("id") Long id);

    /** 榜单：已通过 按点赞降序 */
    List<UserVideoVo> ranking(@Param("storeId") Long storeId,
                              @Param("viewerId") Long viewerId,
                              @Param("limit") Integer limit);

    /** 推荐位（is_recommended=1 且已通过） */
    List<UserVideoVo> recommended(@Param("storeId") Long storeId,
                                  @Param("viewerId") Long viewerId,
                                  @Param("limit") Integer limit);

    /** 我的投稿 */
    List<UserVideoVo> myVideos(@Param("userId") Long userId);

    /** 后台列表 */
    List<UserVideoVo> listAdmin(@Param("storeId") Long storeId, @Param("status") String status);

    /** 点赞数 ±1（delta=1/-1），返回影响行数 */
    int changeLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 审核：通过/驳回（仅 pending 可流转），返回影响行数 */
    int audit(@Param("id") Long id, @Param("status") String status,
              @Param("rejectReason") String rejectReason,
              @Param("adminId") Long adminId);

    /** 推荐/取消推荐 */
    int setRecommended(@Param("id") Long id, @Param("recommended") boolean recommended);

    /** 播放地址数据源（含 file_url） */
    UserVideo findByIdWithUrl(@Param("id") Long id);
}
