package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.mapper.CountdownMapper;
import cn.edu.scnu.momentkeep.service.CountdownService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CountdownServiceImpl implements CountdownService {

    private final CountdownMapper countdownMapper;

    @Override
    public Countdown createCountdown(Countdown countdown, Long userId) {
        if (countdown.getTitle() == null || countdown.getTitle().trim().isEmpty()) {
            throw new BusinessException("倒计时标题不能为空");
        }
        if (countdown.getTargetTime() == null) {
            throw new BusinessException("目标时间不能为空");
        }
        // 原实现完全没有写入 userId，导致数据成为孤儿；这里由服务端强制赋值
        countdown.setId(null);
        countdown.setUserId(userId);
        countdownMapper.insert(countdown);
        return countdown;
    }

    @Override
    public Countdown updateCountdown(Countdown countdown, Long userId) {
        if (countdown.getId() == null) {
            throw new BusinessException("倒计时ID不能为空");
        }
        // 归属校验：不是自己的数据一律按"不存在"处理
        getCountdownById(countdown.getId(), userId);

        UpdateWrapper<Countdown> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", countdown.getId())
                .eq("user_id", userId);

        if (countdown.getTitle() != null) {
            if (countdown.getTitle().trim().isEmpty()) {
                throw new BusinessException("倒计时标题不能为空");
            }
            updateWrapper.set("title", countdown.getTitle());
        }
        if (countdown.getDescription() != null) {
            updateWrapper.set("description", countdown.getDescription());
        }
        if (countdown.getTargetTime() != null) {
            updateWrapper.set("target_time", countdown.getTargetTime());
        }
        if (countdown.getColor() != null) {
            updateWrapper.set("color", countdown.getColor());
        }
        if (countdown.getSortOrder() != null) {
            updateWrapper.set("sort_order", countdown.getSortOrder());
        }

        if (countdownMapper.update(null, updateWrapper) == 0) {
            throw new BusinessException("倒计时不存在");
        }
        return getCountdownById(countdown.getId(), userId);
    }

    @Override
    public void deleteCountdown(Long id, Long userId) {
        int affected = countdownMapper.delete(new QueryWrapper<Countdown>()
                .eq("id", id)
                .eq("user_id", userId));
        if (affected == 0) {
            throw new BusinessException("倒计时不存在");
        }
    }

    @Override
    public Countdown getCountdownById(Long id, Long userId) {
        Countdown countdown = countdownMapper.selectOne(new QueryWrapper<Countdown>()
                .eq("id", id)
                .eq("user_id", userId));
        if (countdown == null) {
            throw new BusinessException("倒计时不存在");
        }
        return countdown;
    }

    @Override
    public List<Countdown> getCountdownsByUserId(Long userId) {
        return countdownMapper.selectList(new QueryWrapper<Countdown>()
                .eq("user_id", userId)
                .orderByAsc("sort_order")
                .orderByAsc("target_time"));
    }

    @Override
    public List<Countdown> getActiveCountdowns(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return countdownMapper.selectList(new QueryWrapper<Countdown>()
                .eq("user_id", userId)
                .gt("target_time", now)
                .orderByAsc("target_time"));
    }
}
