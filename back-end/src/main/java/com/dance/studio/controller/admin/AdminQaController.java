package com.dance.studio.controller.admin;

import com.dance.studio.common.PageResult;
import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.service.QaService;
import com.dance.studio.vo.QaAnswerVo;
import com.dance.studio.vo.QaQuestionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "后台-问答管理")
@RestController
@RequestMapping("/api/admin/qa")
@RequiredArgsConstructor
public class AdminQaController {

    private final QaService qaService;

    @Operation(summary = "问题列表(含隐藏,分页)")
    @GetMapping("/questions")
    public Result<PageResult<QaQuestionVo>> questions(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return Result.success(qaService.listAdmin(ContextHolder.storeId(), page, size));
    }

    @Operation(summary = "隐藏问题(违规,连带隐藏回答)")
    @PostMapping("/questions/{id}/hide")
    public Result<Void> hide(@PathVariable Long id) {
        qaService.hideQuestion(id);
        return Result.success();
    }

    @Operation(summary = "恢复问题")
    @PostMapping("/questions/{id}/show")
    public Result<Void> show(@PathVariable Long id) {
        qaService.showQuestion(id);
        return Result.success();
    }

    @Operation(summary = "某问题的回答列表")
    @GetMapping("/questions/{id}/answers")
    public Result<List<QaAnswerVo>> answers(@PathVariable Long id) {
        return Result.success(qaService.answers(id));
    }

    @Operation(summary = "以老师身份回答")
    @PostMapping("/questions/{id}/answers")
    public Result<Void> teacherAnswer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long teacherId = body.get("teacherId") == null ? null : ((Number) body.get("teacherId")).longValue();
        String content = (String) body.get("content");
        qaService.teacherAnswer(id, teacherId, content);
        return Result.success();
    }

    @Operation(summary = "隐藏回答(违规)")
    @PostMapping("/answers/{id}/hide")
    public Result<Void> hideAnswer(@PathVariable Long id) {
        qaService.hideAnswer(id);
        return Result.success();
    }
}
