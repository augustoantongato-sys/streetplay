package br.com.streetplay

import br.com.streetplay.data.remote.TmdbParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbParserTest {
    private val json = """
        {"results":[
          {"id":1,"media_type":"movie","title":"Filme A","release_date":"2025-06-01","vote_average":7.84,
           "overview":"Sinopse","poster_path":"/p.jpg","backdrop_path":"/b.jpg","adult":false},
          {"id":2,"media_type":"tv","name":"Série B","first_air_date":"2024-01-10","vote_average":8.1,
           "overview":"","poster_path":null,"backdrop_path":null},
          {"id":3,"media_type":"person","name":"Pessoa"},
          {"id":4,"media_type":"movie","title":"Adulto","adult":true}
        ]}
    """.trimIndent()

    private val items = TmdbParser.parseTrending(json)

    @Test fun keepsOnlyMoviesAndSeriesNotAdult() = assertEquals(listOf(1, 2), items.map { it.id })

    @Test fun buildsImageUrlsAndYear() {
        val m = items[0]
        assertEquals("https://image.tmdb.org/t/p/w342/p.jpg", m.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w780/b.jpg", m.backdropUrl)
        assertEquals("2025", m.year)
    }

    @Test fun usesNameForSeriesAndHandlesNullImages() {
        val s = items[1]
        assertEquals("Série B", s.title)
        assertEquals("Série", s.typeLabel)
        assertNull(s.posterUrl)
    }

    @Test fun emptyOnMissingResults() = assertEquals(0, TmdbParser.parseTrending("{}").size)
}
