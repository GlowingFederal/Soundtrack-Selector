package com.glowingfederal.soundtrackselector.domain;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Resolve once by exact native field type, independent of MCP/SRG/official names. */
public final class FieldAccess {
    private FieldAccess() {}
    public static Field unique(Class<?> owner, Class<?> type) {
        Field found = null;
        for (Field field : owner.getDeclaredFields()) {
            if (field.getType() != type || Modifier.isStatic(field.getModifiers())) continue;
            if (found != null) throw new IllegalStateException("Ambiguous " + type.getName() + " field in " + owner.getName());
            found = field;
        }
        if (found == null) throw new IllegalStateException("Missing " + type.getName() + " field in " + owner.getName());
        found.setAccessible(true);
        return found;
    }
    public static Field assignable(Class<?> owner, Class<?> type) {
        Field found = null;
        for (Field field : owner.getDeclaredFields()) {
            if (!type.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) continue;
            if (found != null) throw new IllegalStateException("Ambiguous " + type.getName() + " field in " + owner.getName());
            found = field;
        }
        if (found == null) throw new IllegalStateException("Missing " + type.getName() + " field in " + owner.getName());
        found.setAccessible(true);
        return found;
    }
    public static Field mapValue(Class<?> owner, Class<?> valueType) {
        for (Field field : owner.getDeclaredFields()) {
            if (!(field.getGenericType() instanceof java.lang.reflect.ParameterizedType)) continue;
            java.lang.reflect.ParameterizedType type = (java.lang.reflect.ParameterizedType) field.getGenericType();
            java.lang.reflect.Type[] args = type.getActualTypeArguments();
            if (java.util.Map.class.isAssignableFrom(field.getType()) && args.length == 2 && args[1] == valueType) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalStateException("Missing audio channel map in " + owner.getName());
    }
    public static Object get(Field field, Object owner) {
        try { return field.get(owner); } catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }
    public static void set(Field field, Object owner, Object value) {
        try { field.set(owner, value); } catch (IllegalAccessException failure) { throw new IllegalStateException(failure); }
    }
}
