package no.nav.etterlatte.modules.beregning.etteroppgjoer

import io.kotest.matchers.shouldBe
import no.nav.etterlatte.DatabaseExtension
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.modules.beregning.avkorting.Avkorting
import no.nav.etterlatte.modules.beregning.avkorting.AvkortingRepository
import no.nav.etterlatte.modules.beregning.regler.etteroppgjoer
import no.nav.etterlatte.regler.Beregningstall
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.postgresql.util.PSQLException
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@ExtendWith(DatabaseExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EtteroppgjoerRepositoryTest(
    ds: DataSource,
) {
    private val etteroppgjoerRepository = EtteroppgjoerRepository(ds)
    private val avkortingRepository = AvkortingRepository(ds)

    @Test
    fun `lagrer avkorting og resultat samlet og oppdaterer referanser ved reberegning`() {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val resultat = beregnetEtteroppgjoerResultat(2024, forbehandlingId, behandlingId, 1000, 1000)

        repeat(2) { indeks ->
            val aarsoppgjoer = etteroppgjoer()
            val avkorting = Avkorting(listOf(aarsoppgjoer))
            val oppdatert =
                resultat.copy(
                    referanseAvkorting =
                        resultat.referanseAvkorting.copy(
                            avkortingForbehandling = aarsoppgjoer.id,
                            vedtakReferanse = listOf(indeks.toLong()),
                        ),
                )

            etteroppgjoerRepository.lagreAvkortingOgEtteroppgjoerResultat(SakId(1L), avkorting, oppdatert)

            avkortingRepository.hentAvkorting(forbehandlingId) shouldBe avkorting
            etteroppgjoerRepository.hentEtteroppgjoerResultat(2024, forbehandlingId, behandlingId) shouldBe oppdatert
        }
    }

    @Test
    fun `ruller tilbake avkortingen naar lagring av resultatet feiler`() {
        val sakId = SakId(1L)
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val gammelAvkorting = Avkorting(listOf(etteroppgjoer()))
        avkortingRepository.lagreAvkorting(forbehandlingId, sakId, gammelAvkorting)
        val eksisterendeResultat = beregnetEtteroppgjoerResultat(2024, UUID.randomUUID(), behandlingId, 1000, 1000)
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(eksisterendeResultat)
        val nyttAarsoppgjoer = etteroppgjoer()
        val nyttResultat =
            eksisterendeResultat.copy(
                forbehandlingId = forbehandlingId,
                referanseAvkorting = eksisterendeResultat.referanseAvkorting.copy(avkortingForbehandling = nyttAarsoppgjoer.id),
            )

        assertThrows<PSQLException> {
            etteroppgjoerRepository.lagreAvkortingOgEtteroppgjoerResultat(sakId, Avkorting(listOf(nyttAarsoppgjoer)), nyttResultat)
        }

        avkortingRepository.hentAvkorting(forbehandlingId) shouldBe gammelAvkorting
        etteroppgjoerRepository.hentEtteroppgjoerResultat(2024, forbehandlingId, behandlingId) shouldBe null
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `avviser ulikt aar eller avkortingsreferanse uten aa skrive`(feilAar: Boolean) {
        val forbehandlingId = UUID.randomUUID()
        val behandlingId = UUID.randomUUID()
        val gammelAvkorting = Avkorting(listOf(etteroppgjoer()))
        avkortingRepository.lagreAvkorting(forbehandlingId, SakId(1L), gammelAvkorting)
        val aarsoppgjoer = etteroppgjoer()
        val resultat = beregnetEtteroppgjoerResultat(if (feilAar) 2025 else 2024, forbehandlingId, behandlingId, 1000, 1000)
        val referanse = if (feilAar) aarsoppgjoer.id else UUID.randomUUID()

        assertThrows<UgyldigForespoerselException> {
            etteroppgjoerRepository.lagreAvkortingOgEtteroppgjoerResultat(
                SakId(1L),
                Avkorting(listOf(aarsoppgjoer)),
                resultat.copy(referanseAvkorting = resultat.referanseAvkorting.copy(avkortingForbehandling = referanse)),
            )
        }

        avkortingRepository.hentAvkorting(forbehandlingId) shouldBe gammelAvkorting
        etteroppgjoerRepository.hentEtteroppgjoerResultat(resultat.aar, forbehandlingId, behandlingId) shouldBe null
    }

    @Test
    fun `returnerer null hvis resultatet ikke finnes`() {
        etteroppgjoerRepository.hentEtteroppgjoerResultat(2024, UUID.randomUUID(), UUID.randomUUID()) shouldBe null
    }

    @Test
    fun `skal oppdatere BeregnetEtteroppgjoerResultat hvis aar og forbehandlingId og sisteIverksattteBehandlingId eksisterer`() {
        val (forbehandlingId, sisteIverksatteBehandlingId, aar) =
            EtteroppgjoerBeregnetAvkortingRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2024,
                SakId(1L),
            )

        val etteroppgjoerBeregnetResultat = beregnetEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId, 1000, 1000)
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(etteroppgjoerBeregnetResultat)
        with(etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)!!) {
            utbetaltStoenad shouldBe 1000
            harIngenInntekt shouldBe false
        }

        val nyEtteroppgjoerBeregnetResultat =
            beregnetEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId, 2000, 2000, harIngenInntekt = true)
        etteroppgjoerRepository.lagreEtteroppgjoerResultat(nyEtteroppgjoerBeregnetResultat)
        with(etteroppgjoerRepository.hentEtteroppgjoerResultat(aar, forbehandlingId, sisteIverksatteBehandlingId)!!) {
            utbetaltStoenad shouldBe 2000
            harIngenInntekt shouldBe true
        }
    }

    @Test
    fun `skal lagre og hente BeregnetEtteroppgjoerResultat`() {
        val (forbehandlingId, sisteIverksatteBehandlingId, aar) =
            EtteroppgjoerBeregnetAvkortingRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                2024,
                SakId(1L),
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
        harIngenInntekt: Boolean = false,
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
            resultatType = EtteroppgjoerResultatType.TILBAKEKREVING,
            harIngenInntekt = harIngenInntekt,
            tidspunkt = Tidspunkt.now(),
            regelResultat = mapOf("regel" to "resultat").toObjectNode(),
            kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
            referanseAvkorting =
                ReferanseEtteroppgjoer(
                    avkortingForbehandling = UUID.randomUUID(),
                    avkortingSisteIverksatte = null,
                    vedtakReferanse = listOf(1L),
                ),
        )
}
