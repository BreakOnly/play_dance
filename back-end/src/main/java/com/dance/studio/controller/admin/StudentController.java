package com.dance.studio.controller.admin;

import com.dance.studio.common.PageResult;
import com.dance.studio.common.Result;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.dto.PointAdjustReq;
import com.dance.studio.dto.RechargeReq;
import com.dance.studio.dto.StudentSaveReq;
import com.dance.studio.entity.HourLog;
import com.dance.studio.entity.PointLog;
import com.dance.studio.entity.Student;
import com.dance.studio.service.AccountService;
import com.dance.studio.service.StudentService;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-学员管理")
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AccountService accountService;

    @Operation(summary = "学员列表(分页/搜索/状态)")
    @GetMapping
    public Result<PageResult<Student>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        PageHelper.startPage(page, size);
        List<Student> list = studentService.list(keyword, status);
        return Result.success(PageResult.of(list));
    }

    @Operation(summary = "学员详情")
    @GetMapping("/{id}")
    public Result<Student> detail(@PathVariable Long id) {
        return Result.success(studentService.detail(id));
    }

    @Operation(summary = "新增学员")
    @PostMapping
    public Result<Student> create(@RequestBody StudentSaveReq req) {
        return Result.success(studentService.create(req));
    }

    @Operation(summary = "编辑学员")
    @PutMapping("/{id}")
    public Result<Student> update(@PathVariable Long id, @RequestBody StudentSaveReq req) {
        return Result.success(studentService.update(id, req));
    }

    @Operation(summary = "课时充值")
    @PostMapping("/{id}/recharge")
    public Result<Student> recharge(@PathVariable Long id, @Valid @RequestBody RechargeReq req) {
        accountService.recharge(id, req, ContextHolder.userId());
        return Result.success(studentService.detail(id));
    }

    @Operation(summary = "积分手动增减")
    @PostMapping("/{id}/points")
    public Result<Student> adjustPoints(@PathVariable Long id, @Valid @RequestBody PointAdjustReq req) {
        accountService.adjustPoints(id, req.getDelta(), req.getReason(), ContextHolder.userId());
        return Result.success(studentService.detail(id));
    }

    @Operation(summary = "课时流水")
    @GetMapping("/{id}/hour-logs")
    public Result<List<HourLog>> hourLogs(@PathVariable Long id) {
        return Result.success(accountService.hourLogs(id));
    }

    @Operation(summary = "积分流水")
    @GetMapping("/{id}/point-logs")
    public Result<List<PointLog>> pointLogs(@PathVariable Long id) {
        return Result.success(accountService.pointLogs(id));
    }
}
