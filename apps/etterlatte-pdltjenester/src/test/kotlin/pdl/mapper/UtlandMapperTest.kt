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
        assertEquals(
            "2021-07-01",
            utland.utflyttingFraNorge
                ?.first()
                ?.dato
                .toString(),
        )
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
                                    every { ajourholdstidspunkt } returns LocalDateTime.parse("2021-07-01T00:00:00")
                                }
                        },
                    )
                every { utflyttingFraNorge } returns null
                every { bostedsadresse } returns
                    listOf(
                        mockk {
                            every { angittFlyttedato } returns LocalDate.parse("2021-07-01")
                            every { gyldigFraOgMed } returns LocalDateTime.parse("2021-07-01T00:00:00")
                        },
                    )
            }

        val utland = UtlandMapper.mapUtland(hentPerson)

        assertEquals("FRA", utland.innflyttingTilNorge?.first()?.fraflyttingsland)
        assertEquals(
            "2021-07-01",
            utland.innflyttingTilNorge
                ?.first()
                ?.dato
                .toString(),
        )
        assertEquals(
            "2021-07-01",
            utland.innflyttingTilNorge
                ?.first()
                ?.gyldighetsdato
                .toString(),
        )
        assertEquals(
            "2021-07-01",
            utland.innflyttingTilNorge
                ?.first()
                ?.ajourholdsdato
                .toString(),
        )
    }

    @Test
    fun `skal mappe dato til angittFlyttedato hvis angittFlyttedato er etter gyldighetstidspunkt`() {
        val hentPerson =
            mockk<PdlHentPerson> {
                every { innflyttingTilNorge } returns
                    listOf(
                        mockk {
                            every { fraflyttingsland } returns "FRA"
                            every { folkeregistermetadata } returns
                                mockk {
                                    every { gyldighetstidspunkt } returns LocalDateTime.parse("2021-06-01T00:00:00")
                                    every { ajourholdstidspunkt } returns LocalDateTime.parse("2021-08-01T00:00:00")
                                }
                        },
                    )
                every { utflyttingFraNorge } returns null
                every { bostedsadresse } returns
                    listOf(
                        mockk {
                            every { angittFlyttedato } returns LocalDate.parse("2021-07-01")
                            every { gyldigFraOgMed } returns LocalDateTime.parse("2021-07-01T00:00:00")
                        },
                    )
            }

        val utland = UtlandMapper.mapUtland(hentPerson)

        assertEquals("FRA", utland.innflyttingTilNorge?.first()?.fraflyttingsland)
        assertEquals(
            "2021-07-01",
            utland.innflyttingTilNorge
                ?.first()
                ?.dato
                .toString(),
        )
    }

    @Test
    fun `skal mappe dato til gyldighetstidspunkt hvis angittFlyttedato er før gyldighetstidspunkt`() {
        val hentPerson =
            mockk<PdlHentPerson> {
                every { innflyttingTilNorge } returns
                    listOf(
                        mockk {
                            every { fraflyttingsland } returns "FRA"
                            every { folkeregistermetadata } returns
                                mockk {
                                    every { gyldighetstidspunkt } returns LocalDateTime.parse("2021-08-01T00:00:00")
                                    every { ajourholdstidspunkt } returns LocalDateTime.parse("2021-09-01T00:00:00")
                                }
                        },
                    )
                every { utflyttingFraNorge } returns null
                every { bostedsadresse } returns
                    listOf(
                        mockk {
                            every { angittFlyttedato } returns LocalDate.parse("2021-07-01")
                            every { gyldigFraOgMed } returns LocalDateTime.parse("2021-07-01T00:00:00")
                        },
                    )
            }

        val utland = UtlandMapper.mapUtland(hentPerson)

        assertEquals("FRA", utland.innflyttingTilNorge?.first()?.fraflyttingsland)
        assertEquals(
            "2021-08-01",
            utland.innflyttingTilNorge
                ?.first()
                ?.dato
                .toString(),
        )
    }

    @Test
    fun `skal mappe dato til ajourholdstidspunkt hvis gyldighetstidspunkt er null`() {
        val hentPerson =
            mockk<PdlHentPerson> {
                every { innflyttingTilNorge } returns
                    listOf(
                        mockk {
                            every { fraflyttingsland } returns "FRA"
                            every { folkeregistermetadata } returns
                                mockk {
                                    every { gyldighetstidspunkt } returns null
                                    every { ajourholdstidspunkt } returns LocalDateTime.parse("2021-09-01T00:00:00")
                                }
                        },
                    )
                every { utflyttingFraNorge } returns null
                every { bostedsadresse } returns
                    listOf(
                        mockk {
                            every { angittFlyttedato } returns null
                            every { gyldigFraOgMed } returns LocalDateTime.parse("2021-07-01T00:00:00")
                        },
                    )
            }

        val utland = UtlandMapper.mapUtland(hentPerson)

        assertEquals("FRA", utland.innflyttingTilNorge?.first()?.fraflyttingsland)
        assertEquals(
            "2021-09-01",
            utland.innflyttingTilNorge
                ?.first()
                ?.dato
                .toString(),
        )
    }
}
