package no.nav.etterlatte

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.AInntektReponsData
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlient
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.OpprettJournalpostDto
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.brev.BrevKlient
import no.nav.etterlatte.brev.BrevParametre
import no.nav.etterlatte.brev.BrevPayload
import no.nav.etterlatte.brev.BrevRequest
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
import no.nav.etterlatte.brev.Pdf
import no.nav.etterlatte.brev.model.Adresse
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.brev.model.FerdigstillJournalFoerOgDistribuerOpprettetBrev
import no.nav.etterlatte.brev.model.Mottaker
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.common.Enheter
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.common.klienter.PesysKlient
import no.nav.etterlatte.common.klienter.SakSammendragResponse
import no.nav.etterlatte.common.klienter.SkjermingKlient
import no.nav.etterlatte.grunnlag.BehandlingGrunnlagVersjon
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.grunnlag.PersonMedNavn
import no.nav.etterlatte.grunnlag.PersongalleriSamsvar
import no.nav.etterlatte.grunnlag.PersonopplysningerResponse
import no.nav.etterlatte.kodeverk.Beskrivelse
import no.nav.etterlatte.kodeverk.Betydning
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.kodeverk.KodeverkNavn
import no.nav.etterlatte.kodeverk.KodeverkResponse
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.MottakerFoedselsnummer
import no.nav.etterlatte.libs.common.person.PdlFolkeregisterIdentListe
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingBehandling
import no.nav.etterlatte.libs.common.tilbakekreving.TilbakekrevingVedtak
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.ServiceStatus
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.soeker
import no.nav.etterlatte.oppgaveGosys.EndreStatusRequest
import no.nav.etterlatte.oppgaveGosys.GosysApiOppgave
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaver
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.random.Random

class GrunnlagServiceTest : GrunnlagService {
    override suspend fun grunnlagFinnesForSak(sakId: SakId): Boolean = false

    override suspend fun hentGrunnlagAvType(
        behandlingId: UUID,
        opplysningstype: Opplysningstype,
    ): Grunnlagsopplysning<JsonNode>? {
        val personopplysning = personOpplysning(doedsdato = LocalDate.parse("2022-01-01"))
        return grunnlagsOpplysningMedPersonopplysning(personopplysning)
    }

    override suspend fun lagreNyeSaksopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        // do nothing
    }

    override suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        // do nothing
    }

    override suspend fun lagreNyePersonopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        // do nothing
    }

    override suspend fun hentPersongalleri(sakId: SakId): Persongalleri = defaultPersongalleriGydligeFnr

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri = defaultPersongalleriGydligeFnr

    override suspend fun hentOpplysningsgrunnlagForSak(sakId: SakId): Grunnlag? = GrunnlagTestData().hentOpplysningsgrunnlag()

    override suspend fun hentOpplysningsgrunnlag(behandlingId: UUID): Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

    override suspend fun hentPersonopplysninger(
        behandlingId: UUID,
        sakstype: SakType,
    ): PersonopplysningerResponse = GrunnlagTestData().hentPersonopplysninger()

    override suspend fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<SakId> = setOf(sakId1)

    override suspend fun hentPersonerISak(sakId: SakId): Map<Folkeregisteridentifikator, PersonMedNavn>? {
        TODO()
    }

    override suspend fun opprettGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    ) {
        // do nothing
    }

    override suspend fun oppdaterGrunnlagForSak(oppdaterGrunnlagRequest: OppdaterGrunnlagRequest) {
        // do nothing
    }

    override suspend fun opprettEllerOppdaterGrunnlagForSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
    ) {
        // do nothing
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
    ) {
        // do nothing
    }

    override suspend fun hentHistoriskForeldreansvar(behandlingId: UUID): Grunnlagsopplysning<JsonNode>? {
        TODO("Not yet implemented")
    }

    override suspend fun hentPersongalleriSamsvar(behandlingId: UUID): PersongalleriSamsvar {
        TODO("Not yet implemented")
    }

    override suspend fun laasTilVersjonForBehandling(
        skalLaasesId: UUID,
        idLaasesTil: UUID,
    ): BehandlingGrunnlagVersjon = BehandlingGrunnlagVersjon(UUID.randomUUID(), sakId1, Random.nextLong(), true)

    override suspend fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller =
        PersonMedSakerOgRoller("08071272487", listOf(SakidOgRolle(sakId1, Saksrolle.SOEKER)))

    override suspend fun laasVersjonForBehandling(behandlingId: UUID) {
        TODO("Not yet implemented")
    }

    suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ) = YearMonth.now()
}

