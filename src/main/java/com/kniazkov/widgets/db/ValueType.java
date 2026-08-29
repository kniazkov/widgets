/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.persistence.StoredValue;
import com.kniazkov.widgets.db.persistence.StoredValue.BooleanValue;
import com.kniazkov.widgets.db.persistence.StoredValue.IntegerValue;
import com.kniazkov.widgets.db.persistence.StoredValue.Kind;
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
        "boolean",
        Boolean.class,
        BooleanModel::new,
        Kind.BOOLEAN,
        BooleanValue::new,
        StoredValue::getBoolean,
        null
    );

    /**
     * String values.
     */
    public static final ValueType<String> STRING = stringType(
        "string",
        StringModel::new
    );

    /**
     * Non-empty string values.
     */
    public static final ValueType<String> NOT_EMPTY_STRING =
        stringType("not-empty-string", NotEmptyStringModel::new);

    /**
     * Username values.
     */
    public static final ValueType<String> USERNAME = stringType(
        "username",
        UsernameModel::new
    );

    /**
     * Phone number values.
     */
    public static final ValueType<String> PHONE_NUMBER =
        stringType("phone-number", PhoneNumberModel::new);

    /**
     * Email values.
     */
    public static final ValueType<String> EMAIL = stringType(
        "email",
        EmailModel::new
    );

    /**
     * Integer values.
     */
    public static final ValueType<Integer> INTEGER = of(
        "integer",
        Integer.class,
        IntegerModel::new,
        Kind.INTEGER,
        IntegerValue::new,
        StoredValue::getInteger,
        Comparator.naturalOrder()
    );

    /**
     * Positive integer values.
     */
    public static final ValueType<Integer> POSITIVE_INTEGER = of(
        "positive-integer",
        Integer.class,
        () -> new ValidatedIntegerModel(ValidatedIntegerModel.POSITIVE),
        Kind.INTEGER,
        IntegerValue::new,
        StoredValue::getInteger,
        Comparator.naturalOrder()
    );

    /**
     * Real values.
     */
    public static final ValueType<Double> REAL = of(
        "real",
        Double.class,
        RealNumberModel::new,
        Kind.REAL,
        RealValue::new,
        StoredValue::getReal,
        Comparator.naturalOrder()
    );

    /**
     * Positive real values.
     */
    public static final ValueType<Double> POSITIVE_REAL = of(
        "positive-real",
        Double.class,
        () -> new ValidatedRealNumberModel(ValidatedRealNumberModel.POSITIVE),
        Kind.REAL,
        RealValue::new,
        StoredValue::getReal,
        Comparator.naturalOrder()
    );

    /**
     * UUID values.
     */
    public static final ValueType<UUID> IDENTIFIER = of(
        "identifier",
        UUID.class,
        UuidModel::new,
        Kind.STRING,
        value -> new StringValue(value.toString()),
        value -> UUID.fromString(value.getString()),
        Comparator.naturalOrder()
    );

    /**
     * Stable semantic type name stored in database metadata.
     */
    private final String name;

    /**
     * Physical persistence scalar kind.
     */
    private final Kind storedKind;

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
     * @param name stable semantic type name
     * @param valueClass runtime class
     * @param modelFactory model factory
     * @param storedKind physical persistence scalar kind
     * @param toStoredValue persistence conversion
     * @param fromStoredValue reverse persistence conversion
     * @param comparator comparator, or {@code null}
     */
    private ValueType(
        final String name,
        final Class<T> valueClass,
        final Supplier<Model<T>> modelFactory,
        final Kind storedKind,
        final Function<T, StoredValue> toStoredValue,
        final Function<StoredValue, T> fromStoredValue,
        final Comparator<T> comparator
    ) {
        this.name = Objects.requireNonNull(name, "name");
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("Value type name cannot be blank");
        }
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass");
        this.modelFactory = Objects.requireNonNull(modelFactory, "modelFactory");
        this.storedKind = Objects.requireNonNull(storedKind, "storedKind");
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
     * @param name stable semantic type name
     * @param valueClass runtime class
     * @param modelFactory model factory
     * @param storedKind physical persistence scalar kind
     * @param toStoredValue persistence conversion
     * @param fromStoredValue reverse persistence conversion
     * @param comparator comparator, or {@code null}
     * @param <T> value type
     * @return value type
     */
    public static <T> ValueType<T> of(
        final String name,
        final Class<T> valueClass,
        final Supplier<Model<T>> modelFactory,
        final Kind storedKind,
        final Function<T, StoredValue> toStoredValue,
        final Function<StoredValue, T> fromStoredValue,
        final Comparator<T> comparator
    ) {
        return new ValueType<>(
            name,
            valueClass,
            modelFactory,
            storedKind,
            toStoredValue,
            fromStoredValue,
            comparator
        );
    }

    /**
     * Creates a string value type.
     *
     * @param name semantic type name
     * @param factory model factory
     * @return value type
     */
    private static ValueType<String> stringType(
        final String name,
        final Supplier<Model<String>> factory
    ) {
        return of(
            name,
            String.class,
            factory,
            Kind.STRING,
            StringValue::new,
            StoredValue::getString,
            Comparator.naturalOrder()
        );
    }

    /**
     * Returns the stable semantic type name.
     *
     * @return type name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the physical persistence scalar kind.
     *
     * @return stored kind
     */
    public Kind getStoredKind() {
        return this.storedKind;
    }

    /**
     * Returns the model default converted to its persistence scalar.
     *
     * @return stored default value
     */
    public StoredValue getStoredDefault() {
        return this.toStoredValue(this.createModel().getData());
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
        final StoredValue stored = Objects.requireNonNull(
            this.toStoredValue.apply(Objects.requireNonNull(value, "value")),
            "stored value"
        );
        if (stored.getKind() != this.storedKind) {
            throw new IllegalArgumentException(
                "Value type '" + this.name + "' produced " + stored.getKind()
                    + " instead of " + this.storedKind
            );
        }
        return stored;
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
