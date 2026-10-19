package org.deckgl.compose.demo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import org.deckgl.compose.Position

data class City(val name: String, val position: Position, val population: Int)

/** Approximate city populations. */
val cities = listOf(
  City("Zürich", Position(8.5417, 47.3769), 421_000),
  City("Geneva", Position(6.1432, 46.2044), 203_000),
  City("Paris", Position(2.3522, 48.8566), 2_102_000),
  City("London", Position(-0.1276, 51.5072), 8_866_000),
  City("Berlin", Position(13.4050, 52.5200), 3_755_000),
  City("Madrid", Position(-3.7038, 40.4168), 3_332_000),
  City("Rome", Position(12.4964, 41.9028), 2_755_000),
  City("Vienna", Position(16.3738, 48.2082), 1_982_000),
  City("Amsterdam", Position(4.9041, 52.3676), 918_000),
  City("Brussels", Position(4.3517, 50.8503), 1_222_000),
  City("Milan", Position(9.1900, 45.4642), 1_371_000),
  City("Munich", Position(11.5820, 48.1351), 1_512_000),
  City("Prague", Position(14.4378, 50.0755), 1_357_000),
  City("Warsaw", Position(21.0122, 52.2297), 1_863_000),
  City("Copenhagen", Position(12.5683, 55.6761), 660_000),
  City("Stockholm", Position(18.0686, 59.3293), 984_000),
  City("Lisbon", Position(-9.1393, 38.7223), 545_000),
  City("Barcelona", Position(2.1734, 41.3851), 1_660_000),
  City("Budapest", Position(19.0402, 47.4979), 1_706_000),
  City("Athens", Position(23.7275, 37.9838), 643_000),
)

/** Random points scattered around the cities, for the aggregation layers. */
val samples: List<Position> = run {
  val random = Random(42)
  cities.flatMap { city ->
    val count = (city.population / 4_000).coerceAtMost(1_200)
    List(count) {
      // Gaussian-ish spread with a radius of tens of kilometers
      val r = sqrt(-2.0 * kotlin.math.ln(random.nextDouble(1e-9, 1.0))) * 0.15
      val theta = random.nextDouble() * 2 * PI
      Position(city.position.longitude + r * cos(theta) * 1.4, city.position.latitude + r * sin(theta))
    }
  }
}
