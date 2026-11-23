package ru.bgpu.lk.ref.generic;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Тестовый обобщенный класс с несколькими типовыми параметрами.
 *
 * <ul>
 *   <li>{@code T} — произвольный тип элементов списка;</li>
 *   <li>{@code U} — тип значений карты, ограниченный сверху классом {@link Number}.</li>
 * </ul>
 *
 * Поля специально сделаны разных обобщенных форм, чтобы
 * {@link GenericAnalyzer#analyzeGenericFields(Class)} мог показать:
 * <ul>
 *   <li>простой типовой параметр ({@code List<T>});</li>
 *   <li>типовой параметр с ограничением ({@code Map<String, U extends Number>});</li>
 *   <li>wildcard с верхней границей ({@code Set<? extends Comparable<T>>}).</li>
 * </ul>
 */
public class DataContainer<T, U extends Number> {

    /** Список элементов типа T. */
    private List<T> items;

    /** Карта "строка → число типа U". */
    private Map<String, U> metadata;

    /** Множество элементов, реализующих Comparable<T>. */
    private Set<? extends Comparable<T>> filters;

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public Map<String, U> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, U> metadata) {
        this.metadata = metadata;
    }

    public Set<? extends Comparable<T>> getFilters() {
        return filters;
    }

    public void setFilters(Set<? extends Comparable<T>> filters) {
        this.filters = filters;
    }
}
