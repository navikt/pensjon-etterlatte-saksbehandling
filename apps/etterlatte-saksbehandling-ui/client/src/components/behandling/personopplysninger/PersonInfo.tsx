import {
  PersonInfoWrapper,
  PersonDetailWrapper,
  PersonInfoHeader,
  StatsborgerskapWrap,
  AlderEtterlattWrap,
} from './styled'
import { BodyShort } from '@navikt/ds-react'
import { ChildIcon } from '../../../shared/icons/childIcon'
import { ManIcon } from '../../../shared/icons/manIcon'
import { WomanIcon } from '../../../shared/icons/womanIcon'
import { RelatertPersonsRolle, IPersonFraSak } from './types'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const hentIconOgRolle = () => {
    switch (person.rolle) {
      case RelatertPersonsRolle.BARN:
        return (
          <PersonInfoHeader>
            <ChildIcon /> {person.navn} <span className="personRolle">({person.rolle})</span>
            <AlderEtterlattWrap>{person.alderEtterlatt} år</AlderEtterlattWrap>
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
      case RelatertPersonsRolle.FAR:
        return (
          <PersonInfoHeader>
            <ManIcon /> {person.navn}
            <span className="personRolle">
              ({person.personStatus} {person.rolle})
            </span>
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
      default:
        return (
          <PersonInfoHeader>
            <WomanIcon /> {person.navn}
            <span className="personRolle">
              ({person.personStatus} {person.rolle})
            </span>
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
    }
  }

  return (
    <PersonInfoWrapper>
      {hentIconOgRolle()}
      <div className="personWrapper">
        <PersonDetailWrapper>
          <BodyShort size="small" className="bodyShortHeading">
            Fødselsnummer
          </BodyShort>
          <BodyShort size="small">{person.fnr}</BodyShort>
        </PersonDetailWrapper>
        <PersonDetailWrapper>
          <BodyShort size="small" className="bodyShortHeading">
            Adresse
          </BodyShort>
          <BodyShort size="small">{person.adressenavn}</BodyShort>
        </PersonDetailWrapper>
      </div>
    </PersonInfoWrapper>
  )
}
