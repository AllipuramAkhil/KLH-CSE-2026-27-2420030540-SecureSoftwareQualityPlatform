package com.assqp.platform.service;

import com.assqp.platform.entity.User;
import com.assqp.platform.enums.Role;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.NotificationRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.UserRepository;
import com.assqp.platform.security.CurrentUserProvider;
import com.assqp.platform.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// Phase 12 audit fix coverage: UserServiceImpl.delete() previously called
// userRepository.delete() with zero cleanup. Project.owner is a
// nullable=false FK with no cascade, so deleting a user who still owns
// projects would fail with a raw FK-constraint SQL exception - this is
// now blocked with a clear BadRequestException instead. Notification.user
// is also a nullable=false FK, but safe to auto-cascade (a user's own
// notifications aren't shared), so those are cleaned up automatically.
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserServiceImpl userService;

    private User user(Long id) {
        return User.builder().id(id).fullName("Test User").email("user" + id + "@test.com")
                .password("x").role(Role.DEVELOPER).enabled(true).build();
    }

    @Test
    void delete_userOwnsProjects_throwsBadRequestAndDoesNotDeleteAnything() {
        User target = user(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));
        when(projectRepository.existsByOwnerId(5L)).thenReturn(true);

        assertThatThrownBy(() -> userService.delete(5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("still own");

        verify(notificationRepository, never()).deleteByUserId(anyLong());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void delete_userOwnsNoProjects_cleansUpNotificationsThenDeletesUser() {
        User target = user(6L);
        when(userRepository.findById(6L)).thenReturn(Optional.of(target));
        when(projectRepository.existsByOwnerId(6L)).thenReturn(false);

        userService.delete(6L);

        verify(notificationRepository).deleteByUserId(6L);
        verify(userRepository).delete(target);
    }

    @Test
    void delete_userNotFound_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(notificationRepository);
        // existsByOwnerId must not even be checked if the user doesn't exist.
        verify(projectRepository, never()).existsByOwnerId(anyLong());
    }

    @Test
    void getById_found_returnsMappedResponse() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L)));

        var response = userService.getById(7L);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getRole()).isEqualTo("DEVELOPER");
    }
}
