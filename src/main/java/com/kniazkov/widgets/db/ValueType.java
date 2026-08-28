/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

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
 * Defines the model, runtime type, ordering and persistence encoding of a field value.
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
        Object::toString,
        Boolean::valueOf,
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
        Object::toString,
        Integer::valueOf,
        Comparator.naturalOrder()
    );

    /**
     * Positive integer values.
     */
    public static final ValueType<Integer> POSITIVE_INTEGER = of(
        Integer.class,
        () -> new ValidatedIntegerModel(ValidatedIntegerModel.POSITIVE),
        Object::toString,
        Integer::valueOf,
        Comparator.naturalOrder()
    );

    /**
     * Real values.
     */
    public static final ValueType<Double> REAL = of(
        Double.class,
        RealNumberModel::new,
        Object::toString,
        Double::valueOf,
        Comparator.naturalOrder()
    );

    /**
     * Positive real values.
     */
    public static final ValueType<Double> POSITIVE_REAL = of(
        Double.class,
        () -> new ValidatedRealNumberModel(ValidatedRealNumberModel.POSITIVE),
        Object::toString,
        Double::valueOf,
        Comparator.naturalOrder()
    );

    /**
     * UUID values.
     */
    public static final ValueType<UUID> IDENTIFIER = of(
        UUID.class,
        UuidModel::new,
        Object::toString,
        UUID::fromString,
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
     * Persistence encoder.
     */
    private final Function<T, String> encoder;

    /**
     * Persistence decoder.
     */
    private final Function<String, T> decoder;

    /**
     * Optional value comparator.
     */
    private final Comparator<T> comparator;

    /**
     * Creates a value type.
     *
     * @param valueClass runtime class
     * @param modelFactory model factory
     * @param encoder persistence encoder
     * @param decoder persistence decoder
     * @param comparator comparator, or {@code null}
     */
    private ValueType(
        final Class<T> valueClass,
        final Supplier<Model<T>> modelFactory,
        final Function<T, String> encoder,
        final Function<String, T> decoder,
        final Comparator<T> comparator
    ) {
        this.valueClass = Objects.requireNonNull(valueClass, "valueClass");
        this.modelFactory = Objects.requireNonNull(modelFactory, "modelFactory");
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
        this.comparator = comparator;
    }

    /**
     * Creates a custom value type.
     *
     * @param valueClass runtime class
     * @param modelFactory model factory
     * @param encoder persistence encoder
     * @param decoder persistence decoder
     * @param comparator comparator, or {@code null}
     * @param <T> value type
     * @return value type
     */
    public static <T> ValueType<T> of(
        final Class<T> valueClass,
        final Supplier<Model<T>> modelFactory,
        final Function<T, String> encoder,
        final Function<String, T> decoder,
        final Comparator<T> comparator
    ) {
        return new ValueType<>(
            valueClass,
            modelFactory,
            encoder,
            decoder,
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
            Function.identity(),
            Function.identity(),
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
     * Encodes a value for persistence.
     *
     * @param value value
     * @return encoded value
     */
    public String encode(final T value) {
        return this.encoder.apply(value);
    }

    /**
     * Decodes a persisted value.
     *
     * @param value encoded value
     * @return decoded value
     */
    public T decode(final String value) {
        return this.decoder.apply(value);
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