class BeregningKlientTest :
    BeregningKlient,
    SakTilgangsSjekk {
    override suspend fun slettAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun harOverstyrt(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean = false

    override suspend fun inntektsjusteringAvkortingInfoSjekk(
        sakId: SakId,
        aar: Int,
        sisteBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): InntektsjusteringAvkortingInfoResponse = mockk()

    override suspend fun harTilgangTilSak(
        sakId: SakId,
        skrivetilgang: Boolean,
        bruker: Saksbehandler,
    ): Boolean = true
}

class VedtakKlientTest : VedtakKlient {
    override suspend fun lagreVedtakTilbakekreving(
        tilbakekrevingBehandling: TilbakekrevingBehandling,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

    override suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

    override suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
            every { vedtakFattet } returns
                mockk {
                    every { ansvarligSaksbehandler } returns "saksbehandler"
                    every { ansvarligEnhet } returns Enheter.defaultEnhet.enhetNr
                    every { tidspunkt } returns Tidspunkt.now()
                }
        }

    override suspend fun underkjennVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto =
        mockk<VedtakDto> {
            every { id } returns 123L
        }

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
        sakId: SakId,
        dato: LocalDate,
        brukerTokenInfo: BrukerTokenInfo,
    ): LoependeYtelseDTO = LoependeYtelseDTO(true, false, LocalDate.now())

    override suspend fun hentVedtak(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): VedtakDto? = null
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
        sakId: SakId,
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

    override suspend fun opprettSpesifiktBrev(
        sakId: SakId,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        TODO("Not yet implemented")
    }

    override suspend fun slettBrev(
        brevId: Long,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun oppdaterSpesifiktBrev(
        sakId: SakId,
        brevId: BrevID,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        TODO("Not yet implemented")
    }

    override suspend fun ferdigstillJournalFoerOgDistribuerBrev(
        req: FerdigstillJournalFoerOgDistribuerOpprettetBrev,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevStatusResponse {
        TODO("Not yet implemented")
    }

    override suspend fun opprettKlageOversendelsesbrevISak(
        klageId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev = opprettetBrevDto(brevId++)

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev = opprettetBrevDto(brevId++)

    override suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun ferdigstillOversendelseBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun journalfoerBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): JournalpostIdDto = JournalpostIdDto(listOf(UUID.randomUUID().toString()))

    override suspend fun distribuerBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): BestillingsIdDto = BestillingsIdDto(listOf(UUID.randomUUID().toString()))

    override suspend fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev = opprettetBrevDto(brevId)

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
    ): Brev = opprettetBrevDto(brevId)

    override suspend fun hentOversendelsesbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev = opprettetBrevDto(brevId)

    override suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        TODO("Not yet implemented")
    }

    override suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        sakId: SakId,
        brevtype: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        TODO("Not yet implemented")
    }

    private fun opprettetBrevDto(brevId: Long) =
        Brev(
            id = brevId,
            status = Status.OPPRETTET,
            mottakere =
                listOf(
                    Mottaker(
                        UUID.randomUUID(),
                        navn = "Mottaker mottakersen",
                        foedselsnummer = MottakerFoedselsnummer("19448310410"),
                        orgnummer = null,
                        adresse =
                            Adresse(
                                adresseType = "",
                                landkode = "",
                                land = "",
                            ),
                        journalpostId = null,
                        bestillingId = null,
                    ),
                ),
            sakId = randomSakId(),
            behandlingId = null,
            tittel = null,
            spraak = Spraak.NB,
            prosessType = BrevProsessType.REDIGERBAR,
            soekerFnr = "",
            statusEndret = Tidspunkt.now(),
            opprettet = Tidspunkt.now(),
            brevtype = Brevtype.MANUELT,
            brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
        )
}

