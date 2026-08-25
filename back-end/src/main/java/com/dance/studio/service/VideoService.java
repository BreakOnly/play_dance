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

    private Long store() { return ContextHolder.storeId(); }

    // ---------- 后台 ----------
    @Transactional(rollbackFor = Exception.class)
    public Video save(Video v, List<Long> visibleStudentIds) {
        v.setStoreId(store());
        videoMapper.insert(v);
        if ("custom".equals(v.getScopeType()) && visibleStudentIds != null) {
            for (Long sid : visibleStudentIds) videoMapper.insertVisible(v.getId(), sid);
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
    public List<Video> listForStudent(Long studentId) {
        return videoMapper.listForStudent(store(), studentId);
    }

    /** 鉴权后返回带时效的播放地址 */
    public String playUrl(Long videoId, Long studentId) {
        if (videoMapper.canAccess(videoId, studentId) == 0) {
            throw new BizException(ResultCode.VIDEO_NO_PERMISSION);
        }
        Video v = videoMapper.findById(videoId);
        return storageService.signedUrl(v.getFileUrl(), 3600);
    }
}
