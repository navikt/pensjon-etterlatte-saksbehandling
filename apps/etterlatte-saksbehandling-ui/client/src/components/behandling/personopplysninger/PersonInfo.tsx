import { PersonInfoWrapper, PersonDetailWrapper, PersonInfoHeader } from './styled'
import { Detail, BodyShort } from '@navikt/ds-react'
import { formatterDato } from '../../../utils/index'
import { ChildIcon } from '../../../shared/icons/childIcon'
import { Female } from '@navikt/ds-icons'
import { Male } from '@navikt/ds-icons'
import { RelatertPersonsRolle, IPersonFraSak, PersonStatus } from './types'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const getIcon = () => {
    switch (person.rolle) {
      case RelatertPersonsRolle.BARN:
        return (
          <PersonInfoHeader>
            <ChildIcon /> {person.navn}
          </PersonInfoHeader>
        )
      case RelatertPersonsRolle.MOR:
        return (
          <PersonInfoHeader>
            <Female />
            <span>
              {person.personStatus.toString()} {person.rolle.toString()}
            </span>
          </PersonInfoHeader>
        )
      case RelatertPersonsRolle.MEDMOR:
        return (
          <PersonInfoHeader>
            <Female /> {person.personStatus.toString()} {person.rolle.toString()}
          </PersonInfoHeader>
        )
      case RelatertPersonsRolle.FAR:
        return (
          <PersonInfoHeader>
            <Male /> {person.personStatus.toString()} {person.rolle.toString()}
          </PersonInfoHeader>
        )
    }
  }
  return (
    <PersonInfoWrapper>
      {getIcon()}
        <PersonDetailWrapper>
          <Detail size="small">{' '}</Detail>
          <Detail size="small" className="detail">Fra søknad</Detail>
        </PersonDetailWrapper>
        <PersonDetailWrapper>
          <BodyShort size="small">Fødselsnummer</BodyShort>
          <BodyShort size="small" className="bodyShort">{person.fnr}</BodyShort>
        </PersonDetailWrapper>
        <PersonDetailWrapper>
          <BodyShort size="small">Adresse</BodyShort>
          <BodyShort size="small" className="bodyShort">{person.adressenavn}</BodyShort>
        </PersonDetailWrapper>

        {person.personStatus == PersonStatus.DØD && person.datoForDoedsfall && (
          <PersonDetailWrapper>
            <BodyShort size="small">Dato for dødsfall</BodyShort>
            <BodyShort size="small" className="bodyShort">{formatterDato(person.datoForDoedsfall)}</BodyShort>
          </PersonDetailWrapper>
        )}
    </PersonInfoWrapper>
  )
}
