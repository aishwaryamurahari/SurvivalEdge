import logging
from pathlib import Path
import json

from config import Config
from rag_query_processor import RAGQueryProcessor

# Setup logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def interactive_test():
    """Interactive testing of the RAG system."""
    config = Config()

    # Load RAG system
    rag_processor = RAGQueryProcessor(config)
    rag_processor.load_rag_system()

    print("RAG System Interactive Test")
    print("=" * 40)
    print("Type 'quit' to exit")
    print()

    while True:
        query = input("Enter your query: ").strip()

        if query.lower() == 'quit':
            break

        if not query:
            continue

        print(f"\nProcessing query: '{query}'")
        print("-" * 50)

        result = rag_processor.process_query(query)

        print(f"Found {result['total_results']} relevant chunks")
        print()

        for i, chunk in enumerate(result['chunks'], 1):
            print(f"Result {i}:")
            print(f"  Source: {chunk['source']}")
            print(f"  Section: {chunk['section']}")
            print(f"  Scientific Name: {chunk.get('scientific_name', 'Unknown')}")
            print(f"  Common Name: {chunk.get('common_name', 'Unknown')}")
            print(f"  Family: {chunk.get('family', 'Unknown')}")
            print(f"  Similarity: {chunk['similarity_score']:.3f}")
            print(f"  Text: {chunk['text'][:300]}...")
            print()

        print("=" * 50)
        print()

def batch_test():
    """Run a batch of test queries."""
    config = Config()

    # Load RAG system
    rag_processor = RAGQueryProcessor(config)
    rag_processor.load_rag_system()

    # Test queries
    test_queries = [
        "What is Bryum argenteum and where can it be found?",
        "How to identify different types of moss?",
        "What are the medicinal properties of moss?",
        "Which moss species are found in urban areas?",
        "What is the taxonomy of Leucolepis acanthoneura?",
        "Tell me about Plagiomnium cuspidatum uses",
        "What are the characteristics of badge moss?",
        "How to identify moss by family?",
        "What are the habitat requirements for moss?",
        "Safety considerations for moss identification"
    ]

    print("RAG System Batch Test")
    print("=" * 50)

    for i, query in enumerate(test_queries, 1):
        print(f"\nTest {i}: {query}")
        print("-" * 30)

        result = rag_processor.process_query(query)

        print(f"Results: {result['total_results']} chunks found")

        if result['chunks']:
            # Show top result
            top_chunk = result['chunks'][0]
            print(f"Top match: {top_chunk['source']} - {top_chunk['section']}")
            print(f"Scientific Name: {top_chunk.get('scientific_name', 'Unknown')}")
            print(f"Common Name: {top_chunk.get('common_name', 'Unknown')}")
            print(f"Family: {top_chunk.get('family', 'Unknown')}")
            print(f"Similarity: {top_chunk['similarity_score']:.3f}")
            print(f"Text preview: {top_chunk['text'][:150]}...")
        else:
            print("No relevant chunks found")

        print()

if __name__ == "__main__":
    import sys

    if len(sys.argv) > 1 and sys.argv[1] == "batch":
        batch_test()
    else:
        interactive_test()
