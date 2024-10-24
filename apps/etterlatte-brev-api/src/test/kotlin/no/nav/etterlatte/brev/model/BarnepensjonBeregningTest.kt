package no.nav.etterlatte.brev.model

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.model.bp.IdentMedMetodeIGrunnlagOgAnvendtMetode
import no.nav.etterlatte.brev.model.bp.trygdetidMedBeregningsmetode
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.common.trygdetid.UKJENT_AVDOED
import no.nav.pensjon.brevbaker.api.model.Foedselsnummer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class BarnepensjonBeregningTest {
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
            trygdetidMedBeregningsmetode(
                trygdetidDto(
                    ident = "idTilOle",
                    samletTrygdetidNorge = 40,
                    samletTrygdetidTeoretisk = null,
                    prorataBroek = null,
                    perioder = emptyList(),
                ),
                IdentMedMetodeIGrunnlagOgAnvendtMetode("idTilOle", BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL),
                avdoede,
            ),
        ) {
            navnAvdoed shouldBe "Ole"
        }
    }

    @Test
    fun `trygdetid med beregning utleder avdoedes navn hvis trygdetid gjelder ukjent avdoed`() {
        val avdoede = emptyList<Avdoed>()

        with(
            trygdetidMedBeregningsmetode(
                trygdetidDto(
                    ident = UKJENT_AVDOED,
                    samletTrygdetidNorge = 40,
                    samletTrygdetidTeoretisk = null,
                    prorataBroek = null,
                    perioder = emptyList(),
                ),
                IdentMedMetodeIGrunnlagOgAnvendtMetode(UKJENT_AVDOED, BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL),
                avdoede,
            ),
        ) {
            navnAvdoed shouldBe "ukjent avdød"
        }
    }

    @Test
    fun `trygdetid med beregning feiler i utleding av avdoedes navn hvis ingen avdoede og trygdetid ikke gjelder ukjent avdød`() {
        val avdoede = emptyList<Avdoed>()

        assertThrows<IngenStoetteForUkjentAvdoed> {
            trygdetidMedBeregningsmetode(
                trygdetidDto(
                    ident = "17418340118",
                    samletTrygdetidNorge = 40,
                    samletTrygdetidTeoretisk = null,
                    prorataBroek = null,
                    perioder = emptyList(),
                ),
                IdentMedMetodeIGrunnlagOgAnvendtMetode("17418340118", BeregningsMetode.NASJONAL, BeregningsMetode.NASJONAL),
                avdoede,
            )
        }
    }
}
