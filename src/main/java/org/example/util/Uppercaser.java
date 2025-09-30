package org.example.util;

import java.lang.reflect.Field;
import java.util.Locale;

public final class Uppercaser {
    private Uppercaser() {}

    public static <T> void toUpperCaseStrings(T obj) {
        if (obj == null) return;
        Class<?> cls = obj.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType() == String.class) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(obj);
                        if (v != null) {
                            String s = ((String) v).toUpperCase(Locale.ROOT);
                            f.set(obj, s);
                        }
                    } catch (IllegalAccessException ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
    }
}
