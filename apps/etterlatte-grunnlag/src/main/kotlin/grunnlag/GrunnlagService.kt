package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import org.slf4j.LoggerFactory
import java.util.*

interface GrunnlagService {
    fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>?
    fun hentOpplysningstypeNavnFraFnr(fnr: Folkeregisteridentifikator, navIdent: String): NavnOpplysningDTO?
    fun lagreNyeSaksopplysninger(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun lagreNyePersonopplysninger(
        sak: Long,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    )

    fun hentOpplysningsgrunnlag(sak: Long): Grunnlag?
    fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag?
    fun hentOpplysningsgrunnlag(
        sak: Long,
        persongalleri: Persongalleri
    ): Grunnlag // TODO ai: Kan fjernes når kafka flyten fjernes

    fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller
    fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<Long>
    fun hentPersonerISak(sakId: Long): Map<Folkeregisteridentifikator, PersonMedNavn>?

    suspend fun oppdaterGrunnlag(opplysningsbehov: Opplysningsbehov)
    fun hentHistoriskForeldreansvar(sakId: Long): Grunnlagsopplysning<JsonNode>?
}

class RealGrunnlagService(
    private val pdltjenesterKlient: PdlTjenesterKlientImpl,
    private val opplysningDao: OpplysningDao,
    private val sporingslogg: Sporingslogg
) : GrunnlagService {

    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentOpplysningsgrunnlag(sak: Long): Grunnlag? {
        val persongalleriJsonNode = opplysningDao.finnNyesteGrunnlag(sak, Opplysningstype.PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sak. Fant ikke persongalleri")
            return null
        }
        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlag(sak: Long, persongalleri: Persongalleri): Grunnlag {
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sak)

        return OpplysningsgrunnlagMapper(grunnlag, sak, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag? {
        val grunnlag = opplysningDao.finnGrunnlagOpptilVersjon(sak, versjon)

        val personGalleri = grunnlag.find { it.opplysning.opplysningType === Opplysningstype.PERSONGALLERI_V1 }
            ?.let { objectMapper.readValue(it.opplysning.opplysning.toJson(), Persongalleri::class.java) }
            ?: run {
                logger.info(
                    "Klarte ikke å hente ut grunnlag for sak $sak med versjon $versjon. " +
                        "Fant ikke persongalleri"
                )
                return null
            }

        return OpplysningsgrunnlagMapper(grunnlag, sak, personGalleri).hentGrunnlag()
    }

    override fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller {
        return opplysningDao.finnAllePersongalleriHvorPersonFinnes(fnr)
            .map { it.sakId to deserialize<Persongalleri>(it.opplysning.opplysning.toJson()) }
            .map { (sakId, persongalleri) -> SakOgRolle(sakId, rolle = mapTilRolle(fnr.value, persongalleri)) }
            .let { PersonMedSakerOgRoller(fnr.value, it) }
    }

    override fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<Long> =
        opplysningDao.finnAlleSakerForPerson(fnr)

    override fun hentPersonerISak(sakId: Long): Map<Folkeregisteridentifikator, PersonMedNavn>? {
        val grunnlag = hentOpplysningsgrunnlag(sakId) ?: return null

        val personer = listOf(grunnlag.soeker) + grunnlag.familie
        return personer.mapNotNull {
            val navn = it.hentNavn()?.verdi ?: return@mapNotNull null
            val fnr = it.hentFoedselsnummer()?.verdi ?: return@mapNotNull null
            PersonMedNavn(
                fnr = fnr,
                fornavn = navn.fornavn,
                etternavn = navn.etternavn,
                mellomnavn = navn.mellomnavn
            )
        }.associateBy { it.fnr }
    }

    override suspend fun oppdaterGrunnlag(opplysningsbehov: Opplysningsbehov) {
        val pdlPersondatasGrunnlag = coroutineScope {
            val persongalleri = opplysningsbehov.persongalleri
            val sakType = opplysningsbehov.sakType
            val requesterAvdoed = persongalleri.avdoed.map {
                Pair(
                    async { pdltjenesterKlient.hentPerson(it, PersonRolle.AVDOED, opplysningsbehov.sakType) },
                    async {
                        pdltjenesterKlient.hentOpplysningsperson(
                            it,
                            PersonRolle.AVDOED,
                            opplysningsbehov.sakType
                        )
                    }
                )
            }

            val requesterGjenlevende = persongalleri.gjenlevende.map {
                Pair(
                    async { pdltjenesterKlient.hentPerson(it, PersonRolle.GJENLEVENDE, opplysningsbehov.sakType) },
                    async {
                        pdltjenesterKlient.hentOpplysningsperson(
                            it,
                            PersonRolle.GJENLEVENDE,
                            opplysningsbehov.sakType
                        )
                    }
                )
            }
            val soekerRolle = when (sakType) {
                SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                SakType.BARNEPENSJON -> PersonRolle.BARN
            }
            val soeker = Pair(
                async { pdltjenesterKlient.hentPerson(persongalleri.soeker, soekerRolle, opplysningsbehov.sakType) },
                async {
                    pdltjenesterKlient
                        .hentOpplysningsperson(persongalleri.soeker, soekerRolle, opplysningsbehov.sakType)
                }
            )
            val soekerPersonInfo =
                GrunnlagsopplysningerPersonPdl(
                    soeker.first.await(),
                    soeker.second.await(),
                    Opplysningstype.SOEKER_PDL_V1,
                    soekerRolle
                )
            val avdoedePersonInfo = requesterAvdoed.map {
                GrunnlagsopplysningerPersonPdl(
                    it.first.await(),
                    it.second.await(),
                    Opplysningstype.AVDOED_PDL_V1,
                    PersonRolle.AVDOED
                )
            }
            val gjenlevendePersonInfo = requesterGjenlevende.map {
                GrunnlagsopplysningerPersonPdl(
                    it.first.await(),
                    it.second.await(),
                    Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                    PersonRolle.GJENLEVENDE
                )
            }
            avdoedePersonInfo + gjenlevendePersonInfo.plus(soekerPersonInfo)
        }

        pdlPersondatasGrunnlag.forEach {
            val enkenPdlOpplysning = lagEnkelopplysningerFraPDL(
                it.person,
                it.personDto,
                it.opplysningstype,
                it.personDto.foedselsnummer.verdi,
                it.personRolle
            )
            lagreNyePersonopplysninger(opplysningsbehov.sakid, it.personDto.foedselsnummer.verdi, enkenPdlOpplysning)
        }

        lagreNyeSaksopplysninger(
            opplysningsbehov.sakid,
            listOf(opplysningsbehov.persongalleri.tilGrunnlagsopplysning())
        )
        logger.info("Oppdatert grunnlag for sak ${opplysningsbehov.sakid}")
    }

    private fun Persongalleri.tilGrunnlagsopplysning(): Grunnlagsopplysning<JsonNode> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = if (this.innsender!! == Vedtaksloesning.PESYS.name) {
                Grunnlagsopplysning.Pesys.create()
            } else {
                Grunnlagsopplysning.Privatperson(this.innsender!!, Tidspunkt.now())
            },
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            meta = objectMapper.createObjectNode(),
            opplysning = this.toJsonNode(),
            attestering = null,
            fnr = null,
            periode = null
        )
    }

    override fun hentHistoriskForeldreansvar(sakId: Long): Grunnlagsopplysning<JsonNode>? {
        val opplysning = opplysningDao.finnNyesteGrunnlag(sakId, Opplysningstype.HISTORISK_FORELDREANSVAR)?.opplysning
        if (opplysning != null) {
            return opplysning
        }
        val grunnlag = hentOpplysningsgrunnlag(sakId)
        val soekerFnr = grunnlag?.soeker?.hentFoedselsnummer()?.verdi ?: return null
        val historiskForeldreansvar = pdltjenesterKlient.hentHistoriskForeldreansvar(
            soekerFnr,
            PersonRolle.BARN,
            SakType.BARNEPENSJON
        )
            .tilGrunnlagsopplysning(soekerFnr)
        opplysningDao.leggOpplysningTilGrunnlag(sakId, historiskForeldreansvar, soekerFnr)
        return historiskForeldreansvar
    }

    private fun mapTilRolle(fnr: String, persongalleri: Persongalleri): Saksrolle = when (fnr) {
        persongalleri.soeker -> Saksrolle.SOEKER
        in persongalleri.soesken -> Saksrolle.SOESKEN
        in persongalleri.avdoed -> Saksrolle.AVDOED
        in persongalleri.gjenlevende -> Saksrolle.GJENLEVENDE
        else -> Saksrolle.UKJENT
    }

    override fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>? {
        return opplysningDao.finnNyesteGrunnlag(sak, opplysningstype)?.opplysning
    }

    override fun hentOpplysningstypeNavnFraFnr(fnr: Folkeregisteridentifikator, navIdent: String): NavnOpplysningDTO? {
        val opplysning = opplysningDao.finnNyesteOpplysningPaaFnr(fnr, Opplysningstype.NAVN)?.let {
            val navn: Navn = deserialize(it.opplysning.opplysning.toString())
            NavnOpplysningDTO(
                sakId = it.sakId,
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
                foedselsnummer = fnr.value
            )
        }
        when (opplysning) {
            null -> sporingslogg.logg(feilendeRequest(ident = fnr.value, navIdent = navIdent))
            else -> sporingslogg.logg(vellykkaRequest(ident = fnr.value, navIdent = navIdent))
        }
        return opplysning
    }

    override fun lagreNyePersonopplysninger(
        sak: Long,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag for personopplysninger")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn(
                    "Forsøker å lagre personopplysning ${opplysning.id} i sak $sak men den er allerede gjeldende"
                )
            } else {
                opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, fnr)
            }
        }
    }

    override fun lagreNyeSaksopplysninger(
        sak: Long,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag for saksopplysninger")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre sakopplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
            } else {
                opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, null)
            }
        }
    }

    private fun vellykkaRequest(ident: String, navIdent: String) = Sporingsrequest(
        kallendeApplikasjon = "grunnlag",
        oppdateringstype = HttpMethod.POST,
        brukerId = navIdent,
        hvemBlirSlaattOpp = ident,
        endepunkt = "/person",
        resultat = Decision.Permit,
        melding = "Hent person var vellykka"
    )

    private fun feilendeRequest(ident: String, navIdent: String) = Sporingsrequest(
        kallendeApplikasjon = "grunnlag",
        oppdateringstype = HttpMethod.POST,
        brukerId = navIdent,
        hvemBlirSlaattOpp = ident,
        endepunkt = "/person",
        resultat = Decision.Deny,
        melding = "Hent person feilet"
    )
}

private fun HistorikkForeldreansvar.tilGrunnlagsopplysning(
    fnr: Folkeregisteridentifikator
): Grunnlagsopplysning<JsonNode> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
        opplysningType = Opplysningstype.HISTORISK_FORELDREANSVAR,
        meta = objectMapper.createObjectNode(),
        opplysning = this.toJsonNode(),
        attestering = null,
        fnr = fnr,
        periode = null
    )
}

data class GrunnlagsopplysningerPersonPdl(
    val person: Person,
    val personDto: PersonDTO,
    val opplysningstype: Opplysningstype,
    val personRolle: PersonRolle
)

data class NavnOpplysningDTO(
    val sakId: Long,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val foedselsnummer: String
)