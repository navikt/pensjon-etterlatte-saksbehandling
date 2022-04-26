package model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import hentFnrForeldre
import hentFnrForeldreAnsvar
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.GjenlevendeForelderSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
import setVurdering
import vurderOpplysning
import java.time.LocalDateTime

class GyldigSoeknadService {
    private val logger = LoggerFactory.getLogger(GyldigSoeknadService::class.java)

    fun mapOpplysninger(opplysninger: List<VilkaarOpplysning<ObjectNode>>): GyldighetsResultat {
        logger.info("Map opplysninger for å vurdere om søknad er gyldig")

        val innsender = finnOpplysning<InnsenderSoeknad>(opplysninger, Opplysningstyper.INNSENDER_SOEKNAD_V1)
        val gjenlevendePdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1)
        val gjenlevendeSoeknad =
            finnOpplysning<GjenlevendeForelderSoeknad>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_SOEKNAD_V1)
        val soekerSoeknad = finnOpplysning<SoekerBarnSoeknad>(opplysninger, Opplysningstyper.SOEKER_SOEKNAD_V1)
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)

        val gyldighet = listOf(
            innsenderErForelder(GyldighetsTyper.INNSENDER_ER_FORELDER, gjenlevendeSoeknad, innsender, soekerPdl),
            gjenlevendeForelderHarForeldreansvar(
                GyldighetsTyper.HAR_FORELDREANSVAR_FOR_BARNET,
                gjenlevendeSoeknad,
                soekerPdl
            )
        )

        val gyldighetResultat = setVurdering(gyldighet)
        val vurdertDato = LocalDateTime.now()

        return GyldighetsResultat(gyldighetResultat, gyldighet, vurdertDato)
    }

    fun innsenderErForelder(
        gyldighetstype: GyldighetsTyper,
        gjenlevende: VilkaarOpplysning<GjenlevendeForelderSoeknad>?, //gjenlevende fnr søknad
        innsender: VilkaarOpplysning<InnsenderSoeknad>?, //innsender fnr søknad
        soekerPdl: VilkaarOpplysning<Person>? // familierelasjon foreldre til barnet
    ): VurdertGyldighet {

        val resultat = if (gjenlevende == null || innsender == null || soekerPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning {
                innsender.opplysning.foedselsnummer == gjenlevende.opplysning.foedselsnummer &&
                hentFnrForeldre(soekerPdl).contains(gjenlevende.opplysning.foedselsnummer)
            }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            LocalDateTime.now()
        )
    }

    fun gjenlevendeForelderHarForeldreansvar(
        gyldighetstype: GyldighetsTyper,
        gjenlevende: VilkaarOpplysning<GjenlevendeForelderSoeknad>?, //gjenlevende fnr søknad
        soekerPdl: VilkaarOpplysning<Person>?
    ): VurdertGyldighet {
        val resultat = if (gjenlevende == null || soekerPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning { hentFnrForeldreAnsvar(soekerPdl).contains(gjenlevende.opplysning.foedselsnummer) }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            LocalDateTime.now()
        )
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