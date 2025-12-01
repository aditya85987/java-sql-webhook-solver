# BFHL SQL Solver (Java / Spring Boot)

This project implements the startup-only flow required by the BFHL test:
- On startup send POST to generateWebhook endpoint.
- Download assigned SQL question PDF (based on last 2 digits of regNo).
- Attempt to extract the final SQL query from the PDF.
- POST that final SQL string to the returned webhook URL with JWT Authorization.

## Build & package (create runnable jar)
From repo root:

```bash
mvn clean package
mkdir -p jar_output
cp target/bfhl-sql-solver-1.0.0.jar jar_output/
```

## Run locally

```bash
java -jar jar_output/bfhl-sql-solver-1.0.0.jar
```

Files created at runtime:
- `work/question.pdf`
- `work/question-text.txt`
- `work/download-error.txt` (if download fails)

If you want me to update the project to include a pre-filled `finalQuery`, tell me the SQL and I will add it.
