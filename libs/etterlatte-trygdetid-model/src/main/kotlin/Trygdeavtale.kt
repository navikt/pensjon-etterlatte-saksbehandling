package no.nav.etterlatte.libs.common.trygdetid.avtale

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.util.UUID
import java.util.UUID.randomUUID

data class Trygdeavtale(
    val id: UUID = randomUUID(),
    val behandlingId: UUID,
    val avtaleKode: String,
    val avtaleDatoKode: String?,
    val avtaleKriteriaKode: String?,
    val kilde: Grunnlagsopplysning.Saksbehandler,
)
