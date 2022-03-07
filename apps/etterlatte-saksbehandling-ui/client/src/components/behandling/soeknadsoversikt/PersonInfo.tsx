import { useState } from 'react'
import {
  PersonInfoWrapper,
  PersonDetailWrapper,
  PersonInfoHeader,
  StatsborgerskapWrap,
  AlderEtterlattWrap,
  HistorikkWrapper,
  HistorikkElement,
} from './styled'
import { BodyShort } from '@navikt/ds-react'
import { ChildIcon } from '../../../shared/icons/childIcon'
import { ManIcon } from '../../../shared/icons/manIcon'
import { WomanIcon } from '../../../shared/icons/womanIcon'
import { RelatertPersonsRolle, IPersonFraSak } from './types'
import { TextButton } from './TextButton'
import { format } from 'date-fns'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const [visLogg, setVisLogg] = useState(false)

  const adresser = [
    {
      adressenavn: 'Osloveien 12',
      postnummer: '0125',
      poststed: 'Oslo',
      gyldigFraOgMed: new Date(2015, 1, 6),
    },
    {
      adressenavn: 'Adresse-mock 2',
      postnummer: '0000',
      poststed: 'Oslo',
      gyldigFraOgMed: new Date(2010, 1, 6),
      gyldigTilOgMed: new Date(2015, 1, 5),
    },
  ]

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
          <span className="adresse">
            <BodyShort size="small">{person.adressenavn}</BodyShort>
            <TextButton isOpen={visLogg} setIsOpen={setVisLogg} />
          </span>
          <HistorikkWrapper>
            {visLogg &&
              adresser.map((element, key) => (
                <HistorikkElement key={key}>
                  <span className="date">
                    {format(new Date(element.gyldigFraOgMed), 'dd.MM.yyyy')} -{' '}
                    {element.gyldigTilOgMed && format(new Date(element.gyldigTilOgMed), 'dd.MM.yyyy') + ':'}
                  </span>
                  <span>
                    {element.adressenavn}, {element.poststed}
                  </span>
                </HistorikkElement>
              ))}
          </HistorikkWrapper>
        </PersonDetailWrapper>
      </div>
    </PersonInfoWrapper>
  )
}
