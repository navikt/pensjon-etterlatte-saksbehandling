package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakService
import java.util.UUID

class AktivitetspliktOppgaveService(
    private val aktivitetspliktService: AktivitetspliktService,
    private val oppgaveService: OppgaveService,
    private val sakService: SakService,
    private val aksBrevDao: AktivitetspliktBrevDao,
) {
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

        val brevdata = aksBrevDao.hentBrevdata(oppgaveId = oppgaveId)

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
    ) {
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val sak = sakService.finnSak(oppgave.sakId) ?: throw GenerellIkkeFunnetException()
        aksBrevDao.lagreBrevdata(data.toDaoObjekt(oppgaveId, sakid = sak.id))
    }
}

data class AktivitetspliktInformasjonBrevdataRequest(
    val skalSendeBrev: Boolean,
    val utbetaling: Boolean? = null,
    val redusertEtterInntekt: Boolean? = null,
) {
    fun toDaoObjekt(
        oppgaveId: UUID,
        sakid: SakId,
    ): AktivitetspliktInformasjonBrevdata =
        AktivitetspliktInformasjonBrevdata(
            oppgaveId = oppgaveId,
            sakid = sakid,
            utbetaling = this.utbetaling,
            redusertEtterInntekt = this.redusertEtterInntekt,
            skalSendeBrev = this.skalSendeBrev,
        )
}

data class AktivitetspliktInformasjonBrevdata(
    val oppgaveId: UUID,
    val sakid: SakId,
    val skalSendeBrev: Boolean,
    val utbetaling: Boolean? = null,
    val redusertEtterInntekt: Boolean? = null,
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
