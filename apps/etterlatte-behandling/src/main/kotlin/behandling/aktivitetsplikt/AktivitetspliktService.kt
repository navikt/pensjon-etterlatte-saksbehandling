package no.nav.etterlatte.behandling.aktivitetsplikt

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.revurdering.BehandlingKanIkkeEndres
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.AktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.behandling.OpprettAktivitetspliktOppfolging
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

class AktivitetspliktService(
    private val aktivitetspliktDao: AktivitetspliktDao,
    private val behandlingService: BehandlingService,
) {
    fun hentAktivitetspliktOppfolging(behandlingId: UUID): AktivitetspliktOppfolging? {
        return inTransaction {
            aktivitetspliktDao.finnSenesteAktivitetspliktOppfolging(behandlingId)
        }
    }

    fun lagreAktivitetspliktOppfolging(
        behandlingId: UUID,
        nyOppfolging: OpprettAktivitetspliktOppfolging,
        navIdent: String,
    ): AktivitetspliktOppfolging {
        inTransaction {
            aktivitetspliktDao.lagre(behandlingId, nyOppfolging, navIdent)
        }

        return hentAktivitetspliktOppfolging(behandlingId)!!
    }

    fun hentAktiviteter(behandlingId: UUID) =
        inTransaction {
            aktivitetspliktDao.hentAktiviteter(behandlingId)
        }

    fun opprettAktivitet(
        behandlingId: UUID,
        aktivitet: OpprettAktivitetspliktAktivitet,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling =
            requireNotNull(behandlingService.hentBehandling(behandlingId)) { "Fant ikke behandling $behandlingId" }

        if (!behandling.status.kanEndres()) {
            throw BehandlingKanIkkeEndres()
        }

        if (aktivitet.sakId != behandling.sak.id) {
            throw SakidTilhoererIkkeBehandlingException()
        }

        if (aktivitet.tom != null && aktivitet.tom < aktivitet.fom) {
            throw TomErFoerFomException()
        }

        val kilde = Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
        inTransaction {
            aktivitetspliktDao.opprettAktivitet(behandlingId, aktivitet, kilde)
        }
    }
}

class SakidTilhoererIkkeBehandlingException :
    UgyldigForespoerselException(
        code = "SAK_ID_TILHOERER_IKKE_BEHANDLING",
        detail = "Sak id stemmer ikke over ens med behandling",
    )

class TomErFoerFomException :
    UgyldigForespoerselException(
        code = "TOM_ER_FOER_FOM",
        detail = "Til og med dato er kan ikke være før fra og med dato",
    )
