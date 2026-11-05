package com.assqp.platform.repository;

import com.assqp.platform.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    // Phase 12 audit fix: Notification.user is a nullable=false FK. Unlike
    // Project (which represents real work product, not safe to
    // auto-cascade), a user's own notifications are safe to delete along
    // with their account - nothing else depends on them.
    void deleteByUserId(Long userId);
}
