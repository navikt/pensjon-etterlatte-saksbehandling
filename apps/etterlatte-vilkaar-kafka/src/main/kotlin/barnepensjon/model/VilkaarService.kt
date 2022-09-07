package no.nav.etterlatte.barnepensjon.model

import barnepensjon.kommerbarnettilgode.mapFamiliemedlemmer
import barnepensjon.kommerbarnettilgode.saksbehandlerResultat
import barnepensjon.vilkaar.avdoedesmedlemskap.vilkaarAvdoedesMedlemskap
import barnepensjon.vilkaarFormaalForYtelsen
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentSisteVurderteDato
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraVilkaar
import no.nav.etterlatte.barnepensjon.setVurderingFraKommerBarnetTilGode
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.libs.common.arbeidsforhold.ArbeidsforholdOpplysning
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SaksbehandlerMedlemskapsperioder
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoekerBarnSoeknad
import no.nav.etterlatte.libs.common.inntekt.InntektsOpplysning
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.saksbehandleropplysninger.ResultatKommerBarnetTilgode
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarOpplysning
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.vilkaar.barnepensjon.vilkaarBarnetsMedlemskap
import org.slf4j.LoggerFactory
import vilkaar.barnepensjon.barnIngenOppgittUtlandsadresse
import vilkaar.barnepensjon.barnOgAvdoedSammeBostedsadresse
import vilkaar.barnepensjon.barnOgForelderSammeBostedsadresse
import java.time.LocalDate
import java.time.YearMonth

class VilkaarService {
    private val logger = LoggerFactory.getLogger(VilkaarService::class.java)

