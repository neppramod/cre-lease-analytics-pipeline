# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

CRE Lease Analytics Pipeline — a Spring Boot service that extracts structured fields (landlord, tenant, expiration date) from unstructured Commercial Real Estate Triple-Net (NNN) lease PDFs and exposes them as JSON, with a Kafka streaming leg simulated in tests. Deliberately scoped to a demo/prototype: no upload endpoint, no persistence of parsed results, deterministic regex parsing rather than an LLM.

## Commands

```bash
./mvnw clean test                      # full suite (also boots an in-memory KRaft Kafka broker)
./mvnw spring-boot:run                 # start server on :8080
./mvnw test -Dtest=DeterministicLeaseParserTest             # single test class
./mvnw test -Dtest=DeterministicLeaseParserTest#shouldExtractLeaseFieldsFromRawText   # single method
```

Manual end-to-end sweep — requires the server to already be running in another terminal:

```bash
chmod +x src/main/resources/scripts/runtests.sh && src/main/resources/scripts/runtests.sh
# equivalent to: curl "localhost:8080/pdfreader/parse?file=sample1"   (also sample2, sample3)
```

There is no linter configured; Java 17 is the target release.

## Architecture

Request flow: `PDFReaderController` (`GET /pdfreader/parse?file=<name>`) → `PDFDocumentReader` → `DeterministicLeaseParser` → `LeaseData` serialized to JSON.

- **`PDFDocumentReader`** wraps Spring AI's `PagePdfDocumentReader` (PDFBox underneath) and resolves files through `ClassPathResource`. Consequence: sample PDFs must live under `src/main/resources/` and are read from `target/classes/`, so a rebuild is needed for newly generated PDFs to be visible to the running app. The controller builds the path as `${cre.lease.folder-path}/<file>.pdf` — the `.pdf` suffix and the folder both come from outside the caller's input.
- **`DeterministicLeaseParser`** joins all page `Document`s into one text block and applies regexes. It is **positionally coupled to the document layout**: landlord/tenant match on the literal labels `Landlord / Lessor` and `Tenant / Lessee`, and the expiration date is simply *the second* `Month D, YYYY` match in the whole text (the first is assumed to be the commencement date). Changing the generated PDF layout, or adding an earlier date anywhere in the document, silently breaks extraction. Fields that don't match are left `null` rather than erroring.
- **`PDFGeneratorUtility`** (in `main`, not `test`) writes mock lease PDFs whose line layout mirrors exactly what the parser expects. It is driven by `TestPDFGeneratorUtility`, which is a JUnit test in name only — it regenerates `sample2.pdf` and `sample3.pdf` into `src/main/resources/sampledocumentsfolder/` on every `mvn test` run, overwriting the checked-in files. `sample1.pdf` is a real third-party sample and is not generated.
- **`LeaseStreamingSimulationTest`** is where the Kafka leg lives — there is no Kafka producer/consumer in `main`. It boots `@EmbeddedKafka` (KRaft, ephemeral ports, topic `cre-lease-events`), declares its own `TestKafkaConfig` with **String** serializers on both key and value (JSON is hand-marshalled with `ObjectMapper`, deliberately not `JsonSerializer`/`JsonDeserializer` — see README's AI notes; don't "upgrade" this back). It is a `@RepeatedTest(10)` with a send-retry loop because the broker needs a moment for partition assignment before the consumer sees the first message. This test dominates suite runtime.

## Notable dependency details

Spring Boot parent is **4.1.0**, which uses the newer starter artifact names — `spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-h2console`. Do not substitute the Boot 3.x names (`spring-boot-starter-web`, `spring-boot-starter-test`). Spring AI is pinned via BOM at `2.0.0`.

JPA + H2 are on the classpath but unused — no entities, repositories, or datasource config exist yet.

## Verified behavior and known gaps

Established by running the suite and probing the live endpoint (2026-08-18):

- `./mvnw clean test` passes: 16 tests, `BUILD SUCCESS`, ~11s including the embedded Kafka broker.
- The service works on `sample1.pdf` — a real 2-page third-party lease with prose sections, not one
  `PDFGeneratorUtility` produced. The labelled-summary-table layout is what makes the label regexes hit.
- **The positional date logic works by luck on `sample1`.** Dates appear in reading order as `October 1, 2026`
  (commencement), `September 30, 2031` (expiration), then `October 1, 2026` again in §1.1 prose. Taking the
  second match happens to be right. Any earlier date anywhere in the document — an execution date, a notary
  block — silently breaks it. See `DeterministicLeaseParser.java:43-52`.
- **PDFBox column padding is normalized — keep it that way.** PDFBox reports the whitespace that aligns
  summary-table columns, so raw matches arrive as `"Apex  Commercial    Holdings   LLC"`. The `normalize()` helper
  at the bottom of `DeterministicLeaseParser` collapses runs of whitespace; every field assignment must route
  through it, because `.trim()` alone only strips the ends. Any new extracted field needs the same treatment.
- **`DeterministicLeaseParserTest` cannot catch extraction-layer defects.** Its fixture is a hand-typed string
  with single spaces, so it validates the regexes but not the service contract — the padding bug above lived
  behind a passing assertion in that class. `RealLeasePdfParsingTest` covers the gap by reading the actual PDFs
  through `PDFDocumentReader`. Assert real-PDF behavior there, not in the string-fixture test.
- `?file=nope` returns **HTTP 500** with a stack-trace-backed error body, not 404. The controller has no error
  handling.
- The `file` param is concatenated into a classpath path with no validation, so `?file=../foo` escapes the
  configured folder. It 500s today because nothing resolves, but there is no guard.
- `LeaseStreamingSimulationTest` contributes 10 of the 16 tests (it's a `@RepeatedTest(10)`) and ~4.6s of the
  runtime — the largest single cost in the suite.

## Scope boundary (deliberate, documented)

`sample1.pdf` contains nine extractable fields; `LeaseData` models three. The other six — commencement date,
renewal notice deadline (§4.1), rent escalation schedule (§2.1), pro-rata share (§3.1), net rentable area, lease
structure type — are **intentionally out of scope for the 2-day build**, not oversights. The README's field table
records this with a Yes/No column and explains why each omitted field is harder than an extra regex.

If asked to extend extraction, the renewal notice deadline is the designated next field: it maps to the business
failure the project is framed around (a lost renewal option), and it needs prose parsing plus date arithmetic
against the expiration date. Do not quietly widen `LeaseData` without updating that table — the README's
credibility rests on claims matching the code.

## Documentation conventions

`README.md` follows the Google developer documentation style guide: second person and imperative for procedures,
sentence-case headings, active voice, US spelling, plain language over industry idiom, and no unmeasured
claims (no "high-throughput", no describing test-only code as a shipped feature). It was rewritten from an
earlier draft that overclaimed; keep new prose matched to what the code actually does.

`README.md` currently contains one intentional placeholder — `[Paste two or three of your actual research
prompts here.]` — awaiting the user's own prompt history. Do not fabricate content for it.