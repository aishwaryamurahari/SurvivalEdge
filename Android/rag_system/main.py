import logging
from pathlib import Path
import json

from config import Config
from json_processor import JSONProcessor
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
    """Build the complete RAG system from JSON data."""
    config = Config()

    logger.info("Starting RAG system build process...")

    # Step 1: Process JSON files
    logger.info("Step 1: Processing JSON files...")
    all_chunks = []

    for json_file in config.JSON_FILES:
        json_path = config.JSON_DIR / json_file
        if json_path.exists():
            json_processor = JSONProcessor(json_path)
            chunks = json_processor.process_json_to_chunks()
            all_chunks.extend(chunks)
            logger.info(f"Processed {len(chunks)} chunks from {json_file}")
        else:
            logger.warning(f"JSON file not found: {json_path}")

    if not all_chunks:
        logger.error("No JSON data processed successfully. Exiting.")
        return False

    logger.info(f"Total chunks created: {len(all_chunks)}")

    # Save chunks
    json_processor.save_chunks(all_chunks, config.CHUNKS_FILE)

    # Step 2: Generate embeddings
    logger.info("Step 2: Generating embeddings...")
    embedding_generator = EmbeddingGenerator(config.EMBEDDING_MODEL)
    processed_chunks, embeddings = embedding_generator.process_chunks_for_embedding(all_chunks, batch_size=config.EMBEDDING_BATCH_SIZE)

    # Memory cleanup before FAISS
    logger.info("Step 2.5: Preparing for FAISS index creation...")
    import gc
    gc.collect()  # Force garbage collection

    # Ensure embeddings are in the right format
    if hasattr(embeddings, 'cpu'):
        embeddings = embeddings.cpu().numpy()  # Move from GPU to CPU if needed

    # Step 3: Create FAISS index
    logger.info("Step 3: Creating FAISS index...")
    faiss_indexer = FAISSIndexer(
        dimension=config.EMBEDDING_DIMENSION,
        index_type=config.FAISS_INDEX_TYPE
    )

    faiss_indexer.create_index(embeddings, processed_chunks)

    # Step 4: Save index and metadata
    logger.info("Step 4: Saving index and metadata...")
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
        "What is Bryum argenteum and where can it be found?",
        "How to identify different types of moss?",
        "What are the medicinal properties of moss?",
        "Which moss species are found in urban areas?",
        "What is the taxonomy of Leucolepis acanthoneura?"
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
            logger.info(f"Scientific Name: {chunk.get('scientific_name', 'Unknown')}")
            logger.info(f"Common Name: {chunk.get('common_name', 'Unknown')}")
            logger.info(f"Family: {chunk.get('family', 'Unknown')}")
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
