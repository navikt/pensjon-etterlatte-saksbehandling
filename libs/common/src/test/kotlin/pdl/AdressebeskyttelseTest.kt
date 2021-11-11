package pdl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import no.nav.etterlatte.libs.common.pdl.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseBolkPerson
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelsePerson
import no.nav.etterlatte.libs.common.pdl.AdressebeskyttelseResponse
import no.nav.etterlatte.libs.common.pdl.Gradering
import no.nav.etterlatte.libs.common.pdl.HentAdressebeskyttelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AdressebeskyttelseTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `Verifiser at gradering har korrekt rekkefoelge`() {
        assertEquals(0, Gradering.STRENGT_FORTROLIG_UTLAND.ordinal)
        assertEquals(1, Gradering.STRENGT_FORTROLIG.ordinal)
        assertEquals(2, Gradering.FORTROLIG.ordinal)
        assertEquals(3, Gradering.UGRADERT.ordinal)
    }

    @Test
    fun `Verifiser prioritering av gradering`() {
        val graderinger1 = listOf(
            Gradering.UGRADERT,
            Gradering.STRENGT_FORTROLIG,
            Gradering.STRENGT_FORTROLIG_UTLAND,
            Gradering.FORTROLIG
        )
        assertEquals(Gradering.STRENGT_FORTROLIG_UTLAND, graderinger1.minOrNull())

        val graderinger2 = listOf(
            Gradering.FORTROLIG,
            Gradering.STRENGT_FORTROLIG,
            Gradering.UGRADERT
        )
        assertEquals(Gradering.STRENGT_FORTROLIG, graderinger2.minOrNull())

        val graderinger3 = listOf(
            Gradering.FORTROLIG,
            Gradering.UGRADERT
        )
        assertEquals(Gradering.FORTROLIG, graderinger3.minOrNull())

        val graderinger4 = listOf(
            Gradering.UGRADERT
        )
        assertEquals(Gradering.UGRADERT, graderinger4.minOrNull())

        val graderinger5 = emptyList<Gradering>()
        assertNull(graderinger5.minOrNull())
    }

    @Test
    fun `Sjekk at valueOf paa gradering fungerer som forventet`() {
        assertEquals(Gradering.STRENGT_FORTROLIG_UTLAND, Gradering.valueOf("STRENGT_FORTROLIG_UTLAND"))
        assertEquals(Gradering.STRENGT_FORTROLIG, Gradering.valueOf("STRENGT_FORTROLIG"))
        assertEquals(Gradering.FORTROLIG, Gradering.valueOf("FORTROLIG"))
        assertEquals(Gradering.UGRADERT, Gradering.valueOf("UGRADERT"))

        assertThrows<Throwable> { Gradering.valueOf("ukjent") }
    }

    @Test
    fun `Sjekk at serde av gradering fungerer som forventet`() {
        val serialized = "\"FORTROLIG\""
        val deserialized = mapper.readValue(serialized, jacksonTypeRef<Gradering>())

        assertEquals(Gradering.FORTROLIG, deserialized)

        val reSerialized = mapper.writeValueAsString(deserialized)
        assertEquals(serialized, reSerialized)
    }

    @Test
    fun `Sjekk serde av AdressebeskyttelseResponse`() {
        val response = AdressebeskyttelseResponse(
            HentAdressebeskyttelse(
                listOfNotNull(
                    mockPerson(Gradering.FORTROLIG),
                    mockPerson(Gradering.STRENGT_FORTROLIG_UTLAND, Gradering.STRENGT_FORTROLIG)
                )
            )
        )

        val serialized = mapper.writeValueAsString(response)

        assertTrue(serialized.contains(Gradering.FORTROLIG.name))
        assertTrue(serialized.contains(Gradering.STRENGT_FORTROLIG_UTLAND.name))
        assertTrue(serialized.contains(Gradering.STRENGT_FORTROLIG.name))

        val deserialized = mapper.readValue(serialized, jacksonTypeRef<AdressebeskyttelseResponse>())

        val personBolk = deserialized.data!!.hentPersonBolk!!
        assertEquals(2, personBolk.size)

        val adressebeskyttelseListe = personBolk.flatMap { it.person!!.adressebeskyttelse }
        assertEquals(3, adressebeskyttelseListe.size)
    }

    private fun mockPerson(vararg gradering: Gradering?) =
        AdressebeskyttelseBolkPerson(
            AdressebeskyttelsePerson(
                gradering.filterNotNull().map { Adressebeskyttelse(it) }
            )
        )

}
