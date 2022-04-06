import { useState } from 'react'
import { PersonDetailWrapper, Historikk } from '../../../styled'
import { IAdresse } from '../../../../types'
import { TextButton } from './TextButton'
import { sjekkDataFraSoeknadMotPdl } from '../../../utils'
import { Adressevisning } from '../../../../felles/Adressevisning'

type Props = {
  adresser: IAdresse[]
  adresseFraSoeknadGjenlevende: string | undefined
  gjeldendeAdresse: IAdresse | undefined
  avodedPerson: boolean
}

export const PersonInfoAdresse: React.FC<Props> = ({
  adresser,
  adresseFraSoeknadGjenlevende,
  gjeldendeAdresse,
  avodedPerson,
}) => {
  const [visAdresseHistorikk, setVisAdresseHistorikk] = useState(false)
  return (
    <PersonDetailWrapper adresse={true}>
      <div>
        <strong>{avodedPerson ? 'Bostedadresse dødsfallstidspunkt' : 'Bostedadresse'}</strong>
      </div>
      {gjeldendeAdresse ? (
        adresseFraSoeknadGjenlevende ? (
          sjekkDataFraSoeknadMotPdl(
            `${gjeldendeAdresse.adresseLinje1}, ${gjeldendeAdresse.postnr} ${gjeldendeAdresse.poststed}`,
            adresseFraSoeknadGjenlevende
          )
        ) : (
          <span>
            {gjeldendeAdresse.adresseLinje1}, {gjeldendeAdresse.postnr} {gjeldendeAdresse.poststed}
          </span>
        )
      ) : (
        <span>Ingen bostedadresse</span>
      )}

      {adresser && !avodedPerson && (
        <Historikk>
          <TextButton isOpen={visAdresseHistorikk} setIsOpen={setVisAdresseHistorikk} />
          {visAdresseHistorikk && <Adressevisning adresser={adresser} soeknadsoversikt={true} />}
        </Historikk>
      )}
    </PersonDetailWrapper>
  )
}
