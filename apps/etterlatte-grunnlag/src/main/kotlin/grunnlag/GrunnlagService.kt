package no.nav.etterlatte.grunnlag

import com.fasterxml.jackson.databind.JsonNode
import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakOgRolle
import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.deserialize
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Beregningsgrunnlag
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Navn
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeskenMedIBeregning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.sporingslogg.Decision
import no.nav.etterlatte.libs.sporingslogg.HttpMethod
import no.nav.etterlatte.libs.sporingslogg.Sporingslogg
import no.nav.etterlatte.libs.sporingslogg.Sporingsrequest
import no.nav.etterlatte.token.Bruker
import org.slf4j.LoggerFactory
import java.util.UUID

interface GrunnlagService {
    fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>?
    fun hentOpplysningstypeNavnFraFnr(fnr: Foedselsnummer, navIdent: String): NavnOpplysningDTO?
    fun lagreNyeSaksopplysninger(sak: Long, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun lagreNyePersonopplysninger(sak: Long, fnr: Foedselsnummer, nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>)
    fun hentOpplysningsgrunnlag(sak: Long): Grunnlag?
    fun hentOpplysningsgrunnlagMedVersjon(sak: Long, versjon: Long): Grunnlag?
    fun hentOpplysningsgrunnlag(
        sak: Long,
        persongalleri: Persongalleri
    ): Grunnlag // TODO ai: Kan fjernes når kafka flyten fjernes

    suspend fun lagreSoeskenMedIBeregning(
        behandlingId: UUID,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        bruker: Bruker
    )

    fun hentSakerOgRoller(fnr: Foedselsnummer): PersonMedSakerOgRoller
    fun hentAlleSakerForFnr(fnr: Foedselsnummer): Set<Long>
}

class RealGrunnlagService(
    private val opplysningDao: OpplysningDao,
    private val behandlingKlient: BehandlingKlient,
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

    override suspend fun lagreSoeskenMedIBeregning(
        behandlingId: UUID,
        soeskenMedIBeregning: List<SoeskenMedIBeregning>,
        bruker: Bruker
    ) {
        val opplysning: List<Grunnlagsopplysning<JsonNode>> = listOf(
            lagOpplysning(
                opplysningsType = Opplysningstype.SOESKEN_I_BEREGNINGEN,
                kilde = Grunnlagsopplysning.Saksbehandler.create(bruker.ident()),
                opplysning = Beregningsgrunnlag(soeskenMedIBeregning).toJsonNode()
            )
        )

        val behandling = behandlingKlient.hentBehandling(behandlingId, bruker)

        val sakId = behandling.sak
        lagreNyeSaksopplysninger(sakId, opplysning)
    }

    override fun hentSakerOgRoller(fnr: Foedselsnummer): PersonMedSakerOgRoller {
        return opplysningDao.finnAllePersongalleriHvorPersonFinnes(fnr)
            .map { it.sakId to deserialize<Persongalleri>(it.opplysning.opplysning.toJson()) }
            .map { (sakId, persongalleri) -> SakOgRolle(sakId, rolle = mapTilRolle(fnr.value, persongalleri)) }
            .let { PersonMedSakerOgRoller(fnr.value, it) }
    }

    override fun hentAlleSakerForFnr(fnr: Foedselsnummer): Set<Long> =
        opplysningDao.finnAlleSakerForPerson(fnr)

    private fun mapTilRolle(fnr: String, persongalleri: Persongalleri): Saksrolle = when (fnr) {
        persongalleri.soeker -> Saksrolle.SOEKER
        in persongalleri.soesken -> Saksrolle.SOESKEN
        in persongalleri.avdoed -> Saksrolle.AVDOED
        in persongalleri.gjenlevende -> Saksrolle.GJENLEVENDE
        else -> Saksrolle.UKJENT
    }

    private fun <T> lagOpplysning(
        opplysningsType: Opplysningstype,
        kilde: Grunnlagsopplysning.Kilde,
        opplysning: T,
        fnr: Foedselsnummer? = null,
        periode: Periode? = null
    ): Grunnlagsopplysning<T> {
        return Grunnlagsopplysning(
            id = UUID.randomUUID(),
            kilde = kilde,
            opplysningType = opplysningsType,
            meta = objectMapper.createObjectNode(),
            opplysning = opplysning,
            fnr = fnr,
            periode = periode
        )
    }

    override fun hentGrunnlagAvType(sak: Long, opplysningstype: Opplysningstype): Grunnlagsopplysning<JsonNode>? {
        return opplysningDao.finnNyesteGrunnlag(sak, opplysningstype)?.opplysning
    }

    override fun hentOpplysningstypeNavnFraFnr(fnr: Foedselsnummer, navIdent: String): NavnOpplysningDTO? {
        val opplysning = opplysningDao.finnNyesteOpplysningPaaFnr(fnr, Opplysningstype.NAVN)?.let {
            val navn: Navn = deserialize(it.opplysning.opplysning.toString())
            NavnOpplysningDTO(
                sakId = it.sakId,
                fornavn = navn.fornavn,
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
        fnr: Foedselsnummer,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
            } else {
                opplysningDao.leggOpplysningTilGrunnlag(sak, opplysning, fnr)
            }
        }
    }

    override fun lagreNyeSaksopplysninger(
        sak: Long,
        nyeOpplysninger: List<Grunnlagsopplysning<JsonNode>>
    ) {
        logger.info("Oppretter et grunnlag")
        val gjeldendeGrunnlag = opplysningDao.finnHendelserIGrunnlag(sak).map { it.opplysning.id }

        for (opplysning in nyeOpplysninger) {
            if (opplysning.id in gjeldendeGrunnlag) {
                logger.warn("Forsøker å lagre opplysning ${opplysning.id} i sak $sak men den er allerede gjeldende")
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

data class NavnOpplysningDTO(
    val sakId: Long,
    val fornavn: String,
    val etternavn: String,
    val foedselsnummer: String
)