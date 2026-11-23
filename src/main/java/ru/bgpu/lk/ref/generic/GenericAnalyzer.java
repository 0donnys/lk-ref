package ru.bgpu.lk.ref.generic;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс GenericAnalyzer демонстрирует работу Reflection API с обобщенными типами:
 *
 * <ol>
 *   <li>анализ параметризованных полей и их типовых аргументов;</li>
 *   <li>определение фактических типовых аргументов по цепочке наследования;</li>
 *   <li>создание экземпляров обобщенных коллекций во время выполнения;</li>
 *   <li>вызов обобщенных методов с проверкой ограничений типовых параметров.</li>
 * </ol>
 *
 * Ключевая идея: обобщения в Java существуют только на этапе компиляции
 * (стирание типов / type erasure). Во время выполнения информация о типовых
 * аргументах сохраняется лишь в метаданных класса (поля, сигнатуры методов,
 * цепочка наследования), и добраться до нее можно только через Reflection API.
 */
public class GenericAnalyzer {

    private static final Logger LOG = Logger.getLogger(GenericAnalyzer.class.getName());

    /** Реестр известных коллекций: короткое имя → класс. */
    private static final Map<String, Class<?>> COLLECTIONS = new LinkedHashMap<>();

    static {
        COLLECTIONS.put("ArrayList", ArrayList.class);
        COLLECTIONS.put("LinkedList", LinkedList.class);
        COLLECTIONS.put("HashMap", HashMap.class);
        COLLECTIONS.put("LinkedHashMap", LinkedHashMap.class);
        COLLECTIONS.put("TreeMap", TreeMap.class);
        COLLECTIONS.put("ConcurrentHashMap", ConcurrentHashMap.class);
        COLLECTIONS.put("HashSet", HashSet.class);
        COLLECTIONS.put("LinkedHashSet", LinkedHashSet.class);
        COLLECTIONS.put("TreeSet", TreeSet.class);
        // Интерфейсы сопоставляем с их обычной реализацией по умолчанию
        COLLECTIONS.put("List", ArrayList.class);
        COLLECTIONS.put("Map", HashMap.class);
        COLLECTIONS.put("Set", HashSet.class);
    }

    // =====================================================================
    // 1. Анализ параметризованных полей
    // =====================================================================

    /**
     * Находит все обобщенные поля класса и выводит:
     * имя поля и его обобщенный тип, фактические типовые аргументы,
     * ограничения типовых параметров.
     *
     * @param clazz анализируемый класс
     */
    public void analyzeGenericFields(Class<?> clazz) {
        System.out.println("Анализ обобщённых полей класса " + clazz.getName());
        System.out.println("  Типовые параметры класса: " + formatTypeParameters(clazz.getTypeParameters()));

        Field[] fields = clazz.getDeclaredFields();
        if (fields.length == 0) {
            System.out.println("  (объявленных полей нет)");
            return;
        }
        for (Field field : fields) {
            if (field.isSynthetic()) {
                continue;
            }
            Type genericType = field.getGenericType();
            System.out.println("  Поле \"" + field.getName() + "\": " + genericType.getTypeName()
                    + "   [после стирания типов: " + field.getType().getName() + "]");
            describeType(genericType, "      ");
        }
    }

    // =====================================================================
    // 2. Определение фактических типовых аргументов
    // =====================================================================

    /**
     * Для конкретного экземпляра определяет фактические типовые аргументы,
     * поднимаясь по цепочке наследования (суперклассы и интерфейсы).
     *
     * Например, для {@link StringIntegerMap} метод покажет, что тип ключа —
     * {@code String}, а тип значения — {@code Integer}.
     *
     * @param instance экземпляр (например, new StringIntegerMap())
     * @return карта "роль → фактический тип" (для Map: ключ и значение)
     */
    public Map<String, Type> extractTypeArguments(Object instance) {
        Class<?> clazz = instance.getClass();
        System.out.println("Определение фактических типовых аргументов для " + clazz.getName());

        List<ResolvedType> hierarchy = new ArrayList<>();
        Type superclass = clazz.getGenericSuperclass();
        if (superclass != null) {
            collectResolvedTypes(superclass, Collections.<TypeVariable<?>, Type>emptyMap(), hierarchy);
        }
        for (Type itf : clazz.getGenericInterfaces()) {
            collectResolvedTypes(itf, Collections.<TypeVariable<?>, Type>emptyMap(), hierarchy);
        }

        if (hierarchy.isEmpty()) {
            System.out.println("  (параметризованных предков не найдено)");
        }
        Set<String> printed = new LinkedHashSet<>();
        for (ResolvedType resolved : hierarchy) {
            if (printed.add(resolved.describe())) {
                System.out.println("  " + resolved.describe());
            }
        }

        Map<String, Type> result = new LinkedHashMap<>();
        for (ResolvedType resolved : hierarchy) {
            if (resolved.rawClass == Map.class && resolved.arguments.length == 2) {
                result.put("ключ (K)", resolved.arguments[0]);
                result.put("значение (V)", resolved.arguments[1]);
                break;
            }
        }
        if (!result.isEmpty()) {
            System.out.println("  → тип ключа: " + result.get("ключ (K)").getTypeName()
                    + ", тип значения: " + result.get("значение (V)").getTypeName());
        }
        return result;
    }

