package no.nav.etterlatte

import no.nav.etterlatte.libs.common.EnvEnum

enum class EnvKey : EnvEnum {
    NORG2_URL,
    NAVANSATT_URL,
    SKJERMING_URL,
    ETTERLATTE_KLAGE_API_URL,
    ETTERLATTE_TILBAKEKREVING_URL,
    ETTERLATTE_MIGRERING_URL,
    ;

    override fun name() = name
}
