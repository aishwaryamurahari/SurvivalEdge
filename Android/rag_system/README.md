# RAG System for Edible Wild Plants

This is a Retrieval-Augmented Generation (RAG) system designed to process PDFs about edible wild plants and provide intelligent responses to user queries.

## Setup Instructions

### 1. Install Dependencies

```bash
# Navigate to the rag_system directory
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### 2. Build RAG System

```bash
python main.py
```

This will:
- Extract text from all PDFs
- Split text into chunks
- Generate embeddings
- Create FAISS index
- Save everything for future use

### 3. Test RAG System

#### Interactive Testing
```bash
python test_rag.py
```

#### Batch Testing
```bash
python test_rag.py batch
```
