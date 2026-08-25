package com.dance.studio.service;

import com.dance.studio.common.ResultCode;
import com.dance.studio.common.exception.BizException;
import com.dance.studio.entity.Student;
import com.dance.studio.entity.UserVideo;
import com.dance.studio.mapper.LikeRecordMapper;
import com.dance.studio.mapper.StudentMapper;
import com.dance.studio.mapper.UserVideoMapper;
import com.dance.studio.service.storage.StorageService;
import com.dance.studio.vo.UserVideoVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 投稿视频：投稿(审核制) -> 榜单 -> 点赞(作者得积分)。
 */
@Service
@RequiredArgsConstructor
public class UserVideoService {

    private final UserVideoMapper userVideoMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final StudentMapper studentMapper;
    private final AccountService accountService;
    private final ConfigService configService;
    private final NotificationService notificationService;
    private final StorageService storageService;

    /** 投稿（进 pending，待后台审核） */
    public UserVideo submit(Long studentId, Long storeId, String title, String fileUrl,
                            String coverUrl, String description, Integer duration, Long fileSize) {
        if (title == null || title.isBlank()) throw new BizException("请填写视频标题");
        if (fileUrl == null || fileUrl.isBlank()) throw new BizException("请上传视频");
        UserVideo v = new UserVideo();
        v.setStoreId(storeId);
        v.setStudentId(studentId);
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
    public List<UserVideoVo> myVideos(Long studentId) {
        return userVideoMapper.myVideos(studentId);
    }

    /** 点赞排行（approved 按赞数降序） */
    public List<UserVideoVo> ranking(Long storeId, Long viewerId, Integer limit) {
        return userVideoMapper.ranking(storeId, viewerId, limit);
    }

    /** 推荐位（后台推荐） */
    public List<UserVideoVo> recommended(Long storeId, Long viewerId, Integer limit) {
        return userVideoMapper.recommended(storeId, viewerId, limit);
    }

    /**
     * 点赞/取消点赞（切换式）。点赞时按 points_per_video_like 给作者实时加积分；
     * 取消时同额退回。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long videoId, Long viewerId) {
        UserVideo v = userVideoMapper.findById(videoId);
        if (v == null) throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);

        int inserted = likeRecordMapper.insert(viewerId, "video", videoId);
        boolean liked = inserted > 0;

        int delta = liked ? 1 : -1;
        if (userVideoMapper.changeLikeCount(videoId, delta) == 0) {
            throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        }

        // 作者积分（自己点赞自己不加分）
        int points = configService.getInt(v.getStoreId(), "points_per_video_like", 1);
        if (points > 0 && !v.getStudentId().equals(viewerId)) {
            if (liked) {
                accountService.earnPoints(v.getStudentId(), points, "video_like", "user_video", videoId, "投稿获赞");
            } else {
                accountService.spendPoints(v.getStudentId(), points, "user_video", videoId, "取消点赞退回");
            }
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
        notificationService.push(v.getStudentId(), "video", "投稿已通过",
                "你的视频《" + v.getTitle() + "》已通过审核，现已在点赞排行榜展示，快去分享吧！",
                "user_video", id);
    }

    /** 驳回：通知作者并说明原因 */
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason, Long adminId) {
        UserVideo v = userVideoMapper.findById(id);
        if (v == null) throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        if (userVideoMapper.audit(id, "rejected", reason, adminId) == 0) {
            throw new BizException(ResultCode.USER_VIDEO_STATUS_INVALID);
        }
        notificationService.push(v.getStudentId(), "video", "投稿未通过",
                "你的视频《" + v.getTitle() + "》未通过审核" + (reason == null || reason.isBlank() ? "" : "：" + reason),
                "user_video", id);
    }

    /** 推荐至问答页宫格/首页横滑 */
    public void setRecommended(Long id, boolean recommended) {
        if (userVideoMapper.findById(id) == null) throw new BizException(ResultCode.USER_VIDEO_NOT_FOUND);
        userVideoMapper.setRecommended(id, recommended);
    }

    /** 后台列表 */
    public List<UserVideoVo> listAdmin(Long storeId, String status) {
        return userVideoMapper.listAdmin(storeId, status);
    }
}
