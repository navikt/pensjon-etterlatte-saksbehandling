package no.nav.etterlatte

import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingEnhet
import no.nav.etterlatte.behandling.domain.ArbeidsFordelingRequest
import no.nav.etterlatte.behandling.domain.Navkontor
import no.nav.etterlatte.behandling.etteroppgjoer.HendelseslisteFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.PensjonsgivendeInntektFraSkatt
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.AInntektReponsData
import no.nav.etterlatte.behandling.etteroppgjoer.inntektskomponent.InntektskomponentKlient
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.AxsysKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.behandling.klienter.NavAnsattKlient
import no.nav.etterlatte.behandling.klienter.Norg2Klient
import no.nav.etterlatte.behandling.klienter.OpprettJournalpostDto
import no.nav.etterlatte.behandling.klienter.SaksbehandlerInfo
import no.nav.etterlatte.behandling.klienter.TilbakekrevingKlient
import no.nav.etterlatte.behandling.klienter.TrygdetidKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.behandling.randomSakId
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
import no.nav.etterlatte.kodeverk.Beskrivelse
import no.nav.etterlatte.kodeverk.Betydning
import no.nav.etterlatte.kodeverk.KodeverkKlient
import no.nav.etterlatte.kodeverk.KodeverkNavn
import no.nav.etterlatte.kodeverk.KodeverkResponse
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.beregning.AvkortingDto
import no.nav.etterlatte.libs.common.beregning.BeregnetEtteroppgjoerResultatDto
import no.nav.etterlatte.libs.common.beregning.BeregningOgAvkortingDto
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnFaktiskInntektRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkorting
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerBeregnetAvkortingRequest
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerHentBeregnetResultatRequest
import no.nav.etterlatte.libs.common.beregning.InntektsjusteringAvkortingInfoResponse
import no.nav.etterlatte.libs.common.brev.BestillingsIdDto
import no.nav.etterlatte.libs.common.brev.JournalpostIdDto
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
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
import no.nav.etterlatte.libs.common.vedtak.InnvilgetPeriodeDto
import no.nav.etterlatte.libs.common.vedtak.LoependeYtelseDTO
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.common.vedtak.VedtakSammendragDto
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.PingResultUp
import no.nav.etterlatte.libs.ktor.ServiceStatus
import no.nav.etterlatte.libs.ktor.route.SakTilgangsSjekk
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
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

class BeregningKlientTest :
    BeregningKlient,
    SakTilgangsSjekk {
    override suspend fun hentBeregningOgAvkorting(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregningOgAvkortingDto {
        TODO("Not yet implemented")
    }

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

    override suspend fun hentAvkortingForForbehandlingEtteroppgjoer(
        request: EtteroppgjoerBeregnetAvkortingRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): EtteroppgjoerBeregnetAvkorting =
        EtteroppgjoerBeregnetAvkorting(
            AvkortingDto(
                avkortingGrunnlag = emptyList(),
                avkortetYtelse = emptyList(),
            ),
            null,
        )

    override suspend fun hentBeregnetEtteroppgjoerResultat(
        request: EtteroppgjoerHentBeregnetResultatRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto {
        TODO("Not yet implemented")
    }

    override suspend fun beregnAvkortingFaktiskInntekt(
        request: EtteroppgjoerBeregnFaktiskInntektRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BeregnetEtteroppgjoerResultatDto = throw NotImplementedError("Ikke implementert for testklient")

    override suspend fun opprettBeregningsgrunnlagFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }

    override suspend fun beregnBehandling(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }
}

class TrygdetidKlientTest : TrygdetidKlient {
    override suspend fun kopierTrygdetidFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
    }
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

    override suspend fun hentIverksatteVedtak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<VedtakSammendragDto> {
        TODO("Not yet implemented")
    }

    override suspend fun hentSakerMedUtbetalingForInntektsaar(
        inntektsaar: Int,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<SakId> {
        TODO("Not yet implemented")
    }

    override suspend fun hentInnvilgedePerioder(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<InnvilgetPeriodeDto> =
        listOf(
            InnvilgetPeriodeDto(
                periode =
                    Periode(
                        fom = YearMonth.of(2024, 1),
                        tom = YearMonth.of(2024, 11),
                    ),
                vedtak = emptyList(),
            ),
        )
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
    override suspend fun tilbakestillStrukturertBrev(
        brevID: BrevID,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevPayload {
        TODO("Not yet implemented")
    }

    override suspend fun ferdigstillStrukturertBrev(
        behandlingId: UUID,
        brevtype: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun ferdigstillJournalfoerStrukturertBrev(
        behandlingId: UUID,
        brevType: Brevtype,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun genererPdf(
        brevID: BrevID,
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Pdf {
        TODO("Not yet implemented")
    }

    override suspend fun opprettStrukturertBrev(
        behandlingId: UUID,
        brevRequest: BrevRequest,
        brukerTokenInfo: BrukerTokenInfo,
    ): Brev {
        TODO("Not yet implemented")
    }

    override suspend fun hentVedtaksbrev(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ): Brev? {
        TODO("Not yet implemented")
    }

    override suspend fun hentBrev(
        sakId: SakId,
        brevId: Long,
        brukerTokenInfo: BrukerTokenInfo,
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

    override fun hentPdlModellDoedshendelseForSaktype(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDoedshendelseDto = mockDoedshendelsePerson()

    override fun hentPdlModellFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDTO = mockPerson()

    override fun hentPdlModellDoedshendelseFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDoedshendelseDto = mockDoedshendelsePerson()

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

class SigrunKlienTest : SigrunKlient {
    override suspend fun hentPensjonsgivendeInntekt(
        ident: String,
        inntektsaar: Int,
    ): PensjonsgivendeInntektFraSkatt = PensjonsgivendeInntektFraSkatt.stub()

    override suspend fun hentHendelsesliste(
        antall: Int,
        sekvensnummerStart: Long,
        brukAktoerId: Boolean,
    ): HendelseslisteFraSkatt = HendelseslisteFraSkatt.stub()
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
