package com.dance.studio.mapper;

import com.dance.studio.entity.QaAnswer;
import com.dance.studio.vo.QaAnswerVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QaAnswerMapper {

    int insert(QaAnswer a);

    QaAnswer findById(@Param("id") Long id);

    /** 某问题的回答列表（含作者与点赞态） */
    List<QaAnswerVo> listByQuestion(@Param("questionId") Long questionId,
                                    @Param("viewerId") Long viewerId);

    /** 首条回答预览（列表用） */
    QaAnswerVo findFirst(@Param("questionId") Long questionId, @Param("viewerId") Long viewerId);

    /** 后台：某问题的全部回答（含隐藏；viewerId 传 null，不查点赞态） */
    List<QaAnswerVo> listByQuestionAdmin(@Param("questionId") Long questionId,
                                         @Param("viewerId") Long viewerId);

    /** 点赞数 ±1 */
    int changeLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /** 后台隐藏/恢复 */
    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
