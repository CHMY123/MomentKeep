package cn.edu.scnu.momentkeep.service.impl;

import cn.edu.scnu.momentkeep.common.BusinessException;
import cn.edu.scnu.momentkeep.entity.Countdown;
import cn.edu.scnu.momentkeep.mapper.CountdownMapper;
import cn.edu.scnu.momentkeep.service.CountdownService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CountdownServiceImpl implements CountdownService {

    private final CountdownMapper countdownMapper;

    @Override
    public Countdown createCountdown(Countdown countdown) {
        countdownMapper.insert(countdown);
        return countdown;
    }

    @Override
    public Countdown updateCountdown(Countdown countdown) {
        countdownMapper.updateById(countdown);
        return countdown;
    }

    @Override
    public void deleteCountdown(Long id) {
        countdownMapper.deleteById(id);
    }

    @Override
    public Countdown getCountdownById(Long id) {
        Countdown countdown = countdownMapper.selectById(id);
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