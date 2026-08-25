package com.dance.studio.mapper;

import com.dance.studio.entity.QaQuestion;
import com.dance.studio.vo.QaQuestionVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QaQuestionMapper {

    int insert(QaQuestion q);

    QaQuestion findById(@Param("id") Long id);

    /** 列表（最新在前，含作者与点赞态） */
    List<QaQuestionVo> list(@Param("storeId") Long storeId,
                            @Param("viewerId") Long viewerId,
                            @Param("limit") Integer limit,
                            @Param("offset") Integer offset);

    int count(@Param("storeId") Long storeId);

    /** 后台列表（含隐藏问题） */
    List<QaQuestionVo> listAdmin(@Param("storeId") Long storeId,
                                 @Param("limit") Integer limit,
                                 @Param("offset") Integer offset);

    /** 后台总数（含隐藏问题） */
    int countAdmin(@Param("storeId") Long storeId);

    QaQuestionVo findVoById(@Param("id") Long id, @Param("viewerId") Long viewerId);

    /** 点赞数 ±1 */
    int changeLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 后台隐藏/恢复 */
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    /** 回答数 +1 */
    int incrAnswerCount(@Param("id") Long id);
}
