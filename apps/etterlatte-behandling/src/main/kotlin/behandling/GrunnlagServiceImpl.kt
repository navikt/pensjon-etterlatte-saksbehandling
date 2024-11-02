package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlag.PersonopplysningerResponse
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.util.UUID

interface GrunnlagService {
    suspend fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun leggTilNyeOpplysningerBareSak(
        sakId: SakId,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun laasTilGrunnlagIBehandling(
        revurdering: Revurdering,
        forrigeBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo? = null,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri

    suspend fun hentAlleSakerForPerson(fnr: String): PersonMedSakerOgRoller

    suspend fun hentPersonopplysninger(
        behandlingId: UUID,
        sakType: SakType,
    ): PersonopplysningerResponse
}

class GrunnlagServiceImpl(
    private val grunnlagKlient: GrunnlagKlient,
) : GrunnlagService {
    override suspend fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        val grunnlagsbehov = grunnlagsbehovSak(sak, persongalleri)
        grunnlagKlient.leggInnNyttGrunnlagSak(sak.id, grunnlagsbehov, brukerTokenInfo)
    }

    override suspend fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        val grunnlagsbehov = grunnlagsbehov(behandling, persongalleri)
        grunnlagKlient.leggInnNyttGrunnlag(behandling.id, grunnlagsbehov, brukerTokenInfo)
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo?,
    ) {
        grunnlagKlient.oppdaterGrunnlag(
            behandlingId,
            OppdaterGrunnlagRequest(sakId, sakType),
            brukerTokenInfo,
        )
    }

    override suspend fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo?,
    ) = grunnlagKlient.lagreNyeSaksopplysninger(behandlingId, opplysninger, brukerTokenInfo)

    override suspend fun leggTilNyeOpplysningerBareSak(
        sakId: SakId,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo?,
    ) = grunnlagKlient.lagreNyeSaksopplysningerBareSak(sakId, opplysninger, brukerTokenInfo)

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri =
        grunnlagKlient
            .hentPersongalleri(behandlingId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for behandling id=$behandlingId")

    override suspend fun laasTilGrunnlagIBehandling(
        revurdering: Revurdering,
        forrigeBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo?,
    ) = grunnlagKlient.laasTilGrunnlagIBehandling(revurdering.id, forrigeBehandling)

    override suspend fun hentAlleSakerForPerson(fnr: String) = grunnlagKlient.hentPersonSakOgRolle(fnr)

    override suspend fun hentPersonopplysninger(
        behandlingId: UUID,
        sakType: SakType,
    ): PersonopplysningerResponse {
        TODO("Not yet implemented")
    }

    private fun grunnlagsbehovSak(
        sak: Sak,
        persongalleri: Persongalleri,
    ): Opplysningsbehov =
        Opplysningsbehov(
            sakId = sak.id,
            sakType = sak.sakType,
            persongalleri = persongalleri,
        )

    private fun grunnlagsbehov(
        behandling: Behandling,
        persongalleri: Persongalleri,
    ): Opplysningsbehov =
        Opplysningsbehov(
            sakId = behandling.sak.id,
            sakType = behandling.sak.sakType,
            persongalleri = persongalleri,
        )
}
