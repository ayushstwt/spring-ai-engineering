# PDF Chat Application Architecture

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4+ |
| AI Framework | Spring AI 1.0.0 |
| Database | PostgreSQL + pgvector |
| ORM | Spring Data JPA |
| Build Tool | Maven |
| Authentication | JWT + Spring Security |
| Storage | AWS S3 / MinIO |
| PDF Parsing | Apache PDFBox + Tika |
| Queue | RabbitMQ / Kafka (Optional) |
| Cache | Redis |
| API Docs | Swagger OpenAPI |
| Deployment | Docker + Kubernetes |
| Monitoring | Prometheus + Grafana |
| Logging | ELK Stack |

---

# High Level Architecture

```text
                ┌────────────────────┐
                │   Frontend App     │
                │ React / Next.js    │
                └─────────┬──────────┘
                          │
                          ▼
                ┌────────────────────┐
                │   API Gateway      │
                └─────────┬──────────┘
                          │
           ┌──────────────┴──────────────┐
           ▼                             ▼
┌────────────────────┐      ┌────────────────────┐
│ Authentication     │      │ PDF Chat Service   │
│ Service             │      │ Spring AI          │
└────────────────────┘      └─────────┬──────────┘
                                       │
                 ┌─────────────────────┼────────────────────┐
                 ▼                     ▼                    ▼
      ┌────────────────┐   ┌──────────────────┐   ┌────────────────┐
      │ PostgreSQL     │   │ OpenAI/Gemini   │   │ AWS S3/MinIO   │
      │ + pgvector     │   │ Embedding Model │   │ PDF Storage    │
      └────────────────┘   └──────────────────┘   └────────────────┘
```

---

# Project Structure

```text
pdf-chat-ai/
│
├── src/main/java/com/ayshriv/pdfchat
│
├── config/
│   ├── OpenAiConfig.java
│   ├── PgVectorConfig.java
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   ├── RedisConfig.java
│   └── AsyncConfig.java
│
├── controller/
│   ├── AuthController.java
│   ├── PdfController.java
│   ├── ChatController.java
│   └── AdminController.java
│
├── service/
│   ├── auth/
│   ├── pdf/
│   │   ├── PdfUploadService.java
│   │   ├── PdfParserService.java
│   │   ├── PdfChunkingService.java
│   │   ├── PdfEmbeddingService.java
│   │   ├── PdfVectorStoreService.java
│   │   └── PdfProcessingService.java
│   │
│   ├── chat/
│   │   ├── ChatService.java
│   │   ├── ContextBuilderService.java
│   │   ├── SimilaritySearchService.java
│   │   └── AiResponseService.java
│   │
│   └── storage/
│       ├── S3StorageService.java
│       └── MinioStorageService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── PdfDocumentRepository.java
│   ├── DocumentChunkRepository.java
│   └── ChatHistoryRepository.java
│
├── entity/
│   ├── BaseEntity.java
│   ├── User.java
│   ├── PdfDocument.java
│   ├── DocumentChunk.java
│   ├── ChatHistory.java
│   └── EmbeddingMetadata.java
│
├── dto/
│   ├── request/
│   ├── response/
│   └── common/
│
├── security/
│   ├── JwtService.java
│   ├── JwtFilter.java
│   ├── CustomUserDetailsService.java
│   └── SecurityUtils.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── BadRequestException.java
│   ├── UnauthorizedException.java
│   └── ResourceNotFoundException.java
│
├── util/
│   ├── FileUtils.java
│   ├── PromptUtils.java
│   ├── TokenUtils.java
│   └── ValidationUtils.java
│
├── scheduler/
│   ├── CleanupScheduler.java
│   └── RetryScheduler.java
│
├── listener/
│   ├── PdfUploadListener.java
│   └── EmbeddingGenerationListener.java
│
├── constant/
│   ├── AppConstants.java
│   └── PromptConstants.java
│
└── PdfChatApplication.java
```

---

# Database Design

## users

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## pdf_documents

