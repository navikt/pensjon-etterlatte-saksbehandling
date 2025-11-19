package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.SakidOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.sjekkIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.INNSENDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.logging.sikkerlogger
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.pdl.PersonDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.pdl.HistorikkForeldreansvar
import no.nav.etterlatte.tilgangsstyring.OppdaterTilgangService
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory
import java.util.UUID

interface GrunnlagService {
    fun grunnlagFinnesForSak(sakId: SakId): Boolean

    fun hentGrunnlagAvType(
        behandlingId: UUID,
        opplysningstype: Opplysningstype,
    ): Grunnlagsopplysning<JsonNode>?

    fun lagreNyeSaksopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    )

    fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    )

    fun lagreNyePersonopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    )

    fun hentOpplysningsgrunnlagForSak(sakId: SakId): Grunnlag?

    fun hentPersongalleri(sakId: SakId): Persongalleri?

    fun hentPersongalleri(behandlingId: UUID): Persongalleri?

    fun hentOpplysningsgrunnlag(behandlingId: UUID): Grunnlag?

    fun hentPersonopplysninger(
        behandlingId: UUID,
        sakstype: SakType,
    ): PersonopplysningerResponse

    fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller

    fun laasVersjonForBehandling(behandlingId: UUID)

    fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<SakId>

    suspend fun opprettGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun oppdaterGrunnlagForSak(oppdaterGrunnlagRequest: OppdaterGrunnlagRequest)

    suspend fun opprettEllerOppdaterGrunnlagForSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
    )

    suspend fun hentHistoriskForeldreansvar(behandlingId: UUID): Grunnlagsopplysning<JsonNode>?

    fun hentPersongalleriSamsvar(behandlingId: UUID): PersongalleriSamsvar

    fun laasTilVersjonForBehandling(
        skalLaasesId: UUID,
        idLaasesTil: UUID,
    ): BehandlingGrunnlagVersjon
}

