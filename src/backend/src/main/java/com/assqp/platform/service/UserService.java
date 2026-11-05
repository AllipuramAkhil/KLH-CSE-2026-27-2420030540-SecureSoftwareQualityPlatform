package com.assqp.platform.service;

import com.assqp.platform.dto.user.RoleUpdateRequest;
import com.assqp.platform.dto.user.UserResponse;
import com.assqp.platform.dto.user.UserUpdateRequest;

import java.util.List;

public interface UserService {
    UserResponse getById(Long id);
    List<UserResponse> getAll();
    UserResponse updateSelf(UserUpdateRequest request);
    UserResponse updateById(Long id, UserUpdateRequest request);
    UserResponse updateRole(Long id, RoleUpdateRequest request);
    void delete(Long id);
}
