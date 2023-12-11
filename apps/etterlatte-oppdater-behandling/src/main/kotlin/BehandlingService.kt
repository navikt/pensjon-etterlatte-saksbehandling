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
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 28, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 31, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 32, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 33, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 34, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 35, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 36, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 37, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 38, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 39, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 40, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 41, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 42, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 43, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 44, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 45, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 46, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 47, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 48, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 49, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 50, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 51, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 52, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 53, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 54, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 55, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 56, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 57, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 58, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 59, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 60, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 61, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 62, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 63, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 64, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 65, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 66, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 67, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 68, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 69, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 70, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 71, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 72, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 73, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 74, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 75, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 76, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 77, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 78, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 79, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 80, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 81, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 82, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 83, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 84, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 85, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 86, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 87, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 88, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 89, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 90, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 91, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 92, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 93, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 94, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 96, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 97, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 98, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 99, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 100, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 101, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 102, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 103, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 105, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 106, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 107, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 108, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 109, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 110, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 111, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 112, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 113, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 114, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 115, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 116, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 117, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 119, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 120, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 121, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 122, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 123, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 124, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 127, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 128, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 129, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 130, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 134, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 135, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 136, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 137, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 138, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 139, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 140, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 141, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 142, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 143, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 144, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 145, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 146, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 147, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 148, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 149, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 151, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 152, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 153, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 154, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 155, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 156, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 157, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 158, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 159, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 160, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 161, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 162, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 163, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 167, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 168, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 169, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 170, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 171, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 172, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 173, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 174, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 175, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 176, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 177, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 179, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 180, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 181, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 182, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 183, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 184, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 185, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 186, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 187, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 188, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 189, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 190, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 191, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 192, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 193, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 194, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 195, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 196, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 197, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 198, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 199, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 200, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 201, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 202, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 203, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 204, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 205, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 206, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 207, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 208, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 210, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 223, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 224, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6041, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6042, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6044, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6045, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6155, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6156, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6157, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 6158, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 7663, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 8891, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 8892, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 8899, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 8904, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 8908, "IKKE_RELEVANT"),
                Sak("IKKE_RELEVANT", SakType.BARNEPENSJON, 8909, "IKKE_RELEVANT"),
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
