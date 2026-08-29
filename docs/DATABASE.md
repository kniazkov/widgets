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
| `Persistence` | Loads typed record snapshots and atomically commits change sets |

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

`ValueType` combines the runtime and persistence behavior of a field:

- a stable semantic type name;
- the runtime Java class;
- a factory for the field's reactive model;
- the physical persistence scalar kind and model default;
- conversion to and from a typed persistence scalar;
- an optional comparator used by query conditions and ordering.

The built-in types cover strings and validated strings, booleans, integers, real numbers, and
UUIDs. Applications can create another `ValueType` for a custom model and value class. Its
persistence converter selects a string, integer, real, or boolean `StoredValue`; this type is
retained by every backend.

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

## Persistence formats

Every durable backend stores a schema catalog in addition to records. The catalog has format
version `1` and contains stores and fields in declaration order. Each field definition contains:

| Property | Meaning |
| --- | --- |
| `name` | Field name used in records |
| `type` | Stable semantic `ValueType` name, such as `email` or `positive-integer` |
| `valueKind` | Physical scalar kind: `STRING`, `INTEGER`, `REAL`, or `BOOLEAN` |
| `defaultValue` | Native scalar used when a stored record omits the field |
| `position` | Zero-based declaration position |
| `referencedStore` | Target store for a UUID relation, otherwise absent or SQL `NULL` |

`Database` creates this metadata from its configured schemas before loading records. A durable
backend writes the catalog when a database is created and compares it on every later open. A
mismatch normally raises `PersistenceException`. The only automatic metadata upgrade is appending
fields to the end of an existing store schema. Every previously stored field definition must be
an exact prefix of the configured field list, including its name, position, semantic type, scalar
kind, default value, and referenced store. The set and order of stores must remain unchanged.

Compatible additions update `database.metadata` atomically in JSON persistence and append the new
`db_field_definition` rows in one JDBC transaction. Existing records do not need to be rewritten:
the newly added fields receive their catalog defaults when loaded. Removing a field, inserting one
in the middle, reordering fields, changing old field metadata, changing stores, or changing the
format version fails instead of guessing a migration.

Built-in semantic types are `boolean`, `string`, `not-empty-string`, `username`, `phone-number`,
`email`, `integer`, `positive-integer`, `real`, `positive-real`, and `identifier`. A custom
`ValueType` must provide its own stable name and physical `StoredValue.Kind`.

A relation is declared explicitly so an external editor can discover it:

```java
Field<UUID> departmentId = new Field<>(
    ValueType.IDENTIFIER,
    "departmentId",
    "departments"
);
```

### NoPersistence

`NoPersistence` keeps no external files, tables, or metadata. Its `initialize` method validates
that metadata was supplied and then discards it. Records and the schema catalog disappear with the
server process.

### JSON persistence

`JsonPersistence` stores its schema catalog and one record file per store inside one directory:

```java
Database database = Database.builder()
    .persistence(new JsonPersistence(Path.of("application-data")))
    .store("employees", Schema.of(name, age, active))
    .build();
```

For stores named `employees` and `departments`, the physical layout is:

```text
application-data/
├── database.metadata
├── departments.json
└── employees.json
```

`database.metadata` is JSON despite its extension. The different extension prevents a metadata
file from being mistaken for a store file. A catalog looks like this:

```json
{
  "formatVersion": 1,
  "stores": [
    {
      "name": "employees",
      "position": 0,
      "fields": [
        {
          "name": "name",
          "type": "not-empty-string",
          "valueKind": "STRING",
          "defaultValue": "",
          "position": 0
        },
        {
          "name": "departmentId",
          "type": "identifier",
          "valueKind": "STRING",
          "defaultValue": "a843176c-36df-44bd-b8a2-b1d8c956c431",
          "position": 1,
          "referencedStore": "departments"
        }
      ]
    },
    {
      "name": "departments",
      "position": 1,
      "fields": []
    }
  ]
}
```

Store names are URL-encoded in file names. The store name is not repeated inside its records.
Each store file is a JSON array whose records have this form:

```json
{
  "id": "02d3dbe8-b28e-4c1a-a42e-934b577caafe",
  "createdAt": "2026-08-29T10:15:30Z",
  "revision": 3,
  "fields": {
    "name": "Alice",
    "age": 34,
    "active": true
  }
}
```

Strings, integers, real numbers, and booleans use native JSON scalars. UUIDs and timestamps are
strings because JSON has no corresponding native types. Missing fields receive their model's
default value when loaded; unknown fields are rejected by the configured Java schema.

Each commit writes a temporary file and replaces only the affected store file. An atomic move is
used when the filesystem supports it. A JSON change set cannot span stores because replacing
several files cannot provide a real cross-store transaction. The complete affected store is
rewritten, so drafts should group frequent form changes.

### JDBC persistence

`JdbcPersistence` uses only `java.sql`. The library includes no JDBC driver, ORM, connection pool,
or migration framework. H2 and SQLite differ only in physical SQL column types; their logical
schema is identical.

```java
Database database = Database.builder()
    .persistence(new JdbcPersistence(
        "jdbc:h2:file:./data/application",
        new H2Dialect()
    ))
    .store("employees", Schema.of(name, age, active))
    .build();
```

