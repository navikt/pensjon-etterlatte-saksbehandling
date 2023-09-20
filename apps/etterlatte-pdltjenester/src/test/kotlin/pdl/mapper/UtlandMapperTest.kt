package no.nav.etterlatte.pdl.mapper

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.pdl.PdlHentPerson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class UtlandMapperTest {
    @Test
    fun `skal mappe utflytting fra Norge`() {
        val hentPerson =
            mockk<PdlHentPerson> {
                every { utflyttingFraNorge } returns
                    listOf(
                        mockk {
                            every { tilflyttingsland } returns "FRA"
                            every { utflyttingsdato } returns LocalDate.parse("2021-07-01")
                        },
                    )
                every { innflyttingTilNorge } returns null
            }

        val utland = UtlandMapper.mapUtland(hentPerson)

        assertEquals("FRA", utland.utflyttingFraNorge?.first()?.tilflyttingsland)
        assertEquals("2021-07-01", utland.utflyttingFraNorge?.first()?.dato.toString())
    }

    @Test
    fun `skal mappe innflytting til Norge`() {
        val hentPerson =
            mockk<PdlHentPerson> {
                every { innflyttingTilNorge } returns
                    listOf(
                        mockk {
                            every { fraflyttingsland } returns "FRA"
                            every { folkeregistermetadata } returns
                                mockk {
                                    every { gyldighetstidspunkt } returns LocalDateTime.parse("2021-07-01T00:00:00")
                                }
                        },
                    )
                every { utflyttingFraNorge } returns null
            }

        val utland = UtlandMapper.mapUtland(hentPerson)

        assertEquals("FRA", utland.innflyttingTilNorge?.first()?.fraflyttingsland)
        assertEquals("2021-07-01", utland.innflyttingTilNorge?.first()?.dato.toString())
    }
}
