package no.nav.etterlatte

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
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Bostedsadresse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.SivilstandHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import java.util.UUID

interface BehandlingService {
    fun sendDoedshendelse(doedshendelse: Doedshendelse)

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

    fun migrer(hendelse: MigreringRequest): BehandlingOgSak

    fun avbryt(behandlingId: UUID): HttpResponse
}

data class ReguleringFeiletHendelse(val sakId: Long)

class BehandlingServiceImpl(
    private val behandlingKlient: HttpClient,
    private val url: String,
) : BehandlingService {
    override fun sendDoedshendelse(doedshendelse: Doedshendelse) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/doedshendelse") {
                contentType(ContentType.Application.Json)
                setBody(doedshendelse)
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

    override fun migrer(hendelse: MigreringRequest): BehandlingOgSak =
        runBlocking {
            behandlingKlient.post("$url/migrering") {
                contentType(ContentType.Application.Json)
                setBody(hendelse)
            }.body()
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
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID, val sakType: SakType)
