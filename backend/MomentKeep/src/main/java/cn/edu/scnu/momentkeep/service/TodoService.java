package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.entity.Todo;
import java.time.LocalDate;
import java.util.List;

public interface TodoService {
    Todo createTodo(Todo todo);
    Todo updateTodo(Todo todo);
    void deleteTodo(Long id);
    Todo getTodoById(Long id);
    List<Todo> getTodosByUserId(Long userId);
    List<Todo> getTodosByDate(Long userId, LocalDate date);
    List<Todo> getTodayTodos(Long userId);
    Todo completeTodo(Long id, String completionNote);
    List<Todo> copyYesterdayTodos(Long userId);
}