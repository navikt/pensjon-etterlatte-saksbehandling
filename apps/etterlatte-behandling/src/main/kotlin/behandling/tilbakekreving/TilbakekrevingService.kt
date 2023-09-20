package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgaveNy.OppgaveType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.oppgaveny.OppgaveServiceNy
import no.nav.etterlatte.sak.SakDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TilbakekrevingService(
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveServiceNy: OppgaveServiceNy
) {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) {
        oppgaveServiceNy.opprettNyOppgaveMedSakOgReferanse(
            referanse =  kravgrunnlag.kravgrunnlagId.value.toString(),
            sakId = kravgrunnlag.sakId.value,
            oppgaveKilde = OppgaveKilde.EKSTERN,
            oppgaveType = OppgaveType.TILBAKEKREVING,
            merknad = null
        )
    }

}