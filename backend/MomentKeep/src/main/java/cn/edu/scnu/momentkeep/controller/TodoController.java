package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.Todo;
import cn.edu.scnu.momentkeep.service.TodoService;
import cn.edu.scnu.momentkeep.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/todo")
@Tag(name = "待办管理")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "创建待办")
    public Result<Todo> createTodo(@RequestBody @Valid Todo todo) {
        return Result.success(todoService.createTodo(todo, userService.getCurrentUserId()));
    }

    @PutMapping
    @Operation(summary = "更新待办")
    public Result<Todo> updateTodo(@RequestBody @Valid Todo todo) {
        return Result.success(todoService.updateTodo(todo, userService.getCurrentUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除待办")
    public Result<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id, userService.getCurrentUserId());
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取待办详情")
    public Result<Todo> getTodoById(@PathVariable Long id) {
        return Result.success(todoService.getTodoById(id, userService.getCurrentUserId()));
    }

    @GetMapping
    @Operation(summary = "获取用户所有待办")
    public Result<List<Todo>> getTodosByUserId() {
        return Result.success(todoService.getTodosByUserId(userService.getCurrentUserId()));
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "按创建日期获取待办")
    public Result<List<Todo>> getTodosByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(todoService.getTodosByDate(userService.getCurrentUserId(), date));
    }

    @GetMapping("/today")
    @Operation(summary = "获取今日待办（未完成 + 今日已完成）")
    public Result<List<Todo>> getTodayTodos() {
        return Result.success(todoService.getTodayTodos(userService.getCurrentUserId()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成待办")
    public Result<Todo> completeTodo(@PathVariable Long id, @RequestParam(required = false) String completionNote) {
        return Result.success(todoService.completeTodo(id, userService.getCurrentUserId(), completionNote));
    }
}
