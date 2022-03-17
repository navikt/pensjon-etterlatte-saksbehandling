package no.nav.etterlatte.libs.common.vikaar

import no.nav.etterlatte.libs.common.behandling.Behandlingsopplysning

data class Kriteriegrunnlag<T>(
    val kilde: Behandlingsopplysning.Kilde,
    val opplysning: T
)