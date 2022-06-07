import { IPersoninfoAvdoed } from '../../../../../store/reducers/BehandlingReducer'
import { PersonStatus, RelatertPersonsRolle } from '../../../types'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'
import { ForelderWrap } from '../../styled'
import { format } from 'date-fns'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'

type Props = {
  person: IPersoninfoAvdoed
}

export const AvdoedForelder: React.FC<Props> = ({ person }) => {
  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <PeopleIcon />
        </span>
        {person.navn}
        <span className="personRolle">
          ({PersonStatus.AVDOED} {RelatertPersonsRolle.FORELDER})
        </span>
        <ForelderWrap avdoed={true}>Død {format(new Date(person.doedsdato), 'dd.MM.yyyy')}</ForelderWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.fnr} />
        <PersonInfoAdresse adresser={person.bostedadresser} visHistorikk={false} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
