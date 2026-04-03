package com.videosite.backend.video.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.tracking.service.EventService;
import com.videosite.backend.video.dto.PageResult;
import com.videosite.backend.video.dto.VideoCommentCreateRequest;
import com.videosite.backend.video.dto.VideoCommentItemResponse;
import com.videosite.backend.video.dto.VideoLikeSummaryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VideoInteractionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String INTERACTION_PAGE_PATH = "/videos/interaction";

    private final JdbcTemplate jdbcTemplate;
    private final EventService eventService;

    public VideoInteractionService(JdbcTemplate jdbcTemplate, EventService eventService) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventService = eventService;
    }

    @Transactional(rollbackFor = Exception.class)
    public String addLike(Long userId, Long videoId, String visitorId) {
        ensureVideoInteractable(videoId);
        int affected = jdbcTemplate.update(
                "INSERT IGNORE INTO video_like (id, user_id, video_id, created_at) VALUES (?, ?, ?, NOW())",
                IdWorker.getId(),
                userId,
                videoId
        );
        if (affected > 0) {
            eventService.collectServerEvent(visitorId, videoId, "like", INTERACTION_PAGE_PATH, null);
        }
        return "ok";
    }

    @Transactional(rollbackFor = Exception.class)
    public String removeLike(Long userId, Long videoId, String visitorId) {
        int affected = jdbcTemplate.update("DELETE FROM video_like WHERE user_id = ? AND video_id = ?", userId, videoId);
        if (affected > 0) {
            eventService.collectServerEvent(visitorId, videoId, "unlike", INTERACTION_PAGE_PATH, null);
        }
        return "ok";
    }

    public VideoLikeSummaryResponse getLikeSummary(Long videoId, Long currentUserId) {
        ensureVideoInteractable(videoId);

        Long likeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM video_like WHERE video_id = ?",
                Long.class,
                videoId
        );

        boolean likedByCurrentUser = false;
        if (currentUserId != null) {
            Integer liked = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM video_like WHERE video_id = ? AND user_id = ?",
                    Integer.class,
                    videoId,
                    currentUserId
            );
            likedByCurrentUser = liked != null && liked > 0;
        }

        VideoLikeSummaryResponse response = new VideoLikeSummaryResponse();
        response.setVideoId(videoId);
        response.setLikeCount(likeCount == null ? 0 : likeCount);
        response.setLikedByCurrentUser(likedByCurrentUser);
        return response;
    }

    public PageResult<VideoCommentItemResponse> listComments(Long videoId, int page, int pageSize) {
        ensureVideoInteractable(videoId);

        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        int offset = (safePage - 1) * safePageSize;

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM video_comment WHERE video_id = ? AND status = 'normal'",
                Long.class,
                videoId
        );

        List<VideoCommentItemResponse> records = jdbcTemplate.query(
                "SELECT c.id, c.user_id, u.username, c.content, c.created_at " +
                        "FROM video_comment c " +
                        "JOIN app_user u ON u.id = c.user_id " +
                        "WHERE c.video_id = ? AND c.status = 'normal' " +
                        "ORDER BY c.created_at DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> {
                    VideoCommentItemResponse item = new VideoCommentItemResponse();
                    item.setId(rs.getLong("id"));
                    item.setUserId(rs.getLong("user_id"));
                    item.setUsername(rs.getString("username"));
                    item.setContent(rs.getString("content"));
                    item.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
                    return item;
                },
                videoId,
                safePageSize,
                offset
        );

        return new PageResult<>(total == null ? 0 : total, safePage, safePageSize, records);
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoCommentItemResponse createComment(Long userId, Long videoId, String visitorId, VideoCommentCreateRequest request) {
        ensureVideoInteractable(videoId);

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "评论内容不能为空");
        }

        Long commentId = IdWorker.getId();
        jdbcTemplate.update(
                "INSERT INTO video_comment (id, video_id, user_id, content, status, created_at, updated_at) VALUES (?, ?, ?, ?, 'normal', NOW(), NOW())",
                commentId,
                videoId,
                userId,
                content
        );

        List<VideoCommentItemResponse> rows = jdbcTemplate.query(
                "SELECT c.id, c.user_id, u.username, c.content, c.created_at " +
                        "FROM video_comment c JOIN app_user u ON u.id = c.user_id WHERE c.id = ? LIMIT 1",
                (rs, rowNum) -> {
                    VideoCommentItemResponse item = new VideoCommentItemResponse();
                    item.setId(rs.getLong("id"));
                    item.setUserId(rs.getLong("user_id"));
                    item.setUsername(rs.getString("username"));
                    item.setContent(rs.getString("content"));
                    item.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
                    return item;
                },
                commentId
        );

        eventService.collectServerEvent(
                visitorId,
                videoId,
                "comment",
                INTERACTION_PAGE_PATH,
                "{\"commentId\":\"" + commentId + "\"}"
        );

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "评论创建失败");
        }
        return rows.get(0);
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteComment(Long userId, Long videoId, Long commentId, String visitorId) {
        List<CommentOwnerRow> rows = jdbcTemplate.query(
                "SELECT id, user_id FROM video_comment WHERE id = ? AND video_id = ? AND status = 'normal' LIMIT 1",
                (rs, rowNum) -> new CommentOwnerRow(rs.getLong("id"), rs.getLong("user_id")),
                commentId,
                videoId
        );

        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评论不存在");
        }

        CommentOwnerRow row = rows.get(0);
        if (!row.userId.equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅支持删除自己的评论");
        }

        jdbcTemplate.update("DELETE FROM video_comment WHERE id = ?", row.commentId);

        eventService.collectServerEvent(
            visitorId,
            videoId,
            "comment_delete",
            INTERACTION_PAGE_PATH,
            "{\"commentId\":\"" + commentId + "\"}"
        );
        return "ok";
    }

    private void ensureVideoInteractable(Long videoId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM video WHERE id = ? AND status IN ('ready', 'published')",
                Integer.class,
                videoId
        );
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在或不可见");
        }
    }

    private static class CommentOwnerRow {
        private final Long commentId;
        private final Long userId;

        private CommentOwnerRow(Long commentId, Long userId) {
            this.commentId = commentId;
            this.userId = userId;
        }
    }
}
