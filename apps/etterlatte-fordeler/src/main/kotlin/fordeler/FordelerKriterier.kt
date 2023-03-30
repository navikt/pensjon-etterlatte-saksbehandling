package no.nav.etterlatte.fordeler

import no.nav.etterlatte.libs.common.person.Adresse
import no.nav.etterlatte.libs.common.person.AdresseType
import no.nav.etterlatte.libs.common.person.Adressebeskyttelse
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.aktiv
import no.nav.etterlatte.libs.common.person.alder
import no.nav.etterlatte.libs.common.person.nyeste
import no.nav.etterlatte.libs.common.soeknad.dataklasser.Barnepensjon
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Avdoed
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.PersonType
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.time.LocalDate

private const val NORGE = "NOR"

data class FordelerKriterierResultat(
    val kandidat: Boolean,
    val forklaring: List<FordelerKriterie>
)

enum class FordelerKriterie(val forklaring: String) {
    BARN_ER_IKKE_NORSK_STATSBORGER("Barn er ikke norsk statsborger"),
    BARN_ER_FOR_GAMMELT("Barn er for gammelt"),
    BARN_HAR_ADRESSEBESKYTTELSE("Barn har adressebeskyttelse"),
    BARN_ER_IKKE_FOEDT_I_NORGE("Barn er ikke født i Norge"),
    BARN_HAR_UTVANDRING("Barn har utvandring"),
    BARN_HAR_HUKET_AV_UTLANDSADRESSE("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    BARN_HAR_VERGE("Barn er markert med verge i søknaden"),
    BARN_HAR_REGISTRERT_VERGE("Barn er registrert med verge i PDL"),
    BARN_ER_IKKE_BOSATT_I_NORGE("Barn er ikke bosatt i Norge"),

    BARN_HAR_FOR_GAMLE_SOESKEN("Det finnes barn av avdøde som er for gamle"),

    AVDOED_HAR_UTVANDRING("Avdød har utvandring"),
    AVDOED_HAR_YRKESSKADE("Avdød er markert med yrkesskade i søknaden"),
    AVDOED_ER_IKKE_REGISTRERT_SOM_DOED("Avdød er ikke registrert som død"),
    AVDOED_HAR_HATT_UTLANDSOPPHOLD("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    AVDOED_VAR_IKKE_BOSATT_I_NORGE("Avdød var ikke bosatt i Norge"),
    AVDOED_ER_IKKE_FORELDER_TIL_BARN("Avdød er ikke forelder til barnet"),
    AVDOED_HAR_ADRESSEBESKYTTELSE("Avdød har adressebeskyttelse"),
    AVDOED_HAR_DOEDSDATO_FOR_LANGT_TILBAKE_I_TID("Avdød har dødsdato for langt tilbake i tid"),

    GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE("Gjenlevende er ikke bosatt i Norge"),
    GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE("Gjenlevende har ikke samme adresse som barnet"),
    GJENLEVENDE_HAR_IKKE_FORELDREANSVAR("Gjenlevende har ikke foreldreansvar for barnet"),
    GJENLEVENDE_HAR_ADRESSEBESKYTTELSE("Gjenlevende har adressebeskyttelse"),

    INNSENDER_ER_IKKE_FORELDER("Innsender er ikke markert som forelder til barnet"),

    SOEKNAD_ER_IKKE_PAA_BOKMAAL("Søknaden er ikke sendt inn på bokmål")
}

class FordelerKriterier {

    fun sjekkMotKriterier(
        barn: Person,
        avdoed: Person,
        gjenlevende: Person,
        soeknad: Barnepensjon
    ): FordelerKriterierResultat {
        return fordelerKriterier(barn, avdoed, gjenlevende)
            .filter { it.blirOppfyltAv(soeknad) }
            .map { it.fordelerKriterie }
            .let { FordelerKriterierResultat(it.isEmpty(), it) }
    }

    /**
     * Grunnlag for regler er også dokumenter på Confluence: https://confluence.adeo.no/display/TE/Fordelingsapp
     */
    private fun fordelerKriterier(barn: Person, avdoed: Person, gjenlevende: Person) = listOf(
        // Barn (søker)
        Kriterie(FordelerKriterie.BARN_ER_FOR_GAMMELT) { forGammel(barn) },
        Kriterie(FordelerKriterie.BARN_ER_IKKE_NORSK_STATSBORGER) { ikkeNorskStatsborger(barn) },
        Kriterie(FordelerKriterie.BARN_ER_IKKE_FOEDT_I_NORGE) { foedtUtland(barn) },
        Kriterie(FordelerKriterie.BARN_ER_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(barn) },
        Kriterie(FordelerKriterie.BARN_HAR_HUKET_AV_UTLANDSADRESSE) { harHuketAvForUtenlandsadresse(it) },
        Kriterie(FordelerKriterie.BARN_HAR_UTVANDRING) { harUtvandring(barn) },
        Kriterie(FordelerKriterie.BARN_HAR_ADRESSEBESKYTTELSE) { harAdressebeskyttelse(barn) },
        Kriterie(FordelerKriterie.BARN_HAR_VERGE) { harHuketAvForVerge(it) },
        Kriterie(FordelerKriterie.BARN_HAR_REGISTRERT_VERGE) { harVergemaalPDL(barn) },
        Kriterie(FordelerKriterie.BARN_HAR_FOR_GAMLE_SOESKEN) { barnHarForGamleSoesken(barn, avdoed) },

        // Avdød
        Kriterie(FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED) { personErIkkeRegistrertDoed(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_ER_IKKE_FORELDER_TIL_BARN) { ikkeForelderTilBarn(avdoed, barn) },
        Kriterie(FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorgeForAvdoed(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_HAR_UTVANDRING) { harUtvandring(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD) { harHuketAvForUtenlandsopphold(it) },
        Kriterie(FordelerKriterie.AVDOED_HAR_YRKESSKADE) { harHuketAvForYrkesskade(it) },
        Kriterie(FordelerKriterie.AVDOED_HAR_ADRESSEBESKYTTELSE) { harAdressebeskyttelse(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_HAR_DOEDSDATO_FOR_LANGT_TILBAKE_I_TID) {
            harDoedsdatoForLangtTilbakeITid(avdoed)
        },

        // Gjenlevende
        Kriterie(FordelerKriterie.GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorge(gjenlevende) },
        Kriterie(FordelerKriterie.GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE) {
            gjenlevendeOgBarnHarIkkeSammeAdresse(gjenlevende, barn)
        },
        Kriterie(FordelerKriterie.GJENLEVENDE_HAR_IKKE_FORELDREANSVAR) {
            gjenlevendeHarIkkeForeldreansvar(barn, gjenlevende)
        },
        Kriterie(FordelerKriterie.GJENLEVENDE_HAR_ADRESSEBESKYTTELSE) { harAdressebeskyttelse(gjenlevende) },

        // Innsender
        Kriterie(FordelerKriterie.INNSENDER_ER_IKKE_FORELDER) { innsenderIkkeForelder(it) },

        Kriterie(FordelerKriterie.SOEKNAD_ER_IKKE_PAA_BOKMAAL) {
            it.spraak != Spraak.NB
        }
    )

    private fun ikkeForelderTilBarn(avdoed: Person, barn: Person): Boolean {
        return barn.familieRelasjon?.foreldre?.let { avdoed.folkeregisteridentifikator !in it } ?: true
    }

    private fun harAdressebeskyttelse(person: Person): Boolean {
        return person.adressebeskyttelse != Adressebeskyttelse.UGRADERT
    }

    private fun ikkeNorskStatsborger(person: Person): Boolean {
        return person.statsborgerskap != NORGE
    }

    private fun foedtUtland(person: Person): Boolean {
        return person.foedeland != NORGE
    }

    private fun harUtvandring(person: Person): Boolean {
        return (
            person.utland?.innflyttingTilNorge?.isNotEmpty() == true ||
                person.utland?.utflyttingFraNorge?.isNotEmpty() == true
            )
    }

    private fun personErIkkeRegistrertDoed(person: Person): Boolean {
        return person.doedsdato == null
    }

    private fun harDoedsdatoForLangtTilbakeITid(avdoed: Person): Boolean =
        avdoed.doedsdato?.isBefore(LocalDate.of(2022, 6, 1)) ?: false

    private fun gjenlevendeHarIkkeForeldreansvar(barn: Person, gjenlevende: Person): Boolean {
        return barn.familieRelasjon?.ansvarligeForeldre
            ?.none { it == gjenlevende.folkeregisteridentifikator } == true
    }

    private fun ikkeGyldigBostedsAdresseINorge(person: Person): Boolean {
        val bostedsadresse = person.bostedsadresse?.nyeste()
        return bostedsadresse?.let {
            it.type !in listOf(AdresseType.VEGADRESSE, AdresseType.MATRIKKELADRESSE)
        } ?: true
    }

    private fun ikkeGyldigBostedsAdresseINorgeForAvdoed(person: Person): Boolean {
        val bostedsadresse = person.bostedsadresse?.nyeste(inkluderInaktiv = true)
        return bostedsadresse?.let {
            val ugyldigAdresseType = it.type !in listOf(AdresseType.VEGADRESSE, AdresseType.MATRIKKELADRESSE)
            if (it.gyldigTilOgMed != null) {
                val doedsdatoFoer = it.gyldigFraOgMed?.let { fom ->
                    person.doedsdato?.isBefore(fom.toLocalDate())
                } ?: true
                val doedsdatoEtter = it.gyldigTilOgMed?.let { tom ->
                    person.doedsdato?.isAfter(tom.toLocalDate())
                } ?: true
                doedsdatoFoer || doedsdatoEtter || ugyldigAdresseType
            } else {
                ugyldigAdresseType
            }
        } ?: true
    }

    private fun gjenlevendeOgBarnHarIkkeSammeAdresse(gjenlevende: Person, barn: Person): Boolean {
        val gjenlevendeAdresse = gjenlevende.bostedsadresse?.aktiv()
        val barnAdresse = barn.bostedsadresse?.aktiv()
        return !isAdresserLike(gjenlevendeAdresse, barnAdresse)
    }

    private fun isAdresserLike(adresse1: Adresse?, adresse2: Adresse?) =
        adresse1?.adresseLinje1 == adresse2?.adresseLinje1 &&
            adresse1?.adresseLinje2 == adresse2?.adresseLinje2 &&
            adresse1?.adresseLinje3 == adresse2?.adresseLinje3 &&
            adresse1?.postnr == adresse2?.postnr

    private fun forGammel(person: Person, alder: Int = 14): Boolean {
        return person.alder()?.let { it > alder } ?: true
    }

    private fun harHuketAvForYrkesskade(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi == JaNeiVetIkke.JA }
    }

    private fun innsenderIkkeForelder(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.innsender.folkeregisteridentifikator !in barnepensjon.foreldre
            .filter { it.type == PersonType.GJENLEVENDE_FORELDER }
            .map { it.folkeregisteridentifikator }
    }

    private fun harHuketAvForVerge(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.soeker.verge?.svar?.verdi == JaNeiVetIkke.JA
    }

    private fun harVergemaalPDL(barn: Person): Boolean {
        return if (barn.vergemaalEllerFremtidsfullmakt != null) {
            barn.vergemaalEllerFremtidsfullmakt!!.isNotEmpty()
        } else {
            false
        }
    }

    private fun harHuketAvForUtenlandsopphold(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.utenlandsopphold.svar.verdi == JaNeiVetIkke.JA }
    }

    private fun harHuketAvForUtenlandsadresse(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.soeker.utenlandsAdresse?.svar?.verdi == JaNeiVetIkke.JA
    }

    private fun barnHarForGamleSoesken(barn: Person, avdoed: Person, alder: Int = 14): Boolean {
        return avdoed.familieRelasjon?.barn?.minus(barn.folkeregisteridentifikator)?.let { avdoedAndreBarn ->
            avdoedAndreBarn.any { it.getAge() > alder }
        } ?: false
    }

    private class Kriterie(val fordelerKriterie: FordelerKriterie, private val sjekk: (Barnepensjon) -> Boolean) {
        fun blirOppfyltAv(soeknadBarnepensjon: Barnepensjon): Boolean = sjekk(soeknadBarnepensjon)
    }
}