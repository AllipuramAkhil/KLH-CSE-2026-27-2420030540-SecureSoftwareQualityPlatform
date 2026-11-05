package com.assqp.platform.controller;

import com.assqp.platform.service.SourceCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Source Code", description = "Source code upload and management")
public class SourceCodeController {

    private final SourceCodeService sourceCodeService;

    @PostMapping(value = "/projects/{projectId}/source/upload", consumes = "multipart/form-data")
    @Operation(summary = "Upload a source code file for analysis")
    public ResponseEntity<Map<String, Object>> upload(@PathVariable Long projectId,
                                                        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sourceCodeService.upload(projectId, file));
    }

    @GetMapping("/projects/{projectId}/source")
    @Operation(summary = "List all uploaded source files for a project")
    public ResponseEntity<List<Map<String, Object>>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(sourceCodeService.listByProject(projectId));
    }

    @GetMapping("/source/{id}")
    @Operation(summary = "Get metadata for one uploaded source file")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sourceCodeService.getById(id));
    }

    @DeleteMapping("/source/{id}")
    @Operation(summary = "Delete an uploaded source file")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sourceCodeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
