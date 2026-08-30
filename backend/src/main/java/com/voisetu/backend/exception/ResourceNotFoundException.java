package com.voisetu.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested resource (generation, project, voice) does not exist or is not owned by the user. */
public class ResourceNotFoundException extends AppException {

    public ResourceNotFoundException(String resourceName, Object id) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                resourceName + " not found: " + id);
    }
}
