# Online Store Order Management

A console demo of an online store's back end, built with **Spring (context + ORM)**, **JPA / Hibernate** and an in-memory **H2** database. It covers customers, products, orders, payments, reports and an audit log, and shows transactions, JPQL, the Criteria API, paging and optimistic locking in action.

## Tech stack

| Area | Technology |
|------|-----------|
| Language | Java 17 (Kotlin plugin configured in the POM) |
| Build | Maven |
| DI / transactions | Spring Context & ORM 7.0.0 |
| Persistence | Hibernate 7.1.0.Final (JPA, `jakarta.persistence`) |
| Database | H2 2.3.232, embedded, schema created on startup (`create-drop`) |

## Project structure

```
src/main/java/org/nti
├── Main.java        # Runnable demo scenario (10 sections)
├── config/          # JpaConfig: DataSource, EntityManagerFactory, transaction manager
├── model/           # JPA entities: Customer, Product, Category, Order, OrderItem,
│                    #   Payment, AuditLog, Address (embeddable), BaseEntity, enums
├── repository/      # Data access: Customer, Product, Category, Order, AuditLog, Report
├── service/         # Business logic: Customer, Product, Order, Report, Audit
├── dto/             # Read models (records): OrderSummary, OrderLine, ProductPage, ...
└── exception/       # StoreException hierarchy
```

## Features

- **Customers**: register with a shipping address; duplicate emails are rejected (case-insensitive).
- **Products**: add with SKU, price, stock and one or more categories; restock; change price; duplicate SKUs are rejected.
- **Orders**: place multi-line orders (all-or-nothing stock deduction), then `pay`, `ship` or `cancel`. Cancelling returns stock. Invalid state transitions throw `InvalidOrderStateException`. Order lines keep the price they were placed with.
- **Order lifecycle**: `NEW → PAID → SHIPPED`, or `CANCELLED`. Payment methods are `CARD`, `CASH` and `WALLET`.
- **Product queries**: keyword/price/category search (Criteria API), by category, low stock, and paging.
- **Reports**: revenue by category, top customers, orders per status, products never ordered, monthly sales, and a bulk percentage discount per category (a single JPQL `UPDATE`).
- **Audit log**: written with `Propagation.REQUIRES_NEW`, so entries survive a rolled-back business transaction.
- **Concurrency**: `BaseEntity` carries a `@Version` field for optimistic locking. The demo has two threads race to buy the last item in stock.

## Getting started

### Prerequisites

- JDK 17+
- Maven 3.8+ on your `PATH` (or use IntelliJ IDEA, which bundles Maven)

### Run from IntelliJ IDEA (easiest)

1. Open the project folder; IntelliJ imports the Maven project.
2. Set the project SDK to JDK 17.
3. Open `src/main/java/org/nti/Main.java` and click the green run icon next to `main`.

### Run from the command line

```bash
mvn clean compile exec:java -Dexec.mainClass=org.nti.Main
```

If `mvn` is not found, install Maven (for example `brew install maven`) or call the copy bundled with IntelliJ:

```bash
export JAVA_HOME=/path/to/jdk-17
"/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" \
  clean compile exec:java -Dexec.mainClass=org.nti.Main
```

To hide the Hibernate SQL logging, pipe the output through `grep -E "^(>>>|====)"`.

No database setup is needed. H2 runs in memory and Hibernate creates the schema on startup. SQL is logged to the console (`hibernate.show_sql=true`). Demo output lines are prefixed with `>>>`.

## What the demo does

`Main` runs these scenarios in order:

1. Register customers (and reject a duplicate email)
2. Add products across categories
3. Place orders, including one that fails on insufficient stock and rolls back
4. Pay, ship and cancel orders
5. Change a price and show old orders keep their original price
6. Product queries: JPQL, Criteria API, paging
7. Reports
8. Bulk 10% discount on a category
9. Audit log
10. Two customers buying the last item concurrently

## Configuration

Settings live in `config/JpaConfig.java`:

| Property | Value |
|----------|-------|
| `hibernate.hbm2ddl.auto` | `create-drop` |
| `hibernate.show_sql` | `true` |
| `hibernate.generate_statistics` | `true` |

To use a different database, replace the `dataSource()` bean and the matching JDBC driver dependency.
