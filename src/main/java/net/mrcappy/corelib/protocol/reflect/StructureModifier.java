package net.mrcappy.corelib.protocol.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Structure modifier for reading/writing packet fields.
 * 
 * This is like reflection but with extra steps and more pain.
 * We scan classes for fields, cache the shit out of everything,
 * and pray to the Minecraft gods that Mojang doesn't rename
 * everything in the next update.
 * 
 * Why indexed access instead of named fields? Because field
 * names are obfuscated to hell and back. "a", "b", "c" - 
 * real descriptive, Mojang. Thanks for that.
 * 
 * Performance is about as good as you'd expect from something
 * that uses reflection for literally everything. But hey,
 * at least it's cached! *cries in milliseconds*
 */
public class StructureModifier<T> {
    
    private final Class<?> targetClass;
    private final Class<T> fieldType;
    private final List<FieldAccessor<T>> accessors;
    private final Object target;
    
    // Cache for field lists
    private static final Map<String, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();
    
    public StructureModifier(Class<?> targetClass, Class<T> fieldType) {
        this(targetClass, fieldType, null);
    }
    
    public StructureModifier(Class<?> targetClass, Class<T> fieldType, Object target) {
        this.targetClass = targetClass;
        this.fieldType = fieldType;
        this.target = target;
        this.accessors = new ArrayList<>();
        
        // Find and cache fields
        initializeFields();
    }
    
    /**
     * Initialize field accessors.
     * Scans the class hierarchy for matching fields.
     */
    private void initializeFields() {
        String cacheKey = targetClass.getName() + "#" + fieldType.getName();
        List<Field> fields = FIELD_CACHE.computeIfAbsent(cacheKey, k -> {
            List<Field> result = new ArrayList<>();
            
            // Scan class hierarchy
            Class<?> current = targetClass;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    // Skip static fields
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    
                    // Check if field type matches
                    if (isCompatible(field.getType(), fieldType)) {
                        field.setAccessible(true);
                        result.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            
            return result;
        });
        
        // Create accessors
        for (Field field : fields) {
            accessors.add(new FieldAccessor<>(field));
        }
    }    
    /**
     * Check if a field type is compatible with our target type.
     * Handles primitive/wrapper conversion.
     */
    private boolean isCompatible(Class<?> fieldType, Class<?> targetType) {
        if (targetType.isAssignableFrom(fieldType)) {
            return true;
        }
        
        // Handle primitive/wrapper pairs
        if (targetType == Object.class) {
            return true; // Object matches everything
        }
        
        // Check primitive conversions
        if (isPrimitiveWrapper(targetType) && fieldType.isPrimitive()) {
            return getPrimitive(targetType) == fieldType;
        }
        
        if (isPrimitiveWrapper(fieldType) && targetType.isPrimitive()) {
            return getPrimitive(fieldType) == targetType;
        }
        
        return false;
    }
    
    private boolean isPrimitiveWrapper(Class<?> type) {
        return type == Boolean.class || type == Byte.class ||
               type == Character.class || type == Short.class ||
               type == Integer.class || type == Long.class ||
               type == Float.class || type == Double.class;
    }
    
    private Class<?> getPrimitive(Class<?> wrapper) {
        if (wrapper == Boolean.class) return boolean.class;
        if (wrapper == Byte.class) return byte.class;
        if (wrapper == Character.class) return char.class;
        if (wrapper == Short.class) return short.class;
        if (wrapper == Integer.class) return int.class;
        if (wrapper == Long.class) return long.class;
        if (wrapper == Float.class) return float.class;
        if (wrapper == Double.class) return double.class;
        return wrapper;
    }
    
    /**
     * Read a field value by index.
     */
    @SuppressWarnings("unchecked")
    public T read(int index) {
        if (index < 0 || index >= accessors.size()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " is out of bounds for " + accessors.size() + " fields"
            );
        }
        
        return accessors.get(index).get(target);
    }
    
    /**
     * Write a field value by index.
     */
    public void write(int index, T value) {
        if (index < 0 || index >= accessors.size()) {
            throw new IndexOutOfBoundsException(
                "Index " + index + " is out of bounds for " + accessors.size() + " fields"
            );
        }
        
        accessors.get(index).set(target, value);
    }    
    /**
     * Get the number of fields.
     */
    public int size() {
        return accessors.size();
    }
    
    /**
     * Create a new modifier with a different field type.
     */
    @SuppressWarnings("unchecked")
    public <V> StructureModifier<V> withType(Class<V> newType) {
        return new StructureModifier<>(targetClass, newType, target);
    }
    
    /**
     * Create a new modifier with a specific target instance.
     */
    public StructureModifier<T> withTarget(Object newTarget) {
        return new StructureModifier<>(targetClass, fieldType, newTarget);
    }
    
    /**
     * Read all values into a list.
     */
    public List<T> getValues() {
        List<T> values = new ArrayList<>();
        for (int i = 0; i < size(); i++) {
            values.add(read(i));
        }
        return values;
    }
    
    /**
     * Get field names for debugging.
     */
    public List<String> getFieldNames() {
        List<String> names = new ArrayList<>();
        for (FieldAccessor<T> accessor : accessors) {
            names.add(accessor.getField().getName());
        }
        return names;
    }
}