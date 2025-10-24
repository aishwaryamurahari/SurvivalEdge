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

    def generate_embeddings(self, texts: List[str], batch_size: int = 500) -> np.ndarray:
        """Generate embeddings for a list of texts in batches."""
        if self.model is None:
            self.load_model()

        try:
            logger.info(f"Generating embeddings for {len(texts)} texts in batches of {batch_size}")

            all_embeddings = []
            total_batches = (len(texts) + batch_size - 1) // batch_size

            # Process in batches
            for i in range(0, len(texts), batch_size):
                batch_texts = texts[i:i + batch_size]
                batch_num = i // batch_size + 1

                logger.info(f"Processing batch {batch_num}/{total_batches} ({len(batch_texts)} texts)")

                # Tokenize batch
                inputs = self.tokenizer(batch_texts, padding=True, truncation=True, return_tensors="pt")

                # Generate embeddings for batch
                with torch.no_grad():
                    outputs = self.model(**inputs)
                    batch_embeddings = outputs.last_hidden_state.mean(dim=1)  # Mean pooling

                all_embeddings.append(batch_embeddings.numpy())

                # Memory cleanup
                del inputs, outputs, batch_embeddings
                import gc
                gc.collect()

                # Optional: Add small delay to prevent system overload
                import time
                time.sleep(0.1)

            # Concatenate all batches
            final_embeddings = np.vstack(all_embeddings)
            logger.info(f"Generated embeddings with shape: {final_embeddings.shape}")
            return final_embeddings

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

    def process_chunks_for_embedding(self, chunks: List[Dict], batch_size: int = 500) -> tuple:
        """Process chunks and generate embeddings."""
        texts = [chunk['text'] for chunk in chunks]
        embeddings = self.generate_embeddings(texts, batch_size=batch_size)

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
