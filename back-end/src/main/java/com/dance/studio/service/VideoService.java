package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.context.ContextHolder;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Video;
import com.dance.studio.mapper.VideoMapper;
import com.dance.studio.service.storage.StorageService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoMapper videoMapper;
    private final StorageService storageService;
    private final com.dance.studio.mapper.CourseScheduleMapper schedules;

    private Long store() {
        return ContextHolder.storeId();
    }

    // ---------- 后台 ----------
    @Transactional(rollbackFor = Exception.class)
    public Video save(Video v, List<Long> visibleUserIds) {
        if (v.getTitle() == null
                || v.getTitle().isBlank()
                || v.getTitle().length() > 100
                || v.getFileUrl() == null
                || v.getScheduleId() == null) throw new BizException("请填写标题、课次并上传视频");
        var schedule = schedules.findById(v.getScheduleId());
        if (schedule == null || !store().equals(schedule.getStoreId()))
            throw new BizException("课次不存在");
        if (!List.of("checked_in", "booked", "custom").contains(v.getScopeType()))
            throw new BizException("视频可见范围不正确");
        v.setCourseId(schedule.getCourseId());
        v.setVideoDate(schedule.getStartTime().toLocalDate());
        storageService.signedUrl(v.getFileUrl(), 60);
        v.setStoreId(store());
        videoMapper.insert(v);
        if ("custom".equals(v.getScopeType()) && visibleUserIds != null) {
            for (Long sid : visibleUserIds) videoMapper.insertVisible(v.getId(), sid);
        }
        return videoMapper.findById(v.getId());
    }

    public List<Video> listAdmin(Long courseId, LocalDate date) {
        return videoMapper.listAdmin(store(), courseId, date);
    }

    public void delete(Long id) {
        videoMapper.deleteById(id, store());
    }

    // ---------- 小程序 ----------
    public List<Video> listForUser(Long userId) {
        return videoMapper.listForUser(store(), userId);
    }

    /** 鉴权后返回带时效的播放地址 */
    public String playUrl(Long videoId, Long userId) {
        if (videoMapper.canAccess(videoId, userId) == 0) {
            throw new BizException(ResultCode.VIDEO_NO_PERMISSION);
        }
        Video v = videoMapper.findById(videoId);
        return storageService.signedUrl(v.getFileUrl(), 3600);
    }
}
