package com.dance.studio.controller.mp;

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

@Tag(name = "小程序-资讯问答")
@RestController
@RequestMapping("/api/mp/qa")
@RequiredArgsConstructor
public class MpQaController {

    private final QaService qaService;

    @Operation(summary = "问题列表(分页,含首答预览)")
    @GetMapping("/questions")
    public Result<PageResult<QaQuestionVo>> questions(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return Result.success(qaService.questions(ContextHolder.storeId(), ContextHolder.userId(), page, size));
    }

    @Operation(summary = "问题详情+回答列表")
    @GetMapping("/questions/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.success(qaService.detail(id, ContextHolder.userId()));
    }

    @Operation(summary = "发布问题")
    @PostMapping("/questions")
    public Result<Void> ask(@RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) body.get("images");
        qaService.ask(ContextHolder.userId(), ContextHolder.storeId(), content, images);
        return Result.success();
    }

    @Operation(summary = "回答问题")
    @PostMapping("/questions/{id}/answers")
    public Result<Void> answer(@PathVariable Long id, @RequestBody Map<String, String> body) {
        qaService.answer(id, ContextHolder.userId(), body.get("content"));
        return Result.success();
    }

    @Operation(summary = "点赞/取消(question|answer)")
    @PostMapping("/like")
    public Result<Map<String, Object>> like(@RequestBody Map<String, Object> body) {
        String targetType = (String) body.get("targetType");
        Long targetId = ((Number) body.get("targetId")).longValue();
        return Result.success(qaService.toggleLike(targetType, targetId, ContextHolder.userId()));
    }
}
