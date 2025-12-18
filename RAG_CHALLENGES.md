# RAG Implementation Challenges & Limitations

This document captures real-world challenges encountered during the development of the Luminous Morocco Tours RAG system.

## 1. The "Polluted Context" (or Conflicting Information) Problem

### Observation
When asked, *"Tell me about the owner,"* the RAG system incorrectly identified the owner or conflated Luminous Morocco Tours with its competitors (e.g., *Experience It Tours* or *Mint Tea Tours*).

### Root Cause
The Knowledge Base contained two distinct types of data:
1.  **Ground Truth (HTMLs):** Actual website data (`about-us.html`) correctly identifying "Hamid" as the owner.
2.  **Strategy/Competitor Data (PDFs):** Documents like `Blog-links.pdf` or `Blog-ideas.pdf` which analyzed competitors.

**The Failure Mode:**
When the user asked a generic question, the Vector Database retrieved chunks from *both* sources based on semantic similarity.
*   The query "owner" matched text in the competitor analysis.
*   The LLM received a context window containing *both* "Hamid is the owner" and "Experience It Tours is owned by...".
*   The LLM hallucinated or merged the facts.

### The Solution: "Separation of Concerns" Architecture
We implemented a **Multi-Store Architecture** (Option B) to physically isolate the data sources.

#### 1. Dual Vector Stores (PostgreSQL)
*   **Facts Store** (`embeddings_facts` table): Contains *only* HTML content from the company website.
*   **Strategy Store** (`embeddings_strategy` table): Contains *only* internal strategy PDFs and competitor analysis.

#### 2. Specialized Retrievers
*   `factsRetriever`: Connected exclusively to the Facts Store.
*   `strategyRetriever`: Connected exclusively to the Strategy Store.

#### 3. Explicit Service Wiring
We removed the generic `@AiService` auto-configuration and manually wired the beans in `AiServicesConfiguration.java`:
*   **Chatbot (`RagService`):** Wired to `factsRetriever`. It is physically impossible for the chatbot to access competitor data.
*   **Blog Generator (`BlogService`):** Wired to `strategyRetriever`. It uses the internal documents to guide style and content strategy.

### Result
The Chatbot now accurately identifies Hamid and ignores competitors. The Blog Generator continues to use the rich strategy data for creative tasks.

## 2. HTML Noise
Raw HTML files contain significant noise. We used `Jsoup` to strip tags and extract clean text, ensuring retrieving chunks are dense with information.