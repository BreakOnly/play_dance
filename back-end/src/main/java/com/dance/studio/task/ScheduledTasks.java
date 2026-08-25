package com.dance.studio.task;

import com.dance.studio.mapper.CourseScheduleMapper;
import com.dance.studio.service.AccountService;
import com.dance.studio.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：课次结束流转、爽约判定、课时过期销账。
 * （临期提醒等可在此扩展）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final CourseScheduleMapper scheduleMapper;
    private final BookingService bookingService;
    private final AccountService accountService;

    /** 每 5 分钟：结束已过期课次 + 处理爽约 + 课时过期销账 */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void processFinishedAndNoShow() {
        try {
            int finished = scheduleMapper.finishExpired(LocalDateTime.now());
            List<Long> ids = bookingService.noShowCandidateIds();
            int n = 0;
            for (Long id : ids) {
                try { bookingService.handleNoShow(id); n++; }
                catch (Exception e) { log.warn("爽约处理失败 bookingId={}", id, e); }
            }
            if (finished > 0 || n > 0) {
                log.info("定时任务：结束课次 {} 个，爽约处理 {} 条", finished, n);
            }
        } catch (Exception e) {
            log.error("定时任务执行异常", e);
        }
        // 课时过期销账独立 try：不受上方异常影响
        try {
            int expired = accountService.expireBatches();
            if (expired > 0) {
                log.info("定时任务：课时过期销账 {} 名学员", expired);
            }
        } catch (Exception e) {
            log.error("课时过期销账异常", e);
        }
    }
}
