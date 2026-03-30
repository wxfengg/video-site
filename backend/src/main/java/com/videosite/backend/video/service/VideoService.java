package com.videosite.backend.video.service;

import com.videosite.backend.common.api.ErrorCode;
import com.videosite.backend.common.exception.BusinessException;
import com.videosite.backend.video.dto.PageResult;
import com.videosite.backend.video.dto.VideoDetailResponse;
import com.videosite.backend.video.dto.VideoListItemResponse;
import com.videosite.backend.video.dto.VideoPlaySourcesResponse;
import com.videosite.backend.video.dto.VideoUpdateRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VideoService {

    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbcTemplate;

    public VideoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<VideoListItemResponse> listPublicVideos(int page, int pageSize, String keyword) {
        return listVideos(page, pageSize, "published", keyword);
    }

    public PageResult<VideoListItemResponse> listAdminVideos(int page, int pageSize, String status, String keyword) {
        return listVideos(page, pageSize, status, keyword);
    }

    public VideoDetailResponse getVideoDetail(Long videoId, boolean admin) {
        try {
            VideoDetailResponse detail = jdbcTemplate.queryForObject(
                    "SELECT id, title, description, cover_url, duration_sec, status, publish_at, created_at, updated_at FROM video WHERE id = ?",
                    this::mapVideoDetail,
                    videoId
            );

            if (detail == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
            }

            if (!admin && !"published".equals(detail.getStatus()) && !"ready".equals(detail.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在或不可预览");
            }
            return detail;
        } catch (EmptyResultDataAccessException ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
    }

    public VideoPlaySourcesResponse getVideoPlaySources(Long videoId) {
        VideoDetailResponse detail = getVideoDetail(videoId, false);
        if (!"published".equals(detail.getStatus()) && !"ready".equals(detail.getStatus())) {
            throw new BusinessException(ErrorCode.PLAY_SOURCE_NOT_READY, "视频尚未准备好播放源");
        }

        List<SourceItem> sourceItems = jdbcTemplate.query(
                "SELECT source_type, play_url FROM video_play_source WHERE video_id = ?",
                (rs, rowNum) -> new SourceItem(rs.getString("source_type"), rs.getString("play_url")),
                videoId
        );

        if (sourceItems.isEmpty()) {
            throw new BusinessException(ErrorCode.PLAY_SOURCE_NOT_READY, "播放源尚未生成");
        }

        VideoPlaySourcesResponse response = new VideoPlaySourcesResponse();
        response.setVideoId(videoId);
        for (SourceItem item : sourceItems) {
            switch (item.sourceType) {
                case "hls_master":
                    response.setHlsMasterUrl(item.playUrl);
                    break;
                case "mp4_360":
                    response.setMp4360Url(item.playUrl);
                    break;
                case "mp4_720":
                    response.setMp4720Url(item.playUrl);
                    break;
                case "mp4_1080":
                    response.setMp41080Url(item.playUrl);
                    break;
                default:
                    break;
            }
        }

        if (!StringUtils.hasText(response.getHlsMasterUrl())
                && !StringUtils.hasText(response.getMp4360Url())
                && !StringUtils.hasText(response.getMp4720Url())
                && !StringUtils.hasText(response.getMp41080Url())) {
            throw new BusinessException(ErrorCode.PLAY_SOURCE_NOT_READY, "播放源尚未就绪");
        }

        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoDetailResponse updateVideo(Long videoId, VideoUpdateRequest request) {
        VideoDetailResponse current = getVideoDetail(videoId, true);

        String newTitle = StringUtils.hasText(request.getTitle()) ? request.getTitle() : current.getTitle();
        String newDescription = request.getDescription() != null ? request.getDescription() : current.getDescription();
        String newCoverUrl = request.getCoverUrl() != null ? request.getCoverUrl() : current.getCoverUrl();

        jdbcTemplate.update(
                "UPDATE video SET title = ?, description = ?, cover_url = ?, updated_at = NOW() WHERE id = ?",
                newTitle,
                newDescription,
                newCoverUrl,
                videoId
        );

        return getVideoDetail(videoId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoDetailResponse publishVideo(Long videoId) {
        ensureVideoExists(videoId);
        jdbcTemplate.update(
                "UPDATE video SET status = 'published', publish_at = NOW(), updated_at = NOW() WHERE id = ?",
                videoId
        );
        return getVideoDetail(videoId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public VideoDetailResponse unpublishVideo(Long videoId) {
        ensureVideoExists(videoId);
        jdbcTemplate.update(
                "UPDATE video SET status = 'offline', updated_at = NOW() WHERE id = ?",
                videoId
        );
        return getVideoDetail(videoId, true);
    }

    @Transactional(rollbackFor = Exception.class)
    public String deleteVideo(Long videoId) {
        VideoDetailResponse detail = getVideoDetail(videoId, true);
        if (!"offline".equals(detail.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持删除已下线视频");
        }

        jdbcTemplate.update("DELETE FROM recommendation_result WHERE video_id = ?", videoId);
        jdbcTemplate.update("DELETE FROM video_similarity WHERE video_id = ? OR related_video_id = ?", videoId, videoId);
        jdbcTemplate.update("DELETE FROM video_tfidf_profile WHERE video_id = ?", videoId);
        jdbcTemplate.update("DELETE FROM cover_analysis_task WHERE video_id = ?", videoId);
        jdbcTemplate.update("DELETE FROM video_tag WHERE video_id = ?", videoId);

        jdbcTemplate.update("DELETE FROM video_play_source WHERE video_id = ?", videoId);
        jdbcTemplate.update("DELETE FROM video_variant WHERE video_id = ?", videoId);
        jdbcTemplate.update("DELETE FROM video_transcode_task WHERE video_id = ?", videoId);
        jdbcTemplate.update("DELETE FROM video_file WHERE video_id = ?", videoId);

        jdbcTemplate.update("DELETE FROM video WHERE id = ?", videoId);

        return "deleted";
    }

    private PageResult<VideoListItemResponse> listVideos(int page, int pageSize, String status, String keyword) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize, MAX_PAGE_SIZE));
        int offset = (safePage - 1) * safePageSize;

        StringBuilder countSql = new StringBuilder("SELECT COUNT(1) FROM video WHERE 1=1");
        StringBuilder querySql = new StringBuilder("SELECT id, title, cover_url, status, duration_sec, publish_at, created_at FROM video WHERE 1=1");

        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(status)) {
            countSql.append(" AND status = ?");
            querySql.append(" AND status = ?");
            params.add(status);
        }

        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            countSql.append(" AND (title LIKE ? OR description LIKE ?)");
            querySql.append(" AND (title LIKE ? OR description LIKE ?)");
            params.add(like);
            params.add(like);
        }

        querySql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");

        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(safePageSize);
        queryParams.add(offset);

        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, params.toArray());
        List<VideoListItemResponse> records = jdbcTemplate.query(querySql.toString(), this::mapVideoListItem, queryParams.toArray());

        return new PageResult<>(total == null ? 0 : total, safePage, safePageSize, records);
    }

    private void ensureVideoExists(Long videoId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM video WHERE id = ?", Integer.class, videoId);
        if (count == null || count <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "视频不存在");
        }
    }

    private VideoListItemResponse mapVideoListItem(ResultSet rs, int rowNum) throws SQLException {
        VideoListItemResponse item = new VideoListItemResponse();
        item.setId(rs.getLong("id"));
        item.setTitle(rs.getString("title"));
        item.setCoverUrl(rs.getString("cover_url"));
        item.setStatus(rs.getString("status"));
        item.setDurationSec((Integer) rs.getObject("duration_sec"));
        item.setPublishAt(rs.getTimestamp("publish_at") == null ? null : rs.getTimestamp("publish_at").toLocalDateTime());
        item.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        return item;
    }

    private VideoDetailResponse mapVideoDetail(ResultSet rs, int rowNum) throws SQLException {
        VideoDetailResponse detail = new VideoDetailResponse();
        detail.setId(rs.getLong("id"));
        detail.setTitle(rs.getString("title"));
        detail.setDescription(rs.getString("description"));
        detail.setCoverUrl(rs.getString("cover_url"));
        detail.setDurationSec((Integer) rs.getObject("duration_sec"));
        detail.setStatus(rs.getString("status"));
        detail.setPublishAt(rs.getTimestamp("publish_at") == null ? null : rs.getTimestamp("publish_at").toLocalDateTime());
        detail.setCreatedAt(rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime());
        detail.setUpdatedAt(rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime());
        return detail;
    }

    private static class SourceItem {
        private final String sourceType;
        private final String playUrl;

        private SourceItem(String sourceType, String playUrl) {
            this.sourceType = sourceType;
            this.playUrl = playUrl;
        }
    }
}
