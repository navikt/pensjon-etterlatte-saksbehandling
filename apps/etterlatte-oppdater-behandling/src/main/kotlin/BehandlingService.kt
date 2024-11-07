package no.nav.etterlatte

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BrevutfallOgEtterbetalingDto
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.DoedshendelseBrevDistribuert
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktVarigUnntakDto
import no.nav.etterlatte.libs.common.behandling.OpprettOppgaveForAktivitetspliktVarigUnntakResponse
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktResponse
import no.nav.etterlatte.libs.common.behandling.SakMedBehandlinger
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.inntektsjustering.AarligInntektsjusteringRequest
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.Folkeregisteridentifikatorhendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingRequest
import no.nav.etterlatte.libs.common.revurdering.AutomatiskRevurderingResponse
import no.nav.etterlatte.libs.common.sak.HentSakerRequest
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
import no.nav.etterlatte.libs.tidshendelser.JobbType
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

interface BehandlingService {
    fun sendDoedshendelse(doedshendelse: DoedshendelsePdl)

    fun oppdaterDoedshendelseBrevDistribuert(doedshendelseBrevDistribuert: DoedshendelseBrevDistribuert)

    fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse)

    fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse)

    fun sendAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse)

    fun sendAdresseHendelse(bostedsadresse: Bostedsadresse)

    fun sendVergeMaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt)

    fun sendSivilstandHendelse(sivilstandHendelse: SivilstandHendelse)

    fun sendFolkeregisteridentifikatorhendelse(hendelse: Folkeregisteridentifikatorhendelse): HttpResponse

    fun hentAlleSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<SakId> = listOf(),
        ekskluderteSaker: List<SakId> = listOf(),
        sakType: SakType? = null,
        loependeFom: YearMonth? = null,
    ): Saker

    fun opprettAutomatiskRevurdering(request: AutomatiskRevurderingRequest): AutomatiskRevurderingResponse

    fun opprettRevurderingAktivitetsplikt(
        sakId: SakId,
        frist: Tidspunkt,
        behandlingsmaaned: YearMonth,
        jobbType: JobbType,
    ): OpprettRevurderingForAktivitetspliktResponse

    fun opprettOppgaveAktivitetspliktVarigUnntak(
        sakId: SakId,
        frist: Tidspunkt,
        referanse: String? = null,
        jobbType: JobbType,
    ): OpprettOppgaveForAktivitetspliktVarigUnntakResponse

    fun migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(saker: Saker): SakIDListe

    fun avbryt(behandlingId: UUID): HttpResponse

    fun finnEllerOpprettSak(
        sakType: SakType,
        foedselsNummerDTO: FoedselsnummerDTO,
    ): Sak

    fun hentBehandling(behandlingId: UUID): DetaljertBehandling

    fun hentBehandlingerForSak(foedselsNummerDTO: FoedselsnummerDTO): SakMedBehandlinger

    fun startAarligInntektsjustering(request: AarligInntektsjusteringRequest): HttpResponse

    fun opprettOppgave(
        sakId: SakId,
        oppgaveType: OppgaveType,
        referanse: String? = null,
        merknad: String? = null,
        frist: Tidspunkt? = null,
    ): UUID

    fun leggInnBrevutfall(request: BrevutfallOgEtterbetalingDto)

    fun lagreKjoering(
        sakId: SakId,
        status: KjoeringStatus,
        kjoering: String,
        begrunnelse: String? = null,
        corrId: String? = null,
    )

    fun lagreFullfoertKjoering(request: LagreKjoeringRequest)
}

