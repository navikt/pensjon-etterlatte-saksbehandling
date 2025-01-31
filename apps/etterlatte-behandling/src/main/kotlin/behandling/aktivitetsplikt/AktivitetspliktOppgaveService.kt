package no.nav.etterlatte.behandling.aktivitetsplikt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgrad
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.BrevParametre
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.brev.model.FerdigstillJournalFoerOgDistribuerOpprettetBrev
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.oms.Aktivitetsgrad
import no.nav.etterlatte.brev.model.oms.NasjonalEllerUtland
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

enum class AktivitetspliktOppgaveToggles(
    private val key: String,
) : FeatureToggle {
    UNNTAK_UTEN_FRIST("aktivitetsplikt-oppgave-unntak-ingen-frist"),
    UNNTAK_MED_FRIST("aktivitetsplikt-oppgave-unntak-med-frist"),
    ;

    override fun key(): String = key
}

class AktivitetspliktOppgaveService(
    private val aktivitetspliktService: AktivitetspliktService,
    private val oppgaveService: OppgaveService,
    private val sakService: SakService,
    private val aktivitetspliktBrevDao: AktivitetspliktBrevDao,
    private val brevApiKlient: BrevApiKlient,
    private val behandlingService: BehandlingService,
    private val unleashFeatureToggleService: FeatureToggleService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

    private fun hentSistEndretAktivitetspliktSomErRelevantForBrev(
        brevdata: AktivitetspliktInformasjonBrevdata?,
        vurderinger: AktivitetspliktVurdering?,
    ): Tidspunkt? =
        listOfNotNull(
            brevdata?.kilde?.tidspunkt,
            vurderinger?.aktivitet?.maxOfOrNull {
                it.endret.tidspunkt
            },
        ).maxOrNull()

    fun hentVurderingForOppgave(oppgaveId: UUID): AktivitetspliktOppgaveVurdering {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val vurderingType =
            when (oppgave.type) {
                OppgaveType.AKTIVITETSPLIKT -> VurderingType.SEKS_MAANEDER
                OppgaveType.AKTIVITETSPLIKT_12MND -> VurderingType.TOLV_MAANEDER
                else -> throw UgyldigForespoerselException(
                    "OPPGAVE_ER_IKKE_AKTIVITETSPLIKT",
                    "Oppgaven har ikke typen aktivitetsplikt",
                )
            }
        val sak = sakService.finnSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
        val vurderingerPaaOppgave = aktivitetspliktService.hentVurderingForOppgave(oppgaveId)
        val vurderinger =
            if (vurderingerPaaOppgave.erTom() && !oppgave.erAvsluttet()) {
                // kopier de inn fra sak
                aktivitetspliktService.kopierInnTilOppgave(sak.id, oppgaveId)
            } else {
                vurderingerPaaOppgave
            }

        val brevdata = aktivitetspliktBrevDao.hentBrevdata(oppgaveId = oppgaveId)

        val sistEndret = hentSistEndretAktivitetspliktSomErRelevantForBrev(brevdata, vurderinger)
        return AktivitetspliktOppgaveVurdering(
            sistEndret = sistEndret,
            aktivtetspliktbrevdata = brevdata,
            vurderingType = vurderingType,
            oppgave = oppgave,
            sak = sak,
            vurdering = vurderinger,
        )
    }

    fun lagreBrevdata(
        oppgaveId: UUID,
        data: AktivitetspliktInformasjonBrevdataRequest,
    ): AktivitetspliktInformasjonBrevdata {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        /*
        Hvis oppgaven er ferdigstilt fra forrige vurdering må vi på en måte få lagt den
        inn i brevdataen her.
        Holder det å sjekke på oppgavetype + brev for sak av den typen?
        Tidligere hadde man ingen referanse til denne.
         */
        if (oppgave.status.erAvsluttet()) {
            throw OppgaveErAvsluttet("Oppgave er avsluttet, id $oppgaveId. Kan ikke fjerne brevid")
        }

        val sak = sakService.finnSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
        val saksbehandler =
            Kontekst.get().brukerTokenInfo
                ?: throw IngenSaksbehandler("Fant ingen saksbehandler til lagring av brevdata for oppgave $oppgaveId")
        val kilde = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident())

        val eksisterendeData = aktivitetspliktBrevDao.hentBrevdata(oppgaveId)

        aktivitetspliktBrevDao.lagreBrevdata(data.toDaoObjektBrevutfall(oppgaveId, sakid = sak.id, kilde = kilde))
        val oppdatertData = aktivitetspliktBrevDao.hentBrevdata(oppgaveId = oppgaveId)

        // Hvis brev allerede er opprettet kan det hende vi må endre / slette det
        if (oppdatertData?.brevId != null) {
            // Hvis brevet ikke er relevant skal det slettes
            if (!data.skalSendeBrev) {
                aktivitetspliktBrevDao.fjernBrevId(oppgaveId, kilde)
                runBlocking {
                    brevApiKlient.slettBrev(
                        brevId = oppdatertData.brevId,
                        sakId = sak.id,
                        brukerTokenInfo = saksbehandler,
                    )
                }
                // Hvis brevdata er endret må vi oppdatere brevet
            } else if (!oppdatertData.harLikeUtfall(eksisterendeData)) {
                val brevParametre = mapOgValiderBrevParametre(oppgave, oppdatertData)
                runBlocking {
                    brevApiKlient.oppdaterSpesifiktBrev(
                        sakId = sak.id,
                        brevParametre = brevParametre,
                        brukerTokenInfo = saksbehandler,
                        brevId = oppdatertData.brevId,
                    )
                }
            }
        }
        return aktivitetspliktBrevDao.hentBrevdata(oppgaveId)!!
    }

    private fun mapOgValiderBrevParametre(
        oppgave: OppgaveIntern,
        brevdata: AktivitetspliktInformasjonBrevdata,
    ): BrevParametre {
        val vurderingForOppgave = aktivitetspliktService.hentVurderingForOppgave(oppgave.id)

        val sisteAktivtetsgrad =
            vurderingForOppgave.aktivitet.maxByOrNull { it.fom }
                ?: throw ManglerAktivitetsgrad("Mangler aktivitetsgrad for oppgave: ${oppgave.id}")

        sjekkOmHarNyVurdering(oppgave, vurderingForOppgave.aktivitet)

        if (brevdata.manglerUtfylling()) {
            throw UgyldigForespoerselException(
                "MANGLER_UTFYLLING",
                "Data for brev mangler utfylling, og må redigeres og lagres på nytt for å opprette brevet.",
            )
        }

        val nasjonalEllerUtland =
            behandlingService.hentUtlandstilknytningForSak(oppgave.sakId)
                ?: throw ManglerUtlandstilknytning("Mangler utlandstilknytning for sakid ${oppgave.sakId} oppgaveid: ${oppgave.id}")

        val aktivitetsgrad = mapAktivitetsgradstypeTilAktivtetsgrad(sisteAktivtetsgrad.aktivitetsgrad)
        val utbetaling =
            krevIkkeNull(brevdata.utbetaling) { "Mangler utbetaling for utbetaling for oppgave ${oppgave.id}" }
        val redusertEtterInntekt =
            krevIkkeNull(brevdata.redusertEtterInntekt) { "Mangler redusert-inntekt for oppgave ${oppgave.id}" }
        val spraak = krevIkkeNull(brevdata.spraak) { "Mangler spraak for oppgave ${oppgave.id}" }
        val nasjonalEllerUtlandMapped = mapNasjonalEllerUtland(nasjonalEllerUtland.type)

        val brevparametere =
            when (oppgave.type) {
                OppgaveType.AKTIVITETSPLIKT ->
                    BrevParametre.AktivitetspliktInformasjon4Mnd(
                        aktivitetsgrad = aktivitetsgrad,
                        utbetaling = utbetaling,
                        redusertEtterInntekt = redusertEtterInntekt,
                        nasjonalEllerUtland = nasjonalEllerUtlandMapped,
                        spraak = spraak,
                    )

                OppgaveType.AKTIVITETSPLIKT_12MND ->
                    BrevParametre.AktivitetspliktInformasjon10Mnd(
                        aktivitetsgrad = aktivitetsgrad,
                        utbetaling = utbetaling,
                        redusertEtterInntekt = redusertEtterInntekt,
                        nasjonalEllerUtland = nasjonalEllerUtlandMapped,
                        spraak = spraak,
                    )

                else -> throw FeilOppgavetype("Prøver å lage aktivitetsplikt med oppgavetype: ${oppgave.type} id: ${oppgave.id}")
            }
        return brevparametere
    }

    private fun mapAktivitetsgradstypeTilAktivtetsgrad(aktivitetsgrad: AktivitetspliktAktivitetsgradType): Aktivitetsgrad =
        when (aktivitetsgrad) {
            AktivitetspliktAktivitetsgradType.AKTIVITET_UNDER_50 -> Aktivitetsgrad.UNDER_50_PROSENT
            AktivitetspliktAktivitetsgradType.AKTIVITET_OVER_50 -> Aktivitetsgrad.OVER_50_PROSENT
            AktivitetspliktAktivitetsgradType.AKTIVITET_100 -> Aktivitetsgrad.AKKURAT_100_PROSENT
        }

    private fun mapNasjonalEllerUtland(utland: UtlandstilknytningType): NasjonalEllerUtland =
        when (utland) {
            UtlandstilknytningType.NASJONAL -> NasjonalEllerUtland.NASJONAL
            UtlandstilknytningType.UTLANDSTILSNITT -> NasjonalEllerUtland.UTLAND
            UtlandstilknytningType.BOSATT_UTLAND -> NasjonalEllerUtland.UTLAND
        }

    private fun sjekkOmHarNyVurdering(
        oppgave: OppgaveIntern,
        aktiviteterIOppgave: List<AktivitetspliktAktivitetsgrad>,
    ) {
        val harNyVurdering =
            when (oppgave.type) {
                OppgaveType.AKTIVITETSPLIKT_12MND -> aktiviteterIOppgave.any { it.vurdertFra12Mnd }
                OppgaveType.AKTIVITETSPLIKT -> aktiviteterIOppgave.isNotEmpty()
                else -> throw UgyldigForespoerselException(
                    "FEIL_OPPGAVETYPE",
                    "Kan ikke opprette brev for aktivitetsplikt med oppgavetype ${oppgave.type}",
                )
            }
        if (!harNyVurdering) {
            throw UgyldigForespoerselException(
                "MANGLER_NY_VURDERING",
                "Ny vurdering av aktivitetsplikt i denne oppgaven mangler. Den må legges til før brev kan opprettes",
            )
        }
    }

    fun opprettBrevHvisKraveneErOppfyltOgDetIkkeFinnes(
        oppgaveId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): BrevID {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val brevData = aktivitetspliktBrevDao.hentBrevdata(oppgaveId) ?: throw GenerellIkkeFunnetException()
        if (brevData.brevId != null) {
            return brevData.brevId
        }
        val skalOppretteBrev = skalOppretteBrev(brevData)
        if (skalOppretteBrev) {
            val brevparametereAktivitetspliktVurdering = mapOgValiderBrevParametre(oppgave, brevData)
            val opprettetBrev =
                runBlocking {
                    brevApiKlient.opprettSpesifiktBrev(
                        oppgave.sakId,
                        brevparametereAktivitetspliktVurdering,
                        brukerTokenInfo = brukerTokenInfo,
                    )
                }
            aktivitetspliktBrevDao.lagreBrevId(oppgaveId, opprettetBrev.id)
            return opprettetBrev.id
        } else {
            throw BrevFeil("Kunne ikke opprette brev for $oppgaveId se data: ${brevData.toJson()}")
        }
    }

    private fun skalOppretteBrev(brevdata: AktivitetspliktInformasjonBrevdata): Boolean {
        if (brevdata.skalSendeBrev) {
            if (brevdata.brevId == null) {
                val harUtbetaling = brevdata.utbetaling != null
                val harReduserEtterInntekt = brevdata.redusertEtterInntekt != null
                if (!harUtbetaling || !harReduserEtterInntekt) {
                    logger.info("Mangler brevdatainformasjon for oppgaveid ${brevdata.oppgaveId}")
                    throw ManglerBrevdata("Mangler brevdatainformasjon for oppgaveid ${brevdata.oppgaveId}")
                }
                return true
            } else {
                logger.info("Oppretter ikke brev for oppgaveid ${brevdata.oppgaveId} brevid finnes allerede, id: ${brevdata.brevId}")
                return false
            }
        } else {
            logger.info("Oppretter ikke brev for oppgaveid ${brevdata.oppgaveId} har ikke valgt å sende brev for oppgave")
            return false
        }
    }

    fun ferdigstillBrevOgOppgave(
        oppgaveId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): OppgaveIntern {
        val brevData = aktivitetspliktBrevDao.hentBrevdata(oppgaveId) ?: throw GenerellIkkeFunnetException()
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        logger.info("Ferdigstiller brev aktivitetsplikt for oppgaveid $oppgaveId")
        try {
            oppgaveService.sjekkOmKanFerdigstilleOppgave(oppgave, brukerTokenInfo)
        } catch (e: Exception) {
            throw FeilIOppgave("Kan ikke ferdigstille oppgave for oppgaveid $oppgaveId", e)
        }

        val sak = sakService.finnSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
        val brevId = brevData.brevId ?: throw ManglerBrevdata("Brevid er ikke registrert på oppgaveid $oppgaveId")
        val req =
            FerdigstillJournalFoerOgDistribuerOpprettetBrev(
                brevId = brevId,
                sakId = sak.id,
                enhetsnummer = sak.enhet,
                avsenderRequest = SaksbehandlerOgAttestant(saksbehandlerIdent = brukerTokenInfo.ident()),
            )
        val brevrespons: BrevStatusResponse =
            runBlocking { brevApiKlient.ferdigstillJournalFoerOgDistribuerBrev(req, brukerTokenInfo) }
        if (brevrespons.status.erDistribuert()) {
            if (unleashFeatureToggleService.isEnabled(AktivitetspliktOppgaveToggles.UNNTAK_UTEN_FRIST, false)) {
                val unntakIOppgave = aktivitetspliktService.hentVurderingForOppgave(oppgaveId).unntak
                aktivitetspliktService.opprettOppfoelgingsoppgaveUnntak(oppgaveId, unntakIOppgave, sak)
            }
            return oppgaveService.ferdigstillOppgave(oppgaveId, brukerTokenInfo)
        } else {
            logger.warn("Brev ble ikke distribuert, ferdigstiller ikke oppgave $oppgaveId for aktivitetsplikt")
            throw BrevBleIkkeFerdig(brevrespons.status)
        }
    }
}

