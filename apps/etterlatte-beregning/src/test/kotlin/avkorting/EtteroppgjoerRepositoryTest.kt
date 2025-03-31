package no.nav.etterlatte.beregning.regler.avkorting

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.avkorting.BeregnetEtteroppgjoerResultat
import no.nav.etterlatte.avkorting.EtteroppgjoerRepository
import no.nav.etterlatte.avkorting.EtteroppgjoerResultatType
import no.nav.etterlatte.avkorting.ReferanseEtteroppgjoer
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerGrense
import no.nav.etterlatte.avkorting.regler.EtteroppgjoerRettsgebyr
import no.nav.etterlatte.beregning.regler.DatabaseExtension
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EtteroppgjoerRepositoryTest(
    ds: DataSource,
) {
    private val etteroppgjoerRepository = EtteroppgjoerRepository(ds)

    @Test
    fun `skal oppdatere BeregnetEtteroppgjoerResultat hvis aar og forbehandlingId og sisteIverksattteBehandlingId eksisterer`() {
        val (forbehandlingId, sisteIverksatteBehandlingId, aar) =
            EtteroppgjoerBeregnetAvkortingRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2024,
            )

        val etteroppgjoerBeregnetResultat = beregnetEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId, 1000, 1000)
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(etteroppgjoerBeregnetResultat)
        with(etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)!!) {
            utbetaltStoenad shouldBe 1000
        }

        val nyEtteroppgjoerBeregnetResultat = beregnetEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId, 2000, 2000)
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(nyEtteroppgjoerBeregnetResultat)
        with(etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)!!) {
            utbetaltStoenad shouldBe 2000
        }
    }

    @Test
    fun `skal lagre og hente BeregnetEtteroppgjoerResultat`() {
        val (forbehandlingId, sisteIverksatteBehandlingId, aar) =
            EtteroppgjoerBeregnetAvkortingRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2024,
            )
        val etteroppgjoerBeregnetResultat = beregnetEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId, 1000, 1000)
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(etteroppgjoerBeregnetResultat)
        etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId) shouldBe
            etteroppgjoerBeregnetResultat
    }

    private fun beregnetEtteroppgjoerResultat(
        aar: Int,
        forbehandlingId: UUID,
        sisteIverksatteBehandlingId: UUID,
        utbetaltStoenad: Long,
        nyBruttoStoenad: Long,
    ): BeregnetEtteroppgjoerResultat =
        BeregnetEtteroppgjoerResultat(
            id = UUID.randomUUID(),
            aar = aar,
            forbehandlingId = forbehandlingId,
            sisteIverksatteBehandlingId = sisteIverksatteBehandlingId,
            utbetaltStoenad = utbetaltStoenad,
            nyBruttoStoenad = nyBruttoStoenad,
            differanse = 7572,
            grense =
                EtteroppgjoerGrense(
                    tilbakekreving = Beregningstall(1550.0),
                    etterbetaling = Beregningstall(8407.0),
                    rettsgebyr =
                        EtteroppgjoerRettsgebyr(
                            gyldigFra = LocalDate.now(),
                            rettsgebyr = Beregningstall(2758.0),
                        ),
                ),
            resultatType = EtteroppgjoerResultatType.TILBAKREVING,
            tidspunkt = Tidspunkt.now(),
            regelResultat = mapOf("regel" to "resultat").toObjectNode(),
            kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
            referanseAvkorting =
                ReferanseEtteroppgjoer(
                    avkortingForbehandling = UUID.randomUUID(),
                    avkortingSisteIverksatte = UUID.randomUUID(),
                ),
        )
}
