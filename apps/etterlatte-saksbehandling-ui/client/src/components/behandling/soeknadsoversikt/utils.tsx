import { Alert } from '@navikt/ds-react'
import { AlertWrapper, WarningText } from './styled'
import moment from 'moment'

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

export const WarningAlert = (infomelding: string) => (
  <AlertWrapper>
    <Alert variant="warning" className="alert" size="small">
      {infomelding}
    </Alert>
  </AlertWrapper>
)

export const sjekkAdresseGjenlevendeISoeknadMotPdl = (adresseFraSoeknad: string, adresseFraPdl: string) => {
  return (
    adresseFraPdl !== adresseFraSoeknad &&
    WarningAlert(
      `Adresse til gjenlevende foreldre er ulik fra oppgitt i søknad og PDL. Orienter innsender og avklar hvilken adresse som stemmer.`
    )
  )
}

export const hentAlderVedDoedsdato = (foedselsdato: string, doedsdato: string): string => {
  return Math.floor(moment(doedsdato).diff(moment(foedselsdato), 'years', true)).toString()
}
