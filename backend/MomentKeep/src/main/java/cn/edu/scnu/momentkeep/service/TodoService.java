package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.entity.Todo;
import java.time.LocalDate;
import java.util.List;

/**
 * 待办服务
 *
 * <p>所有方法都必须显式传入 {@code userId}，由调用方（控制器）从登录态取得，
 * 服务层一律以 {@code user_id + id} 作为查询条件，杜绝横向越权。</p>
 */
public interface TodoService {
    Todo createTodo(Todo todo, Long userId);

    Todo updateTodo(Todo todo, Long userId);

    void deleteTodo(Long id, Long userId);

    Todo getTodoById(Long id, Long userId);

    List<Todo> getTodosByUserId(Long userId);

    List<Todo> getTodosByDate(Long userId, LocalDate date);

    List<Todo> getTodayTodos(Long userId);

    Todo completeTodo(Long id, Long userId, String completionNote);
}
