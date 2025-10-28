# Simple Banking (Java, Maven)

This project includes two runnable demos:

1. **In-Memory Demo** (beginner-friendly): no database required.
2. **Starter JDBC Demo**: connects to MySQL using the schema in `/database`.

## Requirements
- JDK 17+
- Maven 3.9+
- (Optional for JDBC) MySQL 8+

## Run the In-Memory Demo
```bash
mvn -q -DskipTests exec:java -Dexec.mainClass="com.jamesbranco.bank.Main"
```

## Run Tests
```bash
mvn test
```

## Set up MySQL for the JDBC Demo
1. Create the schema/tables:
   ```sql
   SOURCE /absolute/path/to/database/simple_banking_schema_mysql.sql;
   ```
2. Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` and set your values:
   ```properties
   db.url=jdbc:mysql://localhost:3306/simple_banking
   db.user=your_mysql_user
   db.password=your_mysql_password
   ```
3. Run the JDBC demo:
   ```bash
   mvn -q -DskipTests exec:java -Dexec.mainClass="com.jamesbranco.bank.MainJdbc"
   ```

## Notes
- Domain classes mirror the database fields.
- JDBC code is intentionally simple (DriverManager + try-with-resources).
- You can later replace the in-memory repository with JDBC within your service layer permanently.
# DBV_Banking
