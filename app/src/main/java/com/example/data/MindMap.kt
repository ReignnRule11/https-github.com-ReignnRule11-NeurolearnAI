package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MindMapNode(
    val id: String,
    val label: String,
    val type: String, // "root", "category", "concept", "card_detail"
    val description: String
)

@JsonClass(generateAdapter = true)
data class MindMapEdge(
    val from: String,
    val to: String,
    val label: String? = null
)

@JsonClass(generateAdapter = true)
data class MindMapGraph(
    val centralTheme: String?,
    val nodes: List<MindMapNode>,
    val edges: List<MindMapEdge>
)
