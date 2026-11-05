package com.assqp.platform.service.impl;

import com.assqp.platform.entity.Project;
import com.assqp.platform.entity.SourceCodeFile;
import com.assqp.platform.exception.FileStorageException;
import com.assqp.platform.exception.ResourceNotFoundException;
import com.assqp.platform.repository.ProjectRepository;
import com.assqp.platform.security.ProjectAccessGuard;
import com.assqp.platform.repository.SourceCodeFileRepository;
import com.assqp.platform.service.SourceCodeService;
import com.assqp.platform.util.FileValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles source-code upload and metadata storage. Files are stored on the
 * local filesystem under a project-scoped directory today; the storagePath
 * field is designed so a future GitHub/GitLab repository connector can
 * populate it directly (e.g. a repo-relative path) without schema changes.
 */
@Service
@RequiredArgsConstructor
public class SourceCodeServiceImpl implements SourceCodeService {

    private final SourceCodeFileRepository sourceCodeFileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessGuard projectAccessGuard;
    private final FileValidationUtil fileValidationUtil;

    @Value("${storage.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    @Transactional
    public Map<String, Object> upload(Long projectId, MultipartFile file) {
        Project project = projectAccessGuard.getAccessibleProject(projectId);

        fileValidationUtil.validate(file);

        String extension = fileValidationUtil.extractExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;

        try {
            Path projectDir = Paths.get(uploadDir, "project-" + projectId);
            Files.createDirectories(projectDir);
            Path targetPath = projectDir.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath);

            int linesOfCode = countLines(targetPath);

            SourceCodeFile entity = SourceCodeFile.builder()
                    .project(project)
                    .fileName(file.getOriginalFilename())
                    .fileType(extension)
                    .fileSize(file.getSize())
                    .storagePath(targetPath.toString())
                    .linesOfCode(linesOfCode)
                    .build();

            entity = sourceCodeFileRepository.save(entity);
            return toMap(entity);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store uploaded file", ex);
        }
    }

    @Override
    public List<Map<String, Object>> listByProject(Long projectId) {
        projectAccessGuard.getAccessibleProject(projectId); // ownership check
        return sourceCodeFileRepository.findByProjectId(projectId).stream().map(this::toMap).toList();
    }

    @Override
    public Map<String, Object> getById(Long fileId) {
        SourceCodeFile file = sourceCodeFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Source file not found with id: " + fileId));
        projectAccessGuard.checkAccess(file.getProject()); // ownership check
        return toMap(file);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        SourceCodeFile file = sourceCodeFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Source file not found with id: " + fileId));
        projectAccessGuard.checkAccess(file.getProject()); // ownership check
        try {
            Files.deleteIfExists(Paths.get(file.getStoragePath()));
        } catch (IOException ignored) {
            // Metadata removal proceeds even if physical file is already gone
        }
        sourceCodeFileRepository.delete(file);
    }

    private int countLines(Path path) {
        try {
            return (int) Files.lines(path).count();
        } catch (IOException e) {
            return 0;
        }
    }

    private Map<String, Object> toMap(SourceCodeFile file) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", file.getId());
        map.put("projectId", file.getProject().getId());
        map.put("fileName", file.getFileName());
        map.put("fileType", file.getFileType());
        map.put("fileSize", file.getFileSize());
        map.put("linesOfCode", file.getLinesOfCode());
        map.put("uploadedAt", file.getUploadedAt());
        return map;
    }
}
