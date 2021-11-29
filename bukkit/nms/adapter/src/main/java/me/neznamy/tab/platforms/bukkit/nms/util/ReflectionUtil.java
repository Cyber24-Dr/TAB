package me.neznamy.tab.platforms.bukkit.nms.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectionUtil {

    public static Field getFieldByPositionAndType(final Class<?> targetClass, final int index, final Class<?> type) {
        int i = 0;
        for (final Field field : targetClass.getDeclaredFields()) {
            if (type.isAssignableFrom(field.getType()) && i++ == index) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalStateException("Could not find field in target " + targetClass + " at index " + index + " with type " + type);
    }

    public static Method getMethodByType(final Class<?> targetClass, final Class<?> type) {
        for (final Method method : targetClass.getDeclaredMethods()) {
            if (type.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new IllegalStateException("Could not find method in target " + targetClass + " with type " + type);
    }

    private ReflectionUtil() {
    }
}
