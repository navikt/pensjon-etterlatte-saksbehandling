package no.nav.etterlatte

import io.ktor.client.HttpClient
import io.ktor.client.call.body
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
import no.nav.etterlatte.libs.common.sak.Sak
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
        // For å regne om ytelsene til nytt regelverk ønsker vi kun å omregne saker som ikke kommer fra migrering
        // Kommenterer ut kall til behandling midlertidig for å håndtere dette manuelt.
//        runBlocking {
//            behandlingKlient.get("$url/saker").body()
//        }
        Saker(
            listOf(
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	36, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	42, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	47, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	50, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	52, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	58, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	60, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	64, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	67, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	72, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	76, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	78, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	79, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	83, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	88, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	92, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	96, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	97, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	101, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	105, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	109, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	110, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	116, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	117, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	122, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	129, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	139, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	157, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	159, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	170, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	171, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	174, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	183, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	185, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	187, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	188, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	198, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	206, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	208, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	6045, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON,	6158, "IKKE_RELEVANT"),
            ),
        )

    override fun avbryt(behandlingId: UUID) =
        runBlocking {
            behandlingKlient.put("$url/migrering/$behandlingId/avbryt") {
                contentType(ContentType.Application.Json)
            }
        }
}

data class OpprettOmregningResponse(val behandlingId: UUID, val forrigeBehandlingId: UUID, val sakType: SakType)
