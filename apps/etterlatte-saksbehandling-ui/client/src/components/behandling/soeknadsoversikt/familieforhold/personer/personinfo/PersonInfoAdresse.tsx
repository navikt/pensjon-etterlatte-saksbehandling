import { useState } from 'react'
import { PersonDetailWrapper, Historikk } from '../../../styled'
import { IAdresse } from '../../../../types'
import { TextButton } from './TextButton'
import { Adressevisning } from '../../../../felles/Adressevisning'
import { IAdresser } from '../../../../../../store/reducers/BehandlingReducer'

type Props = {
  adresser: IAdresser | undefined
  visHistorikk: boolean
}

export const PersonInfoAdresse: React.FC<Props> = ({ adresser, visHistorikk }) => {
  const [visAdresseHistorikk, setVisAdresseHistorikk] = useState(false)

  const gjeldendeAdresse: IAdresse | undefined = adresser?.bostedadresse?.find((adresse: IAdresse) => adresse.aktiv)

  return (
    <PersonDetailWrapper adresse={true}>
      <div>
        <strong>{visHistorikk ? 'Bostedadresse' : 'Bostedadresse dødsfallstidspunkt'}</strong>
      </div>
      {gjeldendeAdresse ? (
        <span>
          {gjeldendeAdresse.adresseLinje1}, {gjeldendeAdresse.postnr} {gjeldendeAdresse.poststed}
        </span>
      ) : (
        <span>Ingen bostedadresse</span>
      )}

      {adresser && visHistorikk && (
        <Historikk>
          <TextButton isOpen={visAdresseHistorikk} setIsOpen={setVisAdresseHistorikk} />
          {visAdresseHistorikk && <Adressevisning adresser={adresser.bostedadresse} soeknadsoversikt={true} />}
        </Historikk>
      )}
    </PersonDetailWrapper>
  )
}
