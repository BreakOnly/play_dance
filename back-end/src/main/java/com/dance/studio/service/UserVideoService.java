package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.UserVideo;
import com.dance.studio.mapper.LikeRecordMapper;
import com.dance.studio.mapper.UserVideoMapper;
import com.dance.studio.service.storage.StorageService;
import com.dance.studio.vo.UserVideoVo;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 公开推荐视频与幂等点赞；点赞不参与账户积分。 */
@Service
@RequiredArgsConstructor
public class UserVideoService {

    private final UserVideoMapper userVideoMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final NotificationService notificationService;
    private final StorageService storageService;
    private final StudioLock lock;

    @Transactional(rollbackFor = Exception.class)
    public UserVideo publish(UserVideo input, Long adminId) {
        lock.acquire();
        if (input.getTitle() == null
                || input.getTitle().isBlank()
                || input.getTitle().length() > 100
                || input.getFileUrl() == null
                || input.getFileUrl().isBlank()) throw new BizException("请填写标题并上传视频");
        // 显式发布动作，不从私有课堂视频自动复制。
        storageService.signedUrl(input.getFileUrl(), 60);
        UserVideo v = new UserVideo();
        v.setStoreId(1L);
        v.setUserId(0L);
        v.setTitle(input.getTitle());
        v.setDescription(input.getDescription());
        v.setFileUrl(input.getFileUrl());
        v.setCoverUrl(input.getCoverUrl());
        v.setDuration(input.getDuration());
        v.setFileSize(input.getFileSize());
        userVideoMapper.insert(v);
        userVideoMapper.audit(v.getId(), "approved", null, adminId);
        userVideoMapper.setRecommended(v.getId(), true);
        return userVideoMapper.findById(v.getId());
    }

    @Transactional
    public void hidePublished(Long id) {
        lock.acquire();
        userVideoMapper.hidePublished(id);
    }

    /** 投稿（进 pending，待后台审核） */
    public UserVideo submit(
            Long userId,
            Long storeId,
            String title,
            String fileUrl,
            String coverUrl,
            String description,
            Integer duration,
            Long fileSize) {
        if (title == null || title.isBlank()) throw new BizException("请填写视频标题");
        if (fileUrl == null || fileUrl.isBlank()) throw new BizException("请上传视频");
        UserVideo v = new UserVideo();
        v.setStoreId(storeId);
        v.setUserId(userId);
        v.setTitle(title.trim());
        v.setFileUrl(fileUrl);
        v.setCoverUrl(coverUrl);
        v.setDescription(description);
        v.setDuration(duration);
        v.setFileSize(fileSize);
        userVideoMapper.insert(v);
        return v;
    }

    /** 我的投稿（含审核状态） */
    public List<UserVideoVo> myVideos(Long userId) {
        return userVideoMapper.myVideos(userId);
    }

    /** 点赞排行（approved 按赞数降序） */
    public List<UserVideoVo> ranking(Long storeId, Long viewerId, Integer limit) {
        return userVideoMapper.ranking(
                storeId, viewerId, limit == null ? 100 : Math.max(1, Math.min(limit, 100)));
    }

    /** 推荐位（后台推荐） */
    public List<UserVideoVo> recommended(Long storeId, Long viewerId, Integer limit) {
        return userVideoMapper.recommended(storeId, viewerId, limit);
    }

    /** 设置明确的点赞状态；同一状态重复提交不会改变计数。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long videoId, Long viewerId, boolean liked) {
        lock.acquire();
        UserVideo v = userVideoMapper.findById(videoId);
        if (v == null
                || !"approved".equals(v.getStatus())
                || !Long.valueOf(1).equals(v.getStoreId()))
            throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);

        int changed =
                liked
                        ? likeRecordMapper.insert(viewerId, "video", videoId)
                        : likeRecordMapper.delete(viewerId, "video", videoId);
        if (changed == 0) return Map.of("liked", liked, "likeCount", v.getLikeCount());

        int delta = liked ? 1 : -1;
        if (userVideoMapper.changeLikeCount(videoId, delta) == 0) {
            throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        }

        return Map.of("liked", liked, "likeCount", v.getLikeCount() + delta);
    }

    /** 播放地址（approved 公开，但 file_url 不直接暴露） */
    public String playUrl(Long videoId) {
        UserVideo v = userVideoMapper.findByIdWithUrl(videoId);
        if (v == null) throw new BizException(ResultCode.USER_VIDEO_NOT_APPROVED);
        return storageService.signedUrl(v.getFileUrl(), 3600);
    }

    // ============ 后台审核 ============

    /** 审核通过：上榜 + 通知作者 */
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long adminId) {
        UserVideo v = userVideoMapper.findById(id);
        if (v == null) throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        if (userVideoMapper.audit(id, "approved", null, adminId) == 0) {
            throw new BizException(ResultCode.USER_VIDEO_STATUS_INVALID);
        }
        notificationService.push(
                v.getUserId(),
                "video",
                "投稿已通过",
                "你的视频《" + v.getTitle() + "》已通过审核，现已在点赞排行榜展示，快去分享吧！",
                "user_video",
                id);
    }

    /** 驳回：通知作者并说明原因 */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason, Long adminId) {
        UserVideo v = userVideoMapper.findById(id);
        if (v == null) throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        if (userVideoMapper.audit(id, "rejected", reason, adminId) == 0) {
            throw new BizException(ResultCode.USER_VIDEO_STATUS_INVALID);
        }
        notificationService.push(
                v.getUserId(),
                "video",
                "投稿未通过",
                "你的视频《"
                        + v.getTitle()
                        + "》未通过审核"
                        + (reason == null || reason.isBlank() ? "" : "：" + reason),
                "user_video",
                id);
    }

    /** 推荐至问答页宫格/首页横滑 */
    public void setRecommended(Long id, boolean recommended) {
        if (userVideoMapper.findById(id) == null)
            throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        userVideoMapper.setRecommended(id, recommended);
    }

    /** 后台列表 */
    public List<UserVideoVo> listAdmin(Long storeId, String status) {
        return userVideoMapper.listAdmin(storeId, status);
    }
}
