package no.nav.etterlatte.common.klienter

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.RetryResult
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ExceptionResponse
import no.nav.etterlatte.libs.common.pdl.FantIkkePersonException
import no.nav.etterlatte.libs.common.pdl.PdlFeilAarsak
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.pdl.PersonDoedshendelseDto
import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdressebeskyttelseGradering
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.GeografiskTilknytning
import no.nav.etterlatte.libs.common.person.HentAdressebeskyttelseRequest
import no.nav.etterlatte.libs.common.person.HentGeografiskTilknytningRequest
import no.nav.etterlatte.libs.common.person.HentPdlIdentRequest
import no.nav.etterlatte.libs.common.person.HentPersonHistorikkForeldreAnsvarRequest
import no.nav.etterlatte.libs.common.person.HentPersonRequest
import no.nav.etterlatte.libs.common.person.HentPersongalleriRequest
import no.nav.etterlatte.libs.common.person.PdlFolkeregisterIdentListe
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonIdent
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.person.Sivilstand
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt
import no.nav.etterlatte.libs.common.person.maskerFnr
import no.nav.etterlatte.libs.common.retry
import no.nav.etterlatte.libs.ktor.PingResult
import no.nav.etterlatte.libs.ktor.Pingable
import no.nav.etterlatte.libs.ktor.ping
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.sikkerLogg
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

interface PdlTjenesterKlient : Pingable {
    fun hentPdlModellForSaktype(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO

    fun hentPdlModellDoedshendelseForSaktype(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDoedshendelseDto

    fun hentPdlModellFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDTO

    fun hentPdlModellDoedshendelseFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDoedshendelseDto

    fun hentGeografiskTilknytning(
        foedselsnummer: String,
        saktype: SakType,
    ): GeografiskTilknytning

    suspend fun hentPdlIdentifikator(ident: String): PdlIdentifikator?

    suspend fun hentPdlFolkeregisterIdenter(ident: String): PdlFolkeregisterIdentListe

    suspend fun hentAdressebeskyttelseForPerson(hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest): AdressebeskyttelseGradering

    suspend fun hentAktoerId(foedselsnummer: String): PdlIdentifikator.AktoerId?

    suspend fun hentPerson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): Person

    // Duplikat hentPdlModellForSaktype?
    suspend fun hentOpplysningsperson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): PersonDTO

    suspend fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
    ): HistorikkForeldreansvar

    suspend fun hentPersongalleri(
        foedselsnummer: String,
        sakType: SakType,
        innsender: String?,
    ): Persongalleri?
}

