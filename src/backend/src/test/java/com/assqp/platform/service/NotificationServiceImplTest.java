package com.assqp.platform.service;

import com.assqp.platform.entity.Notification;
import com.assqp.platform.entity.User;
import com.assqp.platform.enums.NotificationType;
import com.assqp.platform.enums.Role;
import com.assqp.platform.exception.ForbiddenException;
import com.assqp.platform.repository.NotificationRepository;
import com.assqp.platform.repository.UserRepository;
import com.assqp.platform.security.CurrentUserProvider;
import com.assqp.platform.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User owner() {
        return User.builder().id(1L).fullName("Owner").email("o@test.com").password("x").role(Role.DEVELOPER).build();
    }

    @Test
    void getForCurrentUser_paginated_returnsPage() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = Notification.builder().id(1L).user(owner()).type(NotificationType.SYSTEM).title("Hi").build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(new PageImpl<>(List.of(n)));

        var page = notificationService.getForCurrentUser(pageable);

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void getUnreadForCurrentUser_returnsOnlyUnread() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Notification unread = Notification.builder().id(1L).user(owner()).type(NotificationType.SYSTEM)
                .title("Unread").isRead(false).build();
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(unread));

        List<Notification> result = notificationService.getUnreadForCurrentUser();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void markRead_ownedNotification_marksAsRead() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Notification notification = Notification.builder().id(1L).user(owner()).type(NotificationType.SYSTEM)
                .title("Test").isRead(false).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.markRead(1L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markRead_notOwnedNotification_throwsForbidden() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        User otherUser = User.builder().id(2L).fullName("Other").email("other@test.com").password("x").role(Role.DEVELOPER).build();
        Notification notification = Notification.builder().id(1L).user(otherUser).type(NotificationType.SYSTEM).title("Test").build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void markAllRead_marksEveryNotificationForUser() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        Notification n1 = Notification.builder().id(1L).user(owner()).type(NotificationType.SYSTEM).title("A").isRead(false).build();
        Notification n2 = Notification.builder().id(2L).user(owner()).type(NotificationType.SYSTEM).title("B").isRead(false).build();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        notificationService.markAllRead();

        assertThat(n1.isRead()).isTrue();
        assertThat(n2.isRead()).isTrue();
        verify(notificationRepository).saveAll(List.of(n1, n2));
    }
}
