## RAG Web Crawler

A small, production-oriented Java 25 crawler for documentation sites.
It crawls a single host, respects `robots.txt`, extracts useful text/code,
and writes JSONL chunks ready for vector-embedding ingestion.

### Features

- **Single-host crawl** with URL normalization and deduplication.
- **Virtual-thread concurrency** via `Executors.newVirtualThreadPerTaskExecutor()`.
- **Robots-aware**: fetches and applies basic `Disallow` rules.
- **Polite crawling**: per-host concurrency + minimum delay between requests.
- **Content extraction**: title, `h1–h4`, paragraphs, lists, and code blocks.
- **Chunking**: ~1500-character chunks; code blocks emitted as separate chunks.
- **Output**: JSONL (one chunk per line) suitable for downstream embedding + storage.

### Build

You need Java 25 and Maven installed.

```bash
cd rag-webcrawler
mvn -DskipTests package
```

This produces a shaded jar in `target/` (e.g. `rag-webcrawler-0.1.0-SNAPSHOT.jar`).

### CLI

```bash
java -jar target/rag-webcrawler-0.1.0-SNAPSHOT.jar \
  --url https://docs.example.com/ \
  --output ./output/docs.jsonl \
  --maxtime 20 \
  --maxPages 500 \
  --maxDepth 3
```

**Arguments**

- `--url` (required): root documentation URL.
- `--output` (required): path to JSONL output file.
- `--maxtime` (optional, default `20`): max crawl duration in seconds.
- `--maxPages` (optional): hard cap on number of pages to visit.
- `--maxDepth` (optional): maximum distance from the root page (0 = only root).
- `--perHostConcurrency` (optional, default `4`): max concurrent requests per host.
- `--perHostMinDelayMillis` (optional, default `250`): minimum delay between requests per host.
- `--userAgent` (optional): custom User-Agent string.

### JSONL Output

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

### Docker

Build the image:

```bash
docker build -t rag-webcrawler .
```

Run with a simple crawl:

```bash
docker run --rm -v "$PWD/output:/output" rag-webcrawler \
  --url https://docs.example.com/ \
  --output /output/docs.jsonl
```

### Docker Compose (PostgreSQL + pgvector)

The included `docker-compose.yml` starts:

- `db`: PostgreSQL 16 with the `vector` extension enabled via `db/init.sql`.
- `crawler`: the crawler image, writing JSONL to `./output/crawl.jsonl`.

Bring everything up:

```bash
docker-compose up --build
```

After it finishes, you'll have a JSONL file under `output/` that can be ingested
into a `doc_chunks` table (with a `VECTOR` column for embeddings) in PostgreSQL.

### Notes and Next Steps

- The crawler focuses on **HTTP/HTTPS** and a **single host** (same scheme, host, and port).
- Robots handling is intentionally simple (Disallow rules + optional Crawl-delay).
- Extending it (e.g. better language detection, sitemap support, or direct DB writes)
  can be done by adding new components that plug into the existing pipeline.

