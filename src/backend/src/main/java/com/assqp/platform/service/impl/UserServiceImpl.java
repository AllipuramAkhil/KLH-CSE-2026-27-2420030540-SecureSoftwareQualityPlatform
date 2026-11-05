package com.assqp.platform.service.impl;

import com.assqp.platform.dto.user.RoleUpdateRequest;
import com.assqp.platform.dto.user.UserResponse;
import com.assqp.platform.dto.user.UserUpdateRequest;
import com.assqp.platform.entity.User;
import com.assqp.platform.exception.BadRequestException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.NotificationRepository;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.repository.UserRepository;
import com.assqp.platform.security.CurrentUserProvider;
import com.assqp.platform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationRepository notificationRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public UserResponse updateSelf(UserUpdateRequest request) {
        User user = userRepository.findById(currentUserProvider.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateById(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateRole(Long id, RoleUpdateRequest request) {
        // Security fix: role escalation must never be reachable from public
        // registration (see AuthServiceImpl.register). This admin-only,
        // @PreAuthorize-guarded endpoint is the single legitimate path by
        // which a user's role can change after account creation.
        if (id.equals(currentUserProvider.getCurrentUserId())) {
            throw new BadRequestException("Admins cannot change their own role. Ask another admin to do it.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setRole(request.getRole());
        user = userRepository.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Phase 12 audit fix: Project.owner is a nullable=false FK with no
        // cascade configured - deleting a user who still owns projects would
        // otherwise fail with a raw FK-constraint SQL exception. Reassign or
        // delete their projects first (deliberately NOT auto-cascading a
        // silent deletion of someone's entire project portfolio here).
        if (projectRepository.existsByOwnerId(id)) {
            throw new BadRequestException(
                    "Cannot delete user " + id + " - they still own one or more projects. " +
                            "Reassign or delete those projects first.");
        }

        notificationRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
