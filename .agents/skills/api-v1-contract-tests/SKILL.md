---
name: api-v1-contract-tests
description: Add or extend HTTP contract tests for imshaby-api v1 controllers before Spring Boot migration. Use when covering another controller with MockMvc and preserving current API behavior.
---

# API v1 contract tests

Build protective tests for one requested controller at a time. Use `server/src/test/java/by/imsha/rest/CityControllerMockMvcIT.java` and `server/src/test/resources/city-mockmvc/` as examples of the test boundary and fixture layout, not as a checklist to copy mechanically.

## Establish the contract

- Read the controller, DTOs, domain objects, mappers, service and persistence constraints used by its routes. Inventory annotations that affect request or response data, including custom deserializers, Jackson annotations and validation constraints.
- Compare every route and important behavior with the relevant existing Cucumber scenarios. Preserve observed API v1 behavior; report surprising behavior or bugs explicitly instead of silently treating them as intended design.
- Select cases that protect meaningful migration risks: success, authorization, missing resources, validation boundaries, serialization, pagination, uniqueness and state changes where applicable. Do not add permutations without a distinct contract assertion.

## Test shape

- Exercise HTTP through `MockMvc` with the Spring application context and real MongoDB through Testcontainers. Route external HTTP clients to controlled local stubs. Keep tests independent by resetting database, caches and stubs; run shared-state tests sequentially.
- Keep request and expected response JSON in `server/src/test/resources/<controller>-mockmvc/<testMethod>/`. Use unique method names; include the operation when needed to distinguish otherwise identical cases. Give each test that uses fixtures its own folder and copy a fixture when several tests use the same JSON. Use short filenames within the test folder. Tests without fixtures need no folder; an existing empty `.gitkeep` folder may remain.
- Assert status, significant headers, full JSON structure and values, and persisted state when it matters. Read JSON as UTF-8. For generated IDs, assert type and format, then substitute the observed ID in the expected fixture.
- Assert pagination metadata and page contents without depending on unspecified database order. When the API has no stable sort, verify the exact expected set in a single page large enough to contain it, and verify that separate page responses contain only expected records; do not assert that independent page requests form a stable partition.
- Cover every validation and data-format annotation exercised by the controller, including accepted and rejected boundaries. Test error responses and verify failed writes did not persist partial data when relevant.
- Keep helpers small and named for an observable contract. Prefer existing JUnit, Spring Test and AssertJ capabilities; add a dependency only when it materially improves several tests.

## Scope and verification

- Add tests and test resources only unless the user explicitly requests production changes. Do not change dependencies, API v2, or remove Cucumber scenarios as part of protective coverage.
- Follow the checkout's `AGENTS.md` and use its prescribed Testcontainers test runner. Keep local Docker and tunnel settings out of repository files. If the environment is unavailable, report it without changing production code or dependencies to accommodate the machine.
- Run the requested controller's tests, inspect failures, and report the actual result, covered routes and remaining material gaps. Leave unrelated controllers and user files untouched.
