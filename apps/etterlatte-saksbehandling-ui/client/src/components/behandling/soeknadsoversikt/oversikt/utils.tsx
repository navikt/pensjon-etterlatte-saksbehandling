import { WarningText } from '../styled'
import moment from 'moment'
import { AlertVarsel } from './AlertVarsel'
import { IPersonOpplysning } from '../types'

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

export const sjekkPersonFraSoeknadMotPdl = (personFraPdl: IPersonOpplysning, personFraSoeknad: IPersonOpplysning) => {
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

export const hentVirketidspunkt = (doedsdato: string): string => {
  //TODO: når skal virkningsdato være når dodsfall er mere enn 3 år siden?
  return moment(doedsdato).add(1, 'M').startOf('month').toString()
}

export const dodsfallMereEnn3AarSiden = (doedsdato: string, mottattDato: string): boolean => {
  return moment(mottattDato).diff(doedsdato, 'years') >= 3
}
