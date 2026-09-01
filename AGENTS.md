# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 multi-module Maven project. The root `pom.xml` manages shared versions and builds four modules:

- `ringo-boot-problem/`: framework-neutral Problem Details foundations under `io.github.ringotangs.ringoboot.problem`.
- `ringo-boot-verification/`: framework-neutral verification code services and extension points under `io.github.ringotangs.ringoboot.verification`.
- `ringo-boot-autoconfigure/`: Spring Boot Problem Details, verification, issue-limit, client-IP, and Redis auto-configuration.
- `ringo-boot-sample/`: a Spring Boot example under `src/main/java/io/github/ringotangs/ringoboot/sample`; runtime settings live in `src/main/resources/application.yaml`.

Keep production code in each module's `src/main/java` tree. Add tests in the matching `src/test/java` package structure and test resources in `src/test/resources`. Do not commit generated `target/` content or IDE metadata.

## Build, Test, and Development Commands

Run commands from the repository root with Maven 3.9.x:

- `mvn clean verify`: clean and compile every module, then run all tests and verification steps.
- `mvn spotless:apply`: format all Java production and test sources.
- `mvn spotless:check`: verify Java formatting without modifying source files.
- `mvn test`: run the full reactor test suite without packaging.
- `mvn -pl ringo-boot-problem -am test`: test Problem Details foundations and required reactor dependencies.
- `mvn -pl ringo-boot-verification -am test`: test verification code services and extension points.
- `mvn -pl ringo-boot-autoconfigure -am test`: test all Spring Boot auto-configuration.
- `mvn -pl ringo-boot-sample -am spring-boot:run`: build dependencies and start the sample application locally.
- `mvn -pl ringo-boot-sample -am package`: produce the runnable sample JAR.

## Coding Style & Naming Conventions

Use four-space indentation, UTF-8, one public top-level type per file, and the existing `io.github.ringotangs` package hierarchy. Name classes and records in `PascalCase`, methods and fields in `camelCase`, and constants in `UPPER_SNAKE_CASE`. Keep controllers thin and place reusable behavior in the matching framework-neutral module. Lombok is available, but use it only where it improves readability. Java formatting is enforced by Spotless with Palantir Java Format; run `mvn spotless:apply` before committing.

## Testing Guidelines

Tests use JUnit 5, with Spring MVC tests in the sample module. Name unit tests `*Test` and integration tests `*IT`; mirror the production package. Cover success, validation, and exception paths. Run `mvn clean verify` before opening a pull request.

## Commit & Pull Request Guidelines

Git history is not available in this checkout, so no repository-specific commit convention can be inferred. Use short, imperative subjects such as `Add validation to result factory`, and keep unrelated changes separate. Pull requests should explain the problem and solution, list affected modules, include test evidence, and link relevant issues. For API or configuration changes, include example requests, responses, or updated configuration snippets.
