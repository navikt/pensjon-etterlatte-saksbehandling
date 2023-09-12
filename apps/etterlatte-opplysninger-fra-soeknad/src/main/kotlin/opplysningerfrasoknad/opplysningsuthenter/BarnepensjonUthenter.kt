package no.nav.etterlatte.opplysningerfrasoknad.opplysningsuthenter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.lagOpplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.AvdoedSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.InnsenderSoeknad
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.innsendtsoeknad.Spraak
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.Barnepensjon
import no.nav.etterlatte.libs.common.innsendtsoeknad.barnepensjon.GjenlevendeForelder
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.Avdoed
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.JaNeiVetIkke
import no.nav.etterlatte.libs.common.innsendtsoeknad.common.PersonType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Utenlandsadresse
import no.nav.etterlatte.libs.common.person.UtenlandsoppholdOpplysninger
import no.nav.etterlatte.libs.common.tidspunkt.toTidspunkt
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Forelder
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.GjenlevendeForelderSoeknad
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Samtykke
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.SoekerBarnSoeknad
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.SoeknadstypeOpplysning
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Utbetalingsinformasjon
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.UtenlandsadresseBarn
import no.nav.etterlatte.opplysningerfrasoknad.opplysninger.Verge
import java.time.YearMonth
import java.util.*

internal object BarnepensjonUthenter {

    fun lagOpplysningsListe(jsonNode: JsonNode): List<Grunnlagsopplysning<out Any?>> {
        val barnepensjonssoknad = objectMapper.treeToValue<Barnepensjon>(jsonNode)
        val kilde = Grunnlagsopplysning.Privatperson(
            barnepensjonssoknad.innsender.foedselsnummer.svar.value,
            barnepensjonssoknad.mottattDato.toTidspunkt()
        )

        val søkerUtenlandsopphold = barnepensjonssoknad.soeker.utenlandsAdresse?.let {
            lagOpplysning(
                opplysningsType = Opplysningstype.UTENLANDSADRESSE,
                kilde = kilde,
                opplysning = Utenlandsadresse(
                    it.svar.verdi,
                    it.opplysning?.land?.svar?.innhold,
                    it.opplysning?.adresse?.svar?.innhold
                ),
                periode = null
            )
        }
        val utenlandsopphold = hentAvdoedForelder(barnepensjonssoknad)?.utenlandsopphold
        val utenlandsoppholdAvdød: List<Grunnlagsopplysning<out Any?>> = utenlandsopphold?.svar?.verdi?.let { svar ->
            when (svar) {
                JaNeiVetIkke.JA -> utenlandsopphold.opplysning?.map {
                    lagOpplysning(
                        opplysningsType = Opplysningstype.UTENLANDSOPPHOLD,
                        kilde = kilde,
                        opplysning = UtenlandsoppholdOpplysninger(
                            utenlandsopphold.svar.verdi,
                            it.land.svar.innhold,
                            it.oppholdsType.svar.map { utlandsopphold -> utlandsopphold.verdi },
                            it.medlemFolketrygd.svar.verdi,
                            it.pensjonsutbetaling?.svar?.innhold
                        ),
                        periode = it.fraDato?.svar?.innhold?.let { fom ->
                            Periode(YearMonth.from(fom), it.tilDato?.svar?.innhold?.let { tom -> YearMonth.from(tom) })
                        }
                    )
                }

                JaNeiVetIkke.NEI -> hentAvdoedForelder(barnepensjonssoknad)?.foedselsnummer?.svar?.let { avdoedFnr ->
                    listOf(
                        Grunnlagsopplysning.empty(
                            Opplysningstype.UTENLANDSOPPHOLD,
                            kilde,
                            avdoedFnr.toFolkeregisteridentifikator(),
                            YearMonth.from(Folkeregisteridentifikator.of(avdoedFnr.value).getBirthDate())
                        )
                    )
                }

                JaNeiVetIkke.VET_IKKE -> emptyList()
            }
        } ?: emptyList()

        return utenlandsoppholdAvdød + listOfNotNull(
            søkerUtenlandsopphold,
            avdoed(barnepensjonssoknad, Opplysningstype.AVDOED_SOEKNAD_V1),
            soeker(barnepensjonssoknad, Opplysningstype.SOEKER_SOEKNAD_V1),
            gjenlevendeForelder(barnepensjonssoknad, Opplysningstype.GJENLEVENDE_FORELDER_SOEKNAD_V1),
            innsender(barnepensjonssoknad, Opplysningstype.INNSENDER_SOEKNAD_V1),
            utbetalingsinformasjon(barnepensjonssoknad, Opplysningstype.UTBETALINGSINFORMASJON_V1),
            samtykke(barnepensjonssoknad, Opplysningstype.SAMTYKKE),
            spraak(barnepensjonssoknad, Opplysningstype.SPRAAK),
            soeknadMottattDato(barnepensjonssoknad, Opplysningstype.SOEKNAD_MOTTATT_DATO),
            soeknadsType(barnepensjonssoknad, Opplysningstype.SOEKNADSTYPE_V1)
        )
    }