    /**
     * Рекурсивно собирает параметризованные предки, подставляя уже
     * известные типовые переменные (K → String и т.п.).
     */
    private void collectResolvedTypes(Type type, Map<TypeVariable<?>, Type> bindings, List<ResolvedType> out) {
        if (!(type instanceof ParameterizedType)) {
            return;
        }
        ParameterizedType parameterized = (ParameterizedType) type;
        Type raw = parameterized.getRawType();
        Type[] arguments = parameterized.getActualTypeArguments();

        Type[] resolved = new Type[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            resolved[i] = substitute(arguments[i], bindings);
        }

        Class<?> rawClass = raw instanceof Class ? (Class<?>) raw : null;
        out.add(new ResolvedType(rawClass, raw, resolved));

        if (rawClass == null) {
            return;
        }

        Map<TypeVariable<?>, Type> next = new LinkedHashMap<>(bindings);
        TypeVariable<?>[] variables = rawClass.getTypeParameters();
        for (int i = 0; i < variables.length && i < resolved.length; i++) {
            next.put(variables[i], resolved[i]);
        }

        Type superclass = rawClass.getGenericSuperclass();
        if (superclass != null) {
            collectResolvedTypes(superclass, next, out);
        }
        for (Type itf : rawClass.getGenericInterfaces()) {
            collectResolvedTypes(itf, next, out);
        }
    }