class SkjermingKlientTest : SkjermingKlient {
    override suspend fun personErSkjermet(fnr: String): Boolean = false

    override val serviceName: String
        get() = "Navansatt"
    override val beskrivelse: String
        get() = "Henter navn for saksbehandlerident"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}

class GosysOppgaveKlientTest : GosysOppgaveKlient {
    override suspend fun hentOppgaver(
        aktoerId: String?,
        saksbehandler: String?,
        tema: List<String>,
        enhetsnr: Enhetsnummer?,
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
            Enheter.PORSGRUNN.enhetNr,
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

    override suspend fun hentNavkontorForOmraade(omraade: String): Navkontor = Navkontor("1202 NAV BERGEN SØR", Enheter.PORSGRUNN.enhetNr)
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

class KodeverkKlientTest : KodeverkKlient {
    override suspend fun hent(
        kodeverkNavn: KodeverkNavn,
        ekskluderUgyldige: Boolean,
        brukerTokenInfo: BrukerTokenInfo,
    ): KodeverkResponse {
        val betydning =
            Betydning(
                gyldigTil = "1900-01-01",
                gyldigFra = "9999-12-31",
                beskrivelser = mapOf(Pair("nb", Beskrivelse("term", "tekst"))),
            )
        return KodeverkResponse(
            mapOf(
                Pair(
                    LandNormalisert.SOR_GEORGIA_OG_SOR_SANDWICHOYENE.isoCode,
                    listOf(betydning),
                ),
            ),
        )
    }
}

class BrevKlientTest : BrevKlient {
    override suspend fun tilbakestillVedtaksbrev(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): BrevPayload {
        TODO("Not yet implemented")
    }

    override suspend fun ferdigstillVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Pdf {
        TODO("Not yet implemented")
    }

    override suspend fun opprettVedtaksbrev(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        brevRequest: BrevRequest,
    ): Brev {
        TODO("Not yet implemented")
    }
}

class PdltjenesterKlientTest : PdlTjenesterKlient {
    override fun hentPdlModellForSaktype(
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

    override suspend fun hentPdlIdentifikator(ident: String): PdlIdentifikator? {
        TODO("Not yet implemented")
    }

    // Greit å holde denne i sync ellers mocke til om man vil teste relaterte identer. Hvis ikke vil man ikke finne saker på identer man tester på.
    override suspend fun hentPdlFolkeregisterIdenter(ident: String): PdlFolkeregisterIdentListe =
        PdlFolkeregisterIdentListe(
            listOf(PdlIdentifikator.FolkeregisterIdent(Folkeregisteridentifikator.of(ident))),
        )

    override suspend fun hentAdressebeskyttelseForPerson(
        hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest,
    ): AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT

    override suspend fun hentAktoerId(foedselsnummer: String): PdlIdentifikator.AktoerId? = PdlIdentifikator.AktoerId("0123456789")

    override suspend fun hentPerson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): Person = soeker()

    override suspend fun hentOpplysningsperson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): PersonDTO = hentPdlModellForSaktype(foedselsnummer, rolle, sakType)

    override suspend fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
    ): HistorikkForeldreansvar {
        TODO("Not yet implemented")
    }

    override suspend fun hentPersongalleri(
        foedselsnummer: String,
        sakType: SakType,
        innsender: String?,
    ): Persongalleri? = persongalleri()

    override val serviceName: String
        get() = "Pdl tjenester"
    override val beskrivelse: String
        get() = "Henter enheter pdl data"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}

class InntektskomponentKlientTest : InntektskomponentKlient {
    override suspend fun hentInntekt(
        personident: String,
        maanedFom: YearMonth,
        maanedTom: YearMonth,
    ): AInntektReponsData =
        AInntektReponsData(
            emptyList(),
        )
}