class PdlTjenesterKlientImpl(
    config: Config,
    private val client: HttpClient,
) : PdlTjenesterKlient {
    private val url = config.getString("pdltjenester.url")
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun hentAdressebeskyttelseForPerson(
        hentAdressebeskyttelseRequest: HentAdressebeskyttelseRequest,
    ): AdressebeskyttelseGradering {
        logger.info("Henter person med ${hentAdressebeskyttelseRequest.ident.value.maskerFnr()} fra pdltjenester")
        return client
            .post("$url/person/adressebeskyttelse") {
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(hentAdressebeskyttelseRequest)
            }.body<AdressebeskyttelseGradering>()
    }

    override val serviceName: String
        get() = "Pdl tjenester klient"
    override val beskrivelse: String
        get() = "Henter data fra pdl via vår pdl-proxy mapper"
    override val endpoint: String
        get() = this.url

    override suspend fun ping(konsument: String?): PingResult =
        client.ping(
            pingUrl = url.plus("/health/isready"),
            logger = logger,
            serviceName = serviceName,
            beskrivelse = beskrivelse,
        )

    override fun hentPdlModellFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDTO {
        logger.info("Henter Pdl-modell for rolle ${rolle.name} for sakstyper ${saktyper.joinToString { it.name }}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, saktyper)
        val response =
            runBlocking {
                client
                    .post("$url/person/v2") {
                        contentType(ContentType.Application.Json)
                        setBody(personRequest)
                    }.body<PersonDTO>()
            }
        return response
    }

    override fun hentPdlModellDoedshendelseFlereSaktyper(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktyper: List<SakType>,
    ): PersonDoedshendelseDto {
        logger.info("Henter Pdl-modell for dødshendelser for rolle ${rolle.name} og saktyper ${saktyper.joinToString { it.name }}")
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, saktyper)
        val response =
            runBlocking {
                client
                    .post("$url/person/v2/doedshendelse") {
                        contentType(ContentType.Application.Json)
                        setBody(personRequest)
                    }.body<PersonDoedshendelseDto>()
            }
        return response
    }

    override fun hentPdlModellForSaktype(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDTO = hentPdlModellFlereSaktyper(foedselsnummer, rolle, listOf(saktype))

    override fun hentPdlModellDoedshendelseForSaktype(
        foedselsnummer: String,
        rolle: PersonRolle,
        saktype: SakType,
    ): PersonDoedshendelseDto = hentPdlModellDoedshendelseFlereSaktyper(foedselsnummer, rolle, listOf(saktype))

    override fun hentGeografiskTilknytning(
        foedselsnummer: String,
        saktype: SakType,
    ): GeografiskTilknytning {
        val request = HentGeografiskTilknytningRequest(Folkeregisteridentifikator.of(foedselsnummer), saktype)
        val response =
            runBlocking {
                client
                    .post("$url/geografisktilknytning") {
                        contentType(ContentType.Application.Json)
                        setBody(request)
                    }.body<GeografiskTilknytning>()
            }

        return response
    }

    override suspend fun hentPdlIdentifikator(ident: String): PdlIdentifikator? {
        logger.info("Henter ident fra PDL for fnr=${ident.maskerFnr()}")

        return retry<PdlIdentifikator?> {
            client
                .post("$url/pdlident") {
                    contentType(ContentType.Application.Json)
                    setBody(HentPdlIdentRequest(PersonIdent(ident)))
                }.body()
        }.let { result ->
            when (result) {
                is RetryResult.Success -> {
                    result.content
                }

                is RetryResult.Failure -> {
                    logger.error("Feil ved henting av ident fra PDL for fnr=${ident.maskerFnr()}")
                    throw result.samlaExceptions()
                }
            }
        }
    }

    override suspend fun hentPdlFolkeregisterIdenter(ident: String): PdlFolkeregisterIdentListe {
        logger.info("Henter folkeregisteridenter fra PDL for fnr=${ident.maskerFnr()}")

        return retry<PdlFolkeregisterIdentListe> {
            client
                .post("$url/folkeregisteridenter") {
                    contentType(ContentType.Application.Json)
                    setBody(HentPdlIdentRequest(PersonIdent(ident)))
                }.body()
        }.let { result ->
            when (result) {
                is RetryResult.Success -> {
                    result.content
                }

                is RetryResult.Failure -> {
                    logger.error("Feil ved henting av folkeregisteridenter fra PDL for fnr=${ident.maskerFnr()}")
                    val feil = result.samlaExceptions()

                    if (feil !is ResponseException) {
                        throw feil
                    }

                    val pdlExceptionCode =
                        try {
                            feil.response.body<ExceptionResponse>().code
                        } catch (e: Exception) {
                            logger.warn("Noe rart har skjedd her, vi får feil fra PDL som ikke har en exception response", e)
                            throw feil
                        }

                    if (pdlExceptionCode == PdlFeilAarsak.FANT_IKKE_PERSON.name) {
                        throw FantIkkePersonException("Fant ikke angitt person (ident=${ident.maskerFnr()}) i PDL", feil)
                    } else {
                        throw feil
                    }
                }
            }
        }
    }

    override suspend fun hentAktoerId(foedselsnummer: String): PdlIdentifikator.AktoerId? {
        val request = HentPdlIdentRequest(PersonIdent(foedselsnummer))

        return client
            .post("$url/aktoerid") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<PdlIdentifikator.AktoerId?>()
    }

    override suspend fun hentPerson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): Person {
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, listOf(sakType))

        return client
            .post("$url/person") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<Person>()
    }

    override suspend fun hentPersongalleri(
        foedselsnummer: String,
        sakType: SakType,
        innsender: String?,
    ): Persongalleri? =
        try {
            val persongalleriRequest =
                HentPersongalleriRequest(
                    mottakerAvYtelsen = Folkeregisteridentifikator.of(foedselsnummer),
                    saktype = sakType,
                    innsender =
                        innsender
                            ?.takeIf { Folkeregisteridentifikator.isValid(it) }
                            ?.let { Folkeregisteridentifikator.of(it) },
                )

            client
                .post("$url/galleri") {
                    contentType(ContentType.Application.Json)
                    setBody(persongalleriRequest)
                }.body<Persongalleri>()
        } catch (e: Exception) {
            logger.warn(
                "Kunne ikke hente persongalleriet fra PDL, på grunn av feil. " +
                    "Metadata om request er i sikkerlogg",
                e,
            )
            sikkerLogg.warn(
                "Kunne ikke hente persongalleriet fra PDL for soeker=$foedselsnummer i saktype " +
                    "$sakType, på grunn av feil.",
                e,
            )
            null
        }

    override suspend fun hentOpplysningsperson(
        foedselsnummer: String,
        rolle: PersonRolle,
        sakType: SakType,
    ): PersonDTO {
        val personRequest = HentPersonRequest(Folkeregisteridentifikator.of(foedselsnummer), rolle, listOf(sakType))

        return client
            .post("$url/person/v2") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<PersonDTO>()
    }

    override suspend fun hentHistoriskForeldreansvar(
        fnr: Folkeregisteridentifikator,
        rolle: PersonRolle,
        sakType: SakType,
    ): HistorikkForeldreansvar {
        val personRequest = HentPersonHistorikkForeldreAnsvarRequest(fnr, rolle, sakType)

        return client
            .post("$url/foreldreansvar") {
                contentType(ContentType.Application.Json)
                setBody(personRequest)
            }.body<HistorikkForeldreansvar>()
    }
}

fun PersonDTO.hentDoedsdato(): LocalDate? = this.doedsdato?.verdi

fun PersonDoedshendelseDto.hentDoedsdato(): LocalDate? = this.doedsdato?.verdi

fun PersonDTO.hentAnsvarligeForeldre(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.ansvarligeForeldre

fun PersonDTO.hentBarn(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.barn

fun PersonDoedshendelseDto.hentBarn(): List<Folkeregisteridentifikator>? = this.familieRelasjon?.verdi?.barn

fun PersonDTO.hentVergemaal(): List<VergemaalEllerFremtidsfullmakt>? =
    this.vergemaalEllerFremtidsfullmakt?.map {
        it.verdi
    }

fun PersonDTO.hentSivilstand(): List<Sivilstand>? =
    this.sivilstand?.map {
        it.verdi
    }

fun PersonDTO.hentBostedsadresse(): List<Adresse>? =
    this.bostedsadresse?.map {
        it.verdi
    }

fun PersonDTO.hentUtland(): Utland? = this.utland?.verdi
