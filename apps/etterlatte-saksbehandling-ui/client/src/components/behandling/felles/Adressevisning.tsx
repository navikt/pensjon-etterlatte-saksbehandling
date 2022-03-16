import { IAdresse } from '../soeknadsoversikt/types'
import moment from 'moment'
import { HistorikkElement } from '../soeknadsoversikt/styled'

export const Adressevisning = ({
  adresser,
  soeknadsoversikt,
}: {
  adresser: IAdresse[]
  soeknadsoversikt?: boolean
}) => {
  return (
    <div>
      {adresser?.length > 0 ? (
        adresser
          .sort((a, b) => (new Date(b.gyldigFraOgMed) > new Date(a.gyldigFraOgMed) ? 1 : -1))
          .map((adresse, index) => (
            <Adresse adresse={adresse} key={index} soeknadsoversikt={soeknadsoversikt ? soeknadsoversikt : false} />
          ))
      ) : (
        <div>Ingen adresser</div>
      )}
    </div>
  )
}

export const Adresse = ({ adresse, soeknadsoversikt }: { adresse: IAdresse; soeknadsoversikt: boolean }) => {
  const fra = moment(adresse.gyldigFraOgMed).format('DD.MM.YYYY')
  const til = adresse.aktiv ? 'nå' : moment(adresse.gyldigTilOgMed).format('DD.MM.YYYY')

  return (
    <>
      {soeknadsoversikt ? (
        <HistorikkElement>
          <span className="date">
            {fra} - {til}:
          </span>
          <span>
            {adresse.adresseLinje1}, {adresse.poststed}
          </span>
        </HistorikkElement>
      ) : (
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
      )}
    </>
  )
}
