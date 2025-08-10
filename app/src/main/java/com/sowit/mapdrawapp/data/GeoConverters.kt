package com.sowit.mapdrawapp.data
object GeoConverters {
    fun toStorage(points: List<Pair<Double, Double>>): String =
        points.joinToString(";") { "${it.first},${it.second}" }

    fun fromStorage(s: String): List<Pair<Double, Double>> =
        s.split(";").filter { it.isNotBlank() }.map {
            val (a, b) = it.split(",")
            a.toDouble() to b.toDouble()
        }

    fun centroidOf(points: List<Pair<Double, Double>>): Pair<Double, Double> {
        val (sx, sy) = points.fold(0.0 to 0.0) { acc, p -> (acc.first + p.first) to (acc.second + p.second) }
        val n = points.size.coerceAtLeast(1)
        return (sx / n) to (sy / n)
    }
}