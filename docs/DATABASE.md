# Reactive database

The `com.kniazkov.widgets.db` package provides a reactive in-memory database for applications
built with Web Widgets. Its primary purpose is not to execute SQL. It keeps one canonical object
graph in the server process and exposes every record field as a regular
`com.kniazkov.widgets.model.Model`.

Several widgets, including widgets belonging to different browser clients, can bind to the same
field model. When one client changes that model, every bound widget receives the new value without
reloading its page.

Persistence is a separate concern. A database may run only in memory, write atomic JSON snapshots,
or commit changes through JDBC while the live models remain in RAM.

## Core concepts

| Type | Responsibility |
| --- | --- |
| `Database` | Owns named stores, serializes mutations, and controls persistence lifetime |
| `Store` | Holds the canonical records for one schema |
| `Schema` | Defines the uniquely named fields accepted by a store |
| `Field<T>` | Describes a value type and creates conditions and ordering clauses |
| `DataRecord` | Exposes canonical field models, identity, creation time, and revision |
| `Draft` | Isolates several edits until an explicit atomic commit |
| `LiveRecordSet` | Maintains the membership and ordering of a query result |
| `Persistence` | Loads encoded records and atomically commits change sets |

The in-memory representation is always the source used by widgets and queries. A persistence
backend provides durability; it does not replace the canonical models.

## Defining a database

Fields are declared once and grouped into a schema. A schema rejects duplicate names and a record
rejects a `Field` instance that does not belong to its store.

```java
import com.kniazkov.widgets.db.Database;
import com.kniazkov.widgets.db.Field;
import com.kniazkov.widgets.db.Schema;
import com.kniazkov.widgets.db.Store;
import com.kniazkov.widgets.db.ValueType;

Field<String> name = new Field<>(ValueType.NOT_EMPTY_STRING, "name");
Field<Integer> age = new Field<>(ValueType.POSITIVE_INTEGER, "age");
Field<Boolean> active = new Field<>(ValueType.BOOLEAN, "active");

Database database = Database.builder()
    .store("employees", Schema.of(name, age, active))
    .build();

Store employees = database.getStore("employees");
```

Without an explicit backend, the builder uses `NoPersistence`. All records disappear when the
process stops.

`ValueType` combines four pieces of field metadata:

- the runtime Java class;
- a factory for the field's reactive model;
- encoding and decoding functions for persistence;
- an optional comparator used by query conditions and ordering.

The built-in types cover strings and validated strings, booleans, integers, real numbers, and
UUIDs. Applications can create another `ValueType` for a custom model and value class.

## Creating and editing records

New records are created through a draft. Nothing is visible to queries or other clients before the
draft is committed.

```java
Draft draft = employees.createDraft();
draft.model(name).setData("Alice");
draft.model(age).setData(32);
draft.model(active).setData(true);

DataRecord alice = draft.commit();
```

A committed record exposes canonical models:

```java
Model<String> sharedName = alice.model(name);
sharedName.setData("Alice Smith");
```

That call is serialized by the database dispatcher. The persistence backend commits the new
record revision first; only then does the in-memory model change and notify its listeners.

For a multi-field edit, create a draft from the existing record:

```java
Draft edit = alice.edit();
edit.model(name).setData("Alice Brown");
edit.model(age).setData(33);
edit.commit();
```

The draft remembers the revision from which it was created. If another request commits a newer
revision first, `commit()` throws `ConflictException` instead of silently overwriting it. Calling
`cancel()` closes a draft without changing the record.

Records are deleted through `DataRecord.remove()`. Deletion is persisted before the record leaves
the store and its live query results.

## Binding records to widgets

A field model is an ordinary `Model<T>`, so it can be assigned directly to a compatible widget:

```java
InputField nameInput = new InputField();
nameInput.setTextModel(alice.model(name));

InputField ageInput = new InputField();
ageInput.setTextModel(
    new IntegerToStringModel(alice.model(age))
);
```

If two pages bind input fields to `alice.model(name)`, both bindings observe the same server-side
model. Text entered on one page is sent to the server, committed, and then returned to every
client whose widget is bound to it.

