import re
from typing import List, Dict
from pathlib import Path
import json
import logging

logger = logging.getLogger(__name__)

class TextChunker:
    def __init__(self, chunk_size: int = 400, chunk_overlap: int = 50):
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap

    def split_text_into_chunks(self, text: str, source: str) -> List[Dict]:
        """Split text into overlapping chunks with metadata."""
        # Split by paragraphs first
        paragraphs = self._split_by_paragraphs(text)

        chunks = []
        chunk_id = 0

        for para in paragraphs:
            if len(para) <= self.chunk_size:
                # Paragraph fits in one chunk
                chunks.append(self._create_chunk(para, source, chunk_id))
                chunk_id += 1
            else:
                # Split paragraph into multiple chunks
                para_chunks = self._split_long_paragraph(para, source, chunk_id)
                chunks.extend(para_chunks)
                chunk_id += len(para_chunks)

        logger.info(f"Created {len(chunks)} chunks from {source}")
        return chunks

    def _split_by_paragraphs(self, text: str) -> List[str]:
        """Split text into paragraphs."""
        # Split by double newlines or page breaks
        paragraphs = re.split(r'\n\s*\n|--- Page \d+ ---', text)
        return [p.strip() for p in paragraphs if p.strip()]

    def _split_long_paragraph(self, text: str, source: str, start_chunk_id: int) -> List[Dict]:
        """Split long paragraph into overlapping chunks."""
        chunks = []
        start = 0
        chunk_id = start_chunk_id

        while start < len(text):
            end = start + self.chunk_size

            # Try to break at sentence boundary
            if end < len(text):
                # Look for sentence endings within the last 50 characters
                sentence_end = text.rfind('.', start, end)
                if sentence_end > start + self.chunk_size * 0.7:  # At least 70% of chunk size
                    end = sentence_end + 1

            chunk_text = text[start:end].strip()
            if chunk_text:
                chunks.append(self._create_chunk(chunk_text, source, chunk_id))
                chunk_id += 1

            # Move start position with overlap
            start = end - self.chunk_overlap
            if start >= len(text):
                break

        return chunks

    def _create_chunk(self, text: str, source: str, chunk_id: int) -> Dict:
        """Create a chunk dictionary with metadata."""
        # Extract plant name if mentioned
        plant_name = self._extract_plant_name(text)

        # Determine section type
        section = self._determine_section(text)

        return {
            'chunk_id': chunk_id,
            'text': text,
            'source': source,
            'plant_name': plant_name,
            'section': section,
            'length': len(text),
            'word_count': len(text.split())
        }

    def _extract_plant_name(self, text: str) -> str:
        """Extract plant name from text chunk."""
        # Common plant names to look for
        plant_keywords = [
            'dandelion', 'nettle', 'plantain', 'chickweed', 'purslane',
            'wild garlic', 'elderberry', 'blackberry', 'raspberry',
            'mushroom', 'chanterelle', 'morel', 'porcini'
        ]

        text_lower = text.lower()
        for plant in plant_keywords:
            if plant in text_lower:
                return plant.title()

        return "General"

    def _determine_section(self, text: str) -> str:
        """Determine the section type based on content."""
        text_lower = text.lower()

        if any(word in text_lower for word in ['nutrition', 'vitamin', 'mineral', 'calorie']):
            return "Nutrition"
        elif any(word in text_lower for word in ['warning', 'toxic', 'poison', 'danger', 'avoid']):
            return "Safety"
        elif any(word in text_lower for word in ['identify', 'look', 'appearance', 'leaf', 'flower']):
            return "Identification"
        elif any(word in text_lower for word in ['cook', 'prepare', 'recipe', 'eat']):
            return "Preparation"
        else:
            return "General"

    def save_chunks(self, chunks: List[Dict], output_file: Path):
        """Save chunks to JSON file."""
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(chunks, f, indent=2, ensure_ascii=False)
        logger.info(f"Saved {len(chunks)} chunks to {output_file}")
