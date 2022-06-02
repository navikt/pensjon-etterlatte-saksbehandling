import { RelatertPersonsRolle } from '../../../types'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { ChildIcon } from '../../../../../shared/icons/childIcon'
import { TypeStatusWrap } from '../../styled'
import { IPersoninfoSoeker, IVilkaarsproving } from '../../../../../store/reducers/BehandlingReducer'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'

type Props = {
  person: IPersoninfoSoeker
  barnOgGjenlevendeSammeAdresse: IVilkaarsproving | undefined
  alderVedDoedsdato: string
}

export const Barn: React.FC<Props> = ({ person, barnOgGjenlevendeSammeAdresse, alderVedDoedsdato }) => {
  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <ChildIcon />
        </span>
        {person.navn} <span className="personRolle">({RelatertPersonsRolle.BARN})</span>
        <TypeStatusWrap type="barn">{alderVedDoedsdato} år</TypeStatusWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.fnr} />
        <PersonInfoAdresse adresser={person.adresser} visHistorikk={true} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
