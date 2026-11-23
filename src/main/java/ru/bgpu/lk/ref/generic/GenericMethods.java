package ru.bgpu.lk.ref.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Класс с обобщенными методами для демонстрации вызова
 * через Reflection API.
 */
public class GenericMethods {

    /**
     * Создает список из массива. Типовой параметр T выводится из типа массива.
     *
     * @param array исходный массив (может быть {@code null})
     * @return список элементов массива
     */
    public <T> List<T> createList(T[] array) {
        List<T> result = new ArrayList<>();
        if (array != null) {
            for (T item : array) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Создает отсортированную карту из двух списков.
     * Тип ключа K обязан реализовывать {@link Comparable}.
     *
     * @param keys   список ключей
     * @param values список значений
     * @return отсортированная карта (TreeMap)
     */
    public <K extends Comparable<K>, V> Map<K, V> createSortedMap(List<K> keys, List<V> values) {
        Map<K, V> result = new TreeMap<>();
        int size = Math.min(keys == null ? 0 : keys.size(), values == null ? 0 : values.size());
        for (int i = 0; i < size; i++) {
            result.put(keys.get(i), values.get(i));
        }
        return result;
    }
}
