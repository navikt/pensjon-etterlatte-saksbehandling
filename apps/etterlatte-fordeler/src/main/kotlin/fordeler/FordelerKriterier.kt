package no.nav.etterlatte.fordeler

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.fordeler.digdirkrr.KontaktinfoKlient
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
import org.slf4j.LoggerFactory

private const val NORGE = "NOR"

data class FordelerKriterierResultat(
    val kandidat: Boolean,
    val forklaring: List<FordelerKriterie>
)

enum class FordelerKriterie(val forklaring: String) {
    BARN_ER_IKKE_NORSK_STATSBORGER("Barn er ikke norsk statsborger"),
    BARN_ER_FOR_GAMMELT("Barn er for gammelt"),
    BARN_HAR_ADRESSEBESKYTTELSE("Barn har adressebeskyttelse"),
    BARN_ER_IKKE_FOEDT_I_NORGE("Barn ikke fodt i Norge"),
    BARN_HAR_UTVANDRING("Barn har utvandring"),
    BARN_HAR_HUKET_AV_UTLANDSADRESSE("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    BARN_HAR_VERGE("Barn er market med verge i søknaden"),
    BARN_HAR_REGISTRERT_VERGE("Barn er registrert med verge i PDL"),
    BARN_ER_IKKE_BOSATT_I_NORGE("Barn er ikke bosatt i Norge"),

    // Kommentert ut for å teste søskenjustering
    // BARN_ER_IKKE_ALENEBARN("Barn (søker) er ikke alenebarn"),
    BARN_HAR_FOR_GAMLE_SOESKEN("Det finnes barn av avdøde som er for gamle"),

    AVDOED_HAR_UTVANDRING("Avdoed har utvandring"),
    AVDOED_HAR_YRKESSKADE("Avdød er market med yrkesskade i søknaden"),
    AVDOED_ER_IKKE_REGISTRERT_SOM_DOED("Avdød er ikke død"),
    AVDOED_HAR_HATT_UTLANDSOPPHOLD("Det er huket av for utenlandsopphold for avdøde i søknaden"),
    AVDOED_VAR_IKKE_BOSATT_I_NORGE("Avdød er ikke bosatt i Norge"),
    AVDOED_ER_IKKE_FORELDER_TIL_BARN("Avdød er forelder til søker"),
    AVDOED_HAR_ADRESSEBESKYTTELSE("Avdød har adressebeskyttelse"),

    GJENLEVENDE_ER_IKKE_BOSATT_I_NORGE("Gjenlevende er ikke bosatt i Norge"),
    GJENLEVENDE_OG_BARN_HAR_IKKE_SAMME_ADRESSE("Gjenlevende har samme adresse"),
    GJENLEVENDE_HAR_IKKE_FORELDREANSVAR("Gjenlevende søker har ikke foreldreanvar"),
    GJENLEVENDE_HAR_ADRESSEBESKYTTELSE("Gjenlevende har adressebeskyttelse"),

    INNSENDER_ER_IKKE_FORELDER("Søker er ikke markert som forelder i søknaden"),

    SOEKER_HAR_IKKE_SPRAAK_BOKMAAL("Søker må ha bokmål i KRR, eller bokmål på søknad hvis ikke registrert i KRR")
}

class FordelerKriterier(private val kontaktinfoKlient: KontaktinfoKlient) {
    private val logger = LoggerFactory.getLogger(this.javaClass.name)
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
     * Grunnlag for regler er også dokumentert på Confluence: https://confluence.adeo.no/display/TE/Fordelingsapp
     */
    private fun fordelerKriterier(barn: Person, avdoed: Person, gjenlevende: Person) = listOf(
        Kriterie(FordelerKriterie.SOEKER_HAR_IKKE_SPRAAK_BOKMAAL) { harIkkeValgtBokmaal(barn, it) },

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
        // Kommentert ut for å teste søskenjustering
        // Kriterie(BARN_ER_IKKE_ALENEBARN) { barnErIkkeAlenebarn(avdoed, barn, gjenlevende) },
        Kriterie(FordelerKriterie.BARN_HAR_FOR_GAMLE_SOESKEN) { barnHarForGamleSoesken(barn, avdoed) },

        // Avdød
        Kriterie(FordelerKriterie.AVDOED_ER_IKKE_REGISTRERT_SOM_DOED) { personErIkkeRegistrertDoed(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_ER_IKKE_FORELDER_TIL_BARN) { ikkeForelderTilBarn(avdoed, barn) },
        Kriterie(FordelerKriterie.AVDOED_VAR_IKKE_BOSATT_I_NORGE) { ikkeGyldigBostedsAdresseINorgeForAvdoed(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_HAR_UTVANDRING) { harUtvandring(avdoed) },
        Kriterie(FordelerKriterie.AVDOED_HAR_HATT_UTLANDSOPPHOLD) { harHuketAvForUtenlandsopphold(it) },
        Kriterie(FordelerKriterie.AVDOED_HAR_YRKESSKADE) { harHuketAvForYrkesskade(it) },
        Kriterie(FordelerKriterie.AVDOED_HAR_ADRESSEBESKYTTELSE) { harAdressebeskyttelse(avdoed) },

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
        Kriterie(FordelerKriterie.INNSENDER_ER_IKKE_FORELDER) { innsenderIkkeForelder(it) }

    )

    private fun ikkeForelderTilBarn(avdoed: Person, barn: Person): Boolean {
        return barn.familieRelasjon?.foreldre?.let { avdoed.foedselsnummer !in it } ?: true
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

    private fun gjenlevendeHarIkkeForeldreansvar(barn: Person, gjenlevende: Person): Boolean {
        return barn.familieRelasjon?.ansvarligeForeldre
            ?.none { it == gjenlevende.foedselsnummer } == true
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

    private fun harIkkeValgtBokmaal(barn: Person, barnepensjon: Barnepensjon): Boolean {
        val kontaktinfo = runBlocking {
            kontaktinfoKlient.hentSpraak(barn.foedselsnummer)
        }
        logger.info("Fikk språk fra kontaktinfo ${kontaktinfo.spraak} søknad språk: ${barnepensjon.spraak}")
        return if (kontaktinfo.spraak == null) {
            barnepensjon.spraak != Spraak.NB
        } else {
            kontaktinfo.spraak.lowercase() != Spraak.NB.verdi
        }
    }
    private fun forGammel(person: Person, alder: Int = 14): Boolean {
        return person.alder()?.let { it > alder } ?: true
    }

    private fun barnErIkkeAlenebarn(avdoed: Person, barn: Person, gjenlevende: Person): Boolean {
        return avdoed.familieRelasjon?.barn?.minus(barn.foedselsnummer)?.let { avdoedAndreBarn ->
            gjenlevende.familieRelasjon?.barn?.let { gjenlevendeBarn ->
                (avdoedAndreBarn.any { (it in gjenlevendeBarn) })
            }
        } ?: false
    }

    private fun harHuketAvForYrkesskade(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.foreldre
            .filter { it.type == PersonType.AVDOED }
            .any { it is Avdoed && it.doedsaarsakSkyldesYrkesskadeEllerYrkessykdom.svar.verdi == JaNeiVetIkke.JA }
    }

    private fun innsenderIkkeForelder(barnepensjon: Barnepensjon): Boolean {
        return barnepensjon.innsender.foedselsnummer !in barnepensjon.foreldre
            .filter { it.type == PersonType.GJENLEVENDE_FORELDER }
            .map { it.foedselsnummer }
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
        return avdoed.familieRelasjon?.barn?.minus(barn.foedselsnummer)?.let { avdoedAndreBarn ->
            avdoedAndreBarn.any { it.getAge() > alder }
        } ?: false
    }

    private class Kriterie(val fordelerKriterie: FordelerKriterie, private val sjekk: (Barnepensjon) -> Boolean) {
        fun blirOppfyltAv(soeknadBarnepensjon: Barnepensjon): Boolean = sjekk(soeknadBarnepensjon)
    }
}