package com.mercedes.workflowrh.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Service
public class FileUploadService {

    // Maximum file size (e.g., 10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes

    // Supported file types
    private static final String[] ALLOWED_FILE_TYPES = {
            "application/pdf",
            "image/jpeg",
            "image/png"
    };

    public byte[] processFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null; // File is optional
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier dépasse la taille maximale de 10 Mo.");
        }

        // Validate file type
        String contentType = file.getContentType();
        boolean isAllowedType = false;
        for (String allowedType : ALLOWED_FILE_TYPES) {
            if (allowedType.equalsIgnoreCase(contentType)) {
                isAllowedType = true;
                break;
            }
        }
        if (!isAllowedType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de fichier non supporté. Seuls PDF, JPEG et PNG sont autorisés.");
        }

        // Convert file to byte[]
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du traitement du fichier.", e);
        }
    }
}