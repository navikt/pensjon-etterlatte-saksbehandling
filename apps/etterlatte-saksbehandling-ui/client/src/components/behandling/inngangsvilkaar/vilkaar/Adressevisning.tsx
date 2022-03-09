import { IAdresse } from '../../soeknadsoversikt/types'
import moment from 'moment'

export const Adressevisning = ({ adresse, doedsdato }: { adresse: IAdresse; doedsdato: Date }) => {
  const etterDoedsdatoEllerAktiv = moment(adresse.gyldigTilOgMed).isAfter(moment(doedsdato)) || adresse.aktiv

  const fra = moment(adresse.gyldigFraOgMed).format('DD.MM.YYYY')
  const til = adresse.aktiv ? 'nå' : moment(adresse.gyldigTilOgMed).format('DD.MM.YYYY')

  return (
    <div>
      {etterDoedsdatoEllerAktiv && (
        <>
          <div>{adresse.adresseLinje1}</div>
          <div>
            {adresse.postnr} {adresse.poststed}
          </div>
          <div>{adresse.land}</div>
          <div>
            ({fra} - {til})
          </div>
        </>
      )}
    </div>
  )
}
