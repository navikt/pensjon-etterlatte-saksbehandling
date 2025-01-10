package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.behandling.klienter.GrunnlagKlient
import no.nav.etterlatte.grunnlag.PersonopplysningerResponse
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import java.time.YearMonth
import java.util.UUID

interface GrunnlagService {
    suspend fun grunnlagFinnes(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean

    suspend fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun leggTilNyeOpplysningerBareSak(
        sakId: SakId,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun laasTilGrunnlagIBehandling(
        revurdering: Revurdering,
        forrigeBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    )

    suspend fun hentPersongalleri(sakId: SakId): Persongalleri

    suspend fun hentAlleSakerForPerson(fnr: String): PersonMedSakerOgRoller

    suspend fun hentPersonopplysninger(
        behandlingId: UUID,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): PersonopplysningerResponse

    suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): YearMonth
}

class GrunnlagServiceImpl(
    private val grunnlagKlient: GrunnlagKlient,
) : GrunnlagService {
    override suspend fun grunnlagFinnes(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): Boolean = grunnlagKlient.grunnlagFinnes(sakId, brukerTokenInfo)

    override suspend fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val grunnlagsbehov = grunnlagsbehov(sak, persongalleri, brukerTokenInfo)
        grunnlagKlient.leggInnNyttGrunnlagSak(sak.id, grunnlagsbehov, brukerTokenInfo)
    }

    override suspend fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val grunnlagsbehov = grunnlagsbehov(behandling.sak, persongalleri, brukerTokenInfo)
        grunnlagKlient.leggInnNyttGrunnlag(behandling.id, grunnlagsbehov, brukerTokenInfo)
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
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
        brukerTokenInfo: BrukerTokenInfo,
    ) = grunnlagKlient.lagreNyeSaksopplysninger(behandlingId, opplysninger, brukerTokenInfo)

    override suspend fun leggTilNyeOpplysningerBareSak(
        sakId: SakId,
        opplysninger: NyeSaksopplysninger,
        brukerTokenInfo: BrukerTokenInfo,
    ) = grunnlagKlient.lagreNyeSaksopplysningerBareSak(sakId, opplysninger, brukerTokenInfo)

    override suspend fun hentPersongalleri(sakId: SakId): Persongalleri = grunnlagKlient.hentPersongalleri(sakId)

    override suspend fun laasTilGrunnlagIBehandling(
        revurdering: Revurdering,
        forrigeBehandling: UUID,
        brukerTokenInfo: BrukerTokenInfo,
    ) = grunnlagKlient.laasTilGrunnlagIBehandling(revurdering.id, forrigeBehandling)

    override suspend fun hentAlleSakerForPerson(fnr: String) = grunnlagKlient.hentPersonSakOgRolle(fnr)

    override suspend fun hentPersonopplysninger(
        behandlingId: UUID,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): PersonopplysningerResponse = grunnlagKlient.hentPersonopplysningerForBehandling(behandlingId, brukerTokenInfo, sakType)

    override suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ) = grunnlagKlient.aldersovergangMaaned(sakId, sakType, brukerTokenInfo)

    private fun grunnlagsbehov(
        sak: Sak,
        persongalleri: Persongalleri,
        brukerTokenInfo: BrukerTokenInfo,
    ): Opplysningsbehov =
        Opplysningsbehov(
            sakId = sak.id,
            sakType = sak.sakType,
            persongalleri = persongalleri,
            kilde =
                when (brukerTokenInfo) {
                    is Saksbehandler -> Grunnlagsopplysning.Saksbehandler(brukerTokenInfo.ident(), Tidspunkt.now())
                    else -> Grunnlagsopplysning.Gjenny(brukerTokenInfo.ident(), Tidspunkt.now())
                },
        )
}
