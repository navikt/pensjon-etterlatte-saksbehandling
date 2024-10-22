package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class EtterlatteBrevTrygdetidTest {
    @Test
    fun `mapper trygdetid nasjonal beregning`() {
        with(
            trygdetidDto(
                ident = "id",
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = null,
                prorataBroek = null,
                perioder =
                    listOf(
                        trygdetidGrunnlagDto(
                            periodeFra = LocalDate.of(2024, 1, 1),
                            periodeTil = LocalDate.of(2025, 1, 1),
                            bosted = "NOR",
                            type = TrygdetidType.FREMTIDIG,
                        ),
                        trygdetidGrunnlagDto(
                            periodeFra = LocalDate.of(2000, 1, 1),
                            periodeTil = LocalDate.of(2023, 12, 31),
                            bosted = "NOR",
                            type = TrygdetidType.FAKTISK,
                        ),
                        trygdetidGrunnlagDto(
                            periodeFra = LocalDate.of(1990, 1, 1),
                            periodeTil = LocalDate.of(1999, 12, 31),
                            bosted = "SWE",
                            type = TrygdetidType.FAKTISK,
                        ),
                    ),
            ).fromDto(BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL, "Ole"),
        ) {
            beregnetTrygdetidAar shouldBe 40
            prorataBroek shouldBe null
            trygdetidsperioder.size shouldBe 2
            trygdetidsperioder.forEach {
                it.land shouldBe "NOR"
            }
        }
    }

    @Test
    fun `mapper trygdetid med prorata`() {
        with(
            trygdetidDto(
                ident = "id",
                samletTrygdetidNorge = null,
                samletTrygdetidTeoretisk = 29,
                prorataBroek = null,
                perioder =
                    listOf(
                        trygdetidGrunnlagDto(
                            periodeFra = LocalDate.of(2020, 1, 1),
                            periodeTil = LocalDate.of(2024, 1, 1),
                            bosted = "NOR",
                            type = TrygdetidType.FAKTISK,
                            prorata = false,
                        ),
                        trygdetidGrunnlagDto(
                            periodeFra = LocalDate.of(1990, 1, 1),
                            periodeTil = LocalDate.of(2019, 12, 31),
                            bosted = "SWE",
                            type = TrygdetidType.FAKTISK,
                            prorata = true,
                        ),
                    ),
            ).fromDto(BeregningsMetode.PRORATA, BeregningsMetode.BEST, "Ole"),
        ) {
            beregnetTrygdetidAar shouldBe 29
            prorataBroek shouldBe null
            trygdetidsperioder.size shouldBe 1
            trygdetidsperioder.forEach {
                it.land shouldNotBe "NOR"
            }
        }
    }

    @Test
    fun `trygdetid med beregning utleder navn på relevant avdød`() {
        val avdoede =
            listOf(
                Avdoed(
                    fnr = Foedselsnummer("idTilOle"),
                    navn = "Ole",
                    doedsdato = LocalDate.now(),
                ),
                Avdoed(
                    fnr = Foedselsnummer("idTilNasse"),
                    navn = "Nasse",
                    doedsdato = LocalDate.now(),
                ),
            )
        with(
            trygdetidDto(
                ident = "idTilOle",
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = null,
                prorataBroek = null,
                perioder = emptyList(),
            ).fromDto(BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL, avdoede),
        ) {
            navnAvdoed shouldBe "Ole"
        }
    }

    @Test
    fun `trygdetid med beregning utleder avdoedes navn hvis overstyrt for ukjent avdoed`() {
        val avdoede = emptyList<Avdoed>()

        with(
            trygdetidDto(
                ident = UKJENT_AVDOED,
                samletTrygdetidNorge = 40,
                samletTrygdetidTeoretisk = null,
                prorataBroek = null,
                perioder = emptyList(),
                overstyrt = true,
            ).fromDto(BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL, avdoede),
        ) {
            navnAvdoed shouldBe "ukjent avdød"
        }
    }
}

fun trygdetidDto(
    ident: String = "id",
    samletTrygdetidNorge: Int? = 40,
    samletTrygdetidTeoretisk: Int? = null,
    prorataBroek: IntBroek? = null,
    perioder: List<TrygdetidGrunnlagDto> = emptyList(),
    overstyrt: Boolean = false,
) = TrygdetidDto(
    id = UUID.randomUUID(),
    ident = ident,
    behandlingId = UUID.randomUUID(),
    beregnetTrygdetid =
        DetaljertBeregnetTrygdetidDto(
            resultat =
                DetaljertBeregnetTrygdetidResultat(
                    samletTrygdetidNorge = samletTrygdetidNorge,
                    samletTrygdetidTeoretisk = samletTrygdetidTeoretisk,
                    prorataBroek = prorataBroek,
                    faktiskTrygdetidNorge = null,
                    fremtidigTrygdetidNorge = null,
                    faktiskTrygdetidTeoretisk = null,
                    fremtidigTrygdetidTeoretisk = null,
                    beregnetSamletTrygdetidNorge = null,
                    overstyrt = overstyrt,
                    yrkesskade = false,
                ),
            tidspunkt = Tidspunkt.now(),
        ),
    trygdetidGrunnlag = perioder,
    overstyrtNorskPoengaar = null,
    opplysningerDifferanse = mockk(),
    opplysninger = mockk(),
)

fun trygdetidGrunnlagDto(
    periodeFra: LocalDate,
    periodeTil: LocalDate,
    type: TrygdetidType,
    bosted: String,
    prorata: Boolean = false,
) = TrygdetidGrunnlagDto(
    id = null,
    type = type.name,
    periodeFra = periodeFra,
    periodeTil = periodeTil,
    bosted = bosted,
    prorata = prorata,
    kilde = null,
    beregnet = null,
    begrunnelse = null,
    poengUtAar = false,
    poengInnAar = false,
)
