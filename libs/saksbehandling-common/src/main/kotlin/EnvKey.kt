package no.nav.etterlatte

import no.nav.etterlatte.libs.common.EnvEnum

enum class EnvKey : EnvEnum {
    BEHANDLING_AZURE_SCOPE,
    BEHANDLING_URL,
    BEREGNING_AZURE_SCOPE,
    DOKARKIV_URL,
    JOBB_METRIKKER_OPENING_HOURS,
    NORG2_URL,
    NAVANSATT_URL,
    PDFGEN_URL,
    SKJERMING_URL,
    TRYGDETID_AZURE_SCOPE,
    VEDTAK_AZURE_SCOPE,
    ETTERLATTE_KLAGE_API_URL,
    ETTERLATTE_TILBAKEKREVING_URL,
    ETTERLATTE_MIGRERING_URL,
    ;

    override fun key() = name
}
