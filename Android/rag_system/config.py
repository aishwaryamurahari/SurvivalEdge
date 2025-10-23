import os
from pathlib import Path

class Config:
    # Paths
    BASE_DIR = Path(__file__).parent
    DATA_DIR = BASE_DIR / "data"
    PDF_DIR = DATA_DIR / "pdfs"
    INDEX_DIR = DATA_DIR / "index"
    CHUNKS_DIR = DATA_DIR / "chunks"

    # Create directories if they don't exist
    for dir_path in [DATA_DIR, PDF_DIR, INDEX_DIR, CHUNKS_DIR]:
        dir_path.mkdir(parents=True, exist_ok=True)

    # PDF files
    PDF_FILES = [
        "Edible_Wildplants.pdf",
        "edible-wild-plants.pdf",
        "Elias, Thomas & Dykeman, Peter - Edible Wild Plants_ A North American Field Guide (1982).pdf"
    ]

    # Embedding model
    EMBEDDING_MODEL = "all-MiniLM-L6-v2"
    EMBEDDING_DIMENSION = 384

    # Chunking parameters
    CHUNK_SIZE = 400
    CHUNK_OVERLAP = 50

    # FAISS parameters
    FAISS_INDEX_TYPE = "IndexFlatL2"
    TOP_K = 10
    SIMILARITY_THRESHOLD = 0.02  # Adjusted for unnormalized embeddings

    # Index files
    FAISS_INDEX_FILE = INDEX_DIR / "plant_index.faiss"
    METADATA_FILE = INDEX_DIR / "metadata.json"
    CHUNKS_FILE = CHUNKS_DIR / "processed_chunks.json"
