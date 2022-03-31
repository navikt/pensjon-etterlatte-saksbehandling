package model

import OpplysningKanIkkeHentesUt
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import hentBostedsAdresser
import hentFnrAnsvarligeForeldre
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.GjenlevendeForelderSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
import vurderOpplysning
import java.time.LocalDateTime

class GyldigSoeknadService {
    private val logger = LoggerFactory.getLogger(GyldigSoeknadService::class.java)

    fun mapOpplysninger(opplysninger: List<VilkaarOpplysning<ObjectNode>>) {
        logger.info("Map opplysninger for å vurdere om søknad er gyldig")

        val innsender = finnOpplysning<InnsenderSoeknad>(opplysninger, Opplysningstyper.INNSENDER_SOEKNAD_V1)
        val gjenlevendePdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1)
        val gjenlevendeSoeknad =
            finnOpplysning<GjenlevendeForelderSoeknad>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_SOEKNAD_V1)
        val soekerSoeknad = finnOpplysning<SoekerBarnSoeknad>(opplysninger, Opplysningstyper.SOEKER_SOEKNAD_V1)
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)

        val gyldighetResultat = listOf(
            innsenderErForelder(GyldighetsTyper.INNSENDER_ER_FORELDER, gjenlevendeSoeknad, innsender),
            gjenlevendeForelderHarForeldreansvar(
                GyldighetsTyper.HAR_FORELDREANSVAR_FOR_BARNET,
                gjenlevendePdl,
                soekerPdl
            ),
            barnOgForelderSammeBostedsadressePdl(
                GyldighetsTyper.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL,
                soekerPdl,
                gjenlevendePdl
            )
        )
    }

    fun innsenderErForelder(
        gyldighetstype: GyldighetsTyper,
        gjenlevende: VilkaarOpplysning<GjenlevendeForelderSoeknad>?,
        innsender: VilkaarOpplysning<InnsenderSoeknad>?
    ): VurdertGyldighet {
        val resultat = if (gjenlevende == null || innsender == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning { innsender.opplysning.foedselsnummer == gjenlevende.opplysning.foedselsnummer }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            LocalDateTime.now()
        )
    }

    fun gjenlevendeForelderHarForeldreansvar(
        gyldighetstype: GyldighetsTyper,
        gjenlevendePdl: VilkaarOpplysning<Person>?,
        soekerPdl: VilkaarOpplysning<Person>?
    ): VurdertGyldighet {
        val resultat = if (gjenlevendePdl == null || soekerPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning { hentFnrAnsvarligeForeldre(soekerPdl).contains(gjenlevendePdl.opplysning.foedselsnummer) }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            LocalDateTime.now()
        )
    }


    fun barnOgForelderSammeBostedsadressePdl(
        gyldighetstype: GyldighetsTyper,
        soekerPdl: VilkaarOpplysning<Person>?,
        gjenlevendePdl: VilkaarOpplysning<Person>?
    ): VurdertGyldighet {

        val resultat = try {
            if (gjenlevendePdl == null || soekerPdl == null) {
                VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
            } else {
                val adresseBarn = hentBostedsAdresser(soekerPdl).find { it.aktiv }
                val adresseGjenlevende = hentBostedsAdresser(gjenlevendePdl).find { it.aktiv }

                val adresse1 = adresseBarn?.adresseLinje1 == adresseGjenlevende?.adresseLinje1
                val postnr = adresseBarn?.postnr == adresseGjenlevende?.postnr
                val poststed = adresseBarn?.poststed == adresseGjenlevende?.poststed
                vurderOpplysning { adresse1 && postnr && poststed }
            }
        } catch (ex: OpplysningKanIkkeHentesUt) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
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
                    opplysning.opplysningsType,
                    opplysning.kilde,
                    objectMapper.readValue(opplysning.opplysning.toString())
                )
            }
        }

        inline fun <reified T> finnOpplysning(
            opplysninger: List<VilkaarOpplysning<ObjectNode>>,
            type: Opplysningstyper
        ): VilkaarOpplysning<T>? {
            return setOpplysningType(opplysninger.find { it.opplysningsType == type })
        }
    }

}