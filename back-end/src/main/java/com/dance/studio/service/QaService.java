package com.dance.studio.service;

import com.dance.studio.common.PageResult;
import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.QaAnswer;
import com.dance.studio.entity.QaQuestion;
import com.dance.studio.mapper.LikeRecordMapper;
import com.dance.studio.mapper.QaAnswerMapper;
import com.dance.studio.mapper.QaQuestionMapper;
import com.dance.studio.vo.QaAnswerVo;
import com.dance.studio.vo.QaQuestionVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 问答：提问 / 回答（学员或后台以老师身份）/ 点赞。
 */
@Service
@RequiredArgsConstructor
public class QaService {

    private final QaQuestionMapper questionMapper;
    private final QaAnswerMapper answerMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final NotificationService notificationService;

    /** 问题列表（分页，含首答预览） */
    public PageResult<QaQuestionVo> questions(Long storeId, Long viewerId, int page, int size) {
        int total = questionMapper.count(storeId);
        List<QaQuestionVo> list = questionMapper.list(storeId, viewerId, size, (page - 1) * size);
        list.forEach(q -> q.setFirstAnswer(answerMapper.findFirst(q.getId(), viewerId)));
        return new PageResult<>(list, total, page, size);
    }

    /** 问题详情 + 回答列表 */
    public Map<String, Object> detail(Long id, Long viewerId) {
        QaQuestionVo q = questionMapper.findVoById(id, viewerId);
        if (q == null) throw new BizException(ResultCode.QA_NOT_FOUND);
        List<QaAnswerVo> answers = answerMapper.listByQuestion(id, viewerId);
        return Map.of("question", q, "answers", answers);
    }

    /** 发布问题 */
    public QaQuestion ask(Long studentId, Long storeId, String content, List<String> images) {
        if (content == null || content.isBlank()) throw new BizException("问题内容不能为空");
        QaQuestion q = new QaQuestion();
        q.setStoreId(storeId);
        q.setStudentId(studentId);
        q.setContent(content.trim());
        if (images != null && !images.isEmpty()) {
            String json = "[" + images.stream().map(i -> "\"" + i.replace("\"", "\\\"") + "\"")
                    .reduce((a, b) -> a + "," + b).orElse("") + "]";
            q.setImages(json);
        }
        questionMapper.insert(q);
        return q;
    }

    /** 学员回答 */
    @Transactional(rollbackFor = Exception.class)
    public QaAnswer answer(Long questionId, Long studentId, String content) {
        return insertAnswer(questionId, studentId, null, false, content);
    }

    /** 后台以老师身份回答（studentId 为回答关联学员，无则用 0 占位） */
    @Transactional(rollbackFor = Exception.class)
    public QaAnswer teacherAnswer(Long questionId, Long teacherId, String content) {
        return insertAnswer(questionId, null, teacherId, true, content);
    }

    private QaAnswer insertAnswer(Long questionId, Long studentId, Long teacherId, boolean isTeacher, String content) {
        QaQuestion q = questionMapper.findById(questionId);
        if (q == null || q.getStatus() != 1) throw new BizException(ResultCode.QA_NOT_FOUND);
        if (content == null || content.isBlank()) throw new BizException("回答内容不能为空");

        QaAnswer a = new QaAnswer();
        a.setQuestionId(questionId);
        a.setStudentId(studentId != null ? studentId : 0L);
        a.setTeacherId(teacherId);
        a.setIsTeacher(isTeacher ? 1 : 0);
        a.setContent(content.trim());
        answerMapper.insert(a);
        questionMapper.incrAnswerCount(questionId);

        // 通知提问人（自己回答不通知）
        if (studentId != null && !studentId.equals(q.getStudentId())) {
            notificationService.push(q.getStudentId(), "qa", "你的问题有新回答",
                    content.length() > 50 ? content.substring(0, 50) + "…" : content, "qa_question", questionId);
        }
        return a;
    }

    /** 点赞切换（question / answer） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(String targetType, Long targetId, Long viewerId) {
        boolean isQuestion = "question".equals(targetType);
        if (!isQuestion && !"answer".equals(targetType)) {
            throw new BizException(ResultCode.PARAM_ERROR);
        }
        if (isQuestion && (questionMapper.findById(targetId) == null)) {
            throw new BizException(ResultCode.QA_NOT_FOUND);
        }
        if (!isQuestion && (answerMapper.findById(targetId) == null)) {
            throw new BizException(ResultCode.ANSWER_NOT_FOUND);
        }

        boolean liked = likeRecordMapper.insert(viewerId, targetType, targetId) > 0;
        int delta = liked ? 1 : -1;
        int count;
        if (isQuestion) {
            if (questionMapper.changeLikeCount(targetId, delta) == 0) throw new BizException(ResultCode.QA_NOT_FOUND);
            count = questionMapper.findById(targetId).getLikeCount();
        } else {
            if (answerMapper.changeLikeCount(targetId, delta) == 0) throw new BizException(ResultCode.ANSWER_NOT_FOUND);
            count = answerMapper.findById(targetId).getLikeCount();
        }
        return Map.of("liked", liked, "likeCount", count);
    }

    // ============ 后台管理 ============

    public PageResult<QaQuestionVo> listAdmin(Long storeId, int page, int size) {
        int total = questionMapper.countAdmin(storeId);
        List<QaQuestionVo> list = questionMapper.listAdmin(storeId, size, (page - 1) * size);
        return new PageResult<>(list, total, page, size);
    }

    /** 后台：某问题全部回答（含隐藏） */
    public List<QaAnswerVo> answers(Long questionId) {
        return answerMapper.listByQuestionAdmin(questionId);
    }

    /** 隐藏/删除问题（连带隐藏回答） */
    @Transactional(rollbackFor = Exception.class)
    public void hideQuestion(Long id) {
        answerMapper.listByQuestion(id, null).forEach(a -> answerMapper.updateStatus(a.getId(), 0));
        questionMapper.updateStatus(id, 0);
    }

    /** 恢复问题 */
    @Transactional(rollbackFor = Exception.class)
    public void showQuestion(Long id) {
        questionMapper.updateStatus(id, 1);
    }

    /** 隐藏回答 */
    public void hideAnswer(Long id) {
        if (answerMapper.findById(id) == null) throw new BizException(ResultCode.ANSWER_NOT_FOUND);
        answerMapper.updateStatus(id, 0);
    }
}
