package com.focusly.pomodoro.service;

import com.focusly.pomodoro.entity.Task;
import com.focusly.pomodoro.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper mapper;

    public List<Task> list() {
        return mapper.selectList(null);
    }

    public Task create(String title, String description) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(0); // 默认未完成
        mapper.insert(task);
        return task;
    }

    public Task update(Long id, String title, String description, Integer status) {
        Task task = mapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        if (title != null) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (status != null) task.setStatus(status);
        mapper.updateById(task);
        return task;
    }

    public void delete(Long id) {
        if (mapper.selectById(id) == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        mapper.deleteById(id);
    }

    public void clearAll() {
        // 写安全：物理删除全部任务，外键 ON DELETE SET NULL 自动清理会话中的 task_id
        mapper.delete(null);
    }
}