    /** Подставляет фактические типы вместо типовых переменных. */
    private Type substitute(Type type, Map<TypeVariable<?>, Type> bindings) {
        if (type instanceof TypeVariable) {
            Type value = bindings.get(type);
            return value != null ? value : type;
        }
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Type[] arguments = parameterized.getActualTypeArguments();
            Type[] resolved = new Type[arguments.length];
            boolean changed = false;
            for (int i = 0; i < arguments.length; i++) {
                resolved[i] = substitute(arguments[i], bindings);
                if (resolved[i] != arguments[i]) {
                    changed = true;
                }
            }
            return changed
                    ? ParameterizedTypeImpl.make(parameterized.getRawType(), resolved, parameterized.getOwnerType())
                    : parameterized;
        }
        return type;
    }

    // =====================================================================
    // 3. Создание экземпляров обобщенных коллекций
    // =====================================================================

    /**
     * Создает экземпляр коллекции во время выполнения и описывает его
     * параметризованный тип.
     *
     * <p>Важно: сами типовые аргументы в объект «не записываются» —
     * из-за стирания типов объект в памяти всегда один и тот же
     * ({@code ArrayList} без указания элементов). Мы лишь описываем
     * предполагаемый параметризованный тип через {@link ParameterizedTypeImpl}.</p>
     *
     * @param collectionType короткое имя ("ArrayList", "HashMap", ...) или полное имя класса
     * @param typeArgs       типовые аргументы (могут быть классами или ParameterizedType)
     * @return созданный экземпляр коллекции
     */
    public Object createGenericCollection(String collectionType, Type... typeArgs) {
        Class<?> raw = resolveCollectionClass(collectionType);
        Object instance;
        try {
            Constructor<?> constructor = raw.getDeclaredConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (ReflectiveOperationException ex) {
            LOG.log(Level.WARNING, "Не удалось создать " + collectionType, ex);
            throw new IllegalArgumentException("Не удалось создать экземпляр " + collectionType
                    + ": " + ex.getMessage(), ex);
        }

        Type declaredType = typeArgs.length == 0
                ? raw
                : ParameterizedTypeImpl.make(raw, typeArgs, null);
        System.out.println("Создан объект: " + declaredType.getTypeName()
                + "   [реальный класс: " + instance.getClass().getName() + "]");
        return instance;
    }

    private Class<?> resolveCollectionClass(String collectionType) {
        Class<?> known = COLLECTIONS.get(collectionType);
        if (known != null) {
            return known;
        }
        try {
            return Class.forName(collectionType);
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.WARNING, "Неизвестный тип коллекции: " + collectionType);
            throw new IllegalArgumentException("Неизвестный тип коллекции: " + collectionType
                    + ". Известные: " + COLLECTIONS.keySet(), ex);
        }
    }

    // =====================================================================
    // 4. Вызов обобщенных методов
    // =====================================================================

    /**
     * Вызывает обобщенный метод через Reflection API.
     *
     * <p>Из-за стирания типов нельзя «передать» методу конкретные типовые
     * аргументы — они выводятся компилятором. Поэтому массив {@code typeParams}
     * используется для проверки ограничений (например, {@code K extends Comparable<K>})
     * и для наглядности, а сам вызов идет по обычной (стёртой) сигнатуре.</p>
     *
     * @param instance   объект, у которого вызывается метод
     * @param methodName имя метода
     * @param typeParams предполагаемые типовые аргументы (для валидации)
     * @param args       фактические аргументы вызова
     * @return результат вызова метода
     */
    public Object invokeGenericMethod(Object instance, String methodName, Class<?>[] typeParams, Object[] args) {
        Method target = findMethod(instance.getClass(), methodName, args.length);
        if (target == null) {
            throw new IllegalArgumentException("Метод \"" + methodName + "\" с " + args.length
                    + " аргументами не найден в " + instance.getClass().getName());
        }
        System.out.println("Вызов метода: " + target.toGenericString());
        System.out.println("  указанные типовые аргументы: "
                + (typeParams == null ? "(нет)" : Arrays.toString(typeParams)));

        validateTypeArguments(target, typeParams);

        try {
            target.setAccessible(true);
            Object result = target.invoke(instance, args);
            System.out.println("  результат: " + result);
            return result;
        } catch (ReflectiveOperationException ex) {
            LOG.log(Level.WARNING, "Ошибка при вызове метода " + methodName, ex);
            throw new IllegalStateException("Ошибка вызова " + methodName + ": " + ex.getMessage(), ex);
        }
    }

    private Method findMethod(Class<?> clazz, String name, int parameterCount) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }

    /** Проверяет, что переданные типы удовлетворяют ограничениям типовых параметров метода. */
    private void validateTypeArguments(Method method, Class<?>[] typeParams) {
        if (typeParams == null) {
            return;
        }
        TypeVariable<Method>[] variables = method.getTypeParameters();
        for (int i = 0; i < variables.length && i < typeParams.length; i++) {
            Class<?> actual = typeParams[i];
            for (Type bound : variables[i].getBounds()) {
                Class<?> boundClass = rawClassOf(bound);
                if (boundClass != null && boundClass != Object.class && !boundClass.isAssignableFrom(actual)) {
                    throw new IllegalArgumentException("Тип " + actual.getName()
                            + " не удовлетворяет ограничению " + variables[i].getName()
                            + " extends " + bound.getTypeName());
                }
            }
        }
    }

    private Class<?> rawClassOf(Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            Type raw = ((ParameterizedType) type).getRawType();
            return raw instanceof Class ? (Class<?>) raw : null;
        }
        if (type instanceof TypeVariable) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length > 0 ? rawClassOf(bounds[0]) : null;
        }
        return null;
    }

    // =====================================================================
    // Конкретные примеры из задания
    // =====================================================================

    /** Пример 1: анализ java.util.Collections$UnmodifiableMap. */
    public void example1UnmodifiableMap() {
        System.out.println("--- Пример 1: java.util.Collections$UnmodifiableMap ---");
        Map<String, Integer> source = new HashMap<>();
        source.put("один", 1);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(source);
        Class<?> clazz = unmodifiable.getClass();

        System.out.println("Класс: " + clazz.getName());
        System.out.println("Типовые параметры: " + formatTypeParameters(clazz.getTypeParameters()));
        Type superclass = clazz.getGenericSuperclass();
        System.out.println("Суперкласс: " + (superclass == null ? "(нет)" : superclass.getTypeName()));
        for (Type itf : clazz.getGenericInterfaces()) {
            System.out.println("Интерфейс: " + itf.getTypeName());
        }
        System.out.println("Поля класса (видны wildcard-типы):");
        analyzeGenericFields(clazz);
    }

    /** Пример 2: создание ConcurrentHashMap<String, List<Integer>> через Reflection. */
    public void example2ConcurrentHashMap() {
        System.out.println("--- Пример 2: ConcurrentHashMap<String, List<Integer>> ---");
        Type listOfInteger = ParameterizedTypeImpl.make(List.class, new Type[]{Integer.class}, null);
        Object created = createGenericCollection("ConcurrentHashMap", String.class, listOfInteger);

        ConcurrentHashMap<String, List<Integer>> map = new ConcurrentHashMap<>();
        map.put("числа", Arrays.asList(1, 2, 3));
        System.out.println("Обычное использование: " + map);
        System.out.println("Созданный через Reflection объект: " + created.getClass().getName());
    }

    /** Пример 3: анализ поля Map<String, List<Set<Integer>>> complexField. */
    public void example3ComplexField() {
        System.out.println("--- Пример 3: поле Map<String, List<Set<Integer>>> complexField ---");
        try {
            Field field = ComplexHolder.class.getDeclaredField("complexField");
            Type genericType = field.getGenericType();
            System.out.println("Обобщённый тип поля: " + genericType.getTypeName());
            describeType(genericType, "  ");
        } catch (NoSuchFieldException ex) {
            LOG.log(Level.SEVERE, "Поле complexField не найдено", ex);
        }
    }

    /** Вспомогательный класс с вложенным обобщенным полем. */
    public static class ComplexHolder {
        private Map<String, List<Set<Integer>>> complexField;
    }

    // =====================================================================
    // Вспомогательные методы вывода
    // =====================================================================

    /** Рекурсивно выводит структуру обобщенного типа с отступами. */
    private void describeType(Type type, String indent) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterized = (ParameterizedType) type;
            Type raw = parameterized.getRawType();
            System.out.println(indent + "→ параметризованный тип, сырой тип: " + raw.getTypeName());
            if (parameterized.getOwnerType() != null) {
                System.out.println(indent + "  владелец: " + parameterized.getOwnerType().getTypeName());
            }
            Type[] arguments = parameterized.getActualTypeArguments();
            for (int i = 0; i < arguments.length; i++) {
                System.out.println(indent + "  аргумент[" + i + "]: " + arguments[i].getTypeName());
                describeType(arguments[i], indent + "    ");
            }
            if (raw instanceof Class) {
                TypeVariable<?>[] variables = ((Class<?>) raw).getTypeParameters();
                if (variables.length > 0) {
                    System.out.println(indent + "  параметры " + ((Class<?>) raw).getSimpleName()
                            + ": " + formatTypeParameters(variables));
                }
            }
        } else if (type instanceof TypeVariable) {
            TypeVariable<?> variable = (TypeVariable<?>) type;
            System.out.println(indent + "→ типовая переменная " + variable.getName()
                    + ", ограничения: " + formatBounds(variable.getBounds()));
        } else if (type instanceof WildcardType) {
            WildcardType wildcard = (WildcardType) type;
            System.out.println(indent + "→ wildcard (?):");
            for (Type upper : wildcard.getUpperBounds()) {
                System.out.println(indent + "  extends " + upper.getTypeName());
                describeType(upper, indent + "    ");
            }
            for (Type lower : wildcard.getLowerBounds()) {
                System.out.println(indent + "  super " + lower.getTypeName());
                describeType(lower, indent + "    ");
            }
        } else if (type instanceof GenericArrayType) {
            GenericArrayType arrayType = (GenericArrayType) type;
            System.out.println(indent + "→ обобщённый массив из " + arrayType.getGenericComponentType().getTypeName());
            describeType(arrayType.getGenericComponentType(), indent + "    ");
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                System.out.println(indent + "→ массив, компонент: " + clazz.getComponentType().getTypeName());
                describeType(clazz.getComponentType(), indent + "    ");
            } else {
                TypeVariable<?>[] variables = clazz.getTypeParameters();
                if (variables.length > 0) {
                    System.out.println(indent + "→ сырой класс с параметрами: " + formatTypeParameters(variables));
                }
            }
        }
    }

    private String formatTypeParameters(TypeVariable<?>[] variables) {
        if (variables.length == 0) {
            return "(нет)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < variables.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(variables[i].getName());
            Type[] bounds = variables[i].getBounds();
            if (!(bounds.length == 1 && bounds[0] == Object.class)) {
                sb.append(" extends ").append(formatBounds(bounds));
            }
        }
        return sb.toString();
    }

    private String formatBounds(Type[] bounds) {
        if (bounds.length == 0) {
            return "Object";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bounds.length; i++) {
            if (i > 0) {
                sb.append(" & ");
            }
            sb.append(bounds[i].getTypeName());
        }
        return sb.toString();
    }

    /** Результат разрешения одного уровня иерархии типов. */
    private static final class ResolvedType {
        private final Class<?> rawClass;
        private final Type rawType;
        private final Type[] arguments;

        ResolvedType(Class<?> rawClass, Type rawType, Type[] arguments) {
            this.rawClass = rawClass;
            this.rawType = rawType;
            this.arguments = arguments;
        }

        String describe() {
            String name = rawClass != null ? rawClass.getSimpleName() : rawType.getTypeName();
            if (arguments.length == 0) {
                return name;
            }
            StringBuilder sb = new StringBuilder(name).append('<');
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(arguments[i].getTypeName());
            }
            return sb.append('>').toString();
        }
    }
}
