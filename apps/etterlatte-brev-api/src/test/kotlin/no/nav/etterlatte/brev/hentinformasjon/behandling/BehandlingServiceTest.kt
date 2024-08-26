package no.nav.etterlatte.brev.hentinformasjon.behandling

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.brev.behandlingklient.BehandlingKlient
import no.nav.etterlatte.ktor.token.simpleSaksbehandler
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.FormkravMedBeslutter
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.VedtaketKlagenGjelder
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class BehandlingServiceTest {
    @Test
    fun `kan hente og deserialisere klager`() {
        val behandlingId = UUID.randomUUID()
        val klagePaaDenneBehandlinga = klage(behandlingId, 1L)
        val klient =
            mockk<BehandlingKlient>().also {
                coEvery { it.hentKlagerForSak(1L, any()) } returns
                    listOf(
                        klagePaaDenneBehandlinga,
                        klage(UUID.randomUUID(), 2L),
                    )
            }
        val service = BehandlingService(klient)
        runBlocking {
            val klage = service.hentKlageForBehandling(behandlingId, 1L, simpleSaksbehandler())
            assertEquals(klagePaaDenneBehandlinga, klage)
        }
    }

    private fun klage(
        behandlingId: UUID,
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
    ): Klage =
        Klage(
            behandlingId,
            Sak("ident", SakType.BARNEPENSJON, sakId, "einheit"),
            Tidspunkt.now(),
            KlageStatus.OPPRETTET,
            kabalResultat = null,
            kabalStatus = null,
            formkrav =
                FormkravMedBeslutter(
                    Formkrav(
                        vedtaketKlagenGjelder =
                            VedtaketKlagenGjelder(
                                id = UUID.randomUUID().toString(),
                                behandlingId = behandlingId.toString(),
                                datoAttestert = ZonedDateTime.now(),
                                vedtakType = VedtakType.ENDRING,
                            ),
                        erKlagerPartISaken = JaNei.JA,
                        erKlagenSignert = JaNei.JA,
                        gjelderKlagenNoeKonkretIVedtaket = JaNei.JA,
                        erKlagenFramsattInnenFrist = JaNei.JA,
                        erFormkraveneOppfylt = JaNei.JA,
                        begrunnelse = "klage",
                    ),
                    saksbehandler = Grunnlagsopplysning.automatiskSaksbehandler,
                ),
            innkommendeDokument = null,
            resultat = null,
            utfall = null,
            aarsakTilAvbrytelse = null,
            initieltUtfall = null,
        )
}