```sql
CREATE TABLE pdf_documents (
    id UUID PRIMARY KEY,
    user_id UUID,
    file_name VARCHAR(500),
    original_file_name VARCHAR(500),
    file_url TEXT,
    file_size BIGINT,
    total_pages INTEGER,
    processing_status VARCHAR(50),
    uploaded_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## document_chunks

```sql
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID,
    chunk_text TEXT,
    page_number INTEGER,
    chunk_index INTEGER,
    embedding VECTOR(1536),
    created_at TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES pdf_documents(id)
);
```

---

## chat_history

```sql
CREATE TABLE chat_history (
    id UUID PRIMARY KEY,
    user_id UUID,
    document_id UUID,
    question TEXT,
    answer TEXT,
    created_at TIMESTAMP
);
```

---

# pgvector Indexing

## HNSW Index

```sql
CREATE INDEX document_chunks_embedding_idx
ON document_chunks
USING hnsw (embedding vector_cosine_ops);
```

---

# Production PDF Processing Flow

```text
1. Upload PDF
2. Store original file in S3
3. Save metadata in DB
4. Publish async event
5. Parse PDF text
6. Split into chunks
7. Generate embeddings
8. Store vectors in pgvector
9. Update processing status
10. Enable chat
```

---

# Chunking Strategy

## Recommended

| Setting | Value |
|---|---|
| Chunk Size | 500-1000 tokens |
| Chunk Overlap | 100-200 |
| Embedding Model | text-embedding-3-small |
| Similarity TopK | 5 |

---

# Recommended Entity Structure

## PdfDocument Entity

```java
@Entity
@Table(name = "pdf_documents")
public class PdfDocument extends BaseEntity {

    @Id
    private UUID id;

    private String fileName;

    private String originalFileName;

    private String fileUrl;

    private Long fileSize;

    private Integer totalPages;

    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    @ManyToOne
    private User user;
}
```

---

## DocumentChunk Entity

```java
@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    private UUID id;

    @Column(columnDefinition = "TEXT")
    private String chunkText;

    private Integer pageNumber;

    private Integer chunkIndex;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1536)
    private float[] embedding;

    @ManyToOne
    private PdfDocument document;
}
```

---

# REST APIs

## Upload PDF

```http
POST /api/v1/pdfs/upload
```

## Get User PDFs

```http
GET /api/v1/pdfs
```

## Chat With PDF

```http
POST /api/v1/chat
```

Request:

```json
{
  "documentId": "uuid",
  "question": "Explain Spring Boot"
}
```

---

# Similarity Search Query

```sql
SELECT *
FROM document_chunks
ORDER BY embedding <=> CAST(:embedding AS vector)
LIMIT 5;
```

---

# AI Prompt Strategy

```text
You are a helpful AI assistant.
Answer only from provided PDF context.
If answer is unavailable, say:
"I could not find this information in uploaded document."
```

---

# Security Best Practices

| Feature | Recommendation |
|---|---|
| Authentication | JWT |
| Authorization | Role Based |
| File Validation | MIME + Size Check |
| Virus Scan | ClamAV |
| Rate Limiting | Bucket4j |
| Secrets | Vault / AWS Secrets |
| API Security | HTTPS |

---

# Production Features

| Feature | Status |
|---|---|
| Multi PDF Chat | Recommended |
| User Isolation | Required |
| Metadata Filtering | Required |
| Streaming Responses | Recommended |
| OCR PDFs | Recommended |
| Async Processing | Required |
| Retry Mechanism | Required |
| Caching | Recommended |
| Audit Logging | Recommended |
| Monitoring | Required |

---

# Recommended Async Processing

## Flow

```text
Upload PDF
→ Publish Event
→ Background Worker
→ Generate Embeddings
→ Store Vector
→ Update Status
```

---

# Recommended Docker Setup

## docker-compose.yml

```yaml
version: '3.8'

services:

  postgres:
    image: pgvector/pgvector:pg14
    container_name: pgvector
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: vectordb
    ports:
      - "5432:5432"

  redis:
    image: redis:latest
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    command: server /data
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: password
    ports:
      - "9000:9000"
```

---

# Production Deployment

## Recommended Infrastructure

| Component | Recommended |
|---|---|
| Backend | Kubernetes |
| DB | AWS RDS PostgreSQL |
| Storage | AWS S3 |
| CDN | CloudFront |
| Monitoring | Grafana |
| Logs | ELK |
| Secrets | AWS Secrets Manager |

---

# Recommended Future Enhancements

| Feature | Benefit |
|---|---|
| OCR Support | Scanned PDFs |
| Multi-language Chat | Global users |
| Voice Chat | Accessibility |
| Citation Support | Accurate answers |
| AI Agents | Advanced workflows |
| Hybrid Search | Better retrieval |
| Re-ranking | Better context |

---

# Final Production Recommendations

1. Use async PDF processing.
2. Never store raw PDFs in DB.
3. Use pgvector indexes.
4. Use chunk overlap.
5. Add metadata filtering.
6. Implement retries.
7. Add rate limiting.
8. Store chat history.
9. Add observability.
10. Secure APIs properly.

---

# Ideal Roadmap

## Phase 1
- PDF upload
- Chunking
- Embeddings
- Chat

## Phase 2
- Multi-user support
- Authentication
- S3 storage
- Streaming

## Phase 3
- OCR
- Re-ranking
- AI Agents
- Multi-document reasoning

## Phase 4
- Kubernetes
- Horizontal scaling
- Monitoring
- Enterprise features
