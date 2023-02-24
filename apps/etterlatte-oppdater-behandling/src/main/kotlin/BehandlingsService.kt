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
import no.nav.etterlatte.libs.common.behandling.Omberegningshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.sak.Saker

interface Behandling {
    fun grunnlagEndretISak(sak: Long)

    fun sendDoedshendelse(doedshendelse: Doedshendelse)
    fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse)
    fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse)
    fun sendAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse)

    fun hentAlleSaker(): Saker

    fun opprettOmberegning(omberegningshendelse: Omberegningshendelse): HttpResponse
    fun migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert()
}

class BehandlingsService(
    private val behandling_app: HttpClient,
    private val url: String
) : Behandling {
    override fun grunnlagEndretISak(sak: Long) {
        runBlocking {
            behandling_app.post("$url/saker/$sak/hendelse/grunnlagendret") {}
        }
    }

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

    override fun opprettOmberegning(omberegningshendelse: Omberegningshendelse): HttpResponse {
        return runBlocking {
            behandling_app.post("$url/omberegning") {
                contentType(ContentType.Application.Json)
                setBody(omberegningshendelse)
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