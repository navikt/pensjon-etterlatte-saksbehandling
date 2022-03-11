import { useState } from 'react'
import {
  PersonInfoWrapper,
  PersonDetailWrapper,
  PersonInfoHeader,
  StatsborgerskapWrap,
  AlderEtterlattWrap,
  AvdoedWrap,
  HistorikkWrapper,
  HistorikkElement,
  Historikk,
} from './styled'
import { ChildIcon } from '../../../shared/icons/childIcon'
import { IPersonFraSak, PersonStatus } from './types'
import { TextButton } from './TextButton'
import { format } from 'date-fns'
import { sjekkDataFraSoeknadMotPdl, sjekkAdresseGjenlevendeISoeknadMotPdl } from './utils'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const [visAdresseHistorikk, setVisAdresseHistorikk] = useState(false)
  const hentPersonHeaderMedRolle = () => {
    switch (person.personStatus) {
      case PersonStatus.ETTERLATT:
        return (
          <PersonInfoHeader>
            <ChildIcon />
            {person.navn} <span className="personRolle">({person.rolle})</span>
            <AlderEtterlattWrap>{person.alderEtterlatt} år</AlderEtterlattWrap>
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
      case PersonStatus.GJENLEVENDE_FORELDER:
        return (
          <PersonInfoHeader>
            {person.navn}
            <span className="personRolle">
              ({person.personStatus} {person.rolle})
            </span>
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
      case PersonStatus.AVDOED:
        return (
          <PersonInfoHeader>
            {person.navn}
            <span className="personRolle">
              ({person.personStatus} {person.rolle})
            </span>
            {person.datoForDoedsfall && (
              <AvdoedWrap>Død {format(new Date(person?.datoForDoedsfall), 'dd.MM.yyyy')}</AvdoedWrap>
            )}
            <StatsborgerskapWrap>{person.statsborgerskap}</StatsborgerskapWrap>
          </PersonInfoHeader>
        )
    }
  }

  return (
    <PersonInfoWrapper>
      <div>
        {hentPersonHeaderMedRolle()}
        <div className="personWrapper">
          <PersonDetailWrapper>
            <div>
              <strong>Fødselsnummer</strong>
            </div>
            {sjekkDataFraSoeknadMotPdl(person?.fnr, person?.fnrFraSoeknad)}
          </PersonDetailWrapper>
          <PersonDetailWrapper>
            <div>
              <strong> {person.personStatus === PersonStatus.AVDOED ? 'Adresse dødsfallstidspunkt' : 'Adresse'}</strong>
            </div>
            {person.personStatus === PersonStatus.GJENLEVENDE_FORELDER && person.adresseFraSoeknad ? (
              sjekkDataFraSoeknadMotPdl(person.adresseFraPdl, person?.adresseFraSoeknad)
            ) : (
              <span>{person.adresseFraPdl}</span>
            )}

            {person.adresser && (
              <Historikk>
                <TextButton isOpen={visAdresseHistorikk} setIsOpen={setVisAdresseHistorikk} />
                <HistorikkWrapper>
                  {visAdresseHistorikk &&
                    person.adresser
                      .sort((x, y) => (x.gyldigFraOgMed > y.gyldigFraOgMed ? -1 : 1))
                      .map((adresse, key) => (
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
            )}
          </PersonDetailWrapper>
        </div>
      </div>
      {person.personStatus === PersonStatus.GJENLEVENDE_FORELDER &&
        person.adresseFraSoeknad &&
        sjekkAdresseGjenlevendeISoeknadMotPdl(person.adresseFraSoeknad, person.adresseFraPdl)}
    </PersonInfoWrapper>
  )
}
