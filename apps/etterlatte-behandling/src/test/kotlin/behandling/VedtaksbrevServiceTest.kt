package no.nav.etterlatte.behandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.klage.KlageService
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedtaksbrevServiceTest {
    private val klageService = mockk<KlageService>()

    private val service =
        VedtaksbrevService(
            grunnlagService = mockk(relaxed = true),
            vedtakKlient = mockk(relaxed = true),
            brevKlient = mockk(relaxed = true),
            behandlingService = mockk(relaxed = true),
            beregningKlient = mockk(relaxed = true),
            behandlingInfoService = mockk(relaxed = true),
            trygdetidKlient = mockk(relaxed = true),
            vilkaarsvurderingService = mockk(relaxed = true),
            sakService = mockk(relaxed = true),
            klageService = klageService,
            kodeverkService = mockk(relaxed = true),
        )

    @Test
    fun `hentKlageForBehandling returnerer null naar relatertBehandlingId er null`() {
        val behandling = lagDetaljertBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, relatertBehandlingId = null)

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe null
    }

    @Test
    fun `hentKlageForBehandling returnerer null naar behandlingen verken er foerstegangsbehandling eller omgjoering etter klage`() {
        val behandling =
            lagDetaljertBehandling(
                BehandlingType.REVURDERING,
                aarsak = Revurderingaarsak.SOESKENJUSTERING,
                relatertBehandlingId = UUID.randomUUID(),
            )

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe null
    }

    @Test
    fun `hentKlageForBehandling returnerer klage for foerstegangsbehandling`() {
        val relatertBehandlingId = UUID.randomUUID()
        val behandling = lagDetaljertBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, relatertBehandlingId = relatertBehandlingId)
        val klage = lagKlage()
        every { klageService.hentKlage(relatertBehandlingId) } returns klage

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe klage
    }

    @Test
    fun `hentKlageForBehandling returnerer klage for omgjoering etter klage`() {
        val relatertBehandlingId = UUID.randomUUID()
        val behandling =
            lagDetaljertBehandling(
                BehandlingType.REVURDERING,
                aarsak = Revurderingaarsak.OMGJOERING_ETTER_KLAGE,
                relatertBehandlingId = relatertBehandlingId,
            )
        val klage = lagKlage()
        every { klageService.hentKlage(relatertBehandlingId) } returns klage

        val resultat = service.hentKlageForBehandling(behandling)

        resultat shouldBe klage
    }

    private fun lagDetaljertBehandling(
        type: BehandlingType,
        aarsak: Revurderingaarsak? = null,
        relatertBehandlingId: UUID? = null,
    ) = DetaljertBehandling(
        id = UUID.randomUUID(),
        sak = SAK_ID,
        sakType = SakType.OMSTILLINGSSTOENAD,
        soeker = "123",
        status = BehandlingStatus.OPPRETTET,
        behandlingType = type,
        virkningstidspunkt = null,
        boddEllerArbeidetUtlandet = null,
        utlandstilknytning = null,
        revurderingsaarsak = aarsak,
        prosesstype = Prosesstype.MANUELL,
        revurderingInfo = null,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        sendeBrev = true,
        opphoerFraOgMed = null,
        relatertBehandlingId = relatertBehandlingId,
        tidligereFamiliepleier = null,
        opprinnelse = BehandlingOpprinnelse.UKJENT,
    )

    private fun lagKlage() =
        Klage.ny(
            Sak("ident", SakType.OMSTILLINGSSTOENAD, SAK_ID, Enheter.defaultEnhet.enhetNr, null, null),
            null,
        )

    private companion object {
        val SAK_ID = SakId(1L)
    }
}
