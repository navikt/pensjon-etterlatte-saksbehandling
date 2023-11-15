package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import grunnlag.adresse.VergeAdresse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.etterlatte.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
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
import java.util.UUID

interface GrunnlagService {
    fun hentGrunnlagAvType(
        behandlingId: UUID,
        opplysningstype: Opplysningstype,
    ): Grunnlagsopplysning<JsonNode>?

    fun hentOpplysningstypeNavnFraFnr(
        fnr: Folkeregisteridentifikator,
        navIdent: String,
    ): NavnOpplysningDTO?

    fun lagreNyeSaksopplysninger(
        sakId: Long,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    )

    fun lagreNyePersonopplysninger(
        sakId: Long,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    )

    fun hentOpplysningsgrunnlagForSak(sakId: Long): Grunnlag?

    fun hentOpplysningsgrunnlag(behandlingId: UUID): Grunnlag?

    fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller

    // TODO: Fjerne når grunnlag er versjonert (EY-2567)
    fun hentAlleSakIder(): Set<Long>

    // TODO: Fjerne når grunnlag er versjonert (EY-2567)
    fun oppdaterVersjonForBehandling(
        sakId: Long,
        behandlingId: UUID,
        laasVersjon: Boolean,
    )

    fun laasVersjonForBehandling(behandlingId: UUID)

    fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<Long>

    fun hentPersonerISak(sakId: Long): Map<Folkeregisteridentifikator, PersonMedNavn>?

    suspend fun opprettGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: Long,
        sakType: SakType,
    )

    fun hentHistoriskForeldreansvar(behandlingId: UUID): Grunnlagsopplysning<JsonNode>?

    suspend fun hentVergeadresse(folkeregisteridentifikator: Folkeregisteridentifikator): VergeAdresse
}

