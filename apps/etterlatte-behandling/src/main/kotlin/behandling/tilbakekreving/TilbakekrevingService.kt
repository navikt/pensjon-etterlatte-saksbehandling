package no.nav.etterlatte.behandling.tilbakekreving

import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tilbakekreving.Kravgrunnlag
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class TilbakekrevingService(
    private val tilbakekrevingDao: TilbakekrevingDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
) {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    fun hentTilbakekreving(tilbakerevingId: UUID): Tilbakekreving? =
        inTransaction {
            tilbakekrevingDao.hentTilbakekreving(tilbakerevingId)
        }

    fun opprettTilbakekreving(kravgrunnlag: Kravgrunnlag) =
        inTransaction {
            logger.info("Oppretter tilbakekreving=${kravgrunnlag.kravgrunnlagId} på sak=${kravgrunnlag.sakId}")

            val sak = sakDao.hentSak(kravgrunnlag.sakId.value) ?: throw KravgrunnlagHarIkkeEksisterendeSakException()

            val tilbakekreving =
                tilbakekrevingDao.lagreTilbakekreving(
                    Tilbakekreving.ny(kravgrunnlag, sak),
                )

            oppgaveService.opprettNyOppgaveMedSakOgReferanse(
                referanse = tilbakekreving.id.toString(),
                sakId = tilbakekreving.sak.id,
                oppgaveKilde = OppgaveKilde.EKSTERN,
                oppgaveType = OppgaveType.TILBAKEKREVING,
                merknad = null,
            )

            hendelseDao.tilbakekrevingOpprettet(
                tilbakekrevingId = tilbakekreving.id,
                sakId = tilbakekreving.sak.id,
            )
        }
}