class GrunnlagServiceImpl(
    private val pdltjenesterKlient: PdlTjenesterKlient,
    private val opplysningDao: OpplysningDao,
    private val grunnlagHenter: GrunnlagHenter,
    private val oppdaterTilgangService: OppdaterTilgangService,
) : GrunnlagService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun grunnlagFinnesForSak(sakId: SakId): Boolean {
        logger.info("Sjekker om det finnes grunnlag for sak=$sakId")

        return opplysningDao
            .finnesGrunnlagForSak(sakId)
            .also { logger.info("Grunnlag finnes '$it' for sak=$sakId") }
    }

    override fun hentOpplysningsgrunnlagForSak(sakId: SakId): Grunnlag? {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForSak(sakId, PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sakId. Fant ikke persongalleri")
            return null
        }
        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        val grunnlag = opplysningDao.hentAlleGrunnlagForSak(sakId)

        return OpplysningsgrunnlagMapper(grunnlag, persongalleri).hentGrunnlag()
    }

    override fun hentPersongalleri(sakId: SakId): Persongalleri? {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForSak(sakId, PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Fant ikke persongalleri i sak=$sakId")
            return null
        }
        return objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
    }

    override fun hentPersongalleri(behandlingId: UUID): Persongalleri? {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Fant ikke persongalleri i behandling=$behandlingId")
            return null
        }

        return objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
    }

    override fun hentOpplysningsgrunnlag(behandlingId: UUID): Grunnlag? {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, PERSONGALLERI_V1)?.opplysning

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
                PERSONGALLERI_V1,
                INNSENDER_PDL_V1,
                SOEKER_PDL_V1,
                AVDOED_PDL_V1,
                GJENLEVENDE_FORELDER_PDL_V1,
            )

        val persongalleri =
            grunnlag
                .filter { it.opplysning.opplysningType == PERSONGALLERI_V1 }
                .maxBy { it.hendelseNummer }
                .let { deserialize<Persongalleri>(it.opplysning.opplysning.toJson()) }

        // Finn siste grunnlag blant relevante typer for unike personer, som eksisterer i persongalleriet fra søknaden,
        // slik at hver person kun havner i en av kategoriene

        val sisteGrunnlagPerFnr =
            grunnlag
                .filter {
                    setOf(
                        SOEKER_PDL_V1,
                        AVDOED_PDL_V1,
                        GJENLEVENDE_FORELDER_PDL_V1,
                        INNSENDER_PDL_V1,
                    ).contains(it.opplysning.opplysningType)
                }.filter { persongalleri.inkluderer(it.opplysning) }
                .groupBy { it.opplysning.fnr to it.opplysning.opplysningType }
                .map {
                    it.value.maxBy { opplysning -> opplysning.hendelseNummer }
                }

        val innsender =
            sisteGrunnlagPerFnr.find { it.opplysning.opplysningType == INNSENDER_PDL_V1 }
        val soker = sisteGrunnlagPerFnr.find { it.opplysning.opplysningType == SOEKER_PDL_V1 }
        val avdode = sisteGrunnlagPerFnr.filter { it.opplysning.opplysningType == AVDOED_PDL_V1 }
        val gjenlevende = sisteGrunnlagPerFnr.filter { it.opplysning.opplysningType == GJENLEVENDE_FORELDER_PDL_V1 }

        return PersonopplysningerResponse(
            innsender = innsender?.opplysning?.asPersonopplysning(),
            soeker = soker?.opplysning?.asPersonopplysning(),
            avdoede = avdode.map { it.opplysning.asPersonopplysning() },
            gjenlevende = gjenlevende.map { it.opplysning.asPersonopplysning() },
            annenForelder = persongalleri.annenForelder,
        )
    }

    private fun Persongalleri.inkluderer(it: Grunnlagsopplysning<JsonNode>) =
        when (it.opplysningType) {
            AVDOED_PDL_V1 -> it.fnr?.let { fnr -> avdoed.contains(fnr.value) } == true
            GJENLEVENDE_FORELDER_PDL_V1 -> it.fnr?.let { fnr -> gjenlevende.contains(fnr.value) } == true
            SOEKER_PDL_V1 -> it.fnr?.let { fnr -> soeker == fnr.value } == true
            INNSENDER_PDL_V1 -> it.fnr?.let { fnr -> innsender == fnr.value } == true
            else -> false
        }

    override fun hentSakerOgRoller(fnr: Folkeregisteridentifikator): PersonMedSakerOgRoller {
        val result =
            opplysningDao
                .finnAllePersongalleriHvorPersonFinnes(fnr)
                .map { it.sakId to deserialize<Persongalleri>(it.opplysning.opplysning.toJson()) }
                .map { (sakId, persongalleri) -> SakidOgRolle(sakId, rolle = mapTilRolle(fnr.value, persongalleri)) }
                .let { PersonMedSakerOgRoller(fnr.value, it) }

        result.sakiderOgRoller.filter { it.rolle == Saksrolle.UKJENT }.forEach {
            sikkerlogger().warn("Fant ukjent rolle for sakId=${it.sakId} og fnr=${fnr.value}")
        }
        return result
    }

    override fun hentAlleSakerForFnr(fnr: Folkeregisteridentifikator): Set<SakId> = opplysningDao.finnAlleSakerForPerson(fnr)

    override suspend fun opprettGrunnlag(
        behandlingId: UUID,
        opplysningsbehov: Opplysningsbehov,
    ) {
        val oppdatertOpplysningsbehov = oppdaterPersongalleri(opplysningsbehov)

        val grunnlag = grunnlagHenter.hentGrunnlagsdata(oppdatertOpplysningsbehov)

        grunnlag.personopplysninger.forEach { (fnr, opplysninger) ->
            lagreNyePersonopplysninger(
                opplysningsbehov.sakId,
                behandlingId,
                fnr,
                opplysninger,
            )
        }

        lagreNyeSaksopplysninger(
            opplysningsbehov.sakId,
            behandlingId,
            grunnlag.saksopplysninger,
        )
        logger.info("Oppdatert grunnlag (sakId=${opplysningsbehov.sakId}, behandlingId=$behandlingId)")
    }

    private suspend fun oppdaterPersongalleri(opplysningsbehov: Opplysningsbehov): Opplysningsbehov {
        val identerForSoeker = pdltjenesterKlient.hentPdlFolkeregisterIdenter(opplysningsbehov.persongalleri.soeker)
        val gjeldendeIdentForSoeker = identerForSoeker.identifikatorer.first { !it.historisk }
        return opplysningsbehov.copy(
            persongalleri =
                opplysningsbehov.persongalleri.copy(
                    soeker = gjeldendeIdentForSoeker.folkeregisterident.value,
                ),
        )
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
    ) {
        val persongalleriJsonNode = opplysningDao.finnNyesteGrunnlagForSak(sakId, PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak $sakId. Fant ikke persongalleri")
            throw IllegalStateException("Fant ikke grunnlag for sak $sakId. Fant ikke persongalleri")
        }

        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)

        opprettGrunnlag(
            behandlingId,
            Opplysningsbehov(sakId, sakType, persongalleri, persongalleriJsonNode.kilde),
        )
    }

    override suspend fun hentHistoriskForeldreansvar(behandlingId: UUID): Grunnlagsopplysning<JsonNode>? {
        val grunnlagshendelse =
            opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, Opplysningstype.HISTORISK_FORELDREANSVAR)

        if (grunnlagshendelse?.opplysning != null) {
            return grunnlagshendelse.opplysning
        }

        val grunnlag = hentOpplysningsgrunnlag(behandlingId)
        val soekerFnr = grunnlag?.soeker?.hentFoedselsnummer()?.verdi ?: return null
        val historiskForeldreansvar =
            pdltjenesterKlient
                .hentHistoriskForeldreansvar(
                    soekerFnr,
                    PersonRolle.BARN,
                    SakType.BARNEPENSJON,
                ).tilGrunnlagsopplysning(soekerFnr)

        val hendelsenummer =
            opplysningDao.leggOpplysningTilGrunnlag(grunnlag.metadata.sakId, historiskForeldreansvar, soekerFnr)

        opplysningDao.oppdaterVersjonForBehandling(behandlingId, grunnlag.metadata.sakId, hendelsenummer)

        return historiskForeldreansvar
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
                problemer = emptyList(),
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

    override fun laasTilVersjonForBehandling(
        skalLaasesId: UUID,
        idLaasesTil: UUID,
    ): BehandlingGrunnlagVersjon {
        val laastVersjon =
            opplysningDao.hentBehandlingVersjon(idLaasesTil) ?: throw IkkeFunnetException(
                code = "GRUNNLAGVERSJON_IKKE_FUNNET",
                detail = "Fant ikke grunnlagsversjonen for behandling $idLaasesTil",
            )
        opplysningDao.oppdaterVersjonForBehandling(
            behandlingId = skalLaasesId,
            sakId = laastVersjon.sakId,
            hendelsenummer = laastVersjon.hendelsenummer,
        )
        opplysningDao.laasGrunnlagVersjonForBehandling(skalLaasesId)

        return sjekkIkkeNull(opplysningDao.hentBehandlingVersjon(skalLaasesId)) {
            "Fant ikke låst grunnlagsversjon vi akkurat la inn :("
        }
    }

    @TestOnly
    fun valideringsfeilPersongalleriSakPdl(
        persongalleriISak: Persongalleri,
        persongalleriPdl: Persongalleri,
    ): List<MismatchPersongalleri> {
        val forskjellerSoeker =
            forskjellerMellomPersonerPdlOgSak(
                personerPdl = listOf(persongalleriPdl.soeker),
                personerSak = listOf(persongalleriISak.soeker),
            )
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
                personerSak = persongalleriISak.soesken,
            )
        return listOfNotNull(
            MismatchPersongalleri.ENDRET_SOEKER_FNR.takeIf { forskjellerSoeker.kunPdl.isNotEmpty() },
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
            persongalleri.innsender -> Saksrolle.INNSENDER
            else -> Saksrolle.UKJENT
        }

    override fun hentGrunnlagAvType(
        behandlingId: UUID,
        opplysningstype: Opplysningstype,
    ): Grunnlagsopplysning<JsonNode>? = opplysningDao.finnNyesteGrunnlagForBehandling(behandlingId, opplysningstype)?.opplysning

    override fun lagreNyePersonopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Oppretter et grunnlag for personopplysninger")
        oppdaterGrunnlagOgVersjon(sakId, behandlingId, fnr, nyeOpplysninger)
        val persongalleri = hentPersongalleri(sakId)
        if (persongalleri != null) {
            /*
                For å ha konsistens i beskyttelser ved endringer i persongalleriet.
             */
            oppdaterTilgangService.haandtergraderingOgEgenAnsatt(
                sakId,
                persongalleri,
                hentOpplysningsgrunnlagForSak(sakId),
            )
        }
    }

    override fun lagreNyeSaksopplysninger(
        sakId: SakId,
        behandlingId: UUID,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Oppretter et grunnlag for saksopplysninger $sakId")
        oppdaterGrunnlagOgVersjon(sakId, behandlingId, fnr = null, nyeOpplysninger)
    }

    override fun lagreNyeSaksopplysningerBareSak(
        sakId: SakId,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        logger.info("Oppretter et grunnlag for saksopplysninger $sakId")
        oppdaterGrunnlagForSak(sakId = sakId, nyeOpplysninger = nyeOpplysninger, fnr = null)
    }

    override suspend fun oppdaterGrunnlagForSak(oppdaterGrunnlagRequest: OppdaterGrunnlagRequest) {
        val persongalleriJsonNode =
            opplysningDao.finnNyesteGrunnlagForSak(oppdaterGrunnlagRequest.sakId, PERSONGALLERI_V1)?.opplysning

        if (persongalleriJsonNode == null) {
            logger.info("Klarte ikke å hente ut grunnlag for sak ${oppdaterGrunnlagRequest.sakId}. Fant ikke persongalleri")
            throw IllegalStateException("Fant ikke grunnlag for sak ${oppdaterGrunnlagRequest.sakId}. Fant ikke persongalleri")
        }

        val persongalleri = objectMapper.readValue(persongalleriJsonNode.opplysning.toJson(), Persongalleri::class.java)
        opprettEllerOppdaterGrunnlagForSak(
            oppdaterGrunnlagRequest.sakId,
            Opplysningsbehov(
                oppdaterGrunnlagRequest.sakId,
                oppdaterGrunnlagRequest.sakType,
                persongalleri,
                persongalleriJsonNode.kilde,
            ),
        )
    }

    override suspend fun opprettEllerOppdaterGrunnlagForSak(
        sakId: SakId,
        opplysningsbehov: Opplysningsbehov,
    ) {
        val grunnlag = grunnlagHenter.hentGrunnlagsdata(opplysningsbehov)

        grunnlag.personopplysninger.forEach { fnrToOpplysning ->
            oppdaterGrunnlagForSak(
                opplysningsbehov.sakId,
                fnrToOpplysning.first,
                fnrToOpplysning.second,
            )
        }
        oppdaterGrunnlagForSak(sakId, fnr = null, grunnlag.saksopplysninger)

        logger.info("Oppdatert grunnlag (sakId=${opplysningsbehov.sakId})")
    }

    private fun oppdaterGrunnlagForSak(
        sakId: SakId,
        fnr: Folkeregisteridentifikator?,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sakId).map { it.opplysning.id }

        val hendelsenummer =
            nyeOpplysninger
                .mapNotNull { opplysning ->
                    if (opplysning.id in gjeldendeGrunnlag) {
                        logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sakId men den er allerede gjeldende")
                        null
                    } else {
                        opplysningDao.leggOpplysningTilGrunnlag(sakId, opplysning, fnr)
                    }
                }.maxOrNull()

        if (hendelsenummer == null) {
            logger.error("Hendelsenummer er null – kan ikke oppdatere versjon for sak (id=$sakId)")
        }
    }

    private fun oppdaterGrunnlagOgVersjon(
        sakId: SakId,
        behandlingId: UUID,
        fnr: Folkeregisteridentifikator?,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>,
    ) {
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sakId).map { it.opplysning.id }

        val versjon = opplysningDao.hentBehandlingVersjon(behandlingId)
        if (versjon?.laast == true) {
            throw LaastGrunnlagKanIkkeEndres(behandlingId)
        }

        val hendelsenummer =
            nyeOpplysninger
                .mapNotNull { opplysning ->
                    if (opplysning.id in gjeldendeGrunnlag) {
                        logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sakId men den er allerede gjeldende")
                        null
                    } else {
                        opplysningDao.leggOpplysningTilGrunnlag(sakId, opplysning, fnr)
                    }
                }.maxOrNull()

        if (hendelsenummer == null) {
            logger.error("Hendelsenummer er null – kan ikke oppdatere versjon for behandling (id=$behandlingId)")
        } else {
            logger.info("Setter grunnlag for behandling (id=$behandlingId) til hendelsenummer=$hendelsenummer")
            opplysningDao.oppdaterVersjonForBehandling(behandlingId, sakId, hendelsenummer)
        }
    }

    override fun laasVersjonForBehandling(behandlingId: UUID) {
        logger.info("Låser grunnlagsversjon for behandling (id=$behandlingId)")
        opplysningDao.laasGrunnlagVersjonForBehandling(behandlingId)
    }
}

