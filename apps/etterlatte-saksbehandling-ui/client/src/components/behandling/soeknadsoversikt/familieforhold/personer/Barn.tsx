import { IBarnFraSak, RelatertPersonsRolle } from '../../../types'
import { PersonInfo } from './personinfo/PersonInfo'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { ChildIcon } from '../../../../../shared/icons/childIcon'
import { TypeStatusWrap } from '../../styled'

type Props = {
  person: IBarnFraSak
}

export const Barn: React.FC<Props> = ({ person }) => {
  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <ChildIcon />
        </span>
        {person.navn} <span className="personRolle">({RelatertPersonsRolle.BARN})</span>
        <TypeStatusWrap type="barn">{person.alderEtterlatt} år</TypeStatusWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfo
          fnr={person.fnr}
          fnrFraSoeknad={person.fnrFraSoeknad}
          bostedEtterDoedsdato={person.adresser}
          avdoedPerson={false}
        />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
