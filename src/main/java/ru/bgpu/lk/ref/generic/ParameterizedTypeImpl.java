package ru.bgpu.lk.ref.generic;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * Реализация интерфейса {@link java.lang.reflect.ParameterizedType}.
 *
 * В JDK нет публичного класса, позволяющего создать объект ParameterizedType
 * вручную, а для описанных в задании сценариев это нужно: например, чтобы
 * описать тип {@code ArrayList<List<String>>} во время выполнения.
 *
 * Класс создается через статическую фабрику {@link #make(Type, Type[], Type)}.
 */
public final class ParameterizedTypeImpl implements ParameterizedType {

    private final Type rawType;
    private final Type[] actualTypeArguments;
    private final Type ownerType;

    private ParameterizedTypeImpl(Type rawType, Type[] actualTypeArguments, Type ownerType) {
        this.rawType = Objects.requireNonNull(rawType, "rawType");
        this.actualTypeArguments = actualTypeArguments == null ? new Type[0] : actualTypeArguments.clone();
        this.ownerType = ownerType;
    }

    /**
     * Создает описание параметризованного типа.
     *
     * @param rawType             сырой тип, например {@code List.class}
     * @param actualTypeArguments фактические типовые аргументы, например {@code new Type[]{String.class}}
     * @param ownerType           владелец типа (может быть {@code null})
     */
    public static ParameterizedType make(Type rawType, Type[] actualTypeArguments, Type ownerType) {
        return new ParameterizedTypeImpl(rawType, actualTypeArguments, ownerType);
    }

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments.clone();
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType that = (ParameterizedType) other;
        return rawType.equals(that.getRawType())
                && Arrays.equals(actualTypeArguments, that.getActualTypeArguments())
                && Objects.equals(ownerType, that.getOwnerType());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(actualTypeArguments)
                ^ Objects.hashCode(ownerType)
                ^ rawType.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(rawType.getTypeName());
        if (actualTypeArguments.length > 0) {
            sb.append('<');
            for (int i = 0; i < actualTypeArguments.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(actualTypeArguments[i].getTypeName());
            }
            sb.append('>');
        }
        return sb.toString();
    }
}
