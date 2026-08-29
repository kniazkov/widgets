/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.db.persistence.StoredValue.BooleanValue;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.RealValue;
import com.kniazkov.widgets.db.persistence.StoredValue.StringValue;
import com.kniazkov.widgets.model.BooleanModel;
import com.kniazkov.widgets.model.EmailModel;
import com.kniazkov.widgets.model.IntegerModel;
import com.kniazkov.widgets.model.Model;
import com.kniazkov.widgets.model.NotEmptyStringModel;
import com.kniazkov.widgets.model.PhoneNumberModel;
import com.kniazkov.widgets.model.RealNumberModel;
import com.kniazkov.widgets.model.StringModel;
import com.kniazkov.widgets.model.UsernameModel;
import com.kniazkov.widgets.model.UuidModel;
import com.kniazkov.widgets.model.ValidatedIntegerModel;
import com.kniazkov.widgets.model.ValidatedRealNumberModel;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Defines the model, runtime type, ordering and persistence conversion of a field value.
 *
 * @param <T> value type
 */
public final class ValueType<T> {
    /**
     * Boolean values.
     */
    public static final ValueType<Boolean> BOOLEAN = of(
        Boolean.class,
        BooleanModel::new,
        BooleanValue::new,
        StoredValue::getBoolean,
        null
    );

    /**
     * String values.
     */
    public static final ValueType<String> STRING = stringType(StringModel::new);

    /**
     * Non-empty string values.
     */
    public static final ValueType<String> NOT_EMPTY_STRING =
        stringType(NotEmptyStringModel::new);

    /**
     * Username values.
     */
    public static final ValueType<String> USERNAME = stringType(UsernameModel::new);

    /**
     * Phone number values.
     */
    public static final ValueType<String> PHONE_NUMBER =
        stringType(PhoneNumberModel::new);

    /**
     * Email values.
     */
    public static final ValueType<String> EMAIL = stringType(EmailModel::new);

    /**
     * Integer values.
     */
    public static final ValueType<Integer> INTEGER = of(
        Integer.class,
        IntegerModel::new,
        IntegerValue::new,
        StoredValue::getInteger,
        Comparator.naturalOrder()
    );

    /**
     * Positive integer values.
     */
    public static final ValueType<Integer> POSITIVE_INTEGER = of(
        Integer.class,
        () -> new ValidatedIntegerModel(ValidatedIntegerModel.POSITIVE),
        IntegerValue::new,
        StoredValue::getInteger,
        Comparator.naturalOrder()
    );

    /**
     * Real values.
     */
    public static final ValueType<Double> REAL = of(
        Double.class,
        RealNumberModel::new,
        RealValue::new,
        StoredValue::getReal,
        Comparator.naturalOrder()
    );

    /**
     * Positive real values.
     */
    public static final ValueType<Double> POSITIVE_REAL = of(
        Double.class,
        () -> new ValidatedRealNumberModel(ValidatedRealNumberModel.POSITIVE),
        RealValue::new,
        StoredValue::getReal,
        Comparator.naturalOrder()
    );

    /**
     * UUID values.
     */
    public static final ValueType<UUID> IDENTIFIER = of(
        UUID.class,
        UuidModel::new,
        value -> new StringValue(value.toString()),
        value -> UUID.fromString(value.getString()),
        Comparator.naturalOrder()
    );

    /**
     * Runtime value class.
     */
    private final Class<T> valueClass;

    /**
     * Model factory.
     */
    private final Supplier<Model<T>> modelFactory;

    /**
     * Conversion to a stored scalar.
     */
    private final Function<T, StoredValue> toStoredValue;

    /**
     * Conversion from a stored scalar.
     */
    private final Function<StoredValue, T> fromStoredValue;

    /**
     * Optional value comparator.
     */
    private final Comparator<T> comparator;

    /**
     * Creates a value type.
     *
     * @param valueClass runtime class
     * @param modelFactory model factory
     * @param toStoredValue persistence conversion
     * @param fromStoredValue reverse persistence conversion
     * @param comparator comparator, or {@code null}
     */
    private ValueType(
        final Class<T> valueClass,
        final Supplier<Model<T>> modelFactory,
        final Function<T, StoredValue> toStoredValue,
        final Function<StoredValue, T> fromStoredValue,
        final Comparator<T> comparator
    ) {
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass");
        this.modelFactory = Objects.requireNonNull(modelFactory, "modelFactory");
        this.toStoredValue = Objects.requireNonNull(
            toStoredValue,
            "toStoredValue"
        );
        this.fromStoredValue = Objects.requireNonNull(
            fromStoredValue,
            "fromStoredValue"
        );
        this.comparator = comparator;
    }

    /**
     * Creates a custom value type.
     *
     * @param valueClass runtime class
     * @param modelFactory model factory
     * @param toStoredValue persistence conversion
     * @param fromStoredValue reverse persistence conversion
     * @param comparator comparator, or {@code null}
     * @param <T> value type
     * @return value type
     */
    public static <T> ValueType<T> of(
        final Class<T> valueClass,
        final Supplier<Model<T>> modelFactory,
        final Function<T, StoredValue> toStoredValue,
        final Function<StoredValue, T> fromStoredValue,
        final Comparator<T> comparator
    ) {
        return new ValueType<>(
            valueClass,
            modelFactory,
            toStoredValue,
            fromStoredValue,
            comparator
        );
    }

    /**
     * Creates a string value type.
     *
     * @param factory model factory
     * @return value type
     */
    private static ValueType<String> stringType(final Supplier<Model<String>> factory) {
        return of(
            String.class,
            factory,
            StringValue::new,
            StoredValue::getString,
            Comparator.naturalOrder()
        );
    }

    /**
     * Returns the runtime class.
     *
     * @return value class
     */
    public Class<T> getValueClass() {
        return this.valueClass;
    }

    /**
     * Creates a model with its default value.
     *
     * @return model
     */
    public Model<T> createModel() {
        return this.modelFactory.get();
    }

    /**
     * Creates a model initialized with a value.
     *
     * @param value value
     * @return model
     */
    public Model<T> createModel(final T value) {
        return this.modelFactory.get().deriveWithData(value);
    }

    /**
     * Converts a value to a persistence-neutral scalar.
     *
     * @param value value
     * @return stored value
     */
    public StoredValue toStoredValue(final T value) {
        return Objects.requireNonNull(
            this.toStoredValue.apply(Objects.requireNonNull(value, "value")),
            "stored value"
        );
    }

    /**
     * Converts a persistence-neutral scalar to a value.
     *
     * @param value stored value
     * @return restored value
     */
    public T fromStoredValue(final StoredValue value) {
        return Objects.requireNonNull(
            this.fromStoredValue.apply(
                Objects.requireNonNull(value, "stored value")
            ),
            "restored value"
        );
    }

    /**
     * Compares two values.
     *
     * @param first first value
     * @param second second value
     * @return comparison result
     */
    public int compare(final T first, final T second) {
        if (this.comparator == null) {
            throw new UnsupportedOperationException(
                "Values of type " + this.valueClass.getName() + " cannot be ordered"
            );
        }
        return this.comparator.compare(first, second);
    }
}
