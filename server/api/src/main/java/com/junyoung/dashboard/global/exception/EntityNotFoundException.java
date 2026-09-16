package com.junyoung.dashboard.global.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public static EntityNotFoundException of(Class<?> entityType, Long id) {
        String name = entityType.getSimpleName()
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase();
        return new EntityNotFoundException(name + " " + id + " not found");
    }
}
