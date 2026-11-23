package ru.bgpu.lk.ref.generic;

import java.util.HashMap;

/**
 * Конкретный наследник обобщенного класса, у которого типовые параметры
 * уже заданы: ключ — {@link String}, значение — {@link Integer}.
 *
 * Именно на таких классах работает метод
 * {@link GenericAnalyzer#extractTypeArguments(Object)}: поднимаясь по цепочке
 * {@code getGenericSuperclass()}, можно восстановить фактические типовые
 * аргументы, потерянные при стирании типов.
 */
public class StringIntegerMap extends HashMap<String, Integer> {

    private static final long serialVersionUID = 1L;
}
