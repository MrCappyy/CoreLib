package net.mrcappy.corelib.version;

import org.bukkit.Bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection utility because we're masochists who enjoy pain.
 * 
 * This caches everything because reflection is slower than
 * a sloth on ketamine trying to solve differential equations.
 * Every Class.forName() call makes baby Jesus cry, so we
 * cache that shit like our lives depend on it.
 * 
 * PRO TIP: If you're getting ClassNotFoundException, you 
 * either fucked up the class name, the version detection
 * is broken, or Mojang decided to refactor everything again
 * because they were bored on a Tuesday.
 * 
 * "Why not use a library?" Because we're too stubborn and
 * proud to admit defeat. Also, dependencies are for quitters.
 */
public class ReflectionUtil {
    // Cache for classes because Class.forName is expensive as hell
    private static final ConcurrentHashMap<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    
    // Cache for methods because getDeclaredMethod makes me want to die
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    
    // Cache for fields because... you get the idea
    private static final ConcurrentHashMap<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    
    // Cache for constructors
    private static final ConcurrentHashMap<String, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    
    /**
     * Get a class by name, with caching.
     * Throws a runtime exception if not found because
     * we're not dealing with checked exceptions in utility code.
     */
    public static Class<?> getClass(String className) {
        return CLASS_CACHE.computeIfAbsent(className, name -> {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                    "Can't find class: " + name + ". Did you check the version? " +
                    "Current: " + MinecraftVersion.getCurrent(), e
                );
            }
        });
    }    
    /**
     * Get an NMS class. Handles the version-specific package bullshit.
     * Example: getNMSClass("world.entity.Entity") 
     * 
     * Note: This assumes Mojang mappings structure (1.17+)
     */
    public static Class<?> getNMSClass(String className) {
        String fullName = "net.minecraft." + className;
        return getClass(fullName);
    }
    
    /**
     * Get a CraftBukkit class. These fuckers are in different packages
     * depending on the version, but at least they're predictable.
     */
    public static Class<?> getCraftBukkitClass(String className) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        String fullName = "org.bukkit.craftbukkit." + version + "." + className;
        return getClass(fullName);
    }
    
    /**
     * Get a method from a class. Caches the result because
     * method lookup is slower than Congress passing bills.
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        String key = clazz.getName() + "#" + methodName + "#" + Arrays.toString(paramTypes);
        
        return METHOD_CACHE.computeIfAbsent(key, k -> {
            try {
                Method method = clazz.getDeclaredMethod(methodName, paramTypes);
                method.setAccessible(true); // Fuck your private methods
                return method;
            } catch (NoSuchMethodException e) {
                // Try declared methods from superclasses
                try {
                    Method method = clazz.getMethod(methodName, paramTypes);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException e2) {
                    throw new RuntimeException(
                        "Can't find method: " + methodName + " in " + clazz.getName() + 
                        " with params " + Arrays.toString(paramTypes), e2
                    );
                }
            }
        });
    }    
    /**
     * Get a field from a class. Makes it accessible because
     * private fields are just a suggestion.
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "#" + fieldName;
        
        return FIELD_CACHE.computeIfAbsent(key, k -> {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true); // Privacy is dead
                return field;
            } catch (NoSuchFieldException e) {
                // Check superclasses because inheritance is a thing
                Class<?> superClass = clazz.getSuperclass();
                if (superClass != null && superClass != Object.class) {
                    return getField(superClass, fieldName);
                }
                throw new RuntimeException(
                    "Can't find field: " + fieldName + " in " + clazz.getName(), e
                );
            }
        });
    }
    
    /**
     * Get a constructor. Because sometimes you need to
     * instantiate shit that wasn't meant to be instantiated.
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... paramTypes) {
        String key = clazz.getName() + "#<init>#" + Arrays.toString(paramTypes);
        
        return CONSTRUCTOR_CACHE.computeIfAbsent(key, k -> {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor(paramTypes);
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(
                    "Can't find constructor for " + clazz.getName() + 
                    " with params " + Arrays.toString(paramTypes), e
                );
            }
        });
    }
    
    /**
     * Invoke a method. Wraps the exceptions because
     * checked exceptions in reflection are cancer.
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Method method, Object instance, Object... args) {
        try {
            return (T) method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to invoke method: " + method.getName() + 
                " on " + (instance != null ? instance.getClass().getName() : "null"), e
            );
        }
    }    
    /**
     * Get field value. Because getters are for people
     * who plan ahead.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValue(Field field, Object instance) {
        try {
            return (T) field.get(instance);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to get field value: " + field.getName() + 
                " from " + (instance != null ? instance.getClass().getName() : "null"), e
            );
        }
    }
    
    /**
     * Set field value. Because setters are overrated.
     */
    public static void setFieldValue(Field field, Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to set field value: " + field.getName() + 
                " on " + (instance != null ? instance.getClass().getName() : "null"), e
            );
        }
    }
    
    /**
     * Create a new instance using constructor.
     * For when 'new' isn't good enough.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Constructor<?> constructor, Object... args) {
        try {
            return (T) constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to create instance of " + constructor.getDeclaringClass().getName(), e
            );
        }
    }
}