class BehandlingServiceImpl(
    private val behandlingKlient: HttpClient,
    private val url: String,
) : BehandlingService {
    private val logger = LoggerFactory.getLogger(BehandlingServiceImpl::class.java)

    override fun sendDoedshendelse(doedshendelse: DoedshendelsePdl) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/doedshendelse") {
                contentType(ContentType.Application.Json)
                setBody(doedshendelse)
            }
        }
    }

    override fun oppdaterDoedshendelseBrevDistribuert(doedshendelseBrevDistribuert: DoedshendelseBrevDistribuert) {
        runBlocking {
            behandlingKlient.post("$url/doedshendelse/brevdistribuert") {
                contentType(ContentType.Application.Json)
                setBody(doedshendelseBrevDistribuert)
            }
        }
    }

    override fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/utflyttingshendelse") {
                contentType(ContentType.Application.Json)
                setBody(utflyttingsHendelse)
            }
        }
    }

    override fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                contentType(ContentType.Application.Json)
                setBody(forelderBarnRelasjon)
            }
        }
    }

    override fun sendAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/adressebeskyttelse") {
                contentType(ContentType.Application.Json)
                setBody(adressebeskyttelse)
            }
        }
    }

    override fun sendAdresseHendelse(bostedsadresse: Bostedsadresse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/bostedsadresse") {
                contentType(ContentType.Application.Json)
                setBody(bostedsadresse)
            }
        }
    }

    override fun sendVergeMaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/vergemaalellerfremtidsfullmakt") {
                contentType(ContentType.Application.Json)
                setBody(vergeMaalEllerFremtidsfullmakt)
            }
        }
    }

    override fun sendSivilstandHendelse(sivilstandHendelse: SivilstandHendelse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/sivilstandhendelse") {
                contentType(ContentType.Application.Json)
                setBody(sivilstandHendelse)
            }
        }
    }

    override fun sendFolkeregisteridentifikatorhendelse(hendelse: Folkeregisteridentifikatorhendelse) =
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/folkeregisteridentifikatorhendelse") {
                contentType(ContentType.Application.Json)
                setBody(hendelse)
            }
        }

    override fun opprettAutomatiskRevurdering(request: AutomatiskRevurderingRequest): AutomatiskRevurderingResponse =
        runBlocking {
            behandlingKlient
                .post("$url/automatisk-revurdering") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
        }

    override fun migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(saker: Saker): SakIDListe =
        runBlocking {
            behandlingKlient
                .post("$url/behandlinger/settTilbakeTilTrygdetidOppdatert") {
                    contentType(ContentType.Application.Json)
                    setBody(saker)
                }.body()
        }

    override fun hentAlleSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<SakId>,
        ekskluderteSaker: List<SakId>,
        sakType: SakType?,
        loependeFom: YearMonth?,
    ): Saker =
        runBlocking {
            behandlingKlient
                .post("$url/saker/$kjoering/$antall") {
                    contentType(ContentType.Application.Json)
                    setBody(HentSakerRequest(spesifikkeSaker, ekskluderteSaker, sakType, loependeFom))
                }.body()
        }

    override fun avbryt(behandlingId: UUID) =
        runBlocking {
            behandlingKlient.put("$url/migrering/$behandlingId/avbryt") {
                contentType(ContentType.Application.Json)
            }
        }

    override fun finnEllerOpprettSak(
        sakType: SakType,
        foedselsNummerDTO: FoedselsnummerDTO,
    ) = runBlocking {
        behandlingKlient
            .post("$url/personer/saker/${sakType.name}") {
                contentType(ContentType.Application.Json)
                setBody(foedselsNummerDTO)
            }.body<Sak>()
    }

    override fun hentBehandling(behandlingId: UUID): DetaljertBehandling =
        runBlocking {
            behandlingKlient.get("$url/behandlinger/$behandlingId").body()
        }

    override fun hentBehandlingerForSak(foedselsNummerDTO: FoedselsnummerDTO): SakMedBehandlinger =
        runBlocking {
            behandlingKlient
                .post("$url/personer/behandlingerforsak") {
                    contentType(ContentType.Application.Json)
                    setBody(foedselsNummerDTO)
                }.body()
        }

    override fun startAarligInntektsjustering(request: AarligInntektsjusteringRequest) =
        runBlocking {
            behandlingKlient.post("$url/inntektsjustering/jobb") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    override fun opprettOppgave(
        sakId: SakId,
        oppgaveType: OppgaveType,
        referanse: String?,
        merknad: String?,
        frist: Tidspunkt?,
    ): UUID =
        runBlocking {
            behandlingKlient
                .post("$url/oppgaver/sak/${sakId.sakId}/opprett") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        NyOppgaveDto(
                            OppgaveKilde.HENDELSE,
                            oppgaveType,
                            merknad,
                            referanse,
                            frist,
                        ),
                    )
                }.body<ObjectNode>()
                .let {
                    UUID.fromString(it["id"].textValue())
                }
        }

    override fun opprettRevurderingAktivitetsplikt(
        sakId: SakId,
        frist: Tidspunkt,
        behandlingsmaaned: YearMonth,
        jobbType: JobbType,
    ): OpprettRevurderingForAktivitetspliktResponse =
        runBlocking {
            behandlingKlient
                .post("$url/api/sak/${sakId.sakId}/aktivitetsplikt/revurdering") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettRevurderingForAktivitetspliktDto(
                            sakId = sakId,
                            frist = frist,
                            behandlingsmaaned = behandlingsmaaned,
                            jobbType = jobbType,
                        ),
                    )
                }.body<OpprettRevurderingForAktivitetspliktResponse>()
        }

    override fun opprettOppgaveAktivitetspliktVarigUnntak(
        sakId: SakId,
        frist: Tidspunkt,
        referanse: String?,
        jobbType: JobbType,
    ): OpprettOppgaveForAktivitetspliktVarigUnntakResponse =
        runBlocking {
            behandlingKlient
                .post("$url/api/sak/${sakId.sakId}/aktivitetsplikt/varigUnntak") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        OpprettOppgaveForAktivitetspliktVarigUnntakDto(
                            sakId = sakId,
                            referanse = referanse,
                            frist = frist,
                            jobbType = jobbType,
                        ),
                    )
                }.body<OpprettOppgaveForAktivitetspliktVarigUnntakResponse>()
        }

    override fun leggInnBrevutfall(request: BrevutfallOgEtterbetalingDto) {
        runBlocking {
            behandlingKlient.post("$url/api/behandling/${request.behandlingId}/info/brevutfall") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override fun lagreKjoering(
        sakId: SakId,
        status: KjoeringStatus,
        kjoering: String,
        begrunnelse: String?,
        corrId: String?,
    ) {
        runBlocking {
            behandlingKlient.put("$url/omregning/kjoering") {
                contentType(ContentType.Application.Json)
                setBody(
                    KjoeringRequest(
                        kjoering = kjoering,
                        status = status,
                        sakId = sakId,
                        begrunnelse = begrunnelse,
                        corrId = corrId,
                    ),
                )
            }
            logger.debug("$kjoering: kjoeringStatus for sak {} er oppdatert til: {}", sakId, status)
        }
    }

    override fun lagreFullfoertKjoering(request: LagreKjoeringRequest) {
        runBlocking {
            behandlingKlient.post("$url/omregning/kjoeringFullfoert") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}
