package no.nav.etterlatte.behandling.aktivitetsplikt

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.aktivitetsplikt.vurdering.AktivitetspliktAktivitetsgradType
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.brev.BrevParametre
import no.nav.etterlatte.brev.SaksbehandlerOgAttestant
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevStatusResponse
import no.nav.etterlatte.brev.model.FerdigstillJournalFoerOgDistribuerOpprettetBrev
import no.nav.etterlatte.brev.model.Status
import no.nav.etterlatte.brev.model.oms.Aktivitetsgrad
import no.nav.etterlatte.brev.model.oms.NasjonalEllerUtland
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class AktivitetspliktOppgaveService(
    private val aktivitetspliktService: AktivitetspliktService,
    private val oppgaveService: OppgaveService,
    private val sakService: SakService,
    private val aktivitetspliktBrevDao: AktivitetspliktBrevDao,
    private val brevApiKlient: BrevApiKlient,
    private val behandlingService: BehandlingService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.name)

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
            if (vurderingerPaaOppgave == null && !oppgave.erAvsluttet()) {
                // kopier de inn fra sak
                aktivitetspliktService.kopierInnTilOppgave(sak.id, oppgaveId)
            } else {
                vurderingerPaaOppgave
            }

        val brevdata = aktivitetspliktBrevDao.hentBrevdata(oppgaveId = oppgaveId)

        return AktivitetspliktOppgaveVurdering(
            aktivtetspliktbrevdata = brevdata,
            vurderingType = vurderingType,
            oppgave = oppgave,
            sak = sak,
            vurdering =
                vurderinger ?: AktivitetspliktVurdering(
                    emptyList(),
                    emptyList(),
                ),
        )
    }

    fun lagreBrevdata(
        oppgaveId: UUID,
        data: AktivitetspliktInformasjonBrevdataRequest,
    ): AktivitetspliktInformasjonBrevdata {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        if (oppgave.status.erAvsluttet()) {
            throw OppgaveErAvsluttet("Oppgave er avsluttet, id $oppgaveId. Kan ikke fjerne brevid")
        } else {
            val sak = sakService.finnSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
            val saksbehandler =
                Kontekst.get().brukerTokenInfo
                    ?: throw IngenSaksbehandler("Fant ingen saksbehandler til lagring av brevdata for oppgave $oppgaveId")
            val kilde = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident())
            aktivitetspliktBrevDao.lagreBrevdata(data.toDaoObjektBrevutfall(oppgaveId, sakid = sak.id, kilde = kilde))
            val hentBrevId = aktivitetspliktBrevDao.hentBrevdata(oppgaveId = oppgaveId)
            if (!data.skalSendeBrev && hentBrevId?.brevId != null) {
                aktivitetspliktBrevDao.fjernBrevId(oppgaveId, kilde)
                runBlocking { brevApiKlient.slettBrev(brevId = hentBrevId.brevId, sakId = sak.id, brukerTokenInfo = saksbehandler) }
            }
            return aktivitetspliktBrevDao.hentBrevdata(oppgaveId)!!
        }
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
            val vurderingForOppgave = aktivitetspliktService.hentVurderingForOppgave(oppgaveId) ?: throw GenerellIkkeFunnetException()
            val sisteAktivtetsgrad =
                vurderingForOppgave.aktivitet.maxByOrNull { it.fom }
                    ?: throw ManglerAktivitetsgrad("Mangler aktivitetsgrad for oppgave: $oppgaveId")
            val nasjonalEllerUtland = behandlingService.hentUtlandstilknytningForSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
            val brevParametreAktivitetsplikt10mnd =
                BrevParametre.AktivitetspliktInformasjon10Mnd(
                    aktivitetsgrad = mapAktivitetsgradstypeTilAktivtetsgrad(sisteAktivtetsgrad.aktivitetsgrad),
                    utbetaling = brevData.utbetaling!!,
                    redusertEtterInntekt = brevData.redusertEtterInntekt!!,
                    nasjonalEllerUtland = mapNasjonalEllerUtland(nasjonalEllerUtland.type),
                )
            val opprettetBrev =
                runBlocking {
                    brevApiKlient.opprettSpesifiktBrev(
                        oppgave.sakId,
                        brevParametreAktivitetsplikt10mnd,
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
    ) {
        val brevData = aktivitetspliktBrevDao.hentBrevdata(oppgaveId) ?: throw GenerellIkkeFunnetException()
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val sak = sakService.finnSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
        val brevId = brevData.brevId ?: throw ManglerBrevdata("Brevid er ikke registrert på oppgaveid $oppgaveId")
        val req =
            FerdigstillJournalFoerOgDistribuerOpprettetBrev(
                brevId = brevId,
                sakId = sak.id,
                enhetsnummer = sak.enhet,
                avsenderRequest = SaksbehandlerOgAttestant(saksbehandlerIdent = brukerTokenInfo.ident()),
            )
        val brevrespons: BrevStatusResponse = runBlocking { brevApiKlient.ferdigstillBrev(req, brukerTokenInfo) }
        if (brevrespons.status.erDistribuert()) {
            oppgaveService.ferdigstillOppgave(oppgaveId, brukerTokenInfo)
        } else {
            throw BrevBleIkkeFerdig(brevrespons.status)
        }
    }
}

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
) {
    fun toDaoObjektBrevutfall(
        oppgaveId: UUID,
        sakid: SakId,
        kilde: Grunnlagsopplysning.Kilde,
    ): AktivitetspliktInformasjonBrevdata =
        AktivitetspliktInformasjonBrevdata(
            oppgaveId = oppgaveId,
            sakid = sakid,
            kilde = kilde,
            utbetaling = this.utbetaling,
            redusertEtterInntekt = this.redusertEtterInntekt,
            skalSendeBrev = this.skalSendeBrev,
        )
}

data class AktivitetspliktInformasjonBrevdata(
    val oppgaveId: UUID,
    val sakid: SakId,
    val brevId: Long? = null,
    val skalSendeBrev: Boolean,
    val utbetaling: Boolean? = null,
    val redusertEtterInntekt: Boolean? = null,
    val kilde: Grunnlagsopplysning.Kilde,
)

data class AktivitetspliktOppgaveVurdering(
    val vurderingType: VurderingType,
    val oppgave: OppgaveIntern,
    val sak: Sak,
    val vurdering: AktivitetspliktVurdering,
    val aktivtetspliktbrevdata: AktivitetspliktInformasjonBrevdata?,
)

enum class VurderingType {
    SEKS_MAANEDER,
    TOLV_MAANEDER,
}