class FeilIOppgave(
    msg: String,
    override val cause: Throwable? = null,
) : UgyldigForespoerselException(
        code = "OPPGAVE_AVSLUTTET",
        detail = msg,
        cause = cause,
    )

class OppgaveErAvsluttet(
    msg: String,
) : UgyldigForespoerselException(
        code = "OPPGAVE_AVSLUTTET",
        detail = msg,
    )

class IngenSaksbehandler(
    msg: String,
) : UgyldigForespoerselException(
        code = "INGEN_SAKSBEHANDLER",
        detail = msg,
    )

class BrevBleIkkeFerdig(
    status: Status,
) : InternfeilException(
        detail = "Brevet ble ikke helt ferdig, prøv igjen. Om det ikke går kontakt support. Brevstatus: ${status.name}",
    )

class BrevFeil(
    msg: String,
) : UgyldigForespoerselException(
        code = "FEIL_I_BREV_FORESPØRSEL",
        detail = msg,
    )

class FeilOppgavetype(
    msg: String,
) : UgyldigForespoerselException(
        code = "FEIL_OPPGAVETYPE",
        detail = msg,
    )

class ManglerUtlandstilknytning(
    msg: String,
) : UgyldigForespoerselException(
        code = "MANGLER_UTLANDSTILKNTTNING",
        detail = msg,
    )

