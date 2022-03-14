import {
  PersonInfoWrapper,
  PersonDetailWrapper,
  PersonInfoHeader,
  StatsborgerskapWrap,
  AlderEtterlattWrap,
  AvdoedWrap,
  PersonInfoBorder,
} from '../../styled'
import { ChildIcon } from '../../../../../shared/icons/childIcon'
import { IAdresse, IPersonFraSak, PersonStatus } from '../../types'
import { format } from 'date-fns'
import { sjekkDataFraSoeknadMotPdl } from '../utils'
import { PersonInfoAdresse } from './PersonInfoAdresse'

type Props = {
  person: IPersonFraSak
}

export const PersonInfo: React.FC<Props> = ({ person }) => {
  const gjeldendeAdresse: IAdresse | undefined =
    person.adresser && person.adresser.find((adresse: IAdresse) => adresse.aktiv === true)

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
    <PersonInfoBorder>
      {hentPersonHeaderMedRolle()}
      <PersonInfoWrapper>
        <PersonDetailWrapper adresse={false}>
          <div>
            <strong>Fødselsnummer</strong>
          </div>
          {sjekkDataFraSoeknadMotPdl(person?.fnr, person?.fnrFraSoeknad)}
        </PersonDetailWrapper>
        <PersonInfoAdresse
          adresser={person.adresser}
          adresseFraSoeknadGjenlevende={person.adresseFraSoeknad}
          gjeldendeAdresse={gjeldendeAdresse}
        />
      </PersonInfoWrapper>
    </PersonInfoBorder>
  )
}
