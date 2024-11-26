package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.OpprettJournalpostDto
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.brev.BrevParametre
import no.nav.etterlatte.brev.Brevkoder
import no.nav.etterlatte.brev.Brevtype
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
import no.nav.etterlatte.libs.common.beregning.AarligInntektsjusteringAvkortingSjekkResponse
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
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
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.TilbakekrevingVedtakLagretDto
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.ServiceStatus
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.oppgaveGosys.EndreStatusRequest
import no.nav.etterlatte.oppgaveGosys.GosysApiOppgave
import no.nav.etterlatte.oppgaveGosys.GosysOppgaveKlient
import no.nav.etterlatte.oppgaveGosys.GosysOppgaver
import no.nav.etterlatte.person.krr.DigitalKontaktinformasjon
import no.nav.etterlatte.person.krr.KrrKlient
import no.nav.etterlatte.saksbehandler.SaksbehandlerEnhet
import java.time.LocalDate
import java.time.YearMonth
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

    override suspend fun hentPersongalleri(behandlingId: UUID): Grunnlagsopplysning<Persongalleri>? =
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
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag() // evt Grunnlag.empty()

    override suspend fun hentGrunnlagForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): Grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()

    override suspend fun hentGrunnlag(sakId: SakId): Grunnlag? = GrunnlagTestData().hentOpplysningsgrunnlag()

    override suspend fun hentAlleSakIder(fnr: String): Set<SakId> = setOf(sakId1)

    override suspend fun hentPersonSakOgRolle(fnr: String): PersonMedSakerOgRoller =
        PersonMedSakerOgRoller("08071272487", listOf(SakidOgRolle(sakId1, Saksrolle.SOEKER)))

    override suspend fun leggInnNyttGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        // NO-OP
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        request: OppdaterGrunnlagRequest,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        // NO-OP
    }

    override suspend fun lagreNyeSaksopplysninger(
        behandlingId: UUID,
        saksopplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        // NO-OP
    }

    override suspend fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        saksopplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        // NO-OP
    }

    override suspend fun leggInnNyttGrunnlagSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        // NO-OP
    }

    override suspend fun laasTilGrunnlagIBehandling(
        id: UUID,
        forrigeBehandling: UUID,
    ) {
        // NO-OP
    }

    override suspend fun hentPersonopplysningerForBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        sakType: SakType,
    ): PersonopplysningerResponse = GrunnlagTestData().hentPersonopplysninger()

    override suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ) = YearMonth.now()

    override val serviceName: String
        get() = TODO("Not yet implemented")
    override val beskrivelse: String
        get() = TODO("Not yet implemented")
    override val endpoint: String
        get() = TODO("Not yet implemented")

    override suspend fun ping(konsument: String?): PingResult {
        TODO("Not yet implemented")
    }
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

    override suspend fun aarligInntektsjusteringSjekk(
        sakId: SakId,
        aar: Int,
        sisteBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): AarligInntektsjusteringAvkortingSjekkResponse = mockk()

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
    ): Long = 123L

    override suspend fun fattVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): Long = 123L

    override suspend fun attesterVedtakTilbakekreving(
        tilbakekrevingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
        enhet: Enhetsnummer,
    ): TilbakekrevingVedtakLagretDto =
        TilbakekrevingVedtakLagretDto(
            id = 123L,
            fattetAv = "saksbehandler",
            enhet = Enheter.defaultEnhet.enhetNr,
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
        sakId: SakId,
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

    override suspend fun oppdaterSpesifiktBrev(
        sakId: SakId,
        brevParametre: BrevParametre,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        TODO("Not yet implemented")
    }

    override suspend fun ferdigstillBrev(
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
        sakId: SakId,
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

    override suspend fun hentPdlFolkeregisterIdenter(ident: String): PdlFolkeregisterIdentListe =
        PdlFolkeregisterIdentListe(
            listOf(PdlIdentifikator.FolkeregisterIdent(SOEKER_FOEDSELSNUMMER)),
        )

    override suspend fun hentAdressebeskyttelseForPerson(
        hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest,
    ): AdressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT

    override suspend fun hentAktoerId(foedselsnummer: String): PdlIdentifikator.AktoerId? = PdlIdentifikator.AktoerId("0123456789")

    override val serviceName: String
        get() = "Pdl tjenester"
    override val beskrivelse: String
        get() = "Henter enheter pdl data"
    override val endpoint: String
        get() = "endpoint"

    override suspend fun ping(konsument: String?): PingResult = PingResultUp(serviceName, ServiceStatus.UP, endpoint, serviceName)
}
