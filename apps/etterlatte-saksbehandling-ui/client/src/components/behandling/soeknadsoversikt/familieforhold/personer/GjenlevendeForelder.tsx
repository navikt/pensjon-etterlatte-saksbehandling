import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PeopleIcon } from '../../../../../shared/icons/peopleIcon'
import { ForelderWrap } from '../../styled'
import { PersonStatus, RelatertPersonsRolle } from '../../../types'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import { IPersoninfoGjenlevendeForelder } from '../../../../../store/reducers/BehandlingReducer'
import { hentAdresserEtterDoedsdato } from '../../../felles/utils'

type Props = {
  person: IPersoninfoGjenlevendeForelder
  innsenderErGjenlevendeForelder: boolean
  doedsdato: string
}

export const GjenlevendeForelder: React.FC<Props> = ({ person, innsenderErGjenlevendeForelder, doedsdato }) => {
  const adresserEtterDoedsdato = hentAdresserEtterDoedsdato(person.bostedadresser, doedsdato)

  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <PeopleIcon />
        </span>
        {person.navn}
        <span className="personRolle">
          {innsenderErGjenlevendeForelder && PersonStatus.GJENLEVENDE_FORELDER + RelatertPersonsRolle.FORELDER}
        </span>
        {<ForelderWrap>Innsender av søknad</ForelderWrap>}
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.fnr} />
        <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
