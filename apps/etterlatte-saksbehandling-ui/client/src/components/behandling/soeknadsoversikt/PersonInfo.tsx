import { useState } from 'react'
import {
  PersonInfoWrapper,
  PersonDetailWrapper,
  PersonInfoHeader,
  StatsborgerskapWrap,
  AlderEtterlattWrap,
  AvdoedWrap,
  Historikk,
} from './styled'
import { ChildIcon } from '../../../shared/icons/childIcon'
import { IAdresse, IPersonFraSak, PersonStatus } from './types'
import { TextButton } from './TextButton'
import { format } from 'date-fns'
import { sjekkDataFraSoeknadMotPdl, sjekkAdresseGjenlevendeISoeknadMotPdl } from './utils'
import { Adressevisning } from '../felles/Adressevisning'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const [visAdresseHistorikk, setVisAdresseHistorikk] = useState(false)
  const gjeldendeAdresse: IAdresse | undefined =
    person.adresser && person.adresser.find((adresse) => adresse.aktiv === true)

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
      {hentPersonHeaderMedRolle()}
      <div className="personWrapper">
        <PersonDetailWrapper adresse={false}>
          <div>
            <strong>Fødselsnummer</strong>
          </div>
          {sjekkDataFraSoeknadMotPdl(person?.fnr, person?.fnrFraSoeknad)}
        </PersonDetailWrapper>
        <PersonDetailWrapper adresse={true}>
          <div>
            <strong> {person.personStatus === PersonStatus.AVDOED ? 'Adresse dødsfallstidspunkt' : 'Adresse'}</strong>
          </div>
          {gjeldendeAdresse ? (
            person.personStatus === PersonStatus.GJENLEVENDE_FORELDER && person.adresseFraSoeknad ? (
              sjekkDataFraSoeknadMotPdl(
                `${gjeldendeAdresse.adresseLinje1}, ${gjeldendeAdresse.postnr} ${gjeldendeAdresse.poststed}`,
                person?.adresseFraSoeknad
              )
            ) : (
              <span>
                {gjeldendeAdresse.adresseLinje1}, {gjeldendeAdresse.postnr} {gjeldendeAdresse.poststed}
              </span>
            )
          ) : (
            <span>Ingen adresse registrert</span>
          )}

          {person.adresser && (
            <Historikk>
              <TextButton isOpen={visAdresseHistorikk} setIsOpen={setVisAdresseHistorikk} />
              {visAdresseHistorikk && <Adressevisning adresser={person.adresser} soeknadsoversikt={true} />}
            </Historikk>
          )}
        </PersonDetailWrapper>
        <div className="alertWrapper">
          {person.personStatus === PersonStatus.GJENLEVENDE_FORELDER &&
            person.adresseFraSoeknad &&
            gjeldendeAdresse &&
            sjekkAdresseGjenlevendeISoeknadMotPdl(
              person.adresseFraSoeknad,
              `${gjeldendeAdresse.adresseLinje1}, ${gjeldendeAdresse.postnr} ${gjeldendeAdresse.poststed}`
            )}
        </div>
      </div>
    </PersonInfoWrapper>
  )
}
