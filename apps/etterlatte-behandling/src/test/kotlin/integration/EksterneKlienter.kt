package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.BrevStatus
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.OpprettJournalpostDto
import no.nav.etterlatte.behandling.klienter.OpprettetBrevDto
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.behandling.Mottakerident
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.ServiceStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgaveGosys.EndreStatusRequest
import no.nav.etterlatte.oppgaveGosys.GosysApiOppgave
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaver
import no.nav.etterlatte.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import java.time.LocalDate
import java.util.UUID

class GrunnlagKlientTest : GrunnlagKlient {
    override suspend fun finnPersonOpplysning(
        behandlingId: UUID,
        opplysningsType: Opplysningstype,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Person> {
        val personopplysning = personOpplysning(doedsdato = LocalDate.parse("2022-01-01"))
        return grunnlagsOpplysningMedPersonopplysning(personopplysning)
    }

    override suspend fun hentPersongalleri(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlagsopplysning<Persongalleri> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = Grunnlagsopplysning.Privatperson("fnr", Tidspunkt.now()),
            meta = emptyMap<String, String>().toObjectNode(),
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            opplysning =
                Persongalleri(
                    "soeker",
                    "innsender",
                    listOf("soesken"),
                    listOf("avdoed"),
                    listOf("gjenlevende"),
                ),
        )
    }
}

class BeregningKlientTest : BeregningKlient {
    override suspend fun slettAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {}
}

class VedtakKlientTest : VedtakKlient {
    override suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long {
        return 123L
    }

    override suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long {
        return 123L
    }

    override suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): TilbakekrevingVedtakLagretDto {
        return TilbakekrevingVedtakLagretDto(
            id = 123L,
            fattetAv = "saksbehandler",
            enhet = "enhet",
            dato = LocalDate.now(),
        )
    }

    override suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long {
        return 123L
    }

    override suspend fun lagreVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }

    override suspend fun fattVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }

    override suspend fun attesterVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }

    override suspend fun underkjennVedtakKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto {
        return mockk<VedtakDto> {
            every { id } returns 123L
        }
    }
}

class BrevApiKlientTest : BrevApiKlient {
    private var brevId = 1L

    override suspend fun opprettKlageOversendelsesbrevISak(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId++)
    }

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId++)
    }

    override suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun ferdigstillOversendelseBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun journalfoerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto {
        return JournalpostIdDto(UUID.randomUUID().toString())
    }

    override suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto {
        return BestillingsIdDto(UUID.randomUUID().toString())
    }

    override suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId)
    }

    override suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun journalfoerNotatKa(
        klage: Klage,
        brukerInfoToken: BrukerTokenInfo,
    ): OpprettJournalpostDto {
        return OpprettJournalpostDto(UUID.randomUUID().toString())
    }

    override suspend fun slettOversendelsesbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId)
    }

    override suspend fun hentOversendelsesbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto {
        return opprettetBrevDto(brevId)
    }

    private fun opprettetBrevDto(brevId: Long) =
        OpprettetBrevDto(
            id = brevId,
            status = BrevStatus.OPPRETTET,
            mottaker =
                Mottaker(
                    navn = "Mottaker mottakersen",
                    foedselsnummer = Mottakerident("19448310410"),
                    orgnummer = null,
                ),
            journalpostId = null,
            bestillingsID = null,
        )
}

class GosysOppgaveKlientTest : GosysOppgaveKlient {
    override suspend fun hentOppgaver(
        enhetsnr: String?,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver {
        return GosysOppgaver(0, emptyList())
    }

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }

    override suspend fun ferdigstill(
        id: String,
        oppgaveVersjon: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }

    override suspend fun feilregistrer(
        id: String,
        request: EndreStatusRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }

    private fun gosysApiOppgave(): GosysApiOppgave {
        return GosysApiOppgave(
            1,
            2,
            "EYB",
            "-",
            "",
            null,
            Tidspunkt.now(),
            "4808",
            null,
            "aktoerId",
            "beskrivelse",
            "NY",
            LocalDate.now(),
        )
    }

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave {
        return gosysApiOppgave()
    }
}

class Norg2KlientTest : Norg2Klient {
    override fun hentArbeidsfordelingForOmraadeOgTema(request: ArbeidsFordelingRequest): List<ArbeidsFordelingEnhet> {
        return listOf(ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr))
    }

    override suspend fun hentNavkontorForOmraade(omraade: String): Navkontor {
        return Navkontor("1202 NAV BERGEN SØR", "4808")
    }
}

class NavAnsattKlientTest : NavAnsattKlient {
    override suspend fun hentSaksbehanderNavn(ident: String): SaksbehandlerInfo? {
        return SaksbehandlerInfo("ident", "Max Manus")
    }

    override val serviceName: String
        get() = "Navansatt"
    override val beskrivelse: String
        get() = "Henter navn for saksbehandlerident"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult {
        return PingResultUp(serviceName, ServiceStatus.UP, "endpoint", serviceName)
    }
}

class PesysKlientTest : PesysKlient {
    override suspend fun hentSaker(fnr: String): List<SakSammendragResponse> {
        return emptyList()
    }
}

class KrrklientTest : KrrKlient {
    override suspend fun hentDigitalKontaktinformasjon(fnr: String): DigitalKontaktinformasjon? {
        return DigitalKontaktinformasjon(
            personident = "",
            aktiv = true,
            kanVarsles = true,
            reservert = false,
            spraak = "nb",
            epostadresse = null,
            mobiltelefonnummer = null,
            sikkerDigitalPostkasse = null,
        )
    }
}

class AxsysKlientTest : AxsysKlient {
    override suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet> {
        return listOf(
            SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn),
            SaksbehandlerEnhet(Enheter.STEINKJER.enhetNr, Enheter.STEINKJER.navn),
        )
    }

    override val serviceName: String
        get() = "Axsys"
    override val beskrivelse: String
        get() = "Henter enheter for saksbehandlerident"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult {
        return PingResultUp(serviceName, ServiceStatus.UP, "endpoint", serviceName)
    }
}
