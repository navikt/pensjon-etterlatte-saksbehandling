import { useState } from 'react'
import { TextButton } from './TextButton'
import { Adressevisning } from '~components/behandling/felles/Adressevisning'
import { PersonDetailWrapper, Historikk } from '~components/behandling/soeknadsoversikt/styled'
import { IAdresse } from '~shared/types/IAdresse'
import { Label } from '@navikt/ds-react'

type Props = {
  adresser: Readonly<IAdresse[]> | undefined
  visHistorikk: boolean
  adresseDoedstidspunkt: boolean
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
      <Label as={'p'}>{props.adresseDoedstidspunkt ? 'Bostedadresse dødsfallstidspunkt' : 'Bostedadresse'}</Label>
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
