package no.nav.etterlatte.behandling.foerstegangsbehandling

import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingDao
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.filterBehandlingerForEnheter
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.norskKlokke
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.*

interface FoerstegangsbehandlingService {

    fun lagreGyldighetsproeving(
        behandlingId: UUID,
        navIdent: String,
        svar: JaNeiMedBegrunnelse
    ): GyldighetsResultat?

    fun lagreGyldighetsproeving(behandlingId: UUID, gyldighetsproeving: GyldighetsResultat)
}

class FoerstegangsbehandlingServiceImpl(
    private val behandlingDao: BehandlingDao,
    private val featureToggleService: FeatureToggleService,
    private val klokke: Clock = utcKlokke()
) : FoerstegangsbehandlingService {
    private val logger = LoggerFactory.getLogger(FoerstegangsbehandlingServiceImpl::class.java)

    internal fun hentBehandling(id: UUID): Foerstegangsbehandling? =
        (behandlingDao.hentBehandling(id) as? Foerstegangsbehandling)?.sjekkEnhet()

    override fun lagreGyldighetsproeving(
        behandlingId: UUID,
        navIdent: String,
        svar: JaNeiMedBegrunnelse
    ): GyldighetsResultat? {
        return inTransaction {
            hentBehandling(behandlingId)?.let { behandling ->
                val resultat =
                    if (svar.erJa()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
                val gyldighetsResultat = GyldighetsResultat(
                    resultat = resultat,
                    vurderinger = listOf(
                        VurdertGyldighet(
                            navn = GyldighetsTyper.INNSENDER_ER_GJENLEVENDE,
                            resultat = resultat,
                            basertPaaOpplysninger = ManuellVurdering(
                                begrunnelse = svar.begrunnelse,
                                kilde = Grunnlagsopplysning.Saksbehandler(
                                    navIdent,
                                    Tidspunkt.from(klokke.norskKlokke())
                                )
                            )
                        )
                    ),
                    vurdertDato = Tidspunkt(klokke.instant()).toLocalDatetimeNorskTid()
                )

                behandling.lagreGyldighetsproeving(gyldighetsResultat)

                gyldighetsResultat
            }
        }
    }

    override fun lagreGyldighetsproeving(behandlingId: UUID, gyldighetsproeving: GyldighetsResultat) {
        inTransaction {
            hentBehandling(behandlingId)?.lagreGyldighetsproeving(gyldighetsproeving)
        }
    }

    private fun Foerstegangsbehandling.lagreGyldighetsproeving(gyldighetsproeving: GyldighetsResultat) {
        this.oppdaterGyldighetsproeving(gyldighetsproeving)
            .also {
                behandlingDao.lagreGyldighetsproving(it)
                logger.info("behandling ${it.id} i sak: ${it.sak.id} er gyldighetsprøvd. Saktype: ${it.sak.sakType}")
            }
    }

    private fun Foerstegangsbehandling?.sjekkEnhet() = this?.let { behandling ->
        listOf(behandling).filterBehandlingerForEnheter(
            featureToggleService,
            Kontekst.get().AppUser
        ).firstOrNull()
    }
}