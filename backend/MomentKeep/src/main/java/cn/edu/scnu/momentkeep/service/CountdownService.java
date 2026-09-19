package cn.edu.scnu.momentkeep.service;

import cn.edu.scnu.momentkeep.entity.Countdown;
import java.util.List;

/**
 * 倒计时服务
 *
 * <p>所有写操作都必须带上 {@code userId} 作为归属校验条件，
 * 创建时由服务端写入 userId，不接受客户端传入。</p>
 */
public interface CountdownService {
    Countdown createCountdown(Countdown countdown, Long userId);

    Countdown updateCountdown(Countdown countdown, Long userId);

    void deleteCountdown(Long id, Long userId);

    Countdown getCountdownById(Long id, Long userId);

    List<Countdown> getCountdownsByUserId(Long userId);

    List<Countdown> getActiveCountdowns(Long userId);
}
