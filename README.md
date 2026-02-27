## RAG Web Crawler

A production-oriented Java 25 crawler for documentation sites.
It crawls a single host, respects `robots.txt`, extracts useful text/code,
and writes JSONL chunks ready for vector-embedding ingestion.

### Features

- **Single-host crawl** with URL normalization and deduplication (same scheme, host, and port).
- **Virtual-thread concurrency** via `Executors.newVirtualThreadPerTaskExecutor()`.
- **Robots-aware**: fetches and applies basic `Disallow` rules.
- **Polite crawling**: per-host concurrency + minimum delay between requests.
- **Content extraction**: title, `h1–h4`, paragraphs, lists, and code blocks.
- **Chunking**: ~1500-character chunks; code blocks emitted as separate chunks.
- **Output**: JSONL (one chunk per line) suitable for downstream embedding + storage.

---

### 1. Requirements

- Java 25 (JDK)
- Maven 3.x
- Docker + Docker Compose (optional, for containerized runtime and PostgreSQL)

---

### 2. Build the JAR

From the project root:

```bash
cd rag-webcrawler
mvn -DskipTests package
```

This produces a **shaded fat jar** in `target/`:

- `target/rag-webcrawler-0.1.0-SNAPSHOT.jar`

You can run this jar directly with `java -jar`.

---

### 3. CLI Usage (local JVM)

Basic crawl:

```bash
java -jar target/rag-webcrawler-0.1.0-SNAPSHOT.jar \
  --url https://docs.example.com/ \
  --output ./output/docs.jsonl \
  --maxtime 20 \
  --maxPages 500 \
  --maxDepth 3
```

Real example (Spring Boot docs page):

```bash
java -jar target/rag-webcrawler-0.1.0-SNAPSHOT.jar \
  --url https://docs.spring.io/spring-boot/documentation.html \
  --output ./output/spring-boot-docs.jsonl \
  --maxtime 20 \
  --maxPages 500 \
  --maxDepth 4
```

**Arguments**

- `--url` (required): root documentation URL.
- `--output` (required): path to JSONL output file (parent directory is created if needed).
- `--maxtime` (optional, default `20`): max crawl duration in seconds (hard deadline).
- `--maxPages` (optional): hard cap on number of pages to visit.
- `--maxDepth` (optional): maximum distance from the root page (0 = only root).
- `--perHostConcurrency` (optional, default `4`): max concurrent requests per host.
- `--perHostMinDelayMillis` (optional, default `250`): minimum delay between requests per host.
- `--userAgent` (optional): custom User-Agent string.

Notes:

- The crawler logs basic progress (pages crawled) and HTTP timeouts/retries.
- When `--maxtime` is reached, it stops submitting new pages and lets in-flight requests finish or fail.

---

### 4. JSONL Output

Each line is a JSON object with a chunk of content and metadata, including:

- `url`, `canonicalUrl`, `rootUrl`
- `title`, `headings`
- `chunkIndex`, `chunkCount`
- `content`, `contentType`, `blockTypes`, `codeLanguage`
- `pageHash`, `chunkHash`
- `depth`, `hPath`, `lang`
- `crawledAt`, `source`
- `metadata` (e.g. HTTP status and content-type header)

This is designed to map cleanly into a `doc_chunks` table backed by PostgreSQL + pgvector.

---

### 5. Docker

Build the image:

```bash
docker build -t rag-webcrawler .
```

Run with a simple crawl:

```bash
docker run --rm -v "$PWD/output:/output" rag-webcrawler \
  --url https://docs.example.com/ \
  --output /output/docs.jsonl \
  --maxtime 20 \
  --maxPages 500 \
  --maxDepth 3
``>

For the Spring Boot docs specifically:

```bash
docker run --rm -v "$PWD/output:/output" rag-webcrawler \
  --url https://docs.spring.io/spring-boot/documentation.html \
  --output /output/spring-boot-docs.jsonl \
  --maxtime 20 \
  --maxPages 500 \
  --maxDepth 4
```

---

### 6. Docker Compose (PostgreSQL + pgvector)

The included `docker-compose.yml` starts:

- `db`: PostgreSQL 16 with the `vector` extension enabled via `db/init.sql`.
- `crawler`: the crawler image, writing JSONL to `./output/crawl.jsonl` by default.

Bring everything up:

```bash
docker-compose up --build
```

To customize the crawl target or output path, edit the `command` section of the `crawler` service, e.g.:

```yaml
crawler:
  build: .
  depends_on:
    - db
  volumes:
    - ./output:/output
  command: >
    --url https://docs.spring.io/spring-boot/documentation.html
    --maxtime 20
    --maxPages 500
    --maxDepth 4
    --output /output/spring-boot-docs.jsonl
```

After the crawler container finishes, you will have a JSONL file under `output/` that can be ingested
into a `doc_chunks` table (with a `VECTOR` column for embeddings) in PostgreSQL.

---

### 7. Notes and Next Steps

- The crawler focuses on **HTTP/HTTPS** and a **single host** (same scheme, host, and port).
- Robots handling is intentionally simple (Disallow rules + optional Crawl-delay).
- Timeouts and retries are logged but do not stop the crawl; they are expected on slow sites.
- Extending it (e.g. better language detection, sitemap support, or direct DB writes)
  can be done by adding new components that plug into the existing pipeline.

