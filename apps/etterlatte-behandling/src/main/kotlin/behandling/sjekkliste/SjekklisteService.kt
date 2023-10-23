package no.nav.etterlatte.behandling.sjekkliste

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.oppgave.OppgaveService
import java.util.UUID

class SjekklisteService(
    private val dao: SjekklisteDao,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    fun hentSjekkliste(id: UUID): Sjekkliste? {
        return inTransaction {
            dao.hentSjekkliste(id)
        }
    }

    fun opprettSjekkliste(behandlingId: UUID): Sjekkliste {
        val behandling =
            inTransaction {
                requireNotNull(behandlingService.hentBehandling(behandlingId))
            }

        if (!kanEndres(behandling.status)) {
            throw SjekklisteIkkeTillattException(
                kode = "SJEKK01",
                "Kan ikke opprette sjekkliste for behandling ${behandling.id} med status ${behandling.status}",
            )
        } else if (hentSjekkliste(behandlingId) != null) {
            throw SjekklisteUgyldigForespoerselException(
                kode = "SJEKK02",
                "Det finnes allerede en sjekkliste for behandling ${behandling.id}",
            )
        }

        val items =
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON -> defaultSjekklisteItemsBP
                SakType.OMSTILLINGSSTOENAD -> defaultSjekklisteItemsOMS
            }

        inTransaction {
            dao.opprettSjekkliste(behandling.id, items)
        }

        return requireNotNull(hentSjekkliste(behandling.id))
    }

    fun oppdaterSjekkliste(
        behandlingId: UUID,
        oppdaterSjekkliste: OppdatertSjekkliste,
    ): Sjekkliste {
        return inTransaction {
            val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))

            if (!(kanEndres(behandling.status) && behandling.oppgaveUnderArbeidErTildeltGjeldendeSaksbehandler())) {
                throw SjekklisteIkkeTillattException(
                    kode = "SJEKK03",
                    "Kan ikke oppdatere sjekkliste for behandling ${behandling.id} med status ${behandling.status}",
                )
            }

            dao.oppdaterSjekkliste(behandlingId, oppdaterSjekkliste)
            dao.hentSjekkliste(behandlingId)!!
        }
    }

    fun oppdaterSjekklisteItem(
        behandlingId: UUID,
        itemId: Long,
        oppdatering: OppdaterSjekklisteItem,
    ): SjekklisteItem {
        return inTransaction {
            val behandling = requireNotNull(behandlingService.hentBehandling(behandlingId))

            if (!(kanEndres(behandling.status) && behandling.oppgaveUnderArbeidErTildeltGjeldendeSaksbehandler())) {
                throw SjekklisteIkkeTillattException(
                    kode = "SJEKK04",
                    "Kan ikke oppdatere sjekklisteelement for behandling ${behandling.id} med status ${behandling.status}",
                )
            }

            dao.oppdaterSjekklisteItem(itemId, oppdatering)
            dao.hentSjekklisteItem(itemId)
        }
    }

    private fun kanEndres(status: BehandlingStatus): Boolean {
        return BehandlingStatus.underBehandling().contains(status)
    }

    private fun Behandling.oppgaveUnderArbeidErTildeltGjeldendeSaksbehandler(): Boolean {
        return Kontekst.get().AppUser.name() == oppgaveService.hentSaksbehandlerForOppgaveUnderArbeid(this.id)
    }
}

internal class SjekklisteUgyldigForespoerselException(kode: String, detail: String) : UgyldigForespoerselException(
    code = kode,
    detail = detail,
)

internal class SjekklisteIkkeTillattException(kode: String, detail: String) : IkkeTillattException(
    code = kode,
    detail = detail,
)