    private fun <T> setBehandlingsopplysninger(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype,
        data: T
    ): Grunnlagsopplysning<T> {
        return Grunnlagsopplysning(
            UUID.randomUUID(),
            Grunnlagsopplysning.Privatperson(
                barnepensjon.innsender.foedselsnummer.svar.value,
                barnepensjon.mottattDato.toTidspunkt()
            ),
            opplysningsType,
            objectMapper.createObjectNode(),
            data
        )
    }

    private fun hentAvdoedForelder(barnepensjon: Barnepensjon): Avdoed? {
        return barnepensjon.foreldre.find { it.type === PersonType.AVDOED }?.let { it as Avdoed }
    }

    private fun hentGjenlevendeForelder(barnepensjon: Barnepensjon): GjenlevendeForelder? {
        return barnepensjon.foreldre.find { it.type === PersonType.GJENLEVENDE_FORELDER }
            ?.let { it as GjenlevendeForelder }
    }

    private fun avdoed(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out AvdoedSoeknad>? {
        return hentAvdoedForelder(barnepensjon)?.let { avdoed ->
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                avdoedOpplysning(avdoed)
            )
        }
    }

    private fun soeker(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out SoekerBarnSoeknad> {
        val adresse = barnepensjon.soeker.utenlandsAdresse

        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            SoekerBarnSoeknad(
                type = PersonType.BARN,
                fornavn = barnepensjon.soeker.fornavn.svar,
                etternavn = barnepensjon.soeker.etternavn.svar,
                foedselsnummer = barnepensjon.soeker.foedselsnummer.svar.toFolkeregisteridentifikator(),
                statsborgerskap = barnepensjon.soeker.statsborgerskap.svar,
                utenlandsadresse = UtenlandsadresseBarn(
                    adresse?.svar?.verdi,
                    adresse?.opplysning?.land?.svar?.innhold,
                    adresse?.opplysning?.adresse?.svar?.innhold
                ),
                foreldre = barnepensjon.soeker.foreldre.map {
                    Forelder(
                        it.type,
                        it.fornavn.svar,
                        it.etternavn.svar,
                        it.foedselsnummer.svar.toFolkeregisteridentifikator()
                    )
                },
                verge = Verge(
                    barnepensjon.soeker.verge?.svar?.verdi,
                    barnepensjon.soeker.verge?.opplysning?.fornavn?.svar,
                    barnepensjon.soeker.verge?.opplysning?.etternavn?.svar,
                    barnepensjon.soeker.verge?.opplysning?.foedselsnummer?.svar?.toFolkeregisteridentifikator()
                ),
                omsorgPerson = barnepensjon.soeker.dagligOmsorg?.svar?.verdi
            )
        )
    }

    private fun gjenlevendeForelder(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out GjenlevendeForelderSoeknad>? {
        return hentGjenlevendeForelder(barnepensjon)?.let { forelder ->
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                GjenlevendeForelderSoeknad(
                    PersonType.GJENLEVENDE_FORELDER,
                    forelder.fornavn.svar,
                    forelder.etternavn.svar,
                    forelder.foedselsnummer.svar.toFolkeregisteridentifikator(),
                    forelder.adresse.svar,
                    forelder.statsborgerskap.svar,
                    forelder.kontaktinfo.telefonnummer.svar.innhold
                )
            )
        }
    }

    fun innsender(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out InnsenderSoeknad> {
        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            InnsenderSoeknad(
                PersonType.INNSENDER,
                barnepensjon.innsender.fornavn.svar,
                barnepensjon.innsender.etternavn.svar,
                barnepensjon.innsender.foedselsnummer.svar.toFolkeregisteridentifikator()
            )
        )
    }

    fun samtykke(barnepensjon: Barnepensjon, opplysningsType: Opplysningstype): Grunnlagsopplysning<out Samtykke> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, Samtykke(barnepensjon.harSamtykket.svar))
    }

    fun spraak(barnepensjon: Barnepensjon, opplysningsType: Opplysningstype): Grunnlagsopplysning<out Spraak> =
        setBehandlingsopplysninger(barnepensjon, opplysningsType, barnepensjon.spraak)

    fun utbetalingsinformasjon(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out Utbetalingsinformasjon>? {
        return barnepensjon.utbetalingsInformasjon?.let {
            setBehandlingsopplysninger(
                barnepensjon,
                opplysningsType,
                utbetalingsinformasjonOpplysning(it)
            )
        }
    }

    private fun soeknadMottattDato(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out SoeknadMottattDato> {
        return setBehandlingsopplysninger(
            barnepensjon,
            opplysningsType,
            SoeknadMottattDato(barnepensjon.mottattDato)
        )
    }

    private fun soeknadsType(
        barnepensjon: Barnepensjon,
        opplysningsType: Opplysningstype
    ): Grunnlagsopplysning<out SoeknadstypeOpplysning> {
        return setBehandlingsopplysninger(barnepensjon, opplysningsType, SoeknadstypeOpplysning(barnepensjon.type))
    }
}