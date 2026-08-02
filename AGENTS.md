# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 multi-module Maven project. The root `pom.xml` manages shared versions and builds two modules:

- `commons-core/`: reusable error codes and business exceptions under `src/main/java/io/github/ringotangs/commons/core`.
- `springboot-play/`: a Spring Boot web application under `src/main/java/io/github/ringotangs/boot/play`; runtime settings live in `src/main/resources/application.yaml`.

Keep production code in each module's `src/main/java` tree. Add tests in the matching `src/test/java` package structure and test resources in `src/test/resources`. Do not commit generated `target/` content or IDE metadata.

## Build, Test, and Development Commands

Run commands from the repository root with Maven 3.9.x:

- `mvn clean verify`: clean and compile every module, then run all tests and verification steps.
- `mvn test`: run the full reactor test suite without packaging.
- `mvn -pl commons-core -am test`: test `commons-core` and any required reactor dependencies.
- `mvn -pl springboot-play -am spring-boot:run`: build dependencies and start the sample application locally.
- `mvn -pl springboot-play -am package`: produce the runnable application JAR.

## Coding Style & Naming Conventions

Use four-space indentation, UTF-8, one public top-level type per file, and the existing `io.github.ringotangs` package hierarchy. Name classes and records in `PascalCase`, methods and fields in `camelCase`, and constants in `UPPER_SNAKE_CASE`. Keep controllers thin and place generally reusable behavior in `commons-core`. Lombok is available, but use it only where it improves readability. No formatter or lint plugin is configured; follow the surrounding source style and organize imports before committing.

## Testing Guidelines

The repository currently has no test sources or test framework dependency. For new behavior, add JUnit 5 tests and the appropriate Maven test dependency. Name unit tests `*Test` and integration tests `*IT`; mirror the production package. Cover success, validation, and exception paths. Run `mvn clean verify` before opening a pull request.

## Commit & Pull Request Guidelines

Git history is not available in this checkout, so no repository-specific commit convention can be inferred. Use short, imperative subjects such as `Add validation to result factory`, and keep unrelated changes separate. Pull requests should explain the problem and solution, list affected modules, include test evidence, and link relevant issues. For API or configuration changes, include example requests, responses, or updated configuration snippets.
