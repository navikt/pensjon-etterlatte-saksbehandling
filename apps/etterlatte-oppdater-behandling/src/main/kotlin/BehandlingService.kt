package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.Omregningshendelse
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.pdlhendelse.Adressebeskyttelse
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.ForelderBarnRelasjonHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.UtflyttingsHendelse
import no.nav.etterlatte.libs.common.pdlhendelse.VergeMaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.sak.BehandlingOgSak
import no.nav.etterlatte.libs.common.sak.SakIDListe
import no.nav.etterlatte.libs.common.sak.Saker
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import java.util.*

interface BehandlingService {
    fun sendDoedshendelse(doedshendelse: Doedshendelse)
    fun sendUtflyttingshendelse(utflyttingsHendelse: UtflyttingsHendelse)
    fun sendForelderBarnRelasjonHendelse(forelderBarnRelasjon: ForelderBarnRelasjonHendelse)
    fun sendAdressebeskyttelseHendelse(adressebeskyttelse: Adressebeskyttelse)
    fun sendVergeMaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt)
    fun sendReguleringFeiletHendelse(reguleringFeilethendelse: ReguleringFeiletHendelse)
    fun hentAlleSaker(): Saker
    fun opprettOmregning(omregningshendelse: Omregningshendelse): OpprettOmregningResponse
    fun migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert(): SakIDListe
    fun migrer(hendelse: MigreringRequest): BehandlingOgSak
}

data class ReguleringFeiletHendelse(val sakId: Long)

class BehandlingServiceImpl(
    private val behandlingKlient: HttpClient,
    private val url: String
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

    override fun sendVergeMaalEllerFremtidsfullmakt(vergeMaalEllerFremtidsfullmakt: VergeMaalEllerFremtidsfullmakt) {
        runBlocking {
            behandlingKlient.post("$url/grunnlagsendringshendelse/vergemaalellerfremtidsfullmakt") {
                contentType(ContentType.Application.Json)
                setBody(vergeMaalEllerFremtidsfullmakt)
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

    override fun migrerAlleTempBehandlingerTilbakeTilVilkaarsvurdert(): SakIDListe {
        return runBlocking {
            behandlingKlient.post("$url/behandlinger/settTilbakeTilVilkaarsvurdert") {
                contentType(ContentType.Application.Json)
            }.body()
        }
    }

    override fun migrer(hendelse: MigreringRequest): BehandlingOgSak = runBlocking {
        behandlingKlient.post("$url/migrering") {
            contentType(ContentType.Application.Json)
            setBody(hendelse)
        }.body()
    }

    override fun hentAlleSaker(): Saker =
        runBlocking {
            behandlingKlient.get("$url/saker").body()
        }
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID, val sakType: SakType)