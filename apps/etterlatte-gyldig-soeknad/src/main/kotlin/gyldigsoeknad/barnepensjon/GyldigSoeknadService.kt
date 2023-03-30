package no.nav.etterlatte.gyldigsoeknad.barnepensjon

import no.nav.etterlatte.gyldigsoeknad.client.PdlClient
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsTyper
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurderingsResultat
import no.nav.etterlatte.libs.common.gyldigSoeknad.VurdertGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.IngenAnnenVergeEnnForelderGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.InnsenderHarForeldreansvarGrunnlag
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag.PersonInfoGyldighet
import no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlagTyper.InnsenderErForelderGrunnlag
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import org.slf4j.LoggerFactory

class OpplysningKanIkkeHentesUt : IllegalStateException()

class GyldigSoeknadService(private val pdlClient: PdlClient) {
    private val logger = LoggerFactory.getLogger(GyldigSoeknadService::class.java)

    fun hentPersongalleriFraSoeknad(soeknad: Barnepensjon): Persongalleri {
        logger.info("Hent persongalleri fra søknad")

        return Persongalleri(
            soeker = soeknad.soeker.folkeregisteridentifikator.svar.value,
            innsender = soeknad.innsender.folkeregisteridentifikator.svar.value,
            soesken = soeknad.soesken.map { it.folkeregisteridentifikator.svar.value },
            avdoed = soeknad.foreldre.filter {
                it.type == PersonType.AVDOED
            }.map { it.folkeregisteridentifikator.svar.value },
            gjenlevende = soeknad.foreldre.filter { it.type == PersonType.GJENLEVENDE_FORELDER }
                .map { it.folkeregisteridentifikator.svar.value }
        )
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
            soekerPdl
        )

        val vurderingsliste = listOf(
            innsenderErForelder,
            innsenderHarForeldreansvar,
            ingenAnnenVergeEnnForelder
        )

        val gyldighetResultat = setVurdering(vurderingsliste)
        val vurdertDato = Tidspunkt.now().toLocalDatetimeUTC()

        return GyldighetsResultat(gyldighetResultat, vurderingsliste, vurdertDato)
    }

    private fun hentSoekerFraPdl(fnrSoeker: String): Person? {
        return pdlClient.hentPerson(fnrSoeker, PersonRolle.BARN)
    }

    private fun hentNavnFraPdl(fnr: String): PersonInfoGyldighet? {
        val person = pdlClient.hentPerson(fnr, PersonRolle.GJENLEVENDE)
        val navn = person.let { it.fornavn + " " + it.etternavn }
        return PersonInfoGyldighet(navn, fnr)
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

    private fun hentFnrForeldre(familieRelasjon: FamilieRelasjon): List<String> {
        return familieRelasjon.foreldre?.map { it.value }
            ?: throw OpplysningKanIkkeHentesUt()
    }

    private fun hentFnrForeldreAnsvar(familieRelasjon: FamilieRelasjon): List<String> {
        return familieRelasjon.ansvarligeForeldre?.map { it.value }
            ?: throw OpplysningKanIkkeHentesUt()
    }

    private fun vurderOpplysning(vurdering: () -> Boolean): VurderingsResultat = try {
        if (vurdering()) VurderingsResultat.OPPFYLT else VurderingsResultat.IKKE_OPPFYLT
    } catch (ex: OpplysningKanIkkeHentesUt) {
        VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
    }

    private fun setVurdering(liste: List<VurdertGyldighet>): VurderingsResultat {
        val resultat = liste.map { it.resultat }
        return hentVurdering(resultat)
    }

    private fun hentVurdering(resultat: List<VurderingsResultat>): VurderingsResultat {
        return if (resultat.all { it == VurderingsResultat.OPPFYLT }) {
            VurderingsResultat.OPPFYLT
        } else if (resultat.any { it == VurderingsResultat.IKKE_OPPFYLT }) {
            VurderingsResultat.IKKE_OPPFYLT
        } else {
            VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
        }
    }
}