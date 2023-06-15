package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning

enum class BarnepensjonSoeskenjusteringGrunn {
    NYTT_SOESKEN,
    SOESKEN_DOER,
    SOESKEN_INN_INSTITUSJON_INGEN_ENDRING,
    SOESKEN_INN_INSTITUSJON_ENDRING,
    SOESKEN_UT_INSTITUSJON,
    FORPLEID_ETTER_BARNEVERNSLOVEN,
    SOESKEN_BLIR_ADOPTERT
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class RevurderingInfo {
    abstract val kilde: Grunnlagsopplysning.Kilde

    @JsonTypeName("SOESKENJUSTERING")
    data class Soeskenjustering(
        val grunnForSoeskenjustering: BarnepensjonSoeskenjusteringGrunn,
        override val kilde: Grunnlagsopplysning.Saksbehandler
    ) : RevurderingInfo()
}