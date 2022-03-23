import moment from 'moment'
import { AlertVarsel } from './AlertVarsel'
import { IPersonOpplysningFraPdl } from '../../types'
import { WarningText } from '../../../../shared/styled'

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

export const sjekkAdresseGjenlevendeISoeknadMotPdl = (adresseFraSoeknad: string, adresseFraPdl: string) => {
  return adresseFraPdl !== adresseFraSoeknad && <AlertVarsel varselType="ikke lik adresse" />
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
