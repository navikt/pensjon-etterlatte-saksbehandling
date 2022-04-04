import { PersonInfoHeaderWrapper, StatsborgerskapWrap, BarnAlderWrap, AvdoedWrap } from '../../styled'
import { ChildIcon } from '../../../../../shared/icons/childIcon'
import { IPersonFraSak, PersonStatus } from '../../../types'
import { format } from 'date-fns'
import { getStatsborgerskapTekst } from '../utils'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'

type Props = {
  person: IPersonFraSak
}

export const PersonInfoHeader: React.FC<Props> = ({ person }) => {
  switch (person.personStatus) {
    case PersonStatus.BARN:
      return (
        <PersonInfoHeaderWrapper>
          <span className="icon">
            <ChildIcon />
          </span>
          {person.navn} <span className="personRolle">({person.rolle})</span>
          <BarnAlderWrap>{person.alderEtterlatt} år</BarnAlderWrap>
          <StatsborgerskapWrap>{getStatsborgerskapTekst(person.statsborgerskap)}</StatsborgerskapWrap>
        </PersonInfoHeaderWrapper>
      )
    case PersonStatus.GJENLEVENDE_FORELDER:
      return (
        <PersonInfoHeaderWrapper>
          <span className="icon">
            <PeopleIcon />
          </span>
          {person.navn}
          <span className="personRolle">
            ({person.personStatus} {person.rolle})
          </span>
          <StatsborgerskapWrap>{getStatsborgerskapTekst(person.statsborgerskap)}</StatsborgerskapWrap>
        </PersonInfoHeaderWrapper>
      )
    case PersonStatus.AVDOED:
      return (
        <PersonInfoHeaderWrapper>
          <span className="icon">
            <PeopleIcon />
          </span>
          {person.navn}
          <span className="personRolle">
            {person.personStatus} {person.rolle})
          </span>
          <AvdoedWrap>Død {format(new Date(person.datoForDoedsfall), 'dd.MM.yyyy')}</AvdoedWrap>
          <StatsborgerskapWrap>{getStatsborgerskapTekst(person.statsborgerskap)}</StatsborgerskapWrap>
        </PersonInfoHeaderWrapper>
      )
  }
}
