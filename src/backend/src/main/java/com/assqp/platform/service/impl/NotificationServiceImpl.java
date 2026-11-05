package com.assqp.platform.service.impl;

import com.assqp.platform.entity.Notification;
import com.assqp.platform.entity.User;
import com.assqp.platform.enums.NotificationType;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.NotificationRepository;
import com.assqp.platform.repository.UserRepository;
import com.assqp.platform.exception.ForbiddenException;
import com.assqp.platform.security.CurrentUserProvider;
import com.assqp.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional
    public void notify(Long userId, NotificationType type, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getForCurrentUser() {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserProvider.getCurrentUserId());
    }

    @Override
    public Page<Notification> getForCurrentUser(Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserProvider.getCurrentUserId(), pageable);
    }

    @Override
    public List<Notification> getUnreadForCurrentUser() {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Notification notification = getOwnedOrThrow(id);
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        List<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(currentUserProvider.getCurrentUserId());
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Notification notification = getOwnedOrThrow(id);
        notificationRepository.delete(notification);
    }

    private Notification getOwnedOrThrow(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        if (!notification.getUser().getId().equals(currentUserProvider.getCurrentUserId())) {
            throw new ForbiddenException("You do not have access to this notification");
        }
        return notification;
    }
}