Binding a widget to a draft model has different semantics: changes remain private to that draft
until `commit()` is called.

## Live queries

Query conditions form an inspectable expression tree rather than opaque Java predicates. This lets
a live result determine which fields can change its membership or ordering.

```java
Condition condition = age.greaterThan(18)
    .and(
        active.is(true)
            .or(name.is("Administrator"))
    );

LiveRecordSet adults = employees.query(
    Query.where(condition)
        .orderBy(name.ascending())
        .thenBy(age.descending())
);
```

`And`, `Or`, `Not`, and comparison classes are internal query nodes. Applications compose them
through `Condition.and()`, `Condition.or()`, `Condition.not()`, and methods on `Field`.

`getRecords()` returns a stable snapshot of the current result. Subscribe to receive later
changes:

```java
Subscription subscription = adults.subscribe(change -> {
    switch (change.kind()) {
        case ADDED -> addRow(change.record(), change.newIndex());
        case REMOVED -> removeRow(change.record(), change.oldIndex());
        case UPDATED -> updateRow(change.record(), change.newIndex());
        case MOVED -> moveRow(
            change.record(),
            change.oldIndex(),
            change.newIndex()
        );
    }
});
```

Keep the `Subscription` for as long as updates are required and call `subscription.close()` when
the consumer is discarded.

## JSON persistence

`JsonPersistence` stores each configured store in a separate file inside one database directory:

```java
Database database = Database.builder()
    .persistence(
        new JsonPersistence(Path.of("application-data"))
    )
    .store("employees", Schema.of(name, age, active))
    .build();
```

For example, stores named `employees` and `settings` produce this layout:

```text
application-data/
├── employees.json
└── settings.json
```

Each commit builds a new snapshot of the affected store, writes it to a temporary file, and
replaces only that store's target file. An atomic filesystem move is used when supported. A JSON
change set is restricted to one store because replacing several files cannot provide a real
cross-store atomic commit.

This implementation is useful for examples and small, low-write applications. It rewrites the
complete affected store for every committed model change. Binding a text input directly to a
persisted record can therefore rewrite that store's file for every entered character.

Use drafts to group form changes, or choose JDBC when frequent updates and a larger data set make
whole-file snapshots inappropriate.

## JDBC persistence

`JdbcPersistence` uses only the standard `java.sql` API. The widgets library does not include a
JDBC driver, ORM, connection pool, or migration framework.

For an embedded H2 database:

```java
import com.kniazkov.widgets.db.persistence.jdbc.H2Dialect;
import com.kniazkov.widgets.db.persistence.jdbc.JdbcPersistence;

Database database = Database.builder()
    .persistence(
        new JdbcPersistence(
            "jdbc:h2:file:./data/application",
            new H2Dialect()
        )
    )
    .store("employees", Schema.of(name, age, active))
    .build();
```

The application must provide the driver at runtime:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>2.4.240</version>
    <scope>runtime</scope>
</dependency>
```

SQLite is selected in the same way:

```java
new JdbcPersistence(
    "jdbc:sqlite:application.db",
    new SqliteDialect()
);
```

The JDBC backend uses one record table and one field-value table. Values are encoded by their
`ValueType`; queries still run against the canonical models in RAM. A single connection is enough
because the database dispatcher serializes all mutations.

Updating the SQL tables through another connection does **not** update live models. All reactive
changes must pass through `Database`. Likewise, two server JVMs connected to the same SQL database
do not notify each other. Horizontal deployment requires an additional shared change log or
publish/subscribe mechanism.

## Lifecycle and failure behavior

`Database` implements `AutoCloseable`. Long-running applications should close it during orderly
shutdown:

```java
try (Database database = Database.builder()
    .store("employees", Schema.of(name, age))
    .build()) {
    use(database);
}
```

Mutation methods are synchronous from the caller's point of view. A persistence error throws
`PersistenceException`, and the corresponding in-memory change is not published.

The complete runnable integration is available in
[`SimpleDb.java`](../src/main/java/com/kniazkov/widgets/example/SimpleDb.java).
