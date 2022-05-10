package model

import Pdl
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import hentFnrForeldre
import hentFnrForeldreAnsvar
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
import setVurdering
import vurderOpplysning
import java.time.LocalDateTime

class GyldigSoeknadService {
    private val logger = LoggerFactory.getLogger(GyldigSoeknadService::class.java)

    fun hentPersongalleriFraSoeknad(jsonNode: JsonNode): Persongalleri {
        logger.info("Hent persongalleri fra søknad")

        val barnepensjon = objectMapper.treeToValue<Barnepensjon>(jsonNode)!!

        return Persongalleri(
            soeker = barnepensjon.soeker.foedselsnummer.svar.value,
            innsender = barnepensjon.innsender.foedselsnummer.svar.value,
            soesken = barnepensjon.soesken.map { it.foedselsnummer.svar.value },
            avdoed = barnepensjon.foreldre.filter { it.type == PersonType.AVDOED }.map { it.foedselsnummer.svar.value },
            gjenlevende = barnepensjon.foreldre.filter { it.type == PersonType.GJENLEVENDE_FORELDER }
                .map { it.foedselsnummer.svar.value }
        )
    }

    fun hentSoekerFraPdl(fnrSoeker: String, pdl: Pdl): FamilieRelasjon? {
        val soeker = pdl.hentPdlModell(fnrSoeker, PersonRolle.BARN)
        return soeker.familieRelasjon
    }

    fun vurderGyldighet(persongalleri: Persongalleri, familieRelasjonSoeker: FamilieRelasjon?): GyldighetsResultat {
        val gyldighet = listOf(
            innsenderErForelder(
                GyldighetsTyper.INNSENDER_ER_FORELDER,
                persongalleri.gjenlevende,
                persongalleri.innsender,
                familieRelasjonSoeker
            ),
            innsenderHarForeldreansvar(
                GyldighetsTyper.HAR_FORELDREANSVAR_FOR_BARNET,
                persongalleri.innsender,
                familieRelasjonSoeker
            )
        )

        val gyldighetResultat = setVurdering(gyldighet)
        val vurdertDato = LocalDateTime.now()

        return GyldighetsResultat(gyldighetResultat, gyldighet, vurdertDato)
    }


    fun innsenderErForelder(
        gyldighetstype: GyldighetsTyper,
        gjenlevende: List<String>?,
        innsender: String?,
        soekerFamilieRelasjonPdl: FamilieRelasjon?
    ): VurdertGyldighet {
        val resultat = if (gjenlevende == null || innsender == null || soekerFamilieRelasjonPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning {
                gjenlevende.contains(innsender) &&
                        hentFnrForeldre(soekerFamilieRelasjonPdl).contains(innsender)
            }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            LocalDateTime.now()
        )
    }

    fun innsenderHarForeldreansvar(
        gyldighetstype: GyldighetsTyper,
        innsender: String?,
        soekerPdlFamilieRelasjon: FamilieRelasjon?
    ): VurdertGyldighet {
        val resultat = if (innsender == null || soekerPdlFamilieRelasjon == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning { hentFnrForeldreAnsvar(soekerPdlFamilieRelasjon).contains(innsender) }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            LocalDateTime.now()
        )
    }

}