class ManglerBrevdata(
    msg: String,
) : UgyldigForespoerselException(
        code = "MANGLER_BREVDATA",
        detail = msg,
    )

class ManglerAktivitetsgrad(
    msg: String,
) : UgyldigForespoerselException(
        code = "MANGLER_AKITIVITETSGRAD",
        detail = msg,
    )

data class AktivitetspliktInformasjonBrevdataRequest(
    val skalSendeBrev: Boolean,
    val utbetaling: Boolean? = null,
    val redusertEtterInntekt: Boolean? = null,
    val spraak: Spraak? = null,
    val begrunnelse: String? = null,
) {
    fun toDaoObjektBrevutfall(
        oppgaveId: UUID,
        sakid: SakId,
        kilde: Grunnlagsopplysning.Saksbehandler,
    ): AktivitetspliktInformasjonBrevdata =
        AktivitetspliktInformasjonBrevdata(
            oppgaveId = oppgaveId,
            sakid = sakid,
            kilde = kilde,
            utbetaling = this.utbetaling,
            redusertEtterInntekt = this.redusertEtterInntekt,
            skalSendeBrev = this.skalSendeBrev,
            spraak = this.spraak,
            begrunnelse = this.begrunnelse,
        )
}

data class AktivitetspliktInformasjonBrevdata(
    val oppgaveId: UUID,
    val sakid: SakId,
    val brevId: Long? = null,
    val skalSendeBrev: Boolean,
    val utbetaling: Boolean? = null,
    val redusertEtterInntekt: Boolean? = null,
    val begrunnelse: String? = null,
    val spraak: Spraak?,
    val kilde: Grunnlagsopplysning.Saksbehandler,
) {
    fun manglerUtfylling(): Boolean = skalSendeBrev && listOf(utbetaling, spraak, redusertEtterInntekt, spraak).any { it == null }

    fun harLikeUtfall(annen: AktivitetspliktInformasjonBrevdata?): Boolean =
        this.skalSendeBrev == annen?.skalSendeBrev &&
            this.utbetaling == annen.utbetaling &&
            this.redusertEtterInntekt == annen.redusertEtterInntekt &&
            this.spraak == annen.spraak
}

data class AktivitetspliktOppgaveVurdering(
    val vurderingType: VurderingType,
    val oppgave: OppgaveIntern,
    val sak: Sak,
    val vurdering: AktivitetspliktVurdering,
    val aktivtetspliktbrevdata: AktivitetspliktInformasjonBrevdata?,
    val sistEndret: Tidspunkt?,
)

enum class VurderingType {
    SEKS_MAANEDER,
    TOLV_MAANEDER,
}
