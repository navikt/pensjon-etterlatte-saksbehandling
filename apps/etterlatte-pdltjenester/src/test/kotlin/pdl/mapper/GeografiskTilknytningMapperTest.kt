package no.nav.etterlatte.pdl.mapper

import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.pdl.PdlGeografiskTilknytning
import no.nav.etterlatte.pdl.PdlGtType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class GeografiskTilknytningMapperTest {

    @ParameterizedTest
    @MethodSource("valuesForTest")
    fun testMapper(gtType: PdlGtType?, forventet: GeografiskTilknytning) {
        val tilknytning = PdlGeografiskTilknytning(
            gtBydel = "Bydel",
            gtKommune = "Kommune",
            gtLand = "Land",
            gtType = gtType
        )

        val resultat = GeografiskTilknytningMapper.mapGeografiskTilknytning(tilknytning)

        assertEquals(
            forventet.geografiskTilknytning(),
            resultat.geografiskTilknytning()
        )

        assertEquals(
            forventet.ukjent,
            resultat.ukjent
        )
    }

    companion object {

        @JvmStatic
        fun valuesForTest(): List<Arguments> = listOf(
            Arguments.of(PdlGtType.BYDEL, GeografiskTilknytning(bydel = "Bydel", ukjent = false)),
            Arguments.of(PdlGtType.KOMMUNE, GeografiskTilknytning(bydel = "Kommune", ukjent = false)),
            Arguments.of(PdlGtType.UTLAND, GeografiskTilknytning(bydel = "Land", ukjent = false)),
            Arguments.of(PdlGtType.UDEFINERT, GeografiskTilknytning(ukjent = true)),
            Arguments.of(null, GeografiskTilknytning())
        )
    }
}