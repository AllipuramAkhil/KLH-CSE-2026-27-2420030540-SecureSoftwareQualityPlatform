package com.assqp.platform.util;

import com.assqp.platform.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
public class FileValidationUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "java", "js", "jsx", "ts", "tsx", "py", "cpp", "c", "html", "css", "sql"
    );

    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024 * 1024; // 20MB

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BadRequestException("File must have a valid extension");
        }

        // Reject path traversal / malicious filenames
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new BadRequestException("Invalid file name");
        }

        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("File type ." + extension + " is not supported");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds maximum allowed size of 20MB");
        }
    }

    public String extractExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
