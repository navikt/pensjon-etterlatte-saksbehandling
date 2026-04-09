package no.nav.etterlatte.behandling.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.etterlatte.behandling.ViderefoertOpphoer
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingOpprinnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.TidligereFamiliepleier
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BehandlingDomeneTest {
    private val sakId = SakId(1L)
    private val kilde = Grunnlagsopplysning.Saksbehandler.create("saksbehandler")

    private fun foerstegangsbehandling(
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        virkningstidspunkt: Virkningstidspunkt? = null,
        opphoerFraOgMed: YearMonth? = null,
    ) = Foerstegangsbehandling(
        id = UUID.randomUUID(),
        sak = Sak("fnr", SakType.BARNEPENSJON, sakId, Enheter.defaultEnhet.enhetNr, null, false),
        behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
        sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        status = status,
        soeknadMottattDato = Tidspunkt.now().toLocalDatetimeUTC(),
        gyldighetsproeving = null,
        virkningstidspunkt = virkningstidspunkt,
        utlandstilknytning = null,
        boddEllerArbeidetUtlandet = null,
        kommerBarnetTilgode = null,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        sendeBrev = true,
        opphoerFraOgMed = opphoerFraOgMed,
        opprinnelse = BehandlingOpprinnelse.UKJENT,
    )

    private fun revurdering(
        status: BehandlingStatus = BehandlingStatus.OPPRETTET,
        virkningstidspunkt: Virkningstidspunkt? = null,
        opphoerFraOgMed: YearMonth? = null,
        prosesstype: Prosesstype = Prosesstype.MANUELL,
    ) = Revurdering.opprett(
        id = UUID.randomUUID(),
        sak = Sak("fnr", SakType.OMSTILLINGSSTOENAD, sakId, Enheter.defaultEnhet.enhetNr, null, false),
        behandlingOpprettet = Tidspunkt.now().toLocalDatetimeUTC(),
        sistEndret = Tidspunkt.now().toLocalDatetimeUTC(),
        status = status,
        kommerBarnetTilgode = null,
        virkningstidspunkt = virkningstidspunkt,
        utlandstilknytning = null,
        boddEllerArbeidetUtlandet = null,
        revurderingsaarsak = Revurderingaarsak.INNTEKTSENDRING,
        prosesstype = prosesstype,
        vedtaksloesning = Vedtaksloesning.GJENNY,
        revurderingInfo = null,
        begrunnelse = null,
        relatertBehandlingId = null,
        sendeBrev = true,
        opphoerFraOgMed = opphoerFraOgMed,
        tidligereFamiliepleier = null,
        opprinnelse = BehandlingOpprinnelse.UKJENT,
    )

    private fun virkningstidspunkt(maaned: YearMonth) = Virkningstidspunkt(maaned, kilde, "begrunnelse")

    private fun viderefoertOpphoer(
        behandlingId: UUID,
        skalViderefoere: JaNei,
        dato: YearMonth? = YearMonth.of(2024, 1),
        vilkaar: VilkaarType? = VilkaarType.OMS_AKTIVITETSPLIKT,
    ) = ViderefoertOpphoer(
        skalViderefoere = skalViderefoere,
        behandlingId = behandlingId,
        dato = dato,
        vilkaar = vilkaar,
        begrunnelse = "test",
        kilde = kilde,
    )

    @Nested
    inner class OppdaterTidligereFamiliepleier {
        @Test
        fun `lagrer verdier og nullstiller status til OPPRETTET`() {
            val behandling = foerstegangsbehandling(status = BehandlingStatus.VILKAARSVURDERT)
            val pleier =
                TidligereFamiliepleier(
                    svar = true,
                    kilde = kilde,
                    foedselsnummer = "12345",
                    startPleieforhold = LocalDate.of(2010, 1, 1),
                    opphoertPleieforhold = LocalDate.of(2020, 1, 1),
                    begrunnelse = "Test",
                )

            val oppdatert = behandling.oppdaterTidligereFamiliepleier(pleier)

            oppdatert.tidligereFamiliepleier shouldBe pleier
            oppdatert.status shouldBe BehandlingStatus.OPPRETTET
        }

        @Test
        fun `kaster feil hvis svar er ja og start eller opphoer mangler`() {
            val behandling = foerstegangsbehandling()
            val utenDatoer =
                TidligereFamiliepleier(
                    svar = true,
                    kilde = kilde,
                    foedselsnummer = "12345",
                    startPleieforhold = null,
                    opphoertPleieforhold = null,
                    begrunnelse = "Test",
                )

            shouldThrow<PleieforholdMaaHaStartOgOpphoer> {
                behandling.oppdaterTidligereFamiliepleier(utenDatoer)
            }
        }

        @Test
        fun `kaster feil hvis start er etter opphoer`() {
            val behandling = foerstegangsbehandling()
            val feilRekkefoelge =
                TidligereFamiliepleier(
                    svar = true,
                    kilde = kilde,
                    foedselsnummer = "12345",
                    startPleieforhold = LocalDate.of(2020, 6, 1),
                    opphoertPleieforhold = LocalDate.of(2020, 1, 1),
                    begrunnelse = "Test",
                )

            shouldThrow<PleieforholdMaaStarteFoerDetOpphoerer> {
                behandling.oppdaterTidligereFamiliepleier(feilRekkefoelge)
            }
        }

        @Test
        fun `kaster feil hvis start er samme dag som opphoer`() {
            val behandling = foerstegangsbehandling()
            val sammeDag =
                TidligereFamiliepleier(
                    svar = true,
                    kilde = kilde,
                    foedselsnummer = "12345",
                    startPleieforhold = LocalDate.of(2020, 1, 1),
                    opphoertPleieforhold = LocalDate.of(2020, 1, 1),
                    begrunnelse = "Test",
                )

            shouldThrow<PleieforholdMaaStarteFoerDetOpphoerer> {
                behandling.oppdaterTidligereFamiliepleier(sammeDag)
            }
        }

        @Test
        fun `hopper over validering naar svar er nei`() {
            val behandling = foerstegangsbehandling()
            val nei =
                TidligereFamiliepleier(
                    svar = false,
                    kilde = kilde,
                    foedselsnummer = null,
                    startPleieforhold = null,
                    opphoertPleieforhold = null,
                    begrunnelse = "",
                )

            val oppdatert = behandling.oppdaterTidligereFamiliepleier(nei)

            oppdatert.tidligereFamiliepleier shouldBe nei
        }

        @Test
        fun `kaster feil naar behandling ikke er redigerbar`() {
            val fattet = foerstegangsbehandling(status = BehandlingStatus.FATTET_VEDTAK)

            assertThrows<TilstandException.KanIkkeRedigere> {
                fattet.oppdaterTidligereFamiliepleier(
                    TidligereFamiliepleier(false, kilde, null, null, null, ""),
                )
            }
        }

        @Test
        fun `virker ogsaa for revurdering`() {
            val revurdering = revurdering()
            val pleier =
                TidligereFamiliepleier(
                    svar = true,
                    kilde = kilde,
                    foedselsnummer = "12345",
                    startPleieforhold = LocalDate.of(2010, 1, 1),
                    opphoertPleieforhold = LocalDate.of(2020, 1, 1),
                    begrunnelse = "Test",
                )

            val oppdatert = revurdering.oppdaterTidligereFamiliepleier(pleier)

            oppdatert.tidligereFamiliepleier shouldBe pleier
            oppdatert.status shouldBe BehandlingStatus.OPPRETTET
        }
    }

    @Nested
    inner class OppdaterSendeBrev {
        @Test
        fun `foerstegangsbehandling kaster feil naar erOmgjoering er false`() {
            val behandling = foerstegangsbehandling()

            shouldThrow<KanIkkeEndreSendeBrevForFoerstegangsbehandling> {
                behandling.oppdaterSendeBrev(false)
            }
        }

        @Test
        fun `foerstegangsbehandling kan endre sendeBrev naar erOmgjoering er true`() {
            val behandling = foerstegangsbehandling()

            val oppdatert = behandling.oppdaterSendeBrev(skalSendeBrev = false, erOmgjoering = true)

            oppdatert.sendeBrev shouldBe false
        }

        @Test
        fun `foerstegangsbehandling endrer ikke status`() {
            val behandling = foerstegangsbehandling(status = BehandlingStatus.VILKAARSVURDERT)

            val oppdatert = behandling.oppdaterSendeBrev(skalSendeBrev = false, erOmgjoering = true)

            oppdatert.status shouldBe BehandlingStatus.VILKAARSVURDERT
        }

        @Test
        fun `revurdering kan endre sendeBrev fritt`() {
            val revurdering = revurdering()

            val oppdatert = revurdering.oppdaterSendeBrev(false)

            oppdatert.sendeBrev shouldBe false
        }

        @Test
        fun `revurdering endrer ikke status`() {
            val revurdering = revurdering(status = BehandlingStatus.BEREGNET)

            val oppdatert = revurdering.oppdaterSendeBrev(true)

            oppdatert.status shouldBe BehandlingStatus.BEREGNET
        }
    }

    @Nested
    inner class OppdaterOpphoerFom {
        @Test
        fun `oppdaterer opphoerFraOgMed`() {
            val dato = YearMonth.of(2025, 6)
            val behandling = foerstegangsbehandling()

            val oppdatert = behandling.oppdaterOpphoerFom(dato)

            oppdatert.opphoerFraOgMed shouldBe dato
        }

        @Test
        fun `kan sette til null`() {
            val behandling = foerstegangsbehandling(opphoerFraOgMed = YearMonth.of(2025, 1))

            val oppdatert = behandling.oppdaterOpphoerFom(null)

            oppdatert.opphoerFraOgMed shouldBe null
        }

        @Test
        fun `endrer ikke status`() {
            val behandling = foerstegangsbehandling(status = BehandlingStatus.VILKAARSVURDERT)

            val oppdatert = behandling.oppdaterOpphoerFom(YearMonth.of(2025, 1))

            oppdatert.status shouldBe BehandlingStatus.VILKAARSVURDERT
        }
    }

    @Nested
    inner class OppdaterViderefoertOpphoer {
        @Test
        fun `nullstiller opphoerFraOgMed og setter status til OPPRETTET naar argument er null`() {
            val behandling =
                foerstegangsbehandling(
                    status = BehandlingStatus.VILKAARSVURDERT,
                    opphoerFraOgMed = YearMonth.of(2024, 1),
                )

            val oppdatert = behandling.oppdaterViderefoertOpphoer(null)

            oppdatert.opphoerFraOgMed shouldBe null
            oppdatert.status shouldBe BehandlingStatus.OPPRETTET
        }

        @Test
        fun `lagrer dato og setter status til OPPRETTET ved gyldig viderefoert opphoer`() {
            val behandling = foerstegangsbehandling()
            val dato = YearMonth.of(2025, 3)

            val oppdatert =
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA, dato = dato),
                )

            oppdatert.opphoerFraOgMed shouldBe dato
            oppdatert.status shouldBe BehandlingStatus.OPPRETTET
        }

        @Test
        fun `skalViderefoere NEI krever ikke validering og lagrer uten opphoerFraOgMed`() {
            val behandling = foerstegangsbehandling()

            val oppdatert =
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.NEI, dato = null, vilkaar = null),
                )

            oppdatert.opphoerFraOgMed shouldBe null
        }

        @Test
        fun `kaster feil naar vilkaar mangler og skalViderefoere er JA`() {
            val behandling = foerstegangsbehandling()

            shouldThrow<VilkaarMaaFinnesHvisViderefoertOpphoer> {
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA, vilkaar = null),
                )
            }
        }

        @Test
        fun `kaster feil naar dato mangler og skalViderefoere er JA`() {
            val behandling = foerstegangsbehandling()

            shouldThrow<DatoMaaFinnesHvisViderefoertOpphoer> {
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA, dato = null),
                )
            }
        }

        @Test
        fun `kaster feil naar virkningstidspunkt er etter opphoerdato`() {
            val opphoerDato = YearMonth.of(2024, 6)
            val virk = YearMonth.of(2024, 9)
            val behandling = foerstegangsbehandling(virkningstidspunkt = virkningstidspunkt(virk))

            shouldThrow<VirkningstidspunktKanIkkeVaereEtterOpphoer> {
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA, dato = opphoerDato),
                )
            }
        }

        @Test
        fun `aksepterer virkningstidspunkt lik opphoerdato`() {
            val dato = YearMonth.of(2024, 6)
            val behandling = foerstegangsbehandling(virkningstidspunkt = virkningstidspunkt(dato))

            val oppdatert =
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA, dato = dato),
                )

            oppdatert.opphoerFraOgMed shouldBe dato
        }

        @Test
        fun `aksepterer virkningstidspunkt foer opphoerdato`() {
            val dato = YearMonth.of(2024, 6)
            val virk = YearMonth.of(2024, 3)
            val behandling = foerstegangsbehandling(virkningstidspunkt = virkningstidspunkt(virk))

            val oppdatert =
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA, dato = dato),
                )

            oppdatert.opphoerFraOgMed shouldBe dato
        }

        @Test
        fun `kaster feil naar behandling ikke er redigerbar`() {
            val behandling = foerstegangsbehandling(status = BehandlingStatus.FATTET_VEDTAK)

            assertThrows<TilstandException.KanIkkeRedigere> {
                behandling.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(behandling.id, JaNei.JA),
                )
            }
        }

        @Test
        fun `virker ogsaa for revurdering`() {
            val revurdering = revurdering()
            val dato = YearMonth.of(2025, 3)

            val oppdatert =
                revurdering.oppdaterViderefoertOpphoer(
                    viderefoertOpphoer(revurdering.id, JaNei.JA, dato = dato),
                )

            oppdatert.opphoerFraOgMed shouldBe dato
            oppdatert.status shouldBe BehandlingStatus.OPPRETTET
        }
    }
}
