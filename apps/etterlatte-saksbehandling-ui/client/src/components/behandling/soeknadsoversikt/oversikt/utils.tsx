import moment from 'moment'
import { IPersonOpplysningFraPdl } from '../../types'
import { WarningText } from '../../../../shared/styled'
import { GyldighetType, VurderingsResultat, IGyldighetproving } from '../../../../store/reducers/BehandlingReducer'

export const sjekkDataFraSoeknadMotPdl = (dataFraPdl: string, dataFraSoeknad: string) => {
  return dataFraSoeknad === dataFraPdl || dataFraSoeknad === null ? (
    <span>{dataFraPdl}</span>
  ) : (
    <WarningText>
      <div>{dataFraPdl}</div>
      {dataFraSoeknad} (fra søknad)
    </WarningText>
  )
}

export const sjekkPersonFraSoeknadMotPdl = (personFraPdl: IPersonOpplysningFraPdl, personFraSoeknad: any) => {
  //TODO: hvis fnr er likt, men navn forskjellig, hva skal vises?
  return personFraSoeknad.foedselsnummer === personFraPdl.foedselsnummer ? (
    <span>
      {personFraPdl?.fornavn} {personFraPdl?.etternavn}
    </span>
  ) : (
    <WarningText>
      <div>
        {personFraPdl?.fornavn} {personFraPdl?.etternavn}
      </div>
      {personFraSoeknad?.fornavn} {personFraSoeknad?.etternavn} (fra søknad)
    </WarningText>
  )
}

export const sjekkAdresseGjenlevendeISoeknadMotPdl = (adresseFraSoeknad: string, adresseFraPdl: string): boolean => {
  return adresseFraPdl !== adresseFraSoeknad
}

export const hentAlderVedDoedsdato = (foedselsdato: string, doedsdato: string): string => {
  return Math.floor(moment(doedsdato).diff(moment(foedselsdato), 'years', true)).toString()
}

export const hentVirkningstidspunkt = (doedsdato: string, mottattDato: string): string => {
  if (sjekkDodsfallMerEnn3AarSiden(doedsdato, mottattDato)) {
    return moment(moment(doedsdato).add(3, 'years').toString()).add(1, 'M').startOf('month').toString()
  }
  return moment(doedsdato).add(1, 'M').startOf('month').toString()
}

export const sjekkDodsfallMerEnn3AarSiden = (doedsdato: string, mottattDato: string): boolean => {
  return moment(mottattDato).diff(moment(doedsdato), 'years', true) > 3
}

export const getStatsborgerskapTekst = (statsborgerskap: string) => {
  //TODO eller slette? Hva slags strenger får man fra pdl?
  switch (statsborgerskap) {
    case 'NORGE':
      return 'NO'
    case 'NOR':
      return 'NO'
    default:
      return statsborgerskap
  }
}

export const hentGyldighetsTekst = (
  gyldighetResultat: VurderingsResultat,
  bosted: IGyldighetproving,
  foreldreransvar: IGyldighetproving,
  innsender: IGyldighetproving
) => {
  const bostedTekst = bosted && mapGyldighetstyperTilTekst(bosted)
  const foreldreansvarTekst = foreldreransvar && mapGyldighetstyperTilTekst(foreldreransvar)
  const innsenderTekst = innsender && mapGyldighetstyperTilTekst(innsender)

  const gyldigheter = []
  let resultat

  bostedTekst && gyldigheter.push(bostedTekst)
  foreldreansvarTekst && gyldigheter.push(foreldreansvarTekst)
  innsenderTekst && gyldigheter.push(innsenderTekst)

  if (gyldigheter.length > 1) {
    const last = gyldigheter.pop()
    resultat = gyldigheter.join(', ') + ' og ' + last
  } else {
    resultat = gyldigheter[0]
  }

  if (gyldighetResultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
    return `Nei, mangler info om ${resultat}. Må avklares.`
  } else if (gyldighetResultat === VurderingsResultat.IKKE_OPPFYLT) {
    return `Nei, ${resultat}. Må avklares.`
  } else return ''
}

export function mapGyldighetstyperTilTekst(gyldig: IGyldighetproving): String | undefined {
  let svar

  if (gyldig.navn === GyldighetType.INNSENDER_ER_FORELDER) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'innsender har ikke foreldreansvar'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'innsender'
    } else {
      svar = undefined
    }
  } else if (gyldig.navn === GyldighetType.HAR_FORELDREANSVAR_FOR_BARNET) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'gjenlevende forelder har ikke foreldreansvar'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'foreldreansvar'
    } else {
      svar = undefined
    }
  } else if (gyldig.navn === GyldighetType.BARN_GJENLEVENDE_SAMME_BOSTEDADRESSE_PDL) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'barn og gjenlevende forelder bor ikke på samme adresse'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'bostedsadresser'
    }
  } else {
    svar = undefined
  }
  return svar
}
