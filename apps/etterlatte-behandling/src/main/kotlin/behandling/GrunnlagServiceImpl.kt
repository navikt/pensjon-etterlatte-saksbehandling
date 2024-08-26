package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.domain.Revurdering
import no.nav.etterlatte.grunnlagsendring.klienter.GrunnlagKlientImpl
import no.nav.etterlatte.libs.common.behandling.PersonMedSakerOgRoller
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.NyeSaksopplysninger
import no.nav.etterlatte.libs.common.grunnlag.OppdaterGrunnlagRequest
import no.nav.etterlatte.libs.common.grunnlag.Opplysningsbehov
import no.nav.etterlatte.libs.common.sak.Sak
import java.util.UUID

interface GrunnlagService {
    suspend fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
    )

    suspend fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
    )

    suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        sakType: SakType,
    )

    suspend fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
    )

    suspend fun leggTilNyeOpplysningerBareSak(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        opplysninger: NyeSaksopplysninger,
    )

    suspend fun laasTilGrunnlagIBehandling(
        revurdering: Revurdering,
        forrigeBehandling: UUID,
    )

    suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri

    suspend fun hentAlleSakerForPerson(fnr: String): PersonMedSakerOgRoller
}

class GrunnlagServiceImpl(
    private val grunnlagKlient: GrunnlagKlientImpl,
) : GrunnlagService {
    override suspend fun leggInnNyttGrunnlagSak(
        sak: Sak,
        persongalleri: Persongalleri,
    ) {
        val grunnlagsbehov = grunnlagsbehovSak(sak, persongalleri)
        grunnlagKlient.leggInnNyttGrunnlagSak(sak.id, grunnlagsbehov)
    }

    override suspend fun leggInnNyttGrunnlag(
        behandling: Behandling,
        persongalleri: Persongalleri,
    ) {
        val grunnlagsbehov = grunnlagsbehov(behandling, persongalleri)
        grunnlagKlient.leggInnNyttGrunnlag(behandling.id, grunnlagsbehov)
    }

    override suspend fun oppdaterGrunnlag(
        behandlingId: UUID,
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        sakType: SakType,
    ) {
        grunnlagKlient.oppdaterGrunnlag(
            behandlingId,
            OppdaterGrunnlagRequest(sakId, sakType),
        )
    }

    override suspend fun leggTilNyeOpplysninger(
        behandlingId: UUID,
        opplysninger: NyeSaksopplysninger,
    ) = grunnlagKlient.lagreNyeSaksopplysninger(behandlingId, opplysninger)

    override suspend fun leggTilNyeOpplysningerBareSak(
        sakId: no.nav.etterlatte.libs.common.sak.SakId,
        opplysninger: NyeSaksopplysninger,
    ) = grunnlagKlient.lagreNyeSaksopplysningerBareSak(sakId, opplysninger)

    override suspend fun hentPersongalleri(behandlingId: UUID): Persongalleri =
        grunnlagKlient
            .hentPersongalleri(behandlingId)
            ?.opplysning
            ?: throw NoSuchElementException("Persongalleri mangler for behandling id=$behandlingId")

    override suspend fun laasTilGrunnlagIBehandling(
        revurdering: Revurdering,
        forrigeBehandling: UUID,
    ) = grunnlagKlient.laasTilGrunnlagIBehandling(revurdering.id, forrigeBehandling)

    override suspend fun hentAlleSakerForPerson(fnr: String) = grunnlagKlient.hentPersonSakOgRolle(fnr)

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
