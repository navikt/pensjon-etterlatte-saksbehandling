package no.nav.etterlatte.behandling.sjekkliste

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.oppgave.OppgaveService
import java.util.UUID

class SjekklisteService(
    private val dao: SjekklisteDao,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {
    fun hentSjekkliste(id: UUID): Sjekkliste? =
        inTransaction {
            dao.hentSjekkliste(id)
        }

    fun opprettSjekkliste(behandlingId: UUID): Sjekkliste {
        val behandling =
            inTransaction {
                krevIkkeNull(behandlingService.hentBehandling(behandlingId)) {
                    "Fant ikke behandling – kan ikke opprette sjekkliste"
                }
            }

        if (!kanEndres(behandling)) {
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

        val sjekklisteItems =
            when (behandling.sak.sakType) {
                SakType.BARNEPENSJON ->
                    when (behandling.type) {
                        BehandlingType.FØRSTEGANGSBEHANDLING ->
                            if (behandling.erSluttbehandling()) {
                                skjekklisteItemsFoerstegangsbehandlingBP + sjekklisteItemsBosattNorgeSluttbehandling
                            } else {
                                skjekklisteItemsFoerstegangsbehandlingBP
                            }
                        BehandlingType.REVURDERING -> {
                            if (behandling.revurderingsaarsak() == Revurderingaarsak.SLUTTBEHANDLING) {
                                sjekklisteItemsRevurderingBP + sjekklisteItemsBosattNorgeSluttbehandling
                            } else {
                                sjekklisteItemsRevurderingBP
                            }
                        }
                    }
                SakType.OMSTILLINGSSTOENAD -> {
                    when (behandling.type) {
                        BehandlingType.FØRSTEGANGSBEHANDLING ->
                            if (behandling.erSluttbehandling()) {
                                sjekklisteItemsFoerstegangsbehandlingOMS + sjekklisteItemsBosattNorgeSluttbehandling
                            } else {
                                sjekklisteItemsFoerstegangsbehandlingOMS
                            }

                        BehandlingType.REVURDERING -> {
                            if (behandling.revurderingsaarsak() == Revurderingaarsak.SLUTTBEHANDLING) {
                                sjekklisteItemsRevurderingOMS + sjekklisteItemsBosattNorgeSluttbehandling
                            } else {
                                sjekklisteItemsRevurderingOMS
                            }
                        }
                    }
                }
            }

        inTransaction {
            dao.opprettSjekkliste(behandling.id, sjekklisteItems)
        }

        return krevIkkeNull(hentSjekkliste(behandling.id)) {
            "Kunne ikke hente ut sjekkliste som nettopp ble opprettet"
        }
    }

    fun oppdaterSjekkliste(
        behandlingId: UUID,
        oppdaterSjekkliste: OppdatertSjekkliste,
    ): Sjekkliste =
        inTransaction {
            val behandling =
                krevIkkeNull(behandlingService.hentBehandling(behandlingId)) {
                    "Fant ikke behandling – kan ikke oppdatere sjekkliste"
                }

            if (!(kanEndres(behandling) && behandling.oppgaveUnderArbeidErTildeltGjeldendeSaksbehandler())) {
                throw SjekklisteIkkeTillattException(
                    kode = "SJEKK03",
                    "Kan ikke oppdatere sjekkliste for behandling ${behandling.id} med status ${behandling.status}",
                )
            }

            dao.oppdaterSjekkliste(behandlingId, oppdaterSjekkliste)
            dao.hentSjekkliste(behandlingId)!!
        }

    fun oppdaterSjekklisteItem(
        behandlingId: UUID,
        itemId: Long,
        oppdatering: OppdaterSjekklisteItem,
    ): SjekklisteItem =
        inTransaction {
            val behandling =
                krevIkkeNull(behandlingService.hentBehandling(behandlingId)) {
                    "Fant ikke behandling - kan ikke oppdatere sjekklistepunkt"
                }

            if (!(kanEndres(behandling) && behandling.oppgaveUnderArbeidErTildeltGjeldendeSaksbehandler())) {
                throw SjekklisteIkkeTillattException(
                    kode = "SJEKK04",
                    "Kan ikke oppdatere sjekklisteelement for behandling ${behandling.id} med status ${behandling.status}",
                )
            }

            dao.oppdaterSjekklisteItem(itemId, oppdatering)
            dao.hentSjekklisteItem(itemId)
        }

    private fun kanEndres(behandling: Behandling): Boolean =
        BehandlingStatus.underBehandling().contains(behandling.status) &&
            behandling is Foerstegangsbehandling ||
            behandling is Revurdering

    private fun Behandling.oppgaveUnderArbeidErTildeltGjeldendeSaksbehandler(): Boolean =
        Kontekst.get().AppUser.name() ==
            oppgaveService
                .hentOppgaveForAttesterbarBehandling(this.id.toString())
                ?.saksbehandler
                ?.ident
}

internal class SjekklisteUgyldigForespoerselException(
    kode: String,
    detail: String,
) : UgyldigForespoerselException(
        code = kode,
        detail = detail,
    )

internal class SjekklisteIkkeTillattException(
    kode: String,
    detail: String,
) : IkkeTillattException(
        code = kode,
        detail = detail,
    )
