import numpy as np
from typing import List, Dict, Tuple
import logging
from pathlib import Path
import json

from embedding_generator import EmbeddingGenerator
from faiss_indexer import FAISSIndexer
from config import Config

logger = logging.getLogger(__name__)

class RAGQueryProcessor:
    def __init__(self, config: Config):
        self.config = config
        self.embedding_generator = EmbeddingGenerator(config.EMBEDDING_MODEL)
        self.faiss_indexer = FAISSIndexer(
            dimension=config.EMBEDDING_DIMENSION,
            index_type=config.FAISS_INDEX_TYPE
        )
        self.is_loaded = False

    def load_rag_system(self):
        """Load the RAG system (index and embedding model)."""
        try:
            # Load FAISS index and metadata
            self.faiss_indexer.load_index(
                self.config.FAISS_INDEX_FILE,
                self.config.METADATA_FILE
            )

            # Load embedding model
            self.embedding_generator.load_model()

            self.is_loaded = True
            logger.info("RAG system loaded successfully")

        except Exception as e:
            logger.error(f"Error loading RAG system: {str(e)}")
            raise

    def process_query(self, query: str, top_k: int = None) -> Dict:
        """Process a query and return relevant chunks."""
        if not self.is_loaded:
            self.load_rag_system()

        if top_k is None:
            top_k = self.config.TOP_K

        try:
            # Generate query embedding
            query_embedding = self.embedding_generator.generate_embedding_for_query(query)

            # Search for similar chunks
            scores, indices = self.faiss_indexer.search(query_embedding, top_k)

            # Get chunk metadata
            chunks = self.faiss_indexer.get_chunks_by_indices(indices)

            # Filter by similarity threshold
            filtered_results = []
            for i, (score, chunk) in enumerate(zip(scores, chunks)):
                if score >= self.config.SIMILARITY_THRESHOLD:
                    chunk['similarity_score'] = float(score)
                    chunk['rank'] = i + 1
                    filtered_results.append(chunk)

            # Prepare response
            response = {
                'query': query,
                'total_results': len(filtered_results),
                'chunks': filtered_results,
                'context': self._format_context(filtered_results)
            }

            logger.info(f"Processed query: '{query}' - Found {len(filtered_results)} relevant chunks")
            return response

        except Exception as e:
            logger.error(f"Error processing query: {str(e)}")
            return {
                'query': query,
                'error': str(e),
                'total_results': 0,
                'chunks': [],
                'context': ""
            }

    def _format_context(self, chunks: List[Dict]) -> str:
        """Format chunks into context for LLM."""
        if not chunks:
            return "No relevant information found."

        context_parts = []
        for i, chunk in enumerate(chunks, 1):
            context_part = f"[Source {i}: {chunk['source']} - {chunk['section']}]\n"
            context_part += f"{chunk['text']}\n"
            context_parts.append(context_part)

        return "\n".join(context_parts)

    def get_plant_specific_info(self, plant_name: str) -> Dict:
        """Get information about a specific plant."""
        query = f"information about {plant_name} plant"
        return self.process_query(query)

    def search_by_section(self, section: str, query: str = "") -> Dict:
        """Search within a specific section (Nutrition, Safety, etc.)."""
        if query:
            full_query = f"{query} {section}"
        else:
            full_query = section

        results = self.process_query(full_query)

        # Filter results by section
        filtered_chunks = [
            chunk for chunk in results['chunks']
            if chunk['section'].lower() == section.lower()
        ]

        results['chunks'] = filtered_chunks
        results['total_results'] = len(filtered_chunks)
        results['context'] = self._format_context(filtered_chunks)

        return results
