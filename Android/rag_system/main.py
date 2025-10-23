import logging
from pathlib import Path
import json

from config import Config
from pdf_processor import PDFProcessor
from text_chunker import TextChunker
from embedding_generator import EmbeddingGenerator
from faiss_indexer import FAISSIndexer
from rag_query_processor import RAGQueryProcessor

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def build_rag_system():
    """Build the complete RAG system from PDFs."""
    config = Config()

    logger.info("Starting RAG system build process...")

    # Step 1: Process PDFs
    logger.info("Step 1: Processing PDFs...")
    pdf_processor = PDFProcessor(config.PDF_DIR)
    pdf_texts = pdf_processor.process_all_pdfs(config.PDF_FILES)

    if not pdf_texts:
        logger.error("No PDFs processed successfully. Exiting.")
        return False

    # Step 2: Chunk texts
    logger.info("Step 2: Chunking texts...")
    chunker = TextChunker(
        chunk_size=config.CHUNK_SIZE,
        chunk_overlap=config.CHUNK_OVERLAP
    )

    all_chunks = []
    for pdf_file, text in pdf_texts.items():
        chunks = chunker.split_text_into_chunks(text, pdf_file)
        all_chunks.extend(chunks)

    logger.info(f"Total chunks created: {len(all_chunks)}")

    # Save chunks
    chunker.save_chunks(all_chunks, config.CHUNKS_FILE)

    # Step 3: Generate embeddings
    logger.info("Step 3: Generating embeddings...")
    embedding_generator = EmbeddingGenerator(config.EMBEDDING_MODEL)
    processed_chunks, embeddings = embedding_generator.process_chunks_for_embedding(all_chunks)

    #Memory cleanup before FAISS
    logger.info("Step 3.5: Preparing for FAISS index creation...")
    import gc
    gc.collect()  # Force garbage collection

    # Ensure embeddings are in the right format
    if hasattr(embeddings, 'cpu'):
        embeddings = embeddings.cpu().numpy()  # Move from GPU to CPU if needed
    # Step 4: Create FAISS index
    logger.info("Step 4: Creating FAISS index...")
    faiss_indexer = FAISSIndexer(
        dimension=config.EMBEDDING_DIMENSION,
        index_type=config.FAISS_INDEX_TYPE
    )

    faiss_indexer.create_index(embeddings, processed_chunks)

    # Step 5: Save index and metadata
    logger.info("Step 5: Saving index and metadata...")
    faiss_indexer.save_index(config.FAISS_INDEX_FILE, config.METADATA_FILE)

    # Print statistics
    stats = faiss_indexer.get_index_stats()
    logger.info(f"RAG system built successfully!")
    logger.info(f"Statistics: {json.dumps(stats, indent=2)}")

    return True

def test_rag_system():
    """Test the built RAG system."""
    config = Config()

    logger.info("Testing RAG system...")

    # Load RAG system
    rag_processor = RAGQueryProcessor(config)
    rag_processor.load_rag_system()

    # Test queries
    test_queries = [
        "What are the nutritional benefits of dandelion?",
        "How to identify edible wild berries?",
        "Are there any toxic plants that look like edible ones?",
        "What plants are safe to eat in spring?",
        "How to prepare wild mushrooms safely?"
    ]

    for query in test_queries:
        logger.info(f"\n{'='*50}")
        logger.info(f"Query: {query}")
        logger.info(f"{'='*50}")

        result = rag_processor.process_query(query)

        logger.info(f"Found {result['total_results']} relevant chunks")
        for i, chunk in enumerate(result['chunks'][:3], 1):  # Show top 3
            logger.info(f"\nChunk {i}:")
            logger.info(f"Source: {chunk['source']}")
            logger.info(f"Section: {chunk['section']}")
            logger.info(f"Plant: {chunk['plant_name']}")
            logger.info(f"Similarity: {chunk['similarity_score']:.3f}")
            logger.info(f"Text: {chunk['text'][:200]}...")

if __name__ == "__main__":
    # Build the RAG system
    success = build_rag_system()

    if success:
        # Test the system
        test_rag_system()
    else:
        logger.error("Failed to build RAG system")
