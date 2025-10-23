import faiss
import numpy as np
import json
from pathlib import Path
from typing import List, Dict, Tuple
import logging
import torch

logger = logging.getLogger(__name__)

class FAISSIndexer:
    def __init__(self, dimension: int = 384, index_type: str = "IndexFlatIP"):
        self.dimension = dimension
        self.index_type = index_type
        self.index: faiss.Index | None = None
        self.metadata: List[Dict] = []

    # ---------------------------
    # Internal utilities
    # ---------------------------
    def _validate_embeddings(self, embeddings: np.ndarray) -> np.ndarray:
        """Validate and clean embeddings before index creation."""
        if embeddings is None:
            raise ValueError("Embeddings is None")

        if not isinstance(embeddings, np.ndarray):
            embeddings = np.array(embeddings)

        if embeddings.ndim != 2:
            raise ValueError(f"Embeddings must be 2D (N, D). Got shape {embeddings.shape}")

        if embeddings.shape[0] == 0:
            raise ValueError("Embeddings array is empty (0 rows)")

        # Replace NaN/Inf BEFORE normalization
        if np.any(np.isnan(embeddings)) or np.any(np.isinf(embeddings)):
            logger.warning("Found NaN/Inf in embeddings; replacing with zeros")
            embeddings = np.nan_to_num(embeddings, nan=0.0, posinf=0.0, neginf=0.0)

        # Ensure contiguous float32
        embeddings = np.ascontiguousarray(embeddings, dtype=np.float32)

        # Dimension check
        if embeddings.shape[1] != self.dimension:
            raise ValueError(
                f"Embedding dimension mismatch: expected {self.dimension}, got {embeddings.shape[1]}"
            )

        return embeddings

    def _normalize_in_place_safe(self, X: np.ndarray) -> None:
        """Normalize to unit L2 length, guarding against zero-norm rows and post-NaNs."""
        # Guard 1: detect zero-norm rows to avoid NaNs from normalize_L2
        norms = np.linalg.norm(X, axis=1, keepdims=True)
        zero_mask = (norms == 0.0)
        if np.any(zero_mask):
            count = int(zero_mask.sum())
            logger.warning(f"Detected {count} zero-norm vectors; adding tiny epsilon noise to avoid NaNs")
            eps = 1e-12
            # Put tiny noise in exactly-zero rows
            X[zero_mask.flatten()] = eps

        # Normalize in-place
        faiss.normalize_L2(X)

        # Guard 2: ensure no NaN/Inf AFTER normalization
        if np.any(np.isnan(X)) or np.any(np.isinf(X)):
            raise ValueError("NaN/Inf detected AFTER normalize_L2 — check your input vectors.")

    # ---------------------------
    # Public API
    # ---------------------------
    def create_index(self, embeddings: np.ndarray, chunks: List[Dict]) -> faiss.Index:
        """Create FAISS index from embeddings."""
        try:
            logger.info("Validating embeddings…")


            if isinstance(embeddings, torch.Tensor):
                logger.info("Converting torch.Tensor to NumPy array…")
                embeddings = embeddings.detach().cpu().numpy()
            embeddings = np.ascontiguousarray(embeddings, dtype=np.float32)

            embeddings = self._validate_embeddings(embeddings)

            # Normalize for cosine/IP flow; for L2 this is fine too but optional.
            logger.info("Normalizing embeddings (L2)…")
            self._normalize_in_place_safe(embeddings)

            # Create index
            logger.info(f"Building index type: {self.index_type}")
            if self.index_type == "IndexFlatL2":
                self.index = faiss.IndexFlatL2(self.dimension)
            elif self.index_type == "IndexFlatIP":
                self.index = faiss.IndexFlatIP(self.dimension)
            else:
                raise ValueError(f"Unsupported index type: {self.index_type}")

            # Optional sanity check: metadata length
            if len(chunks) != len(embeddings):
                logger.warning(
                    f"Metadata length {len(chunks)} != embeddings rows {len(embeddings)}"
                )

            # Add in batches with strict checks
            batch_size = 1000
            n = len(embeddings)
            total_batches = (n - 1) // batch_size + 1
            for b, i in enumerate(range(0, n, batch_size), start=1):
                batch = embeddings[i:i + batch_size]
                if batch.ndim != 2 or batch.shape[1] != self.dimension:
                    raise ValueError(
                        f"Bad batch shape {batch.shape}; expected (*, {self.dimension})"
                    )
                # Ensure dtype float32
                if batch.dtype != np.float32:
                    batch = batch.astype(np.float32, copy=False)

                self.index.add(batch)
                logger.info(f"Added batch {b}/{total_batches} (rows {i}..{min(i+batch_size, n)-1})")

            self.metadata = list(chunks) if chunks is not None else []
            logger.info(f"Created FAISS index with {self.index.ntotal} vectors")
            return self.index

        except Exception as e:
            logger.error(f"Error creating FAISS index: {e}")
            raise

    def search(self, query_embedding: np.ndarray, top_k: int = 5) -> Tuple[np.ndarray, np.ndarray]:
        """Search for similar vectors in the index."""
        if self.index is None:
            raise ValueError("Index not created. Call create_index first.")
        if self.index.ntotal == 0:
            raise ValueError("Index is empty. Add vectors before searching.")

        try:
            # Ensure shape (1, D), float32
            if not isinstance(query_embedding, np.ndarray):
                query_embedding = np.array(query_embedding)
            query_embedding = query_embedding.reshape(1, -1).astype('float32', copy=False)

            if query_embedding.shape[1] != self.dimension:
                raise ValueError(
                    f"Query dim mismatch: expected {self.dimension}, got {query_embedding.shape[1]}"
                )

            # Normalize query if using IP (cosine-like)
            if self.index_type == "IndexFlatIP":
                self._normalize_in_place_safe(query_embedding)

            # Search
            scores, indices = self.index.search(query_embedding, top_k)

            # Convert L2 distance to similarity in [0, 1)-ish (monotonic transform)
            if self.index_type == "IndexFlatL2":
                scores = 1.0 / (1.0 + scores)

            return scores[0], indices[0]

        except Exception as e:
            logger.error(f"Error searching index: {e}")
            raise

    def get_chunks_by_indices(self, indices: np.ndarray) -> List[Dict]:
        """Get chunk metadata for given indices."""
        chunks_out: List[Dict] = []
        if indices is None:
            return chunks_out
        for idx in indices:
            if 0 <= int(idx) < len(self.metadata):
                chunks_out.append(self.metadata[int(idx)])
        return chunks_out

    def save_index(self, index_file: Path, metadata_file: Path):
        """Save FAISS index and metadata to files."""
        try:
            if self.index is None:
                raise ValueError("No index to save. Create/load an index first.")
            faiss.write_index(self.index, str(index_file))

            with open(metadata_file, 'w', encoding='utf-8') as f:
                json.dump(self.metadata, f, indent=2, ensure_ascii=False)

            logger.info(f"Saved index to {index_file} and metadata to {metadata_file}")
        except Exception as e:
            logger.error(f"Error saving index: {e}")
            raise

    def load_index(self, index_file: Path, metadata_file: Path):
        """Load FAISS index and metadata from files."""
        try:
            self.index = faiss.read_index(str(index_file))
            with open(metadata_file, 'r', encoding='utf-8') as f:
                self.metadata = json.load(f)

            # Optional: sanity check
            if not isinstance(self.metadata, list):
                raise ValueError("Metadata file must contain a JSON list")

            logger.info(
                f"Loaded index with {self.index.ntotal} vectors and {len(self.metadata)} metadata entries"
            )
        except Exception as e:
            logger.error(f"Error loading index: {e}")
            raise

    def get_index_stats(self) -> Dict:
        """Get simple statistics about the index."""
        if self.index is None:
            return {"error": "Index not loaded"}
        return {
            "total_vectors": self.index.ntotal,
            "dimension": self.dimension,
            "index_type": self.index_type,
            "metadata_entries": len(self.metadata),
        }
