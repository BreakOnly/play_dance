package com.dance.studio.controller.admin;

import com.dance.studio.common.Result;
import com.dance.studio.entity.Room;
import com.dance.studio.service.BasicDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "后台-教室")
@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final BasicDataService service;

    @Operation(summary = "列表")
    @GetMapping
    public Result<List<Room>> list() {
        return Result.success(service.listRooms(false));
    }

    @Operation(summary = "新增")
    @PostMapping
    public Result<Room> create(@RequestBody Room d) {
        d.setId(null);
        return Result.success(service.saveRoom(d));
    }

    @Operation(summary = "编辑")
    @PutMapping("/{id}")
    public Result<Room> update(@PathVariable Long id, @RequestBody Room d) {
        d.setId(id);
        return Result.success(service.saveRoom(d));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteRoom(id);
        return Result.success();
    }
}