    fun mapVilkaarForstegangsbehandling(
        opplysninger: List<VilkaarOpplysning<ObjectNode>>,
        virkningstidspunkt: LocalDate
    ): VilkaarResultat {
        logger.info(
            "Mapper vilkaar fra grunnlagsdata for virkningstidspunkt $virkningstidspunkt for førstegangsbehandling"
        )

        val avdoedSoeknad = finnOpplysning<AvdoedSoeknad>(opplysninger, Opplysningstyper.AVDOED_SOEKNAD_V1)
        val soekerSoeknad = finnOpplysning<SoekerBarnSoeknad>(opplysninger, Opplysningstyper.SOEKER_SOEKNAD_V1)
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)
        val avdoedPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)
        val gjenlevendePdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1)
        val avdoedeInntektsOpplysning =
            finnOpplysning<InntektsOpplysning>(opplysninger, Opplysningstyper.AVDOED_INNTEKT_V1)
        val arbeidsforhold = finnOpplysning<ArbeidsforholdOpplysning>(opplysninger, Opplysningstyper.ARBEIDSFORHOLD_V1)
        val saksbehandlerPerioder = finnOpplysning<SaksbehandlerMedlemskapsperioder>(
            opplysninger,
            Opplysningstyper.SAKSBEHANDLER_AVDOED_MEDLEMSKAPS_PERIODE
        )
        // todo finn ut av hvordan vi kan lagre flere opplysninger av samme type til grunnlag

        val vilkaar = listOf(
            vilkaarFormaalForYtelsen(soekerPdl, virkningstidspunkt),
            vilkaarBrukerErUnder20(soekerPdl, avdoedPdl, virkningstidspunkt),
            vilkaarDoedsfallErRegistrert(avdoedPdl, soekerPdl),
            vilkaarAvdoedesMedlemskap(
                avdoedSoeknad,
                avdoedPdl,
                avdoedeInntektsOpplysning,
                arbeidsforhold,
                saksbehandlerPerioder
            ),
            vilkaarBarnetsMedlemskap(
                soekerPdl,
                soekerSoeknad,
                gjenlevendePdl,
                avdoedPdl
            )
        )

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)
    }

    fun mapVilkaarRevurdering(
        opplysninger: List<VilkaarOpplysning<ObjectNode>>,
        virkningstidspunkt: LocalDate,
        revurderingAarsak: RevurderingAarsak
    ): VilkaarResultat {
        logger.info(
            "Mapper vilkaar fra grunnlagsdata for virkningstidspunkt $virkningstidspunkt for revurdering " +
                "med årsak $revurderingAarsak"
        )
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)

        val vilkaar = when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> listOf(vilkaarFormaalForYtelsen(soekerPdl, virkningstidspunkt))
            RevurderingAarsak.MANUELT_OPPHOER -> TODO("Ikke implementert vurdering av denne enda")
        }

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)
    }

    fun beregnVirkningstidspunktFoerstegangsbehandling(
        opplysninger: List<VilkaarOpplysning<ObjectNode>>,
        soeknadMottattDato: LocalDate
    ): YearMonth {
        val avdoedPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)
        return hentVirkningstidspunktFoerstegangssoeknad(avdoedPdl?.opplysning?.doedsdato, soeknadMottattDato)
    }

    fun beregnVirkningstidspunktRevurdering(
        opplysninger: List<VilkaarOpplysning<ObjectNode>>,
        revurderingAarsak: RevurderingAarsak
    ): YearMonth {
        return when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> {
                val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)
                hentVirkningstidspunktRevurderingSoekerDoedsfall(soekerPdl?.opplysning?.doedsdato)
            }
            RevurderingAarsak.MANUELT_OPPHOER -> TODO("Ikke implementert")
        }
    }

    fun hentVirkningstidspunktRevurderingSoekerDoedsfall(soekerDoedsdato: LocalDate?): YearMonth {
        if (soekerDoedsdato == null) {
            throw OpplysningKanIkkeHentesUt(
                """
                Vi har en revurdering av en avdød mottaker, men mottaker er ikke død i grunnlagsdata fra PDL. 
                Vi kan dermed ikke sette riktig virkningstidspunkt og vilkårsvurdere revurderingen.
                """.trimIndent()
            )
        }
        return YearMonth.from(soekerDoedsdato).plusMonths(1)
    }

    fun hentVirkningstidspunktFoerstegangssoeknad(doedsdato: LocalDate?, mottattDato: LocalDate): YearMonth {
        if (doedsdato == null) {
            throw OpplysningKanIkkeHentesUt(
                """
                Vi har en førstegangssøknad der vi ikke kan hente ut avdød sin dødsdato. 
                Vi kan dermed heller ikke beslutte virkningstidspunkt og vilkårsvurdere
                """.trimIndent()
            )
        }
        if (mottattDato.year - doedsdato.year > 3) {
            return YearMonth.of(mottattDato.year - 3, mottattDato.month)
        }
        return YearMonth.from(doedsdato).plusMonths(1)
    }

    fun mapKommerSoekerTilGode(opplysninger: List<VilkaarOpplysning<ObjectNode>>): KommerSoekerTilgode {
        logger.info("Map opplysninger for å vurdere om penger kommer søker til gode")
        val soekerPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.SOEKER_PDL_V1)
        val gjenlevendePdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.GJENLEVENDE_FORELDER_PDL_V1)
        val soekerSoeknad =
            finnOpplysning<SoekerBarnSoeknad>(opplysninger, Opplysningstyper.SOEKER_SOEKNAD_V1)
        val avdoedPdl = finnOpplysning<Person>(opplysninger, Opplysningstyper.AVDOED_PDL_V1)
        val saksbehandlerKommerBarnetTilgode = finnOpplysning<ResultatKommerBarnetTilgode>(
            opplysninger,
            Opplysningstyper.SAKSBEHANDLER_KOMMER_BARNET_TILGODE_V1
        )

        val kommerBarnetTilGode = listOf(
            barnOgForelderSammeBostedsadresse(
                soekerPdl,
                gjenlevendePdl
            ),
            barnIngenOppgittUtlandsadresse(soekerSoeknad),
            barnOgAvdoedSammeBostedsadresse(
                soekerPdl,
                avdoedPdl
            ),
            saksbehandlerResultat(
                saksbehandlerKommerBarnetTilgode
            )
        )

        val vilkaarResultat = setVurderingFraKommerBarnetTilGode(kommerBarnetTilGode.filterNotNull())
        val vurdertDato = hentSisteVurderteDato(kommerBarnetTilGode.filterNotNull())
        val vurdering = VilkaarResultat(vilkaarResultat, kommerBarnetTilGode.filterNotNull(), vurdertDato)

        val familieforhold = mapFamiliemedlemmer(soekerPdl, soekerSoeknad, gjenlevendePdl, avdoedPdl)

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