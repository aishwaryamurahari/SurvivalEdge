import numpy as np
from transformers import AutoTokenizer, AutoModel  # ← Add this
import torch  # ← Add this
from typing import List, Dict
import logging
from pathlib import Path
import json

logger = logging.getLogger(__name__)

class EmbeddingGenerator:
    def __init__(self, model_name: str = "all-MiniLM-L6-v2"):
        self.model_name = model_name
        self.model = None
        self.tokenizer = None  # ← Add this
        self.embedding_dimension = 384  # for all-MiniLM-L6-v2

    def load_model(self):
        """Load the transformer model and tokenizer."""
        try:
            logger.info(f"Loading embedding model: {self.model_name}")
            self.tokenizer = AutoTokenizer.from_pretrained(f"sentence-transformers/{self.model_name}")
            self.model = AutoModel.from_pretrained(f"sentence-transformers/{self.model_name}")
            logger.info("Model loaded successfully")
        except Exception as e:
            logger.error(f"Error loading model: {str(e)}")
            raise

    def generate_embeddings(self, texts: List[str]) -> np.ndarray:
        """Generate embeddings for a list of texts."""
        if self.model is None:
            self.load_model()

        try:
            logger.info(f"Generating embeddings for {len(texts)} texts")
            # Tokenize texts
            inputs = self.tokenizer(texts, padding=True, truncation=True, return_tensors="pt")

            # Generate embeddings
            with torch.no_grad():
                outputs = self.model(**inputs)
                embeddings = outputs.last_hidden_state.mean(dim=1)  # Mean pooling

            logger.info(f"Generated embeddings with shape: {embeddings.shape}")
            return embeddings.numpy()
        except Exception as e:
            logger.error(f"Error generating embeddings: {str(e)}")
            raise

    def generate_embedding_for_query(self, query: str) -> np.ndarray:
        """Generate embedding for a single query."""
        if self.model is None:
            self.load_model()

        try:
            # Tokenize query
            inputs = self.tokenizer([query], padding=True, truncation=True, return_tensors="pt")

            # Generate embedding
            with torch.no_grad():
                outputs = self.model(**inputs)
                embedding = outputs.last_hidden_state.mean(dim=1)  # Mean pooling

            return embedding[0].numpy()  # Return single embedding
        except Exception as e:
            logger.error(f"Error generating query embedding: {str(e)}")
            raise

    def process_chunks_for_embedding(self, chunks: List[Dict]) -> tuple:
        """Process chunks and generate embeddings."""
        texts = [chunk['text'] for chunk in chunks]
        embeddings = self.generate_embeddings(texts)

        # Add embedding info to chunks
        for i, chunk in enumerate(chunks):
            chunk['embedding_index'] = i

        return chunks, embeddings

    def save_embeddings(self, embeddings: np.ndarray, output_file: Path):
        """Save embeddings to file."""
        np.save(output_file, embeddings)
        logger.info(f"Saved embeddings to {output_file}")

    def load_embeddings(self, input_file: Path) -> np.ndarray:
        """Load embeddings from file."""
        embeddings = np.load(input_file)
        logger.info(f"Loaded embeddings with shape: {embeddings.shape}")
        return embeddings
