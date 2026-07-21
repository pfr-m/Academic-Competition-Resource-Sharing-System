package com.competition.hub.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class DomainModels {
    private DomainModels() {
    }

    public enum Role {
        STUDENT, MENTOR, ADMIN, SUPER_ADMIN
    }

    public enum PrivacyLevel {
        HIDDEN, TEAM_ONLY, PUBLIC
    }

    public enum ReviewStatus {
        PENDING, APPROVED, REJECTED
    }

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    public enum ApplicationStatus {
        PENDING, APPROVED, REJECTED
    }

    public record PrivacySettings(PrivacyLevel realName, PrivacyLevel wechat, PrivacyLevel phone) {
        public PrivacySettings with(PrivacyLevel realName, PrivacyLevel wechat, PrivacyLevel phone) {
            return new PrivacySettings(
                    realName == null ? this.realName : realName,
                    wechat == null ? this.wechat : wechat,
                    phone == null ? this.phone : phone
            );
        }
    }

    public record User(
            long id,
            String studentNo,
            String username,
            String passwordHash,
            String nickname,
            String realName,
            Role role,
            List<String> badges,
            String phone,
            String wechat,
            List<String> skills,
            PrivacySettings privacy
    ) {
        public User withPrivacy(PrivacySettings nextPrivacy) {
            return new User(id, studentNo, username, passwordHash, nickname, realName, role, badges, phone, wechat, skills, nextPrivacy);
        }
    }

    public record SafeUser(
            long id,
            String studentNo,
            String username,
            String nickname,
            String realName,
            Role role,
            List<String> badges,
            String phone,
            String wechat,
            List<String> skills,
            PrivacySettings privacy
    ) {
    }

    public record ResourceItem(
            long id,
            String title,
            String contest,
            String type,
            String summary,
            long uploaderId,
            String fileKey,
            int downloadCount,
            ReviewStatus status,
            Instant createdAt
    ) {
        public ResourceItem withDownloadCount(int nextCount) {
            return new ResourceItem(id, title, contest, type, summary, uploaderId, fileKey, nextCount, status, createdAt);
        }

        public ResourceItem withStatus(ReviewStatus nextStatus) {
            return new ResourceItem(id, title, contest, type, summary, uploaderId, fileKey, downloadCount, nextStatus, createdAt);
        }
    }

    public record BlogPost(
            long id,
            String title,
            String content,
            long authorId,
            boolean anonymous,
            Visibility visibility,
            boolean commentsOpen,
            ReviewStatus status,
            Instant createdAt
    ) {
        public BlogPost withStatus(ReviewStatus nextStatus) {
            return new BlogPost(id, title, content, authorId, anonymous, visibility, commentsOpen, nextStatus, createdAt);
        }
    }

    public record BlogComment(
            long id,
            long blogId,
            Long parentId,
            long authorId,
            String content,
            boolean anonymous,
            Instant createdAt
    ) {
    }

    public record Salon(
            long id,
            String title,
            String description,
            long speakerId,
            String startsAt,
            String place,
            int capacity,
            int booked,
            ReviewStatus status
    ) {
        public Salon withBooked(int nextBooked) {
            return new Salon(id, title, description, speakerId, startsAt, place, capacity, nextBooked, status);
        }

        public Salon withStatus(ReviewStatus nextStatus) {
            return new Salon(id, title, description, speakerId, startsAt, place, capacity, booked, nextStatus);
        }
    }

    public record SalonBooking(long id, long salonId, long userId, String certificateCode, Instant createdAt) {
    }

    public record TeamApplication(long id, long teamId, long applicantId, ApplicationStatus status, Instant createdAt) {
        public TeamApplication withStatus(ApplicationStatus nextStatus) {
            return new TeamApplication(id, teamId, applicantId, nextStatus, createdAt);
        }
    }

    public record TeamCard(
            long id,
            String title,
            String contest,
            String need,
            long leaderId,
            List<String> skills,
            List<Long> contactUnlockedUserIds,
            List<TeamApplication> applications,
            ReviewStatus status,
            Instant createdAt
    ) {
        public TeamCard withStatus(ReviewStatus nextStatus) {
            return new TeamCard(id, title, contest, need, leaderId, skills, contactUnlockedUserIds, applications, nextStatus, createdAt);
        }

        public TeamCard copy() {
            return new TeamCard(id, title, contest, need, leaderId, new ArrayList<>(skills), new ArrayList<>(contactUnlockedUserIds), new ArrayList<>(applications), status, createdAt);
        }
    }

    public record ReviewItem(long id, String targetType, long targetId, String title, long ownerId, ReviewStatus status, Instant createdAt) {
        public ReviewItem withStatus(ReviewStatus nextStatus) {
            return new ReviewItem(id, targetType, targetId, title, ownerId, nextStatus, createdAt);
        }
    }
}
