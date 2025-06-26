package net.mrcappy.corelib.protocol.reflect;

import java.lang.reflect.Field;

/**
 * Wrapper for field access with better error handling.
 * 
 * Because raw reflection is ugly and throws checked
 * exceptions everywhere. This makes it cleaner.
 */
public class FieldAccessor<T> {
    
    private final Field field;
    
    public FieldAccessor(Field field) {
        this.field = field;
        field.setAccessible(true);
    }
    
    /**
     * Get field value from target.
     */
    @SuppressWarnings("unchecked")
    public T get(Object target) {
        try {
            return (T) field.get(target);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to read field " + field.getName() + " from " + 
                (target != null ? target.getClass().getName() : "null"), e
            );
        }
    }
    
    /**
     * Set field value on target.
     */
    public void set(Object target, T value) {
        try {
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to write field " + field.getName() + " on " + 
                (target != null ? target.getClass().getName() : "null"), e
            );
        }
    }
    
    /**
     * Get the underlying field.
     */
    public Field getField() {
        return field;
    }
    
    /**
     * Get field name.
     */
    public String getName() {
        return field.getName();
    }
    
    /**
     * Get field type.
     */
    public Class<?> getType() {
        return field.getType();
    }
}