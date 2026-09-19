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
    public Todo createTodo(Todo todo, Long userId) {
        if (todo.getTitle() == null || todo.getTitle().trim().isEmpty()) {
            throw new BusinessException("待办标题不能为空");
        }
        todo.setId(null);
        todo.setUserId(userId);
        todo.setCompleted(false);
        todo.setCompletedTime(null);
        todo.setCompletionNote(null);
        todoMapper.insert(todo);
        return todo;
    }

    @Override
    public Todo updateTodo(Todo todo, Long userId) {
        if (todo.getId() == null) {
            throw new BusinessException("待办ID不能为空");
        }
        // 先确认这条待办属于当前用户，否则一律按"不存在"处理（不暴露他人数据是否存在）
        Todo existing = getTodoById(todo.getId(), userId);

        existing.setTitle(todo.getTitle() == null ? existing.getTitle() : todo.getTitle());
        existing.setDescription(todo.getDescription());
        existing.setPriority(todo.getPriority());
        if (todo.getCompleted() != null) {
            existing.setCompleted(todo.getCompleted());
        }
        // userId / createTime 不允许被客户端覆盖
        existing.setUserId(userId);

        if (todoMapper.updateById(existing) == 0) {
            throw new BusinessException("待办已被修改，请刷新后重试");
        }
        return existing;
    }

    @Override
    public void deleteTodo(Long id, Long userId) {
        int affected = todoMapper.delete(new QueryWrapper<Todo>()
                .eq("id", id)
                .eq("user_id", userId));
        if (affected == 0) {
            throw new BusinessException("待办事项不存在");
        }
    }

    @Override
    public Todo getTodoById(Long id, Long userId) {
        Todo todo = todoMapper.selectOne(new QueryWrapper<Todo>()
                .eq("id", id)
                .eq("user_id", userId));
        if (todo == null) {
            throw new BusinessException("待办事项不存在");
        }
        return todo;
    }

    @Override
    public List<Todo> getTodosByUserId(Long userId) {
        return todoMapper.selectList(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .orderByAsc("completed")
                .orderByDesc("create_time"));
    }

    @Override
    public List<Todo> getTodosByDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();

        return todoMapper.selectList(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .ge("create_time", startOfDay)
                .lt("create_time", startOfNextDay)
                .orderByDesc("create_time"));
    }

    /**
     * 今日待办 = 所有未完成的待办 + 今天已完成的待办。
     *
     * <p>表结构中没有 todo_date 字段，因此不能按"计划日期"过滤；
     * 原先该接口直接返回全部待办，语义与命名不符，这里按"未完成 + 今日完成"收敛，
     * 既符合使用直觉，也不会把几天前已完成的事项一直堆在今日列表里。</p>
     */
    @Override
    public List<Todo> getTodayTodos(Long userId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        return todoMapper.selectList(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .and(wrapper -> wrapper
                        .eq("completed", false)
                        .or()
                        .ge("completed_time", startOfToday))
                .orderByAsc("completed")
                .orderByDesc("create_time"));
    }

    @Override
    public Todo completeTodo(Long id, Long userId, String completionNote) {
        Todo todo = getTodoById(id, userId);
        todo.setCompleted(true);
        todo.setCompletedTime(LocalDateTime.now());
        todo.setCompletionNote(completionNote);
        if (todoMapper.updateById(todo) == 0) {
            throw new BusinessException("待办已被修改，请刷新后重试");
        }
        return todo;
    }
}
