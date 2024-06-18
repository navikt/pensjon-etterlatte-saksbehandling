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
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktDto.JobbType
import no.nav.etterlatte.libs.common.behandling.OpprettRevurderingForAktivitetspliktResponse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.omregning.OpprettOmregningResponse
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sak.HentSakerRequest
import no.nav.etterlatte.libs.common.sak.KjoeringRequest
import no.nav.etterlatte.libs.common.sak.KjoeringStatus
import no.nav.etterlatte.libs.common.sak.LagreKjoeringRequest
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
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

    fun hentAlleSaker(
        kjoering: String,
        antall: Int,
        spesifikkeSaker: List<Long> = listOf(),
        ekskluderteSaker: List<Long> = listOf(),
        sakType: SakType? = null,
    ): Saker

    fun opprettOmregning(omregningshendelse: Omregningshendelse): OpprettOmregningResponse

    fun opprettRevurderingAktivitetsplikt(
        sakId: Long,
        frist: Tidspunkt,
        behandlingsmaaned: YearMonth,
        jobbType: JobbType,
    ): OpprettRevurderingForAktivitetspliktResponse

    fun migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(saker: Saker): SakIDListe

    fun avbryt(behandlingId: UUID): HttpResponse

    fun finnEllerOpprettSak(
        sakType: SakType,
        foedselsNummerDTO: FoedselsnummerDTO,
    ): Sak

    fun hentBehandling(behandlingId: UUID): DetaljertBehandling

    fun opprettOppgave(
        sakId: Long,
        oppgaveType: OppgaveType,
        referanse: String? = null,
        merknad: String? = null,
        frist: Tidspunkt? = null,
    ): UUID

    fun leggInnBrevutfall(request: BrevutfallOgEtterbetalingDto)

    fun lagreKjoering(
        sakId: Long,
        status: KjoeringStatus,
        kjoering: String,
    )

    fun lagreFullfoertKjoering(request: LagreKjoeringRequest)
}

class BehandlingServiceImpl(
    private val behandlingKlient: HttpClient,
    private val url: String,
) : BehandlingService {
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

    override fun opprettOmregning(omregningshendelse: Omregningshendelse): OpprettOmregningResponse =
        runBlocking {
            behandlingKlient
                .post("$url/omregning") {
                    contentType(ContentType.Application.Json)
                    setBody(omregningshendelse)
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
        spesifikkeSaker: List<Long>,
        ekskluderteSaker: List<Long>,
        sakType: SakType?,
    ): Saker =
        runBlocking {
            behandlingKlient
                .post("$url/saker/$kjoering/$antall") {
                    contentType(ContentType.Application.Json)
                    setBody(HentSakerRequest(spesifikkeSaker, ekskluderteSaker, sakType))
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

    override fun opprettOppgave(
        sakId: Long,
        oppgaveType: OppgaveType,
        referanse: String?,
        merknad: String?,
        frist: Tidspunkt?,
    ): UUID =
        runBlocking {
            behandlingKlient
                .post("$url/oppgaver/sak/$sakId/opprett") {
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
        sakId: Long,
        frist: Tidspunkt,
        behandlingsmaaned: YearMonth,
        jobbType: JobbType,
    ): OpprettRevurderingForAktivitetspliktResponse =
        runBlocking {
            behandlingKlient
                .post("$url/api/sak/$sakId/aktivitetsplikt/revurdering") {
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

    override fun leggInnBrevutfall(request: BrevutfallOgEtterbetalingDto) {
        runBlocking {
            behandlingKlient.post("$url/api/behandling/${request.behandlingId}/info/brevutfall") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override fun lagreKjoering(
        sakId: Long,
        status: KjoeringStatus,
        kjoering: String,
    ) {
        runBlocking {
            behandlingKlient.put("$url/omregning/kjoering") {
                contentType(ContentType.Application.Json)
                setBody(
                    KjoeringRequest(
                        kjoering = kjoering,
                        status = status,
                        sakId = sakId,
                    ),
                )
            }
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