The application supplies the H2 driver at runtime. SQLite is selected with
`jdbc:sqlite:application.db` and `SqliteDialect`.

#### JDBC metadata tables

`db_metadata` contains exactly one logical row:

| Column | H2 | SQLite | Meaning |
| --- | --- | --- | --- |
| `metadata_id` | `INTEGER` | `INTEGER` | Constant identifier `1`, primary key |
| `format_version` | `INTEGER` | `INTEGER` | Persistence format version |

`db_store` contains one row per configured store:

| Column | H2 | SQLite | Meaning |
| --- | --- | --- | --- |
| `store_name` | `VARCHAR(255)` | `TEXT` | Store name, primary key |
| `store_order` | `INTEGER` | `INTEGER` | Zero-based declaration position, unique |

`db_field_definition` contains the field catalog:

| Column | H2 | SQLite | Meaning |
| --- | --- | --- | --- |
| `store_name` | `VARCHAR(255)` | `TEXT` | Owning store; part of the primary key |
| `field_name` | `VARCHAR(255)` | `TEXT` | Field name; part of the primary key |
| `field_order` | `INTEGER` | `INTEGER` | Zero-based declaration position |
| `type_name` | `VARCHAR(255)` | `TEXT` | Stable semantic `ValueType` name |
| `value_kind` | `VARCHAR(16)` | `TEXT` | `STRING`, `INTEGER`, `REAL`, or `BOOLEAN` |
| `default_string` | `CLOB` | `TEXT` | String default or `NULL` |
| `default_integer` | `INTEGER` | `INTEGER` | Integer default or `NULL` |
| `default_real` | `DOUBLE PRECISION` | `REAL` | Real default or `NULL` |
| `default_boolean` | `BOOLEAN` | `INTEGER` | Boolean default or `NULL` |
| `referenced_store` | `VARCHAR(255)` | `TEXT` | Target store, or `NULL` |

Exactly one default column is non-`NULL`, selected by `value_kind`.

#### JDBC data tables

`db_record` contains record identity and revision metadata:

| Column | H2 | SQLite | Meaning |
| --- | --- | --- | --- |
| `store_name` | `VARCHAR(255)` | `TEXT` | Store name; part of the primary key |
| `record_id` | `VARCHAR(36)` | `TEXT` | UUID string; part of the primary key |
| `created_at` | `VARCHAR(40)` | `TEXT` | ISO-8601 `Instant` string |
| `revision` | `BIGINT` | `INTEGER` | Positive optimistic-lock revision |

`db_field` contains one typed value per present record field:

| Column | H2 | SQLite | Meaning |
| --- | --- | --- | --- |
| `store_name` | `VARCHAR(255)` | `TEXT` | Store name; part of the primary key |
| `record_id` | `VARCHAR(36)` | `TEXT` | Record UUID; part of the primary key |
| `field_name` | `VARCHAR(255)` | `TEXT` | Field name; part of the primary key |
| `value_type` | `VARCHAR(16)` | `TEXT` | Physical scalar kind |
| `string_value` | `CLOB` | `TEXT` | String payload or `NULL` |
| `integer_value` | `INTEGER` | `INTEGER` | Integer payload or `NULL` |
| `real_value` | `DOUBLE PRECISION` | `REAL` | Real payload or `NULL` |
| `boolean_value` | `BOOLEAN` | `INTEGER` | Boolean payload or `NULL` |

Exactly one payload column must be non-`NULL`, and it must match `value_type`. A complete record
replacement deletes its old `db_field` rows and inserts the new set in the same transaction.

#### Rules for an external editor

A program that edits JDBC tables directly must run while the widgets server is stopped. The
server keeps canonical records in RAM, does not poll SQL, and may overwrite unseen external
changes. An online editor must call a server API instead of writing these tables directly.

For an offline edit:

1. Read `db_metadata`, `db_store`, and `db_field_definition` before displaying records.
2. Update the one payload column selected by `value_type`; clear the other payload columns.
3. Increase the corresponding `db_record.revision` once per logical record edit.
4. Preserve UUID and `Instant` string formats.
5. Commit all changes to one record in a single SQL transaction.

Two server JVMs connected to the same SQL database do not notify one another. Horizontal
deployment still requires a shared change log or publish/subscribe mechanism.

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

## Examples

The runnable examples are located in the common `com.kniazkov.widgets.example` package:

- [`SimpleDb.java`](../src/main/java/com/kniazkov/widgets/example/SimpleDb.java) combines live
  queries, direct model bindings, record creation, deletion, and JSON persistence.
- [`DatabaseSearch.java`](../src/main/java/com/kniazkov/widgets/example/DatabaseSearch.java) shows
  equality and comparison conditions, `and`, `or`, `not`, and multi-field ordering.
- [`DatabaseEditing.java`](../src/main/java/com/kniazkov/widgets/example/DatabaseEditing.java) edits
  several fields through an isolated `Draft` and commits them atomically.
- [`DatabaseRelations.java`](../src/main/java/com/kniazkov/widgets/example/DatabaseRelations.java)
  stores a related record UUID and binds widgets to the resolved record from another store.
