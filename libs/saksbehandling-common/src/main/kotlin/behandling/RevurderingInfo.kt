package no.nav.etterlatte.libs.common.behandling

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.etterlatte.behandling.utland.LandMedDokumenter
import java.time.LocalDate
import java.util.UUID

enum class BarnepensjonSoeskenjusteringGrunn {
    NYTT_SOESKEN,
    SOESKEN_DOER,
    SOESKEN_INN_INSTITUSJON_INGEN_ENDRING,
    SOESKEN_INN_INSTITUSJON_ENDRING,
    SOESKEN_UT_INSTITUSJON,
    FORPLEID_ETTER_BARNEVERNSLOVEN,
    SOESKEN_BLIR_ADOPTERT,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed class RevurderingInfo {
    @JsonTypeName("ANNEN")
    data class RevurderingAarsakAnnen(
        val aarsak: String,
    ) : RevurderingInfo()

    @JsonTypeName("ADOPSJON")
    data object Adopsjon : RevurderingInfo()

    @JsonTypeName("OMGJOERING_AV_FARSKAP")
    data object OmgjoeringAvFarskap : RevurderingInfo()

    @JsonTypeName("ANNEN_UTEN_BREV")
    data class RevurderingAarsakAnnenUtenBrev(
        val aarsak: String,
    ) : RevurderingInfo()

    @JsonTypeName("SOESKENJUSTERING")
    data class Soeskenjustering(
        val grunnForSoeskenjustering: BarnepensjonSoeskenjusteringGrunn,
    ) : RevurderingInfo()

    @JsonTypeName("FENGSELSOPPHOLD")
    data class Fengselsopphold(
        val fraDato: LocalDate,
        val tilDato: LocalDate,
    ) : RevurderingInfo()

    @JsonTypeName("UT_AV_FENGSEL")
    data class UtAvFengsel(
        val erEtterbetalingMerEnnTreeMaaneder: Boolean,
    ) : RevurderingInfo()

    @JsonTypeName("YRKESSKADE")
    data class Yrkesskade(
        val dinForelder: String,
        val yrkesskadeEllerYrkessykdom: String,
    ) : RevurderingInfo()

    @JsonTypeName("INSTITUSJONSOPPHOLD")
    data class Institusjonsopphold(
        val erEtterbetalingMerEnnTreMaaneder: Boolean,
        val prosent: Int?,
        val innlagtdato: LocalDate?,
        val utskrevetdato: LocalDate?,
    ) : RevurderingInfo()

    @JsonTypeName("OMGJOERING_ETTER_KLAGE")
    data class OmgjoeringEtterKlage(
        val klageId: UUID,
    ) : RevurderingInfo()

    @JsonTypeName("SLUTTBEHANDLING_UTLAND")
    data class SluttbehandlingUtland(
        val landMedDokumenter: List<LandMedDokumenter>,
    ) : RevurderingInfo()
}
