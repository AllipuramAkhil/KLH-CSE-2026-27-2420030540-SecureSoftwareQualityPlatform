package com.assqp.platform.service;

import com.assqp.platform.entity.Notification;
import com.assqp.platform.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    void notify(Long userId, NotificationType type, String title, String message);
    List<Notification> getForCurrentUser();
    Page<Notification> getForCurrentUser(Pageable pageable);
    List<Notification> getUnreadForCurrentUser();
    void markRead(Long id);
    void markAllRead();
    void delete(Long id);
}
