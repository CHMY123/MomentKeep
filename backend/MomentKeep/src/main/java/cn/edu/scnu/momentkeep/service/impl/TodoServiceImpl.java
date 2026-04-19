package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.entity.Todo;
import cn.edu.scnu.momentkeep.mapper.TodoMapper;
import cn.edu.scnu.momentkeep.service.TodoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoMapper todoMapper;

    @Override
    public Todo createTodo(Todo todo) {
        todo.setCompleted(false);
        todoMapper.insert(todo);
        return todo;
    }

    @Override
    public Todo updateTodo(Todo todo) {
        todoMapper.updateById(todo);
        return todo;
    }

    @Override
    public void deleteTodo(Long id) {
        todoMapper.deleteById(id);
    }

    @Override
    public Todo getTodoById(Long id) {
        Todo todo = todoMapper.selectById(id);
        if (todo == null) {
            throw new BusinessException("待办事项不存在");
        }
        return todo;
    }

    @Override
    public List<Todo> getTodosByUserId(Long userId) {
        return todoMapper.selectList(new QueryWrapper<Todo>().eq("user_id", userId));
    }

    @Override
    public List<Todo> getTodosByDate(Long userId, LocalDate date) {
        return todoMapper.selectList(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .eq("todo_date", date));
    }

    @Override
    public List<Todo> getTodayTodos(Long userId) {
        return getTodosByDate(userId, LocalDate.now());
    }

    @Override
    public Todo completeTodo(Long id, String completionNote) {
        Todo todo = todoMapper.selectById(id);
        if (todo != null) {
            todo.setCompleted(true);
            todo.setCompletedTime(LocalDateTime.now());
            todo.setCompletionNote(completionNote);
            todoMapper.updateById(todo);
        }
        return todo;
    }

    @Override
    public List<Todo> copyYesterdayTodos(Long userId) {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate today = LocalDate.now();
        
        // 获取昨天未完成的待办
        List<Todo> yesterdayTodos = todoMapper.selectList(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .eq("todo_date", yesterday)
                .eq("completed", false));
        
        // 复制到今天
        for (Todo todo : yesterdayTodos) {
            Todo newTodo = new Todo();
            newTodo.setUserId(todo.getUserId());
            newTodo.setTitle(todo.getTitle());
            newTodo.setDescription(todo.getDescription());
            newTodo.setTodoDate(today);
            newTodo.setCompleted(false);
            newTodo.setCreateTime(LocalDateTime.now());
            todoMapper.insert(newTodo);
        }
        
        return getTodayTodos(userId);
    }
}