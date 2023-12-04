package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.grunnlag.adresse.BrevMottaker
import no.nav.etterlatte.grunnlag.klienter.PdlTjenesterKlientImpl
import no.nav.etterlatte.grunnlag.klienter.PersondataKlient
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentNavn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.INNSENDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
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
import org.jetbrains.annotations.TestOnly
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

    fun hentPersonopplysninger(
        behandlingId: UUID,
        sakstype: SakType,
    ): PersonopplysningerResponse

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

    fun hentVergeadresse(folkeregisteridentifikator: String): BrevMottaker?

    fun hentPersongalleriSamsvar(behandlingId: UUID): PersongalleriSamsvar
}

class RealGrunnlagService(
    private val pdltjenesterKlient: PdlTjenesterKlientImpl,
    private val opplysningDao: OpplysningDao,
    private val sporingslogg: Sporingslogg,
    private val persondataKlient: PersondataKlient,
    private val grunnlagHenter: GrunnlagHenter,
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

    override fun hentPersonopplysninger(
        behandlingId: UUID,
        sakstype: SakType,
    ): PersonopplysningerResponse {
        val grunnlag =
            opplysningDao.hentGrunnlagAvTypeForBehandling(
                behandlingId,
                INNSENDER_PDL_V1,
                SOEKER_PDL_V1,
                AVDOED_PDL_V1,
                GJENLEVENDE_FORELDER_PDL_V1,
            )

        // Finn siste grunnlag blant relevante typer for unike personer,
        // slik at hver person kun havner i en av kategoriene
        val sisteGrunnlagPerFnr =
            grunnlag.filter {
                setOf(
                    SOEKER_PDL_V1,
                    AVDOED_PDL_V1,
                    GJENLEVENDE_FORELDER_PDL_V1,
                ).contains(it.opplysning.opplysningType)
            }
                .groupBy { it.opplysning.fnr }
                .map {
                    it.value.maxBy { opplysning -> opplysning.hendelseNummer }
                }

        val innsender =
            grunnlag
                .filter { it.opplysning.opplysningType == INNSENDER_PDL_V1 }
                .maxByOrNull { it.hendelseNummer }
        val soker = sisteGrunnlagPerFnr.find { it.opplysning.opplysningType == SOEKER_PDL_V1 }
        val avdode = sisteGrunnlagPerFnr.filter { it.opplysning.opplysningType == AVDOED_PDL_V1 }
        val gjenlevende =
            if (sakstype == SakType.OMSTILLINGSSTOENAD) {
                listOf(soker)
            } else {
                sisteGrunnlagPerFnr.filter { it.opplysning.opplysningType == GJENLEVENDE_FORELDER_PDL_V1 }
            }

        return PersonopplysningerResponse(
            innsender = innsender?.opplysning?.asPersonopplysning(),
            soeker = soker?.opplysning?.asPersonopplysning(),
            avdoede = avdode.map { it.opplysning.asPersonopplysning() },
            gjenlevende = gjenlevende.mapNotNull { it?.opplysning?.asPersonopplysning() },
        )
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
        val grunnlag = grunnlagHenter.hentGrunnlagsdata(opplysningsbehov)

        grunnlag.personopplysninger.forEach { fnrToOpplysning ->
            lagreNyePersonopplysninger(
                opplysningsbehov.sakId,
                behandlingId,
                fnrToOpplysning.first,
                fnrToOpplysning.second,
            )
        }

        lagreNyeSaksopplysninger(
            opplysningsbehov.sakId,
            behandlingId,
            grunnlag.saksopplysninger,
        )
        logger.info("Oppdatert grunnlag (sakId=${opplysningsbehov.sakId}, behandlingId=$behandlingId)")
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

    override fun hentVergeadresse(folkeregisteridentifikator: String): BrevMottaker? {
        return persondataKlient.hentAdresseForVerge(folkeregisteridentifikator)
            ?.toBrevMottaker()
    }

    override fun hentPersongalleriSamsvar(behandlingId: UUID): PersongalleriSamsvar {
        val grunnlag =
            opplysningDao.hentGrunnlagAvTypeForBehandling(behandlingId, PERSONGALLERI_PDL_V1, PERSONGALLERI_V1)
        val opplysningPersongalleriSak =
            grunnlag.filter { it.opplysning.opplysningType == PERSONGALLERI_V1 }.maxByOrNull { it.hendelseNummer }
        val opplysningPersongalleriPdl =
            grunnlag.filter { it.opplysning.opplysningType == PERSONGALLERI_PDL_V1 }.maxByOrNull { it.hendelseNummer }
        if (opplysningPersongalleriSak == null) {
            throw GenerellIkkeFunnetException()
        }
        val persongalleriISak: Persongalleri =
            objectMapper.readValue(opplysningPersongalleriSak.opplysning.opplysning.toJson())

        if (opplysningPersongalleriPdl == null) {
            logger.info("Fant ikke persongalleri fra PDL for behandling med id=$behandlingId, gjør ingen samsvarsjekk")
            return PersongalleriSamsvar(
                persongalleri = persongalleriISak,
                kilde = opplysningPersongalleriSak.opplysning.kilde.tilGenerellKilde(),
                persongalleriPdl = null,
                kildePdl = null,
                problemer = listOf(),
            )
        }
        val persongalleriPdl: Persongalleri =
            objectMapper.readValue(opplysningPersongalleriPdl.opplysning.opplysning.toJson())

        val valideringsfeil =
            valideringsfeilPersongalleriSakPdl(
                persongalleriISak = persongalleriISak,
                persongalleriPdl = persongalleriPdl,
            )

        return PersongalleriSamsvar(
            persongalleri = persongalleriISak,
            kilde = opplysningPersongalleriSak.opplysning.kilde.tilGenerellKilde(),
            persongalleriPdl = persongalleriPdl,
            kildePdl = opplysningPersongalleriPdl.opplysning.kilde.tilGenerellKilde(),
            problemer = valideringsfeil,
        )
    }

    @TestOnly
    fun valideringsfeilPersongalleriSakPdl(
        persongalleriISak: Persongalleri,
        persongalleriPdl: Persongalleri,
    ): List<MismatchPersongalleri> {
        val forskjellerAvdoede =
            forskjellerMellomPersonerPdlOgSak(
                personerPdl = persongalleriPdl.avdoed,
                personerSak = persongalleriISak.avdoed,
            )
        val forskjellerGjenlevende =
            forskjellerMellomPersonerPdlOgSak(
                personerPdl = persongalleriPdl.gjenlevende,
                personerSak = persongalleriISak.gjenlevende,
            )
        val forskjellerSoesken =
            forskjellerMellomPersonerPdlOgSak(
                personerPdl = persongalleriPdl.soesken,
                persongalleriISak.soesken,
            )
        return listOfNotNull(
            MismatchPersongalleri.MANGLER_GJENLEVENDE.takeIf { forskjellerGjenlevende.kunPdl.isNotEmpty() },
            MismatchPersongalleri.MANGLER_AVDOED.takeIf { forskjellerAvdoede.kunPdl.isNotEmpty() },
            MismatchPersongalleri.MANGLER_SOESKEN.takeIf { forskjellerSoesken.kunPdl.isNotEmpty() },
            MismatchPersongalleri.EKSTRA_GJENLEVENDE.takeIf { forskjellerGjenlevende.kunSak.isNotEmpty() },
            MismatchPersongalleri.EKSTRA_AVDOED.takeIf { forskjellerAvdoede.kunSak.isNotEmpty() },
            MismatchPersongalleri.EKSTRA_SOESKEN.takeIf { forskjellerSoesken.kunSak.isNotEmpty() },
            MismatchPersongalleri.HAR_PERSONER_UTEN_IDENTER.takeIf { !persongalleriPdl.personerUtenIdent.isNullOrEmpty() },
        )
    }

    private fun forskjellerMellomPersonerPdlOgSak(
        personerPdl: List<String>,
        personerSak: List<String>,
    ): ForskjellMellomPersoner {
        val pdl = personerPdl.toSet()
        val sak = personerSak.toSet()

        val kunIPdl = pdl subtract sak
        val kunISak = sak subtract pdl
        return ForskjellMellomPersoner(
            kunSak = kunISak.toList(),
            kunPdl = kunIPdl.toList(),
        )
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
            null -> {
                sporingslogg.logg(feilendeRequest(ident = fnr.value, navIdent = navIdent))
                logger.warn("Fant ikke navn for person i grunnlaget")
            }
            else -> {
                sporingslogg.logg(vellykkaRequest(ident = fnr.value, navIdent = navIdent))
                logger.debug("Fant navn for person i grunnlaget")
            }
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

private fun Grunnlagsopplysning<JsonNode>.asPersonopplysning(): Personopplysning {
    return Personopplysning(
        id = this.id,
        kilde = this.kilde.tilGenerellKilde(),
        opplysningType = this.opplysningType,
        opplysning = objectMapper.treeToValue(opplysning, Person::class.java),
    )
}

private fun Grunnlagsopplysning.Kilde.tilGenerellKilde() =
    when (this) {
        is Grunnlagsopplysning.Pdl ->
            GenerellKilde(
                type = this.type,
                tidspunkt = this.tidspunktForInnhenting,
                detalj = this.registersReferanse,
            )

        is Grunnlagsopplysning.Persondata ->
            GenerellKilde(
                type = this.type,
                tidspunkt = this.tidspunktForInnhenting,
                detalj = this.registersReferanse,
            )

        is Grunnlagsopplysning.Pesys -> GenerellKilde(type = this.type, tidspunkt = this.tidspunkt)
        is Grunnlagsopplysning.Privatperson ->
            GenerellKilde(
                type = this.type,
                tidspunkt = this.mottatDato,
                detalj = this.fnr,
            )

        is Grunnlagsopplysning.RegelKilde -> GenerellKilde(type = this.type, tidspunkt = this.ts)
        is Grunnlagsopplysning.Saksbehandler ->
            GenerellKilde(
                type = this.type,
                tidspunkt = this.tidspunkt,
                detalj = this.ident,
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

data class PersonopplysningerResponse(
    val innsender: Personopplysning?,
    val soeker: Personopplysning?,
    val avdoede: List<Personopplysning>,
    val gjenlevende: List<Personopplysning>,
)

data class Personopplysning(
    val opplysningType: Opplysningstype,
    val id: UUID,
    val kilde: GenerellKilde,
    val opplysning: Person,
)

data class GenerellKilde(
    val type: String,
    val tidspunkt: Tidspunkt,
    val detalj: String? = null,
)

data class ForskjellMellomPersoner(val kunSak: List<String>, val kunPdl: List<String>)
