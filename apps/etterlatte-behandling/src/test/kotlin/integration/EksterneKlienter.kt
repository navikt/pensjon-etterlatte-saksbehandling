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
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.klienter.VilkaarsvurderingKlient
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Mottaker
import no.nav.etterlatte.libs.common.behandling.Mottakerident
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.ServiceStatus
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
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
    ): Grunnlagsopplysning<Persongalleri> =
        Grunnlagsopplysning(
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

    override suspend fun hentGrunnlagForSak(
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

    override suspend fun hentGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
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
    ): Long = 123L

    override suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): Long = 123L

    override suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: String,
    ): TilbakekrevingVedtakLagretDto =
        TilbakekrevingVedtakLagretDto(
            id = 123L,
            fattetAv = "saksbehandler",
            enhet = "enhet",
            dato = LocalDate.now(),
        )

    override suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Long = 123L

    override suspend fun lagreVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

    override suspend fun fattVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

    override suspend fun attesterVedtakKlage(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

    override suspend fun underkjennVedtakKlage(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

    override suspend fun sakHarLopendeVedtakPaaDato(
        sakId: Long,
        dato: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): LoependeYtelseDTO = LoependeYtelseDTO(true, false, LocalDate.now())
}

class TilbakekrevingKlientTest : TilbakekrevingKlient {
    override suspend fun sendTilbakekrevingsvedtak(
        brukerTokenInfo: BrukerTokenInfo,
        tilbakekrevingVedtak: TilbakekrevingVedtak,
    ) {
        return
    }

    override suspend fun hentKravgrunnlag(
        brukerTokenInfo: BrukerTokenInfo,
        sakId: Long,
        kravgrunnlagId: Long,
    ): Kravgrunnlag {
        TODO("Not yet implemented")
    }

    override val serviceName: String
        get() = this.javaClass.simpleName
    override val beskrivelse: String
        get() = "Sender tilbakekrevingsvedtak til tilbakekreving"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}

class BrevApiKlientTest : BrevApiKlient {
    private var brevId = 1L

    override suspend fun opprettKlageOversendelsesbrevISak(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto = opprettetBrevDto(brevId++)

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto = opprettetBrevDto(brevId++)

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
    ): JournalpostIdDto = JournalpostIdDto(UUID.randomUUID().toString())

    override suspend fun distribuerBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto = BestillingsIdDto(UUID.randomUUID().toString())

    override suspend fun hentBrev(
        sakId: Long,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto = opprettetBrevDto(brevId)

    override suspend fun slettVedtaksbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun journalfoerNotatKa(
        klage: Klage,
        brukerInfoToken: BrukerTokenInfo,
    ): OpprettJournalpostDto = OpprettJournalpostDto(UUID.randomUUID().toString())

    override suspend fun slettOversendelsesbrev(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto = opprettetBrevDto(brevId)

    override suspend fun hentOversendelsesbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OpprettetBrevDto = opprettetBrevDto(brevId)

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
        saksbehandler: String?,
        tema: List<String>,
        enhetsnr: String?,
        harTildeling: Boolean?,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver = GosysOppgaver(0, emptyList())

    override suspend fun hentJournalfoeringsoppgave(
        journalpostId: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysOppgaver = GosysOppgaver(0, emptyList())

    override suspend fun hentOppgave(
        id: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave = gosysApiOppgave()

    override suspend fun ferdigstill(
        id: String,
        oppgaveVersjon: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave = gosysApiOppgave()

    override suspend fun feilregistrer(
        id: String,
        request: EndreStatusRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave = gosysApiOppgave()

    private fun gosysApiOppgave(): GosysApiOppgave =
        GosysApiOppgave(
            1,
            2,
            "EYB",
            "-",
            "",
            null,
            Tidspunkt.now(),
            "4808",
            null,
            "beskrivelse",
            "NY",
            LocalDate.now(),
            bruker = null,
        )

    override suspend fun tildelOppgaveTilSaksbehandler(
        oppgaveId: String,
        oppgaveVersjon: Long,
        tildeles: String,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave = gosysApiOppgave()

    override suspend fun endreFrist(
        oppgaveId: String,
        oppgaveVersjon: Long,
        nyFrist: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): GosysApiOppgave = gosysApiOppgave()
}

class Norg2KlientTest : Norg2Klient {
    override fun hentArbeidsfordelingForOmraadeOgTema(request: ArbeidsFordelingRequest): List<ArbeidsFordelingEnhet> =
        listOf(ArbeidsFordelingEnhet(Enheter.STEINKJER.navn, Enheter.STEINKJER.enhetNr))

    override suspend fun hentNavkontorForOmraade(omraade: String): Navkontor = Navkontor("1202 NAV BERGEN SØR", "4808")
}

class NavAnsattKlientTest : NavAnsattKlient {
    override suspend fun hentSaksbehanderNavn(ident: String): SaksbehandlerInfo? = SaksbehandlerInfo("ident", "Max Manus")

    override val serviceName: String
        get() = "Navansatt"
    override val beskrivelse: String
        get() = "Henter navn for saksbehandlerident"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}

class PesysKlientTest : PesysKlient {
    override suspend fun hentSaker(
        fnr: String,
        bruker: BrukerTokenInfo,
    ): List<SakSammendragResponse> = emptyList()

    override suspend fun erTilstoetendeBehandlet(
        fnr: String,
        doedsdato: LocalDate,
        bruker: BrukerTokenInfo,
    ): Boolean = false
}

class KrrklientTest : KrrKlient {
    override suspend fun hentDigitalKontaktinformasjon(fnr: String): DigitalKontaktinformasjon =
        DigitalKontaktinformasjon(
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

class AxsysKlientTest : AxsysKlient {
    override suspend fun hentEnheterForIdent(ident: String): List<SaksbehandlerEnhet> =
        listOf(
            SaksbehandlerEnhet(Enheter.defaultEnhet.enhetNr, Enheter.defaultEnhet.navn),
            SaksbehandlerEnhet(Enheter.STEINKJER.enhetNr, Enheter.STEINKJER.navn),
        )

    override val serviceName: String
        get() = "Axsys"
    override val beskrivelse: String
        get() = "Henter enheter for saksbehandlerident"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}

class VilkaarsvurderingTest : VilkaarsvurderingKlient {
    override suspend fun kopierVilkaarsvurdering(
        kopierTilBehandling: UUID,
        kopierFraBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        // NO-OP
    }

    override val serviceName: String
        get() = "Vilkårsvurderinglient"
    override val beskrivelse: String
        get() = "Snakker med vilkårsvurdering"
    override val endpoint: String
        get() = "vilkårsvurdering"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}

class PdltjenesterKlientTest : PdlTjenesterKlient {
    override fun hentPdlModellFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO = mockPerson()

    override fun hentPdlModellFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDTO = mockPerson()

    override fun hentGeografiskTilknytning(
        foedselsnummer: String,
        saktype: SakType,
    ): GeografiskTilknytning = GeografiskTilknytning(kommune = "0301")

    override fun hentFolkeregisterIdenterForAktoerIdBolk(aktoerIds: Set<String>): Map<String, String?> = emptyMap<String, String>()

    override suspend fun hentAdressebeskyttelseForPerson(
        hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest,
    ): AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT

    override val serviceName: String
        get() = "Pdl tjenester"
    override val beskrivelse: String
        get() = "Henter enheter pdl data"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}
