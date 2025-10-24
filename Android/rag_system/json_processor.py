import json
import re
from pathlib import Path
from typing import List, Dict
import logging

logger = logging.getLogger(__name__)

class JSONProcessor:
    def __init__(self, json_file_path: Path):
        self.json_file_path = json_file_path

    def load_json_data(self) -> List[Dict]:
        """Load JSON data from file."""
        try:
            with open(self.json_file_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            logger.info(f"Loaded {len(data)} entries from {self.json_file_path}")
            return data
        except Exception as e:
            logger.error(f"Error loading JSON file {self.json_file_path}: {str(e)}")
            return []

    def filter_valid_entries(self, entries: List[Dict]) -> List[Dict]:
        """Filter out entries with empty content or errors."""
        valid_entries = []

        for entry in entries:
            # Skip entries with errors
            if entry.get('error', '').strip():
                logger.debug(f"Skipping entry with error: {entry.get('scientific_name', 'Unknown')}")
                continue

            # Skip entries with empty content and summary
            content = entry.get('content', '').strip()
            summary = entry.get('summary', '').strip()

            if not content and not summary:
                logger.debug(f"Skipping entry with empty content: {entry.get('scientific_name', 'Unknown')}")
                continue

            valid_entries.append(entry)

        logger.info(f"Filtered to {len(valid_entries)} valid entries out of {len(entries)} total")
        return valid_entries

    def create_chunk_from_json_entry(self, entry: Dict, chunk_id: int) -> Dict:
        """Create a chunk from JSON entry with all metadata."""

        # Get the main text content (prefer content over summary)
        text = entry.get('content', '').strip()
        if not text:
            text = entry.get('summary', '').strip()

        # Determine section type based on content
        section = self._determine_section(text)

        # Create the chunk with all metadata
        chunk = {
            # Core fields (required by system)
            'chunk_id': chunk_id,
            'text': text,
            'source': self.json_file_path.name,
            'plant_name': entry.get('scientific_name', 'Unknown'),
            'section': section,
            'length': len(text),
            'word_count': len(text.split()),

            # JSON-specific fields
            'scientific_name': entry.get('scientific_name'),
            'common_name': entry.get('common_name'),
            'family': entry.get('family'),
            'genus': entry.get('genus'),
            'order': entry.get('order'),
            'class': entry.get('class'),
            'phylum': entry.get('phylum'),
            'wikipedia_url': entry.get('wikipedia_url'),
            'categories': entry.get('categories', []),
            'summary': entry.get('summary'),
            'images': entry.get('images', []),
            'error': entry.get('error', '')
        }

        return chunk

    def _determine_section(self, text: str) -> str:
        """Determine the section type based on content."""
        text_lower = text.lower()

        # Check for specific content types
        if any(word in text_lower for word in ['nutrition', 'vitamin', 'mineral', 'calorie', 'nutrient']):
            return "Nutrition"
        elif any(word in text_lower for word in ['warning', 'toxic', 'poison', 'danger', 'avoid', 'harmful']):
            return "Safety"
        elif any(word in text_lower for word in ['identify', 'identification', 'look', 'appearance', 'leaf', 'flower', 'description']):
            return "Identification"
        elif any(word in text_lower for word in ['cook', 'prepare', 'recipe', 'eat', 'consumption', 'edible']):
            return "Preparation"
        elif any(word in text_lower for word in ['habitat', 'distribution', 'grow', 'found', 'location']):
            return "Habitat"
        elif any(word in text_lower for word in ['reproduction', 'spore', 'seed', 'flowering']):
            return "Reproduction"
        elif any(word in text_lower for word in ['use', 'usage', 'medicinal', 'traditional', 'practical']):
            return "Uses"
        else:
            return "General"

    def process_json_to_chunks(self) -> List[Dict]:
        """Process JSON file and convert entries to chunks."""
        logger.info(f"Processing JSON file: {self.json_file_path}")

        # Load JSON data
        entries = self.load_json_data()
        if not entries:
            logger.error("No data loaded from JSON file")
            return []

        # Filter valid entries
        valid_entries = self.filter_valid_entries(entries)
        if not valid_entries:
            logger.error("No valid entries found in JSON file")
            return []

        # Convert entries to chunks
        chunks = []
        for i, entry in enumerate(valid_entries):
            chunk = self.create_chunk_from_json_entry(entry, i)
            chunks.append(chunk)

        logger.info(f"Created {len(chunks)} chunks from JSON data")
        return chunks

    def save_chunks(self, chunks: List[Dict], output_file: Path):
        """Save chunks to JSON file."""
        try:
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(chunks, f, indent=2, ensure_ascii=False)
            logger.info(f"Saved {len(chunks)} chunks to {output_file}")
        except Exception as e:
            logger.error(f"Error saving chunks to {output_file}: {str(e)}")

    def get_processing_stats(self, chunks: List[Dict]) -> Dict:
        """Get statistics about processed chunks."""
        if not chunks:
            return {}

        # Count by section
        section_counts = {}
        for chunk in chunks:
            section = chunk.get('section', 'Unknown')
            section_counts[section] = section_counts.get(section, 0) + 1

        # Count by family
        family_counts = {}
        for chunk in chunks:
            family = chunk.get('family', 'Unknown')
            family_counts[family] = family_counts.get(family, 0) + 1

        return {
            'total_chunks': len(chunks),
            'section_distribution': section_counts,
            'family_distribution': family_counts,
            'avg_text_length': sum(chunk.get('length', 0) for chunk in chunks) / len(chunks),
            'avg_word_count': sum(chunk.get('word_count', 0) for chunk in chunks) / len(chunks)
        }
