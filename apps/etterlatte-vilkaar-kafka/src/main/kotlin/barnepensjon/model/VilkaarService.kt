package no.nav.etterlatte.barnepensjon.model

import barnepensjon.domain.Aarsak
import barnepensjon.kommerbarnettilgode.mapFamiliemedlemmer
import barnepensjon.kommerbarnettilgode.saksbehandlerResultat
import barnepensjon.vilkaar.avdoedesmedlemskap.vilkaarAvdoedesMedlemskap
import barnepensjon.vilkaar.vilkaarKanBehandleSakenISystemet
import barnepensjon.vilkaarFormaalForYtelsen
import no.nav.etterlatte.barnepensjon.GrunnlagForAvdoedMangler
import no.nav.etterlatte.barnepensjon.OpplysningKanIkkeHentesUt
import no.nav.etterlatte.barnepensjon.hentSisteVurderteDato
import no.nav.etterlatte.barnepensjon.setVilkaarVurderingFraVilkaar
import no.nav.etterlatte.barnepensjon.setVurderingFraKommerBarnetTilGode
import no.nav.etterlatte.barnepensjon.vilkaarBrukerErUnder20
import no.nav.etterlatte.barnepensjon.vilkaarDoedsfallErRegistrert
import no.nav.etterlatte.domene.vedtak.Behandling
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentKommerBarnetTilgode
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
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

    private fun mapVilkaarForstegangsbehandling(
        grunnlag: Grunnlag,
        virkningstidspunkt: LocalDate
    ): VilkaarResultat {
        logger.info(
            "Mapper vilkaar fra grunnlagsdata for virkningstidspunkt $virkningstidspunkt for førstegangsbehandling"
        )

        val soeker = grunnlag.soeker
        val avdoed = grunnlag.hentAvdoed()
        val gjenlevende = grunnlag.hentGjenlevende()

        val vilkaar = listOf(
            vilkaarFormaalForYtelsen(soeker, virkningstidspunkt),
            vilkaarBrukerErUnder20(soeker, avdoed, virkningstidspunkt),
            vilkaarDoedsfallErRegistrert(avdoed, soeker),
            vilkaarAvdoedesMedlemskap(avdoed),
            vilkaarBarnetsMedlemskap(
                soeker,
                gjenlevende,
                avdoed
            )
        )

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)
    }

    private fun mapVilkaarRevurdering(
        grunnlag: Grunnlag,
        virkningstidspunkt: LocalDate,
        revurderingAarsak: RevurderingAarsak
    ): VilkaarResultat {
        logger.info(
            "Mapper vilkaar fra grunnlagsdata for virkningstidspunkt $virkningstidspunkt for revurdering " +
                "med årsak $revurderingAarsak"
        )
        val soeker = grunnlag.soeker
        val vilkaar = when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> listOf(vilkaarFormaalForYtelsen(soeker, virkningstidspunkt))
            RevurderingAarsak.MANUELT_OPPHOER -> throw IllegalArgumentException(
                "Du kan ikke ha et manuelt opphør på en revurdering"
            )
        }

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)
    }

    private fun beregnVirkningstidspunktFoerstegangsbehandling(
        grunnlag: Grunnlag,
        soeknadMottattDato: LocalDate
    ): YearMonth {
        val avdoedDoedsdato = grunnlag.hentAvdoed().hentDoedsdato()
        return hentVirkningstidspunktFoerstegangssoeknad(avdoedDoedsdato?.verdi, soeknadMottattDato)
    }

    private fun beregnVirkningstidspunktRevurdering(
        grunnlag: Grunnlag,
        revurderingAarsak: RevurderingAarsak
    ): YearMonth {
        return when (revurderingAarsak) {
            RevurderingAarsak.SOEKER_DOD -> {
                val soekerDoedsdato = grunnlag.soeker.hentDoedsdato()
                hentVirkningstidspunktRevurderingSoekerDoedsfall(soekerDoedsdato?.verdi)
            }

            RevurderingAarsak.MANUELT_OPPHOER -> throw IllegalArgumentException(
                "Kan ikke ha en revurdering på grunn av manuelt opphør!"
            )
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
            throw GrunnlagForAvdoedMangler
        }
        if (mottattDato.year - doedsdato.year > 3) {
            return YearMonth.of(mottattDato.year - 3, mottattDato.month)
        }
        return YearMonth.from(doedsdato).plusMonths(1)
    }

    private fun mapKommerSoekerTilGode(
        grunnlag: Grunnlag
    ): KommerSoekerTilgode {
        logger.info("Map opplysninger for å vurdere om penger kommer søker til gode")
        val soeker = grunnlag.soeker
        val gjenlevende = grunnlag.hentGjenlevende()
        val avdoed = grunnlag.hentAvdoed()
        val saksbehandlerKommerBarnetTilgode = grunnlag.sak.hentKommerBarnetTilgode()

        val kommerBarnetTilGode = listOf(
            barnOgForelderSammeBostedsadresse(
                soeker,
                gjenlevende
            ),
            barnIngenOppgittUtlandsadresse(soeker),
            barnOgAvdoedSammeBostedsadresse(
                soeker,
                avdoed
            ),
            saksbehandlerResultat(saksbehandlerKommerBarnetTilgode)
        )

        val vilkaarResultat = setVurderingFraKommerBarnetTilGode(kommerBarnetTilGode.filterNotNull())
        val vurdertDato = hentSisteVurderteDato(kommerBarnetTilGode.filterNotNull())
        val vurdering = VilkaarResultat(vilkaarResultat, kommerBarnetTilGode.filterNotNull(), vurdertDato)

        val familieforhold = mapFamiliemedlemmer(soeker, gjenlevende, avdoed)

        return KommerSoekerTilgode(vurdering, familieforhold)
    }

    fun finnVirkningstidspunktOgVilkaarForBehandling(
        behandling: Behandling,
        grunnlag: Grunnlag,
        behandlingopprettet: LocalDate,
        aarsak: Aarsak
    ): Triple<YearMonth, VilkaarResultat, KommerSoekerTilgode?> {
        val virkningstidspunkt = when (behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> beregnVirkningstidspunktFoerstegangsbehandling(
                grunnlag,
                behandlingopprettet
            )

            BehandlingType.REVURDERING -> beregnVirkningstidspunktRevurdering(
                grunnlag,
                requireNotNull(aarsak.revurderingAarsak) { "Må ha en revurderingAarsak på en revurdering" }
            )

            BehandlingType.MANUELT_OPPHOER -> beregnVirkningstidspunktManueltOpphoer(
                grunnlag,
                behandlingopprettet
            )
        }
        val vilkaarResultat = when (behandling.type) {
            BehandlingType.REVURDERING -> mapVilkaarRevurdering(
                grunnlag,
                virkningstidspunkt.atDay(1),
                requireNotNull(aarsak.revurderingAarsak) { "Må ha en revurderingAarsak på en revurdering" }
            )

            BehandlingType.FØRSTEGANGSBEHANDLING -> mapVilkaarForstegangsbehandling(
                grunnlag,
                virkningstidspunkt.atDay(1)
            )

            BehandlingType.MANUELT_OPPHOER -> mapVilkaarManueltOpphoer(virkningstidspunkt, aarsak)
        }
        val kommerSoekerTilgode = when (behandling.type) {
            BehandlingType.FØRSTEGANGSBEHANDLING -> mapKommerSoekerTilGode(grunnlag)
            else -> null
        }
        return Triple(virkningstidspunkt, vilkaarResultat, kommerSoekerTilgode)
    }

    private fun mapVilkaarManueltOpphoer(virkningstidspunkt: YearMonth, aarsak: Aarsak): VilkaarResultat {
        logger.info(
            "Mapper vilkaar fra grunnlagsdata for virkningstidspunkt $virkningstidspunkt for manuelt opphør av sak"
        )

        val vilkaar = listOf(
            vilkaarKanBehandleSakenISystemet(aarsak.manueltOpphoerKjenteGrunner, aarsak.manueltOpphoerFritekstgrunn)
        )

        val vilkaarResultat = setVilkaarVurderingFraVilkaar(vilkaar)
        val vurdertDato = hentSisteVurderteDato(vilkaar)

        return VilkaarResultat(vilkaarResultat, vilkaar, vurdertDato)
    }

    private fun beregnVirkningstidspunktManueltOpphoer(
        grunnlag: Grunnlag,
        soeknadMottattDato: LocalDate
    ): YearMonth {
        val avdoedDoedsdato = grunnlag.hentAvdoed().hentDoedsdato()
        return hentVirkningstidspunktFoerstegangssoeknad(avdoedDoedsdato?.verdi, soeknadMottattDato)
    }
}