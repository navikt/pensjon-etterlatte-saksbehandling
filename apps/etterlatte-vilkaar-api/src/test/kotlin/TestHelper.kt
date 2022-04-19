package no.nav.etterlatte

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import no.nav.etterlatte.model.VurdertVilkaar
import java.time.LocalDateTime
import java.util.*

fun vilkaarResultatForBehandling(behandlingId: String) = VurdertVilkaar(
    behandling = UUID.fromString(behandlingId),
    avdoedSoeknad = null,
    soekerSoeknad = null,
    soekerPdl = null,
    avdoedPdl = null,
    gjenlevendePdl = null,
    versjon = 1,
    objectMapper.valueToTree(
        VilkaarResultat(
            VurderingsResultat.OPPFYLT,
            emptyList(),
            LocalDateTime.now()
        )
    )
)