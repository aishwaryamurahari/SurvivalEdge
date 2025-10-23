import PyPDF2
import re
from pathlib import Path
from typing import List, Dict
import logging
import fitz  # PyMuPDF

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class PDFProcessor:
    def __init__(self, pdf_dir: Path):
        self.pdf_dir = pdf_dir


    def extract_text_from_pdf(self, pdf_path: Path) -> str:
        """Extract text from a single PDF file using PyMuPDF."""
        try:
            doc = fitz.open(pdf_path)
            text = ""

            for page_num in range(len(doc)):
                page = doc.load_page(page_num)
                page_text = page.get_text()

                if page_text:
                    # Clean up the text
                    page_text = self._clean_text(page_text)
                    text += f"\n--- Page {page_num + 1} ---\n{page_text}\n"

            doc.close()
            logger.info(f"Extracted text from {pdf_path.name}: {len(text)} characters")
            return text

        except Exception as e:
            logger.error(f"Error extracting text from {pdf_path}: {str(e)}")
            return ""

    def _clean_text(self, text: str) -> str:
        """Clean and normalize extracted text."""
        # Remove extra whitespace
        text = re.sub(r'\s+', ' ', text)
        # Remove special characters but keep basic punctuation
        text = re.sub(r'[^\w\s\.\,\!\?\;\:\-\(\)]', '', text)
        # Remove page numbers and headers/footers
        text = re.sub(r'^\d+\s*$', '', text, flags=re.MULTILINE)
        return text.strip()

    def process_all_pdfs(self, pdf_files: List[str]) -> Dict[str, str]:
        """Process all PDF files and return extracted text."""
        pdf_texts = {}

        for pdf_file in pdf_files:
            pdf_path = self.pdf_dir / pdf_file
            if pdf_path.exists():
                text = self.extract_text_from_pdf(pdf_path)
                if text:
                    pdf_texts[pdf_file] = text
                else:
                    logger.warning(f"No text extracted from {pdf_file}")
            else:
                logger.warning(f"PDF file not found: {pdf_file}")

        return pdf_texts

    def get_pdf_metadata(self, pdf_path: Path) -> Dict:
        """Extract metadata from PDF file."""
        try:
            with open(pdf_path, 'rb') as file:
                pdf_reader = PyPDF2.PdfReader(file)
                return {
                    'num_pages': len(pdf_reader.pages),
                    'title': pdf_reader.metadata.get('/Title', '') if pdf_reader.metadata else '',
                    'author': pdf_reader.metadata.get('/Author', '') if pdf_reader.metadata else '',
                    'file_size': pdf_path.stat().st_size
                }
        except Exception as e:
            logger.error(f"Error extracting metadata from {pdf_path}: {str(e)}")
            return {}
