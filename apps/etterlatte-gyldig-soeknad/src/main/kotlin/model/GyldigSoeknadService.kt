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
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.IngenAnnenVergeEnnForelderGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderHarForeldreansvarGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper.InnsenderErForelderGrunnlag
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.vikaar.VurderingsResultat
import org.slf4j.LoggerFactory
import setVurdering
import vurderOpplysning
import java.time.LocalDateTime

class GyldigSoeknadService(private val pdl: Pdl) {
    private val logger = LoggerFactory.getLogger(GyldigSoeknadService::class.java)

    fun hentPersongalleriFraSoeknad(jsonNode: JsonNode): Persongalleri {
        logger.info("Hent persongalleri fra søknad")

        val barnepensjon = objectMapper.treeToValue<Barnepensjon>(jsonNode)

        return Persongalleri(
            soeker = barnepensjon.soeker.foedselsnummer.svar.value,
            innsender = barnepensjon.innsender.foedselsnummer.svar.value,
            soesken = barnepensjon.soesken.map { it.foedselsnummer.svar.value },
            avdoed = barnepensjon.foreldre.filter { it.type == PersonType.AVDOED }.map { it.foedselsnummer.svar.value },
            gjenlevende = barnepensjon.foreldre.filter { it.type == PersonType.GJENLEVENDE_FORELDER }
                .map { it.foedselsnummer.svar.value }
        )
    }

    fun hentSoekerFraPdl(fnrSoeker: String): Person? {
        return pdl.hentPdlModell(fnrSoeker, PersonRolle.BARN)
    }

    fun hentNavnFraPdl(fnr: String): PersonInfoGyldighet? {
        val person = pdl.hentPdlModell(fnr, PersonRolle.GJENLEVENDE)
        val navn = person.let { it.fornavn + " " + it.etternavn }
        return PersonInfoGyldighet(navn, fnr)
    }

    fun vurderGyldighet(persongalleri: Persongalleri): GyldighetsResultat {
        val soekerPdl = hentSoekerFraPdl(persongalleri.soeker)
        val familieRelasjonSoeker = soekerPdl?.familieRelasjon
        val personinfoInnsender = persongalleri.innsender?.let { hentNavnFraPdl(it) }
        val personinfoGjenlevende = persongalleri.gjenlevende.map { hentNavnFraPdl(it) }


        val innsenderErForelder = innsenderErForelder(
            GyldighetsTyper.INNSENDER_ER_FORELDER,
            personinfoGjenlevende,
            personinfoInnsender,
            familieRelasjonSoeker
        )

        val innsenderHarForeldreansvar = innsenderHarForeldreansvar(
            GyldighetsTyper.HAR_FORELDREANSVAR_FOR_BARNET,
            personinfoInnsender,
            familieRelasjonSoeker
        )

        val ingenAnnenVergeEnnForelder = ingenAnnenVergeEnnForelder(
            GyldighetsTyper.INGEN_ANNEN_VERGE_ENN_FORELDER,
            soekerPdl,
        )

        val vurderingsliste = listOf(
            innsenderErForelder,
            innsenderHarForeldreansvar,
            ingenAnnenVergeEnnForelder
        )

        val gyldighetResultat = setVurdering(vurderingsliste)
        val vurdertDato = LocalDateTime.now()

        return GyldighetsResultat(gyldighetResultat, vurderingsliste, vurdertDato)
    }


    fun innsenderErForelder(
        gyldighetstype: GyldighetsTyper,
        gjenlevende: List<PersonInfoGyldighet?>,
        innsender: PersonInfoGyldighet?,
        soekerFamilieRelasjonPdl: FamilieRelasjon?
    ): VurdertGyldighet {
        val resultat = if (gjenlevende.isEmpty() || innsender == null || soekerFamilieRelasjonPdl == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning {
                gjenlevende.map { it?.fnr }.contains(innsender.fnr) &&
                        hentFnrForeldre(soekerFamilieRelasjonPdl).contains(innsender.fnr)
            }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            InnsenderErForelderGrunnlag(soekerFamilieRelasjonPdl, innsender, gjenlevende)
        )
    }

    fun innsenderHarForeldreansvar(
        gyldighetstype: GyldighetsTyper,
        innsender: PersonInfoGyldighet?,
        soekerPdlFamilieRelasjon: FamilieRelasjon?
    ): VurdertGyldighet {
        val resultat = if (innsender == null || soekerPdlFamilieRelasjon == null) {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        } else {
            vurderOpplysning { hentFnrForeldreAnsvar(soekerPdlFamilieRelasjon).contains(innsender.fnr) }
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            InnsenderHarForeldreansvarGrunnlag(soekerPdlFamilieRelasjon, innsender)
        )
    }

    fun ingenAnnenVergeEnnForelder(gyldighetstype: GyldighetsTyper, soekerPdl: Person?): VurdertGyldighet {
        fun harVergemaalPdl(barn: Person?): Boolean {
            return if (barn?.vergemaalEllerFremtidsfullmakt != null) {
                barn.vergemaalEllerFremtidsfullmakt!!.isNotEmpty()
            } else {
                false
            }
        }

        val soekerHarVergemaal = harVergemaalPdl(soekerPdl)

        val resultat = if (soekerHarVergemaal == true) {
            VurderingsResultat.IKKE_OPPFYLT
        } else {
            VurderingsResultat.OPPFYLT
        }

        return VurdertGyldighet(
            gyldighetstype,
            resultat,
            IngenAnnenVergeEnnForelderGrunnlag(soekerPdl?.vergemaalEllerFremtidsfullmakt)
        )
    }

}