class RealGrunnlagService(
    private val pdltjenesterKlient: PdlTjenesterKlientImpl,
    private val opplysningDao: OpplysningDao,
    private val sporingslogg: Sporingslogg,
    private val persondataKlient: PersondataKlient,
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(RealGrunnlagService::class.java)

    override fun hentOpplysningsgrunnlagForSak(sakId: Long): Grunnlag? {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForSak(sakId, Opplysningstype.PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sakId. Fant ikke persongalleri")
            return null
        }
        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sakId)

        return OpplysningsgrunnlagMapper(grunnlag, persongalleri).hentGrunnlag()
    }

    override fun hentOpplysningsgrunnlag(behandlingId: UUID): Grunnlag? {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, Opplysningstype.PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for behandling (id=$behandlingId). Fant ikke persongalleri")
            return null
        }
        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        val grunnlag = opplysningDao.hentAlleGrunnlagForBehandling(behandlingId)

        return OpplysningsgrunnlagMapper(grunnlag, persongalleri).hentGrunnlag()
    }

    override fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller {
        return opplysningDao.finnAllePersongalleriHvorPersonFinnes(fnr)
            .map { it.sakId to deserialize<Persongalleri>(it.opplysning.opplysning.toJson()) }
            .map { (sakId, persongalleri) -> SakOgRolle(sakId, rolle = mapTilRolle(fnr.value, persongalleri)) }
            .let { PersonMedSakerOgRoller(fnr.value, it) }
    }

    // TODO: Fjerne når grunnlag er versjonert (EY-2567)
    override fun hentAlleSakIder(): Set<Long> = opplysningDao.finnAlleSakIder()

    override fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<Long> = opplysningDao.finnAlleSakerForPerson(fnr)

    override fun hentPersonerISak(sakId: Long): Map<Folkeregisteridentifikator, PersonMedNavn>? {
        val grunnlag = hentOpplysningsgrunnlagForSak(sakId) ?: return null

        val personer = listOf(grunnlag.soeker) + grunnlag.familie
        return personer.mapNotNull {
            val navn = it.hentNavn()?.verdi ?: return@mapNotNull null
            val fnr = it.hentFoedselsnummer()?.verdi ?: return@mapNotNull null
            PersonMedNavn(
                fnr = fnr,
                fornavn = navn.fornavn,
                etternavn = navn.etternavn,
                mellomnavn = navn.mellomnavn,
            )
        }.associateBy { it.fnr }
    }

    override suspend fun opprettGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    ) {
        val pdlPersondatasGrunnlag =
            coroutineScope {
                val persongalleri = opplysningsbehov.persongalleri
                val sakType = opplysningsbehov.sakType
                val requesterAvdoed =
                    persongalleri.avdoed.map {
                        Pair(
                            async { pdltjenesterKlient.hentPerson(it, PersonRolle.AVDOED, opplysningsbehov.sakType) },
                            async {
                                pdltjenesterKlient.hentOpplysningsperson(
                                    it,
                                    PersonRolle.AVDOED,
                                    opplysningsbehov.sakType,
                                )
                            },
                        )
                    }

                val requesterGjenlevende =
                    persongalleri.gjenlevende.map {
                        Pair(
                            async {
                                pdltjenesterKlient.hentPerson(
                                    it,
                                    PersonRolle.GJENLEVENDE,
                                    opplysningsbehov.sakType,
                                )
                            },
                            async {
                                pdltjenesterKlient.hentOpplysningsperson(
                                    it,
                                    PersonRolle.GJENLEVENDE,
                                    opplysningsbehov.sakType,
                                )
                            },
                        )
                    }
                val soekerRolle =
                    when (sakType) {
                        SakType.OMSTILLINGSSTOENAD -> PersonRolle.GJENLEVENDE
                        SakType.BARNEPENSJON -> PersonRolle.BARN
                    }
                val soeker =
                    Pair(
                        async {
                            pdltjenesterKlient.hentPerson(
                                persongalleri.soeker,
                                soekerRolle,
                                opplysningsbehov.sakType,
                            )
                        },
                        async {
                            pdltjenesterKlient
                                .hentOpplysningsperson(persongalleri.soeker, soekerRolle, opplysningsbehov.sakType)
                        },
                    )
                val innsender =
                    persongalleri.innsender
                        ?.takeIf { it != Vedtaksloesning.PESYS.name }
                        ?.let { innsenderFnr ->
                            Pair(
                                async {
                                    pdltjenesterKlient.hentPerson(
                                        innsenderFnr,
                                        PersonRolle.INNSENDER,
                                        opplysningsbehov.sakType,
                                    )
                                },
                                async {
                                    pdltjenesterKlient.hentOpplysningsperson(
                                        innsenderFnr,
                                        PersonRolle.INNSENDER,
                                        opplysningsbehov.sakType,
                                    )
                                },
                            )
                        }
                val innsenderPersonInfo =
                    innsender?.let { (person, personDTO) ->
                        GrunnlagsopplysningerPersonPdl(
                            person.await(),
                            personDTO.await(),
                            Opplysningstype.INNSENDER_PDL_V1,
                            PersonRolle.INNSENDER,
                        )
                    }
                val soekerPersonInfo =
                    GrunnlagsopplysningerPersonPdl(
                        soeker.first.await(),
                        soeker.second.await(),
                        Opplysningstype.SOEKER_PDL_V1,
                        soekerRolle,
                    )
                val avdoedePersonInfo =
                    requesterAvdoed.map {
                        GrunnlagsopplysningerPersonPdl(
                            it.first.await(),
                            it.second.await(),
                            Opplysningstype.AVDOED_PDL_V1,
                            PersonRolle.AVDOED,
                        )
                    }
                val gjenlevendePersonInfo =
                    requesterGjenlevende.map {
                        GrunnlagsopplysningerPersonPdl(
                            it.first.await(),
                            it.second.await(),
                            Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1,
                            PersonRolle.GJENLEVENDE,
                        )
                    }

                listOfNotNull(soekerPersonInfo, innsenderPersonInfo)
                    .plus(avdoedePersonInfo)
                    .plus(gjenlevendePersonInfo)
            }

        pdlPersondatasGrunnlag.forEach {
            val enkenPdlOpplysning =
                lagEnkelopplysningerFraPDL(
                    it.person,
                    it.personDto,
                    it.opplysningstype,
                    it.personDto.foedselsnummer.verdi,
                    it.personRolle,
                )
            lagreNyePersonopplysninger(
                opplysningsbehov.sakId,
                behandlingId,
                it.personDto.foedselsnummer.verdi,
                enkenPdlOpplysning,
            )
        }

        lagreNyeSaksopplysninger(
            opplysningsbehov.sakId,
            behandlingId,
            listOf(opplysningsbehov.persongalleri.tilGrunnlagsopplysning()),
        )
        logger.info("Oppdatert grunnlag (sakId=${opplysningsbehov.sakId}, behandlingId=$behandlingId)")
    }

    private fun Persongalleri.tilGrunnlagsopplysning(): Grunnlagsopplysning<JsonNode> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde =
                if (this.innsender == Vedtaksloesning.PESYS.name) {
                    Grunnlagsopplysning.Pesys.create()
                } else {
                    Grunnlagsopplysning.Privatperson(this.innsender!!, Tidspunkt.now())
                },
            opplysningType = Opplysningstype.PERSONGALLERI_V1,
            meta = objectMapper.createObjectNode(),
            opplysning = this.toJsonNode(),
            attestering = null,
            fnr = null,
            periode = null,
        )
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: Long,
        sakType: SakType,
    ) {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForSak(sakId, Opplysningstype.PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sakId. Fant ikke persongalleri")
            throw IllegalStateException("Fant ikke grunnlag for sak $sakId. Fant ikke persongalleri")
        }

        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)

        opprettGrunnlag(
            behandlingId,
            Opplysningsbehov(sakId, sakType, persongalleri),
        )
    }

    override fun hentHistoriskForeldreansvar(behandlingId: UUID): Grunnlagsopplysning<JsonNode>? {
        val grunnlagshendelse =
            opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, Opplysningstype.HISTORISK_FORELDREANSVAR)

        if (grunnlagshendelse?.opplysning != null) {
            return grunnlagshendelse.opplysning
        }

        val grunnlag = hentOpplysningsgrunnlag(behandlingId)
        val soekerFnr = grunnlag?.soeker?.hentFoedselsnummer()?.verdi ?: return null
        val historiskForeldreansvar =
            pdltjenesterKlient.hentHistoriskForeldreansvar(
                soekerFnr,
                PersonRolle.BARN,
                SakType.BARNEPENSJON,
            )
                .tilGrunnlagsopplysning(soekerFnr)

        val hendelsenummer =
            opplysningDao.leggOpplysningTilGrunnlag(grunnlag.metadata.sakId, historiskForeldreansvar, soekerFnr)

        opplysningDao.oppdaterVersjonForBehandling(behandlingId, grunnlag.metadata.sakId, hendelsenummer)

        return historiskForeldreansvar
    }

    override suspend fun hentVergeadresse(folkeregisteridentifikator: Folkeregisteridentifikator): VergeAdresse {
        return persondataKlient.hentAdresseForVerge(folkeregisteridentifikator)
            .toVergeAdresse()
    }

    private fun mapTilRolle(
        fnr: String,
        persongalleri: Persongalleri,
    ): Saksrolle =
        when (fnr) {
            persongalleri.soeker -> Saksrolle.SOEKER
            in persongalleri.soesken -> Saksrolle.SOESKEN
            in persongalleri.avdoed -> Saksrolle.AVDOED
            in persongalleri.gjenlevende -> Saksrolle.GJENLEVENDE
            else -> Saksrolle.UKJENT
        }

    override fun hentGrunnlagAvType(
        behandlingId: UUID,
        opplysningstype: Opplysningstype,
    ): Grunnlagsopplysning<JsonNode>? {
        return opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, opplysningstype)?.opplysning
    }

    override fun hentOpplysningstypeNavnFraFnr(
        fnr: Folkeregisteridentifikator,
        navIdent: String,
    ): NavnOpplysningDTO? {
        val opplysning =
            opplysningDao.finnNyesteOpplysningPaaFnr(fnr, Opplysningstype.NAVN)?.let {
                val navn: Navn = deserialize(it.opplysning.opplysning.toString())
                NavnOpplysningDTO(
                    sakId = it.sakId,
                    fornavn = navn.fornavn,
                    mellomnavn = navn.mellomnavn,
                    etternavn = navn.etternavn,
                    foedselsnummer = fnr.value,
                )
            }
        when (opplysning) {
            null -> sporingslogg.logg(feilendeRequest(ident = fnr.value, navIdent = navIdent))
            else -> sporingslogg.logg(vellykkaRequest(ident = fnr.value, navIdent = navIdent))
        }
        return opplysning
    }

    override fun lagreNyePersonopplysninger(
        sakId: Long,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Oppretter et grunnlag for personopplysninger")
        oppdaterGrunnlagOgVersjon(sakId, behandlingId, fnr, nyeOpplysninger)
    }

    override fun lagreNyeSaksopplysninger(
        sakId: Long,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Oppretter et grunnlag for saksopplysninger")
        oppdaterGrunnlagOgVersjon(sakId, behandlingId, fnr = null, nyeOpplysninger)
    }

    private fun oppdaterGrunnlagOgVersjon(
        sak: Long,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator?,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        val versjon = opplysningDao.hentBehandlingVersjon(behandlingId)
        if (versjon?.laast == true) {
            throw LaastGrunnlagKanIkkeEndres(behandlingId)
        }

        val hendelsenummer =
            nyeOpplysninger.mapNotNull { opplysning ->
                if (opplysning.id in gjeldendeGrunnlag) {
                    logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
                    null
                } else {
                    opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, fnr)
                }
            }.maxOrNull()

        if (hendelsenummer == null) {
            logger.error("Hendelsenummer er null – kan ikke oppdatere versjon for behandling (id=$behandlingId)")
        } else {
            logger.info("Setter grunnlag for behandling (id=$behandlingId) til hendelsenummer=$hendelsenummer")
            opplysningDao.oppdaterVersjonForBehandling(behandlingId, sak, hendelsenummer)
        }
    }

    // TODO: Fjerne når grunnlag er versjonert (EY-2567)
    override fun oppdaterVersjonForBehandling(
        sakId: Long,
        behandlingId: UUID,
        laasVersjon: Boolean,
    ) {
        val grunnlag = hentOpplysningsgrunnlagForSak(sakId)
        if (grunnlag == null) {
            logger.warn("Ingen grunnlag funnet for sak=$sakId - kan ikke sette versjon for behandlingId=$behandlingId")
            return
        }

        val versjonErLaast = opplysningDao.hentBehandlingVersjon(behandlingId)?.laast ?: false
        if (versjonErLaast) {
            throw IllegalStateException("Kan ikke oppdatere versjon som er låst (behandlingId=$behandlingId)")
        }

        val hendelsenummer = grunnlag.metadata.versjon
        val oppdatertOK = opplysningDao.oppdaterVersjonForBehandling(behandlingId, sakId, hendelsenummer) > 0
        if (oppdatertOK) {
            logger.info("Versjon satt til hendelsenummer=$hendelsenummer (sakId=$sakId, id=$behandlingId)")
        } else {
            logger.warn("Kunne ikke sette versjon til hendelsenummer=$hendelsenummer (sakId=$sakId, id=$behandlingId)")
        }

        if (laasVersjon) {
            logger.info("Låser grunnlag (sakId=$sakId, behandlingId=$behandlingId)")
            opplysningDao.laasGrunnlagVersjonForBehandling(behandlingId)
        } else {
            logger.info("Skal ikke låse grunnlag (sakId=$sakId, behandlingId=$behandlingId)")
        }
    }

    override fun laasVersjonForBehandling(behandlingId: UUID) {
        logger.info("Låser grunnlagsversjon for behandling (id=$behandlingId)")
        opplysningDao.laasGrunnlagVersjonForBehandling(behandlingId)
    }

    private fun vellykkaRequest(
        ident: String,
        navIdent: String,
    ) = Sporingsrequest(
        kallendeApplikasjon = "grunnlag",
        oppdateringstype = HttpMethod.POST,
        brukerId = navIdent,
        hvemBlirSlaattOpp = ident,
        endepunkt = "/person",
        resultat = Decision.Permit,
        melding = "Hent person var vellykka",
    )

    private fun feilendeRequest(
        ident: String,
        navIdent: String,
    ) = Sporingsrequest(
        kallendeApplikasjon = "grunnlag",
        oppdateringstype = HttpMethod.POST,
        brukerId = navIdent,
        hvemBlirSlaattOpp = ident,
        endepunkt = "/person",
        resultat = Decision.Deny,
        melding = "Hent person feilet",
    )
}

private fun HistorikkForeldreansvar.tilGrunnlagsopplysning(fnr: Folkeregisteridentifikator): Grunnlagsopplysning<JsonNode> {
    return Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
        opplysningType = Opplysningstype.HISTORISK_FORELDREANSVAR,
        meta = objectMapper.createObjectNode(),
        opplysning = this.toJsonNode(),
        attestering = null,
        fnr = fnr,
        periode = null,
    )
}

data class GrunnlagsopplysningerPersonPdl(
    val person: Person,
    val personDto: PersonDTO,
    val opplysningstype: Opplysningstype,
    val personRolle: PersonRolle,
)

data class NavnOpplysningDTO(
    val sakId: Long,
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val foedselsnummer: String,
)

class LaastGrunnlagKanIkkeEndres(val behandlingId: UUID) :
    IkkeTillattException(
        code = "LAAST_GRUNNLAG_KAN_IKKE_ENDRES",
        detail = """
            Kan ikke sette ny grunnlagsversjon på behandling som er
            låst til en versjon av grunnlag (behandlingId=$behandlingId)
            """,
    )
