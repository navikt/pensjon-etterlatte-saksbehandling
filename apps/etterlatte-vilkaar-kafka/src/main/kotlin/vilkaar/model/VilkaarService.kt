package no.nav.etterlatte.model

import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.node.ObjectNode
import no.nav.etterlatte.barnepensjon.hentSisteVurderteDato
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraVilkaar
import no.nav.etterlatte.barnepensjon.vilkaarAvdoedesMedlemskap
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.libs.common.vikaar.Vilkaartyper
import no.nav.etterlatte.vilkaar.barnepensjon.*
import org.slf4j.LoggerFactory
import vilkaar.grunnlag.Vilkaarsgrunnlag


class VilkaarService {
    private val logger = LoggerFactory.getLogger(VilkaarService::class.java)

    fun mapVilkaar(opplysninger: List<VilkaarOpplysning<ObjectNode>>): VilkaarResultat {
        logger.info("Map vilkaar")
        return mapVilkaar(Vilkaarsgrunnlag(
            avdoedSoeknad = finnOpplysning(opplysninger, Opplysningstyper.AVDOED_SOEKNAD_V1),
            soekerSoeknad = finnOpplysning(opplysninger, Opplysningstyper.SOEKER_SOEKNAD_V1),
            soekerPdl = finnOpplysning(opplysninger, Opplysningstyper.SOEKER_PDL_V1),
            avdoedPdl = finnOpplysning(opplysninger, Opplysningstyper.AVDOED_PDL_V1),
            gjenlevendePdl = finnOpplysning(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1),
        ))

    }


    fun mapVilkaar(grunnlag: Vilkaarsgrunnlag): VilkaarResultat {

        val vilkaar = listOf(
            vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, grunnlag.soekerPdl, grunnlag.avdoedPdl),
            vilkaarDoedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT, grunnlag.avdoedPdl, grunnlag.soekerPdl),
            vilkaarAvdoedesMedlemskap(
                Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
                grunnlag.avdoedSoeknad,
                grunnlag.avdoedPdl
            ),
            vilkaarBarnetsMedlemskap(
                Vilkaartyper.BARNETS_MEDLEMSKAP,
                grunnlag.soekerPdl,
                grunnlag.soekerSoeknad,
                grunnlag.gjenlevendePdl,
                grunnlag.avdoedPdl,
            )
        )

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)

    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<ObjectNode>?): VilkaarOpplysning<T>? {
            return opplysning?.let {
                VilkaarOpplysning(
                    opplysning.opplysningType,
                    opplysning.kilde,
                    objectMapper.readValue(opplysning.opplysning.toString())
                )
            }
        }

        inline fun <reified T> finnOpplysning(
            opplysninger: List<VilkaarOpplysning<ObjectNode>>,
            type: Opplysningstyper
        ): VilkaarOpplysning<T>? {
            return setOpplysningType(opplysninger.find { it.opplysningType == type })
        }
    }
}

