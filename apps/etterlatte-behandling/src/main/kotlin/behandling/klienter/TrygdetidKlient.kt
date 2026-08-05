package no.nav.etterlatte.behandling.klienter

import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.trygdetid.TrygdetidService
import no.nav.etterlatte.trygdetid.toDto
import java.util.UUID

interface TrygdetidKlient {
    suspend fun kopierTrygdetidFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<TrygdetidDto>
}

class TrygdetidKlientIntern(
    private val trygdetidService: TrygdetidService,
) : TrygdetidKlient {
    override suspend fun hentTrygdetid(
        behandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<TrygdetidDto> = trygdetidService.hentTrygdetiderIBehandling(behandlingId, brukerTokenInfo).map { it.toDto() }

    override suspend fun kopierTrygdetidFraForrigeBehandling(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        trygdetidService.kopierSisteTrygdetidberegninger(behandlingId, forrigeBehandlingId, brukerTokenInfo)
    }
}
