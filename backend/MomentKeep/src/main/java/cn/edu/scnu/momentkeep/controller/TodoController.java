package cn.edu.scnu.momentkeep.controller;

import cn.edu.scnu.momentkeep.common.Result;
import cn.edu.scnu.momentkeep.entity.Todo;
import cn.edu.scnu.momentkeep.service.TodoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/todo")
@Tag(name = "待办管理")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    @Operation(summary = "创建待办")
    public Result<Todo> createTodo(@RequestBody Todo todo) {
        Todo createdTodo = todoService.createTodo(todo);
        return Result.success(createdTodo);
    }

    @PutMapping
    @Operation(summary = "更新待办")
    public Result<Todo> updateTodo(@RequestBody Todo todo) {
        Todo updatedTodo = todoService.updateTodo(todo);
        return Result.success(updatedTodo);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除待办")
    public Result<Void> deleteTodo(@PathVariable Long id) {
        todoService.deleteTodo(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取待办详情")
    public Result<Todo> getTodoById(@PathVariable Long id) {
        Todo todo = todoService.getTodoById(id);
        return Result.success(todo);
    }

    @GetMapping
    @Operation(summary = "获取用户所有待办")
    public Result<List<Todo>> getTodosByUserId() {
        // 从当前登录用户获取userId
        Long userId = 1L; // 这里需要从SecurityContext中获取，暂时硬编码
        List<Todo> todos = todoService.getTodosByUserId(userId);
        return Result.success(todos);
    }

    @GetMapping("/date/{date}")
    @Operation(summary = "按日期获取待办")
    public Result<List<Todo>> getTodosByDate(@PathVariable LocalDate date) {
        // 从当前登录用户获取userId
        Long userId = 1L; // 这里需要从SecurityContext中获取，暂时硬编码
        List<Todo> todos = todoService.getTodosByDate(userId, date);
        return Result.success(todos);
    }

    @GetMapping("/today")
    @Operation(summary = "获取今日待办")
    public Result<List<Todo>> getTodayTodos() {
        // 从当前登录用户获取userId
        Long userId = 1L; // 这里需要从SecurityContext中获取，暂时硬编码
        List<Todo> todos = todoService.getTodayTodos(userId);
        return Result.success(todos);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成待办")
    public Result<Todo> completeTodo(@PathVariable Long id, @RequestParam String completionNote) {
        Todo completedTodo = todoService.completeTodo(id, completionNote);
        return Result.success(completedTodo);
    }

    @PostMapping("/copy-yesterday")
    @Operation(summary = "复制昨天的待办到今天")
    public Result<List<Todo>> copyYesterdayTodos() {
        // 从当前登录用户获取userId
        Long userId = 1L; // 这里需要从SecurityContext中获取，暂时硬编码
        List<Todo> todos = todoService.copyYesterdayTodos(userId);
        return Result.success(todos);
    }
}