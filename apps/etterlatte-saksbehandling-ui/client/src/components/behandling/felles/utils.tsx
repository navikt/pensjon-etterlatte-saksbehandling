import { IAdresse } from '../soeknadsoversikt/types'
import moment from 'moment'

export function hentAdresserEtterDoedsdato(adresser: IAdresse[], doedsdato: Date): IAdresse[] {
  const etterDoedsdatoEllerAktiv = adresser?.filter(
    (adresse) => moment(adresse.gyldigTilOgMed).isAfter(moment(doedsdato)) || adresse.aktiv
  )

  return etterDoedsdatoEllerAktiv
}
