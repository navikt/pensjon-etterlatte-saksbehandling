package no.nav.etterlatte.model

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.barnepensjon.*
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Adresser
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.GjenlevendeForelderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.vikaar.*
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarBarnetsMedlemskap
import org.slf4j.LoggerFactory
import vilkaar.barnepensjon.barnOgForelderSammeBostedsadresse
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters


class VilkaarService {
    private val logger = LoggerFactory.getLogger(VilkaarService::class.java)

    fun mapVilkaar(opplysninger: List<VilkaarOpplysning<ObjectNode>>): VilkaarResultat {
        logger.info("Map vilkaar")

        val avdoedSoeknad = finnOpplysning<AvdoedSoeknad>(opplysninger, Opplysningstyper.AVDOED_SOEKNAD_V1)
        val soekerSoeknad = finnOpplysning<SoekerBarnSoeknad>(opplysninger, Opplysningstyper.SOEKER_SOEKNAD_V1)
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)
        val avdoedPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)
        val gjenlevendePdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1)

        val vilkaar = listOf(
            vilkaarBrukerErUnder20(Vilkaartyper.SOEKER_ER_UNDER_20, soekerPdl, avdoedPdl),
            vilkaarDoedsfallErRegistrert(Vilkaartyper.DOEDSFALL_ER_REGISTRERT, avdoedPdl, soekerPdl),
            vilkaarAvdoedesMedlemskap(
                Vilkaartyper.AVDOEDES_FORUTGAAENDE_MEDLEMSKAP,
                avdoedSoeknad,
                avdoedPdl
            ),
            vilkaarBarnetsMedlemskap(
                Vilkaartyper.BARNETS_MEDLEMSKAP,
                soekerPdl,
                soekerSoeknad,
                gjenlevendePdl,
                avdoedPdl,
            )
        )

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)
    }

    //TODO riktig å bruke localdate? endre til zoned
    fun beregnVilkaarstidspunkt (opplysninger: List<VilkaarOpplysning<ObjectNode>>, opprettet: LocalDate): LocalDate? {
        logger.info("beregner virkningstidspunkt")
        val avdoedPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)
        return avdoedPdl?.opplysning?.doedsdato?.let { hentVirkningstidspunkt(it, opprettet) }
    }
    fun hentVirkningstidspunkt (doedsdato: LocalDate, mottattDato: LocalDate): LocalDate{
        if (mottattDato.year - doedsdato.year > 3) {
            return mottattDato.minusYears(3).with(TemporalAdjusters.firstDayOfNextMonth())
        }
        return mottattDato.with(TemporalAdjusters.firstDayOfNextMonth())
    }


    fun mapKommerSoekerTilGode(opplysninger: List<VilkaarOpplysning<ObjectNode>>): KommerSoekerTilgode {
        logger.info("Map opplysninger for å vurdere om penger kommer søker til gode")
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)
        val gjenlevendePdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1)
        val gjenlevendeSoeknad =
            finnOpplysning<GjenlevendeForelderSoeknad>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_SOEKNAD_V1)
        val avdoedPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)


        val sammeAdresser = listOf(
            barnOgForelderSammeBostedsadresse(
                Vilkaartyper.SAMME_ADRESSE,
                soekerPdl,
                gjenlevendePdl
            )
        )

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(sammeAdresser)
        val vurdertDato = hentSisteVurderteDato(sammeAdresser)
        val vurdering = VilkaarResultat(vilkaarResultat, sammeAdresser, vurdertDato)

        val familieforhold = mapFamiliemedlemmer(soekerPdl, gjenlevendePdl, gjenlevendeSoeknad, avdoedPdl)


        return KommerSoekerTilgode(vurdering, familieforhold)
    }

    companion object {
        inline fun <reified T> setOpplysningType(opplysning: VilkaarOpplysning<ObjectNode>?): VilkaarOpplysning<T>? {
            return opplysning?.let {
                VilkaarOpplysning(
                    opplysning.id,
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

fun mapFamiliemedlemmer(
    soeker: VilkaarOpplysning<Person>?,
    gjenlevende: VilkaarOpplysning<Person>?,
    gjenlevendeSoeknad: VilkaarOpplysning<GjenlevendeForelderSoeknad>?,
    avdoed: VilkaarOpplysning<Person>?,
): Familiemedlemmer {
    return Familiemedlemmer(
        avdoed = avdoed?.opplysning.let {
            PersoninfoAvdoed(
                navn = it?.fornavn + " " + it?.etternavn,
                fnr = it?.foedselsnummer,
                rolle = PersonRolle.AVDOED,
                adresser = Adresser(it?.bostedsadresse, it?.oppholdsadresse, it?.kontaktadresse),
                doedsdato = it?.doedsdato
            )
        },
        soeker = soeker?.opplysning.let {
            PersoninfoSoeker(
                navn = it?.fornavn + " " + it?.etternavn,
                fnr = it?.foedselsnummer,
                rolle = PersonRolle.AVDOED,
                adresser = Adresser(it?.bostedsadresse, it?.oppholdsadresse, it?.kontaktadresse),
                foedselsdato = it?.foedselsdato
            )
        },
        gjenlevendeForelder = gjenlevende?.opplysning.let {
            PersoninfoGjenlevendeForelder(
                navn = it?.fornavn + " " + it?.etternavn,
                fnr = it?.foedselsnummer,
                rolle = PersonRolle.AVDOED,
                adresser = Adresser(it?.bostedsadresse, it?.oppholdsadresse, it?.kontaktadresse),
                adresseSoeknad = gjenlevendeSoeknad?.opplysning?.adresse,
            )
        })
}