private fun HistorikkForeldreansvar.tilGrunnlagsopplysning(fnr: Folkeregisteridentifikator): Grunnlagsopplysning<JsonNode> =
    Grunnlagsopplysning(
        id = UUID.randomUUID(),
        kilde = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, null),
        opplysningType = Opplysningstype.HISTORISK_FORELDREANSVAR,
        meta = objectMapper.createObjectNode(),
        opplysning = this.toJsonNode(),
        attestering = null,
        fnr = fnr,
        periode = null,
    )

private fun Grunnlagsopplysning<JsonNode>.asPersonopplysning(): Personopplysning =
    Personopplysning(
        id = this.id,
        kilde = this.kilde.tilGenerellKilde(),
        opplysningType = this.opplysningType,
        opplysning = objectMapper.treeToValue(opplysning, Person::class.java),
    )

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

        is Grunnlagsopplysning.Gjenoppretting -> GenerellKilde(type = this.type, tidspunkt = this.tidspunkt)
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

        is Grunnlagsopplysning.UkjentInnsender -> GenerellKilde(this.type, tidspunkt = this.tidspunkt, detalj = null)
        is Grunnlagsopplysning.Gjenny -> GenerellKilde(this.type, tidspunkt = this.tidspunkt, detalj = null)
        is Grunnlagsopplysning.Alderspensjon -> GenerellKilde(this.type, tidspunkt = this.tidspunkt, detalj = null)
        is Grunnlagsopplysning.Ufoeretrygd -> GenerellKilde(this.type, tidspunkt = this.tidspunkt, detalj = null)
    }

data class GrunnlagsopplysningerPersonPdl(
    val person: Person,
    val personDto: PersonDTO,
    val opplysningstype: Opplysningstype,
    val personRolle: PersonRolle,
)

class LaastGrunnlagKanIkkeEndres(
    behandlingId: UUID,
) : IkkeTillattException(
        code = "LAAST_GRUNNLAG_KAN_IKKE_ENDRES",
        detail = """
            Kan ikke sette ny grunnlagsversjon på behandling som er
            låst til en versjon av grunnlag (behandlingId=$behandlingId)
            """,
    )

data class Personopplysning(
    val opplysningType: Opplysningstype,
    val id: UUID,
    val kilde: GenerellKilde,
    val opplysning: Person,
)

data class ForskjellMellomPersoner(
    val kunSak: List<String>,
    val kunPdl: List<String>,
)
