package no.nav.etterlatte

import com.fasterxml.jackson.databind.node.ObjectNode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.oppgave.EndrePaaVentRequest
import no.nav.etterlatte.libs.common.oppgave.NyOppgaveDto
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.VentefristGaarUtRequest
import no.nav.etterlatte.libs.common.oppgave.VentefristerGaarUtResponse
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.DoedshendelsePdl
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.route.FoedselsnummerDTO
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

    fun sendReguleringFeiletHendelse(reguleringFeilethendelse: ReguleringFeiletHendelse)

    fun hentAlleSaker(): Saker

    fun opprettOmregning(omregningshendelse: Omregningshendelse): OpprettOmregningResponse

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

    fun taAvVent(request: VentefristGaarUtRequest): VentefristerGaarUtResponse

    fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
    )

    fun leggInnBrevutfall(request: BrevutfallOgEtterbetalingDto)
}

data class ReguleringFeiletHendelse(val sakId: Long)

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

    override fun sendReguleringFeiletHendelse(reguleringFeilethendelse: ReguleringFeiletHendelse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/reguleringfeilet") {
                contentType(ContentType.Application.Json)
                setBody(reguleringFeilethendelse)
            }
        }
    }

    override fun opprettOmregning(omregningshendelse: Omregningshendelse): OpprettOmregningResponse {
        return runBlocking {
            behandlingKlient.post("$url/omregning") {
                contentType(ContentType.Application.Json)
                setBody(omregningshendelse)
            }.body()
        }
    }

    override fun migrerAlleTempBehandlingerTilbakeTilTrygdetidOppdatert(saker: Saker): SakIDListe {
        return runBlocking {
            behandlingKlient.post("$url/behandlinger/settTilbakeTilTrygdetidOppdatert") {
                contentType(ContentType.Application.Json)
                setBody(saker)
            }.body()
        }
    }

    override fun hentAlleSaker(): Saker =
        runBlocking {
            behandlingKlient.get("$url/saker").body()
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
        behandlingKlient.post("$url/personer/saker/${sakType.name}") {
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
    ): UUID {
        return runBlocking {
            behandlingKlient.post("$url/oppgaver/sak/$sakId/opprett") {
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
            }.body<ObjectNode>().let {
                UUID.fromString(it["id"].textValue())
            }
        }
    }

    override fun taAvVent(request: VentefristGaarUtRequest): VentefristerGaarUtResponse =
        runBlocking {
            behandlingKlient.put("$url/oppgaver/ventefrist-gaar-ut") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }

    override fun oppdaterStatusOgMerknad(
        oppgaveId: UUID,
        merknad: String,
    ) {
        runBlocking {
            behandlingKlient.patch("$url/api/oppgaver/$oppgaveId/merknad") {
                contentType(ContentType.Application.Json)
                setBody(EndrePaaVentRequest(merknad, true))
            }
        }
    }

    override fun leggInnBrevutfall(request: BrevutfallOgEtterbetalingDto) {
        runBlocking {
            behandlingKlient.post("$url/api/behandling/${request.behandlingId}/info/brevutfall") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID, val sakType: SakType)
