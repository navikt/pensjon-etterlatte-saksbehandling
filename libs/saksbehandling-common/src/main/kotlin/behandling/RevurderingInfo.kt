package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.LocalDate

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

    @JsonTypeName("SOESKENJUSTERING")
    data class Soeskenjustering(
        val grunnForSoeskenjustering: BarnepensjonSoeskenjusteringGrunn
    ) : RevurderingInfo()

    @JsonTypeName("ADOPSJON")
    data class Adopsjon(
        val adoptertAv1: Navn,
        val adoptertAv2: Navn? = null
    ) : RevurderingInfo()

    @JsonTypeName("OMGJOERING_AV_FARSKAP")
    data class OmgjoeringAvFarskap(
        val naavaerendeFar: Navn,
        val forrigeFar: Navn
    ) : RevurderingInfo()

    @JsonTypeName("FENGSELSOPPHOLD")
    data class Fengselsopphold(
        val fraDato: LocalDate,
        val tilDato: LocalDate
    ) : RevurderingInfo()

    @JsonTypeName("UT_AV_FENGSEL")
    data class UtAvFengsel(
        val erEtterbetalingMerEnnTreeMaaneder: Boolean
    ) : RevurderingInfo()
}