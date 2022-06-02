import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'
import { ForelderWrap } from '../../styled'
import { PersonStatus, RelatertPersonsRolle } from '../../../types'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import { IPersoninfoGjenlevendeForelder } from '../../../../../store/reducers/BehandlingReducer'

type Props = {
  person: IPersoninfoGjenlevendeForelder
  innsenderErGjenlevendeForelder: boolean
}

export const GjenlevendeForelder: React.FC<Props> = ({ person, innsenderErGjenlevendeForelder }) => {
  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <PeopleIcon />
        </span>
        {person.navn}
        <span className="personRolle">
          ({PersonStatus.GJENLEVENDE_FORELDER} {RelatertPersonsRolle.FORELDER})
        </span>
        {innsenderErGjenlevendeForelder && <ForelderWrap>Innsender av søknad</ForelderWrap>}
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.fnr} />
        <PersonInfoAdresse adresser={person.adresser} visHistorikk={true} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
