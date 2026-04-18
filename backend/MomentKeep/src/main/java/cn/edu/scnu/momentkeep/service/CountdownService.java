package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.entity.Countdown;
import java.util.List;

public interface CountdownService {
    Countdown createCountdown(Countdown countdown);
    Countdown updateCountdown(Countdown countdown);
    void deleteCountdown(Long id);
    Countdown getCountdownById(Long id);
    List<Countdown> getCountdownsByUserId(Long userId);
    List<Countdown> getActiveCountdowns(Long userId);
}