import { IAdresse } from '../types'
import {format} from 'date-fns'
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
            <Adresse
              adresse={adresse}
              key={index}
              soeknadsoversikt={soeknadsoversikt ? soeknadsoversikt : false}
              index={index}
            />
          ))
      ) : (
        <div>Ingen adresser</div>
      )}
    </div>
  )
}

export const Adresse = ({
  adresse,
  soeknadsoversikt,
  index,
}: {
  adresse: IAdresse
  soeknadsoversikt: boolean
  index: number
}) => {
  const fra = format(new Date(adresse.gyldigFraOgMed), 'dd.MM.yyyy')
  const til = adresse.aktiv ? 'nå' : format(new Date(adresse.gyldigTilOgMed!), 'dd.MM.yyyy')

  const padding = index > 0 ? '5px' : '0px'
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
        <div style={{ paddingTop: padding }}>
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
