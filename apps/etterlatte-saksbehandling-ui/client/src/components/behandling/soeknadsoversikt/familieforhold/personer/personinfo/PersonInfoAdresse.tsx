import { useState } from 'react'
import { PersonDetailWrapper, Historikk } from '../../../styled'
import { TextButton } from './TextButton'
import { Adressevisning } from '../../../../felles/Adressevisning'
import { IAdresse } from '../../../../../../store/reducers/BehandlingReducer'

type Props = {
  adresser: Readonly<IAdresse[]> | undefined
  visHistorikk: boolean
}

export const PersonInfoAdresse: React.FC<Props> = (props) => {
  const [visAdresseHistorikk, setVisAdresseHistorikk] = useState(false)
  const adresser = props.adresser ? [...props.adresser] : []

  const aktivAdresse: IAdresse | undefined = adresser?.find((adresse: IAdresse) => adresse.aktiv)
  let sisteEllerAktivAdresse

  if (aktivAdresse == undefined) {
    sisteEllerAktivAdresse = adresser?.sort((a, b) =>
      new Date(b.gyldigFraOgMed!) > new Date(a.gyldigFraOgMed!) ? 1 : -1
    )[0]
  } else {
    sisteEllerAktivAdresse = aktivAdresse
  }

  return (
    <PersonDetailWrapper adresse={true}>
      <div>
        <strong>{props.visHistorikk ? 'Bostedadresse' : 'Bostedadresse dødsfallstidspunkt'}</strong>
      </div>
      {sisteEllerAktivAdresse ? (
        <span>
          {sisteEllerAktivAdresse.adresseLinje1}, {sisteEllerAktivAdresse.postnr} {sisteEllerAktivAdresse.poststed}
        </span>
      ) : (
        <span>Ingen bostedadresse</span>
      )}

      {adresser && props.visHistorikk && (
        <Historikk>
          <TextButton isOpen={visAdresseHistorikk} setIsOpen={setVisAdresseHistorikk} />
          {visAdresseHistorikk && <Adressevisning adresser={adresser} soeknadsoversikt={true} />}
        </Historikk>
      )}
    </PersonDetailWrapper>
  )
}
