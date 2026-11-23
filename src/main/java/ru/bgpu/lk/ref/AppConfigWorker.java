package ru.bgpu.lk.ref;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppConfigWorker {

    private static final Logger LOG = Logger.getLogger(AppConfigWorker.class.getName());

    private AppConfigWorker() {
    }

    public static void init(String packageName, String propertyFile) {
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(propertyFile)) {
            properties.load(in);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Не удалось прочитать " + propertyFile, e);
            return;
        }

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(packageName))
                .setScanners(Scanners.FieldsAnnotated));

        for (Field field : reflections.getFieldsAnnotatedWith(AppConfig.class)) {
            if (!Modifier.isStatic(field.getModifiers())) {
                LOG.warning("Поле " + field.getName() + " не static, пропущено");
                continue;
            }
            String text = properties.getProperty(field.getName(),
                    field.getAnnotation(AppConfig.class).value());
            try {
                field.setAccessible(true);
                field.set(null, convert(text, field));
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Ошибка в поле " + field.getName()
                        + " со значением '" + text + "'", e);
            }
        }
    }

    private static Object convert(String text, Field field) {
        Class<?> type = field.getType();
        if (type == String.class) {
            return text;
        }
        if (type == int.class || type == Integer.class) {
            return Integer.valueOf(text);
        }
        if (type == long.class || type == Long.class) {
            return Long.valueOf(text);
        }
        if (type == short.class || type == Short.class) {
            return Short.valueOf(text);
        }
        if (type == byte.class || type == Byte.class) {
            return Byte.valueOf(text);
        }
        if (type == float.class || type == Float.class) {
            return Float.valueOf(text);
        }
        if (type == double.class || type == Double.class) {
            return Double.valueOf(text);
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.valueOf(text);
        }
        if (type.isArray()) {
            return array(text, type.getComponentType());
        }
        if (List.class.isAssignableFrom(type)) {
            return list(text, field);
        }
        throw new IllegalArgumentException("Тип не поддерживается: " + type.getName());
    }

    private static Object array(String text, Class<?> component) {
        String[] parts = parts(text);
        Object result = Array.newInstance(component, parts.length);
        for (int i = 0; i < parts.length; i++) {
            Array.set(result, i, scalar(parts[i], component));
        }
        return result;
    }

    private static List<?> list(String text, Field field) {
        Type generic = field.getGenericType();
        if (!(generic instanceof ParameterizedType)) {
            return List.of(parts(text));
        }
        ParameterizedType parameterized = (ParameterizedType) generic;
        Type argument = parameterized.getActualTypeArguments()[0];
        if (!(argument instanceof Class<?>)) {
            throw new IllegalArgumentException("Не удалось определить тип списка");
        }
        Class<?> elementType = (Class<?>) argument;
        List<Object> result = new ArrayList<>();
        for (String part : parts(text)) {
            result.add(scalar(part, elementType));
        }
        return result;
    }

    private static Object scalar(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.valueOf(value);
        if (type == long.class || type == Long.class) return Long.valueOf(value);
        if (type == short.class || type == Short.class) return Short.valueOf(value);
        if (type == byte.class || type == Byte.class) return Byte.valueOf(value);
        if (type == float.class || type == Float.class) return Float.valueOf(value);
        if (type == double.class || type == Double.class) return Double.valueOf(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.valueOf(value);
        throw new IllegalArgumentException("Тип элемента не поддерживается: " + type.getName());
    }

    private static String[] parts(String text) {
        String[] parts = text.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}
