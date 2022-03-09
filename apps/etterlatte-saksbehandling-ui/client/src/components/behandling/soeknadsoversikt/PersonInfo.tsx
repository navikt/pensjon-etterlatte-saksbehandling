import { useState } from 'react'
import {
  PersonInfoWrapper,
  PersonDetailWrapper,
  PersonInfoHeader,
  StatsborgerskapWrap,
  AlderEtterlattWrap,
  HistorikkWrapper,
  HistorikkElement,
  Historikk,
} from './styled'
import { BodyShort } from '@navikt/ds-react'
import { ChildIcon } from '../../../shared/icons/childIcon'
import { RelatertPersonsRolle, IPersonFraSak, IAdresse } from './types'
import { TextButton } from './TextButton'
import { format } from 'date-fns'
import { sjekkDataFraSoeknadMotPdl } from './utils'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const [visAdresseHistorikk, setVisAdresseHistorikk] = useState(false)
  const gjeldendeAdresseFraPdl = person.adresser && person.adresser.find((adresse: IAdresse) => adresse.aktiv)

  const hentPersonHeaderMedRolle = () => {
    switch (person.rolle) {
      case RelatertPersonsRolle.BARN:
        return (
          <PersonInfoHeader>
            <ChildIcon />
            {person.navn} <span className="personRolle">({person.rolle})</span>
            <AlderEtterlattWrap>{person.alderEtterlatt} år</AlderEtterlattWrap>
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
      case RelatertPersonsRolle.FORELDER:
        return (
          <PersonInfoHeader>
            {person.navn}
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
      {hentPersonHeaderMedRolle()}
      <div className="personWrapper">
        <PersonDetailWrapper>
          <BodyShort size="small" className="bodyShortHeading">
            Fødselsnummer
          </BodyShort>
          <BodyShort size="small">{sjekkDataFraSoeknadMotPdl(person?.fnr, person?.fnrFraSoeknad)}</BodyShort>
        </PersonDetailWrapper>
        <PersonDetailWrapper>
          <BodyShort size="small" className="bodyShortHeading">
            Adresse
          </BodyShort>
          <span className="adresse">
            <BodyShort size="small">
              {gjeldendeAdresseFraPdl &&
                sjekkDataFraSoeknadMotPdl(gjeldendeAdresseFraPdl.adresseLinje1, person?.adresseFraSoeknad)}
            </BodyShort>

            <Historikk>
              <TextButton isOpen={visAdresseHistorikk} setIsOpen={setVisAdresseHistorikk} />
              <HistorikkWrapper>
                {visAdresseHistorikk &&
                  person.adresser.map((adresse, key) => (
                    <HistorikkElement key={key}>
                      <span className="date">
                        {format(new Date(adresse.gyldigFraOgMed), 'dd.MM.yyyy')} -{' '}
                        {adresse.gyldigTilOgMed && format(new Date(adresse.gyldigTilOgMed), 'dd.MM.yyyy') + ':'}
                      </span>
                      <span>
                        {adresse.adresseLinje1}, {adresse.poststed}
                      </span>
                    </HistorikkElement>
                  ))}
              </HistorikkWrapper>
            </Historikk>
          </span>
        </PersonDetailWrapper>
      </div>
    </PersonInfoWrapper>
  )
}
