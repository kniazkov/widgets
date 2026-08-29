/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.db;

import com.kniazkov.widgets.db.DatabaseTestSupport.Fixture;
import com.kniazkov.widgets.db.query.Condition;
import com.kniazkov.widgets.db.query.Conditions;
import com.kniazkov.widgets.db.query.Order;
import com.kniazkov.widgets.db.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static com.kniazkov.widgets.db.DatabaseTestSupport.ACTIVE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.AGE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.NAME;
import static com.kniazkov.widgets.db.DatabaseTestSupport.SCORE;
import static com.kniazkov.widgets.db.DatabaseTestSupport.addEmployee;
import static com.kniazkov.widgets.db.DatabaseTestSupport.names;
import static com.kniazkov.widgets.db.DatabaseTestSupport.open;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests for query conditions, dependencies, and ordering.
 */
public final class QueryTest {
    /**
     * Verifies equality and inequality conditions.
     */
    @Test
    public void evaluatesEqualityConditions() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );

            assertTrue(NAME.is("Alice").matches(alice));
            assertFalse(NAME.is("Bob").matches(alice));
            assertTrue(NAME.isNot("Bob").matches(alice));
            assertFalse(NAME.isNot("Alice").matches(alice));
        }
    }

    /**
     * Verifies ordered comparison conditions.
     */
    @Test
    public void evaluatesOrderedComparisons() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );

            assertTrue(AGE.greaterThan(30).matches(alice));
            assertFalse(AGE.greaterThan(34).matches(alice));
            assertTrue(AGE.lessThan(40).matches(alice));
            assertFalse(AGE.lessThan(34).matches(alice));
            assertTrue(SCORE.greaterThan(1.0).matches(alice));
        }
    }

    /**
     * Verifies logical condition composition.
     */
    @Test
    public void composesLogicalConditions() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );
            final Condition adult = AGE.greaterThan(18);
            final Condition namedAlice = NAME.is("Alice");

            assertTrue(adult.and(namedAlice).matches(alice));
            assertFalse(adult.and(NAME.is("Bob")).matches(alice));
            assertTrue(NAME.is("Bob").or(namedAlice).matches(alice));
            assertFalse(namedAlice.not().matches(alice));
            assertTrue(Conditions.all().matches(alice));
        }
    }

    /**
     * Verifies conditions report the exact fields they inspect.
     */
    @Test
    public void reportsConditionDependencies() {
        final Condition condition = AGE.greaterThan(18)
            .and(ACTIVE.is(true).or(NAME.is("Administrator").not()));

        assertEquals(Set.of(AGE, ACTIVE, NAME), condition.dependencies());
        assertTrue(Conditions.all().dependencies().isEmpty());
    }

    /**
     * Verifies query dependencies include both filters and ordering.
     */
    @Test
    public void reportsQueryDependencies() {
        final Query query = Query.where(ACTIVE.is(true))
            .orderBy(NAME.ascending())
            .thenBy(AGE.descending());

        assertEquals(Set.of(ACTIVE, NAME, AGE), query.dependencies());
        assertTrue(Query.all().dependencies().isEmpty());
    }

    /**
     * Verifies ascending and descending ordering.
     */
    @Test
    public void ordersByOneField() {
        try (Fixture fixture = open()) {
            addEmployee(fixture.store(), "Carol", 41, true, 3.0);
            addEmployee(fixture.store(), "Alice", 34, true, 1.5);
            addEmployee(fixture.store(), "Bob", 28, false, 2.0);

            assertEquals(
                Arrays.asList("Alice", "Bob", "Carol"),
                names(fixture.store().query(
                    Query.all().orderBy(NAME.ascending())
                ).getRecords())
            );
            assertEquals(
                Arrays.asList("Carol", "Bob", "Alice"),
                names(fixture.store().query(
                    Query.all().orderBy(NAME.descending())
                ).getRecords())
            );
        }
    }

    /**
     * Verifies multi-field ordering uses later clauses only for ties.
     */
    @Test
    public void ordersBySeveralFields() {
        try (Fixture fixture = open()) {
            addEmployee(fixture.store(), "Carol", 40, true, 3.0);
            addEmployee(fixture.store(), "Alice", 30, true, 1.5);
            addEmployee(fixture.store(), "Bob", 40, false, 2.0);

            final Query query = Query.all()
                .orderBy(AGE.descending())
                .thenBy(NAME.ascending());

            assertEquals(
                Arrays.asList("Bob", "Carol", "Alice"),
                names(fixture.store().query(query).getRecords())
            );
        }
    }

    /**
     * Verifies equal ordering values are deterministically resolved by UUID.
     */
    @Test
    public void breaksOrderingTiesByIdentifier() {
        try (Fixture fixture = open()) {
            final DataRecord first = addEmployee(
                fixture.store(), "Same", 30, true, 1.0
            );
            final DataRecord second = addEmployee(
                fixture.store(), "Same", 30, false, 2.0
            );
            final List<DataRecord> expected = new ArrayList<>(
                Arrays.asList(first, second)
            );
            expected.sort(Comparator.comparing(DataRecord::getId));

            assertEquals(
                expected,
                fixture.store().query(
                    Query.all().orderBy(NAME.ascending())
                ).getRecords()
            );
        }
    }

    /**
     * Verifies adding ordering clauses creates new immutable queries.
     */
    @Test
    public void keepsQueriesImmutable() {
        final Query base = Query.where(ACTIVE.is(true));
        final Query ordered = base.orderBy(NAME.ascending());

        assertEquals(Set.of(ACTIVE), base.dependencies());
        assertEquals(Set.of(ACTIVE, NAME), ordered.dependencies());
        assertSame(NAME, NAME.ascending().getField());
    }

    /**
     * Verifies invalid query arguments fail immediately.
     */
    @Test
    public void rejectsInvalidQueryArguments() {
        assertThrows(NullPointerException.class, () -> Query.where(null));
        assertThrows(NullPointerException.class,
            () -> Query.all().orderBy(null));
        assertThrows(NullPointerException.class,
            () -> Conditions.equal(NAME, null));
        assertThrows(NullPointerException.class,
            () -> Conditions.greaterThan(AGE, null));
        assertThrows(NullPointerException.class,
            () -> Order.ascending(null));
    }

    /**
     * Verifies unordered value types reject greater-than evaluation.
     */
    @Test
    public void rejectsOrderingForUnorderedTypes() {
        try (Fixture fixture = open()) {
            final DataRecord alice = addEmployee(
                fixture.store(), "Alice", 34, true, 1.5
            );

            assertThrows(UnsupportedOperationException.class,
                () -> ACTIVE.greaterThan(false).matches(alice));
            assertThrows(UnsupportedOperationException.class,
                () -> ACTIVE.ascending().compare(alice, alice));
        }
    }
}
