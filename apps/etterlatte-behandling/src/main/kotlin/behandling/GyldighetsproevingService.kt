package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.JaNeiMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.ManuellVurdering
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeNorskTid
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import org.slf4j.LoggerFactory
import java.time.Clock
import java.util.UUID

interface GyldighetsproevingService {
    fun lagreGyldighetsproeving(
        behandlingId: UUID,
        svar: JaNeiMedBegrunnelse,
        kilde: Grunnlagsopplysning.Saksbehandler,
    ): GyldighetsResultat?

    fun lagreGyldighetsproeving(
        behandlingId: UUID,
        gyldighetsproeving: GyldighetsResultat,
    )
}

class GyldighetsproevingServiceImpl(
    private val behandlingDao: BehandlingDao,
    private val klokke: Clock = utcKlokke(),
) : GyldighetsproevingService {
    private val logger = LoggerFactory.getLogger(GyldighetsproevingServiceImpl::class.java)

    override fun lagreGyldighetsproeving(
        behandlingId: UUID,
        svar: JaNeiMedBegrunnelse,
        kilde: Grunnlagsopplysning.Saksbehandler,
    ): GyldighetsResultat? {
        val resultat = if (svar.erJa()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
        val gyldighetsResultat =
            GyldighetsResultat(
                resultat = resultat,
                vurderinger =
                    listOf(
                        VurdertGyldighet(
                            navn = GyldighetsTyper.MANUELL_VURDERING,
                            resultat = resultat,
                            basertPaaOpplysninger =
                                ManuellVurdering(
                                    begrunnelse = svar.begrunnelse,
                                    kilde = kilde,
                                ),
                        ),
                    ),
                vurdertDato = Tidspunkt(klokke.instant()).toLocalDatetimeNorskTid(),
            )

        val behandling = behandlingDao.hentBehandling(behandlingId) ?: return null

        when (behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> {
                behandling.lagreGyldighetsproeving(gyldighetsResultat)
            }

            BehandlingType.REVURDERING -> {
                val revurdering = behandling as Revurdering
                if (revurdering.revurderingsaarsak != Revurderingaarsak.NY_SOEKNAD) return null
                revurdering.lagreGyldighetsproeving(gyldighetsResultat)
            }
        }

        return gyldighetsResultat
    }

    override fun lagreGyldighetsproeving(
        behandlingId: UUID,
        gyldighetsproeving: GyldighetsResultat,
    ) {
        inTransaction {
            behandlingDao.hentBehandling(behandlingId)?.lagreGyldighetsproeving(gyldighetsproeving)
        }
    }

    private fun Behandling.lagreGyldighetsproeving(gyldighetsproeving: GyldighetsResultat) {
        val oppdatert = oppdaterGyldighetsproeving(gyldighetsproeving)
        behandlingDao.lagreGyldighetsproeving(oppdatert.id, gyldighetsproeving)
        behandlingDao.lagreStatus(oppdatert)
        logger.info("behandling ${oppdatert.id} i sak: ${oppdatert.sak.id} er gyldighetsprøvd. Saktype: ${oppdatert.sak.sakType}")
    }
}
