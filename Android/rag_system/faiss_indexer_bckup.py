import faiss
import numpy as np
import json
from pathlib import Path
from typing import List, Dict, Tuple
import logging

logger = logging.getLogger(__name__)

class FAISSIndexer:
    def __init__(self, dimension: int = 384, index_type: str = "IndexFlatIP"):
        self.dimension = dimension
        self.index_type = index_type
        self.index = None
        self.metadata = []


    def _validate_embeddings(self, embeddings: np.ndarray) -> np.ndarray:
        """Validate and clean embeddings before index creation."""
        # Check for NaN or infinite values
        if np.any(np.isnan(embeddings)) or np.any(np.isinf(embeddings)):
            logger.warning("Found NaN or infinite values in embeddings, replacing with zeros")
            embeddings = np.nan_to_num(embeddings, nan=0.0, posinf=0.0, neginf=0.0)

        # Ensure embeddings are contiguous and float32
        embeddings = np.ascontiguousarray(embeddings, dtype=np.float32)

        # Check embedding shape
        if embeddings.shape[1] != self.dimension:
            raise ValueError(f"Embedding dimension mismatch: expected {self.dimension}, got {embeddings.shape[1]}")

        return embeddings

    def create_index(self, embeddings: np.ndarray, chunks: List[Dict]) -> faiss.Index:
        """Create FAISS index from embeddings."""
        try:
            # Validate embeddings first
            embeddings = self._validate_embeddings(embeddings)

            # Normalize embeddings for cosine similarity
            faiss.normalize_L2(embeddings)

            # Create index with error handling
            if self.index_type == "IndexFlatL2":
                self.index = faiss.IndexFlatL2(self.dimension)
            elif self.index_type == "IndexFlatIP":
                self.index = faiss.IndexFlatIP(self.dimension)
            else:
                raise ValueError(f"Unsupported index type: {self.index_type}")

            # Add embeddings in smaller batches to avoid memory issues
            batch_size = 1000
            for i in range(0, len(embeddings), batch_size):
                batch = embeddings[i:i + batch_size]
                self.index.add(batch.astype('float32'))
                logger.info(f"Added batch {i//batch_size + 1}/{(len(embeddings)-1)//batch_size + 1}")

            # Store metadata
            self.metadata = chunks.copy()

            logger.info(f"Created FAISS index with {self.index.ntotal} vectors")
            return self.index

        except Exception as e:
            logger.error(f"Error creating FAISS index: {str(e)}")
            raise

    # def search(self, query_embedding: np.ndarray, top_k: int = 5) -> Tuple[np.ndarray, np.ndarray]:
    #     """Search for similar vectors in the index."""
    #     if self.index is None:
    #         raise ValueError("Index not created. Call create_index first.")

    #     try:
    #         # Normalize query embedding
    #         query_embedding = query_embedding.reshape(1, -1)
    #         faiss.normalize_L2(query_embedding)

    #         # Search
    #         scores, indices = self.index.search(query_embedding.astype('float32'), top_k)

    #         return scores[0], indices[0]  # Return first (and only) query results

    #     except Exception as e:
    #         logger.error(f"Error searching index: {str(e)}")
    #         raise
    def search(self, query_embedding: np.ndarray, top_k: int = 5) -> Tuple[np.ndarray, np.ndarray]:
        """Search for similar vectors in the index."""
        if self.index is None:
            raise ValueError("Index not created. Call create_index first.")

        try:
            # For L2 distance, no need to normalize query
            query_embedding = query_embedding.reshape(1, -1).astype('float32')

            # Search
            scores, indices = self.index.search(query_embedding, top_k)

            # Convert L2 distances to similarity scores (lower distance = higher similarity)
            if self.index_type == "IndexFlatL2":
                scores = 1.0 / (1.0 + scores)  # Convert distance to similarity

            return scores[0], indices[0]

        except Exception as e:
            logger.error(f"Error searching index: {str(e)}")
            raise


    def get_chunks_by_indices(self, indices: np.ndarray) -> List[Dict]:
        """Get chunk metadata for given indices."""
        chunks = []
        for idx in indices:
            if 0 <= idx < len(self.metadata):
                chunks.append(self.metadata[idx])
        return chunks

    def save_index(self, index_file: Path, metadata_file: Path):
        """Save FAISS index and metadata to files."""
        try:
            # Save FAISS index
            faiss.write_index(self.index, str(index_file))

            # Save metadata
            with open(metadata_file, 'w', encoding='utf-8') as f:
                json.dump(self.metadata, f, indent=2, ensure_ascii=False)

            logger.info(f"Saved index to {index_file} and metadata to {metadata_file}")

        except Exception as e:
            logger.error(f"Error saving index: {str(e)}")
            raise

    def load_index(self, index_file: Path, metadata_file: Path):
        """Load FAISS index and metadata from files."""
        try:
            # Load FAISS index
            self.index = faiss.read_index(str(index_file))

            # Load metadata
            with open(metadata_file, 'r', encoding='utf-8') as f:
                self.metadata = json.load(f)

            logger.info(f"Loaded index with {self.index.ntotal} vectors and {len(self.metadata)} metadata entries")

        except Exception as e:
            logger.error(f"Error loading index: {str(e)}")
            raise

    def get_index_stats(self) -> Dict:
        """Get statistics about the index."""
        if self.index is None:
            return {"error": "Index not loaded"}

        return {
            "total_vectors": self.index.ntotal,
            "dimension": self.dimension,
            "index_type": self.index_type,
            "metadata_entries": len(self.metadata)
        }
