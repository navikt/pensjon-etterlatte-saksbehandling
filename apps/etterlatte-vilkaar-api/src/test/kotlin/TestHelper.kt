package no.nav.etterlatte

import no.nav.etterlatte.libs.common.vikaar.GrunnlagHendelseType
import no.nav.etterlatte.libs.common.vikaar.Grunnlagshendelse
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaarsgrunnlag
import no.nav.etterlatte.libs.common.vikaar.VilkarIBehandling
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import java.time.LocalDateTime
import java.util.*

fun vilkaarResultatForBehandling(behandlingId: String) = VilkarIBehandling(
    behandling = UUID.fromString(behandlingId),
    grunnlag = Vilkaarsgrunnlag(),
    versjon = 1,
    VilkaarResultat(
        VurderingsResultat.OPPFYLT,
        emptyList(),
        LocalDateTime.now()
    )
)

fun grunnlagsHendelsesListeForBehandling(behandlingId: String) = listOf(
    Grunnlagshendelse(
        behandling = UUID.fromString(behandlingId),
        opplysning = null,
        hendelsetype = GrunnlagHendelseType.BEHANDLING_OPPRETTET,
        hendelsenummer = 1,
        hendelsereferanse = null
    )
)