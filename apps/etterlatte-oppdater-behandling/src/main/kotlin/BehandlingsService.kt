package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.sak.Saker

interface Behandling {
    fun sendDoedshendelse(doedshendelse: Doedshendelse)
    fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse)
    fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse)
    fun sendAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse)

    fun hentAlleSaker(): Saker

    fun opprettOmregning(omregningshendelse: Omregningshendelse): HttpResponse
    fun migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert()
}

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String
) : Behandling {
    override fun sendDoedshendelse(doedshendelse: Doedshendelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/doedshendelse") {
                contentType(ContentType.Application.Json)
                setBody(doedshendelse)
            }
        }
    }

    override fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/utflyttingshendelse") {
                contentType(ContentType.Application.Json)
                setBody(utflyttingsHendelse)
            }
        }
    }

    override fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/forelderbarnrelasjonhendelse") {
                contentType(ContentType.Application.Json)
                setBody(forelderBarnRelasjon)
            }
        }
    }

    override fun sendAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse) {
        runBlocking {
            behandling_app.post("$url/grunnlagsendringshendelse/adressebeskyttelse") {
                contentType(ContentType.Application.Json)
                setBody(adressebeskyttelse)
            }
        }
    }

    override fun opprettOmregning(omregningshendelse: Omregningshendelse): HttpResponse {
        return runBlocking {
            behandling_app.post("$url/omregning") {
                contentType(ContentType.Application.Json)
                setBody(omregningshendelse)
            }
        }
    }

    override fun migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert() {
        return runBlocking {
            behandling_app.post("$url/behandlinger/settTilbakeTilVilkaarsvurdert") {
                contentType(ContentType.Application.Json)
            }
        }
    }

    override fun hentAlleSaker(): Saker =
        runBlocking {
            behandling_app.get("$url/saker").body()
        }
}