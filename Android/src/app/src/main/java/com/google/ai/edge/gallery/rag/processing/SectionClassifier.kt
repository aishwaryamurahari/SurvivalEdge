package com.google.ai.edge.gallery.rag.processing

class SectionClassifier {
    fun determineSection(text: String): String {
        val textLower = text.lowercase()

        return when {
            textLower.containsAny(listOf("nutrition", "vitamin", "mineral", "calorie", "nutrient")) -> "Nutrition"
            textLower.containsAny(listOf("warning", "toxic", "poison", "danger", "avoid", "harmful")) -> "Safety"
            textLower.containsAny(listOf("identify", "identification", "look", "appearance", "leaf", "flower", "description")) -> "Identification"
            textLower.containsAny(listOf("cook", "prepare", "recipe", "eat", "consumption", "edible")) -> "Preparation"
            textLower.containsAny(listOf("habitat", "distribution", "grow", "found", "location")) -> "Habitat"
            textLower.containsAny(listOf("reproduction", "spore", "seed", "flowering")) -> "Reproduction"
            textLower.containsAny(listOf("use", "usage", "medicinal", "traditional", "practical")) -> "Uses"
            else -> "General"
        }
    }

    private fun String.containsAny(keywords: List<String>): Boolean {
        return keywords.any { this.contains(it) }
    }
}
