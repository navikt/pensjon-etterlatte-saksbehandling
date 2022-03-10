import { IAdresse } from '../soeknadsoversikt/types'
import moment from 'moment'

export const Adressevisning = ({ adresser }: { adresser: IAdresse[] }) => {
  return (
    <div>
      {adresser?.length > 0 ? (
        adresser.map((adresse, index) => <Adresse adresse={adresse} key={index} />)
      ) : (
        <div>Ingen adresser</div>
      )}
    </div>
  )
}

export const Adresse = ({ adresse }: { adresse: IAdresse }) => {
  const fra = moment(adresse.gyldigFraOgMed).format('DD.MM.YYYY')
  const til = adresse.aktiv ? 'nå' : moment(adresse.gyldigTilOgMed).format('DD.MM.YYYY')

  return (
    <div style={{ paddingBottom: '10px' }}>
      <div>{adresse.adresseLinje1}</div>
      <div>
        {adresse.postnr} {adresse.poststed}
      </div>
      <div>{adresse.land}</div>
      <div>
        {fra} - {til}
      </div>
    </div>
  )
}
