package no.nav.etterlatte.sanksjon

import no.nav.etterlatte.klienter.BehandlingKlient
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.time.YearMonth
import java.util.UUID

class SanksjonService(
    private val behandlingKlient: BehandlingKlient,
    private val sanksjonRepository: SanksjonRepository,
) {
    private val logger = LoggerFactory.getLogger(SanksjonService::class.java)

    fun hentSanksjon(behandlingId: UUID): List<Sanksjon>? {
        logger.info("Henter sanksjoner med behandlingID=$behandlingId")
        return sanksjonRepository.hentSanksjon(behandlingId)?.sortedBy { it.fom }
    }

    suspend fun opprettEllerOppdaterSanksjon(
        behandlingId: UUID,
        sanksjon: LagreSanksjon,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val behandling = behandlingKlient.hentBehandling(behandlingId, brukerTokenInfo)

        if (behandling.virkningstidspunkt == null) ManglerVirkningstidspunktException()
        if (sanksjon.sakId != behandling.sak) throw SakidTilhoererIkkeBehandlingException()
        if (YearMonth.of(
                sanksjon.fom.year,
                sanksjon.fom.month,
            ).isBefore(behandling.virkningstidspunkt?.dato)
        ) {
            throw FomErFoerVirkningstidpunktException()
        }
        if (sanksjon.tom != null && sanksjon.tom.isBefore(sanksjon.fom)) throw TomErFoerFomException()

        if (sanksjon.id === null) {
            logger.info("Oppretter sanksjon med behandlingID=$behandlingId")
            return sanksjonRepository.opprettSanksjon(behandlingId, behandling.sak, brukerTokenInfo.ident(), sanksjon)
        }

        logger.info("Oppdaterer sanksjon med behandlingID=$behandlingId")
        return sanksjonRepository.oppdaterSanksjon(sanksjon, brukerTokenInfo.ident())
    }

    fun slettSanksjon(
        behandlingId: UUID,
        sanksjonId: UUID,
    ) {
        logger.info("Sletter sanksjon med behandlingID=$behandlingId og sanksjonID=$sanksjonId")
        sanksjonRepository.slettSanksjon(behandlingId, sanksjonId)
    }
}

class FomErFoerVirkningstidpunktException :
    UgyldigForespoerselException(
        code = "FOM_ER_FOER_VIRKNINGSTIDSPUNKT",
        detail = "Fra og med dato kan ikke være før virkningstidspunkt",
    )

class TomErFoerFomException :
    UgyldigForespoerselException(
        code = "TOM_ER_FOER_FOM",
        detail = "Til og med dato er kan ikke være før fra og med dato",
    )

class SakidTilhoererIkkeBehandlingException :
    UgyldigForespoerselException(
        code = "SAK_ID_TILHOERER_IKKE_BEHANDLING",
        detail = "Sak id stemmer ikke over ens med behandling",
    )

class ManglerVirkningstidspunktException :
    UgyldigForespoerselException(
        code = "MANGLER_VIRK",
        detail = "Behandling mangler virkningstidspunkt",
    )
