package ru.bgpu.lk.ref.generic;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

/**
 * Демонстрационный класс для Задания 2.
 *
 * Запуск:
 * <pre>
 * mvn exec:java -Dexec.mainClass="ru.bgpu.lk.ref.generic.GenericAnalyzerDemo"
 * </pre>
 */
public class GenericAnalyzerDemo {

    public static void main(String[] args) {
        GenericAnalyzer analyzer = new GenericAnalyzer();

        System.out.println("========== Задание 2: Работа с генериками и параметризованными типами ==========");
        System.out.println();

        System.out.println("### 1. Анализ параметризованных полей (DataContainer) ###");
        analyzer.analyzeGenericFields(DataContainer.class);
        System.out.println();

        System.out.println("### 2. Определение фактических типовых аргументов ###");
        analyzer.extractTypeArguments(new StringIntegerMap());
        System.out.println();

        System.out.println("### 3. Создание обобщённых коллекций через Reflection ###");
        analyzer.createGenericCollection("ArrayList", String.class);
        analyzer.createGenericCollection("HashMap", Integer.class, Double.class);
        analyzer.createGenericCollection("ArrayList",
                ParameterizedTypeImpl.make(List.class, new Type[]{String.class}, null));
        System.out.println();

        System.out.println("### 4. Вызов обобщённых методов через Reflection ###");
        GenericMethods methods = new GenericMethods();
        analyzer.invokeGenericMethod(methods, "createList",
                new Class<?>[]{String.class},
                new Object[]{new String[]{"один", "два", "три"}});
        analyzer.invokeGenericMethod(methods, "createSortedMap",
                new Class<?>[]{Integer.class, String.class},
                new Object[]{Arrays.asList(3, 1, 2), Arrays.asList("три", "один", "два")});
        System.out.println();

        analyzer.example1UnmodifiableMap();
        System.out.println();
        analyzer.example2ConcurrentHashMap();
        System.out.println();
        analyzer.example3ComplexField();
    }
}
