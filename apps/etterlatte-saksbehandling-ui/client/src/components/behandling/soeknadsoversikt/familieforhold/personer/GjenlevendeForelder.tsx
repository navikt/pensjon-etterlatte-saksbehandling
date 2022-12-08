import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PeopleIcon } from '~shared/icons/peopleIcon'
import { ForelderWrap } from '../../styled'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import { hentAdresserEtterDoedsdato } from '~components/behandling/felles/utils'
import { PersonStatus, RelatertPersonsRolle } from '~components/behandling/types'
import { IPdlPerson } from '~shared/types/Person'

type Props = {
  person: IPdlPerson
  innsenderErGjenlevendeForelder: boolean
  doedsdato: string
}

export const GjenlevendeForelder: React.FC<Props> = ({ person, innsenderErGjenlevendeForelder, doedsdato }) => {
  const adresserEtterDoedsdato = hentAdresserEtterDoedsdato(person.bostedsadresse ?? [], doedsdato)

  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <PeopleIcon />
        </span>
        {`${person.fornavn} ${person.etternavn}`}
        <span className="personRolle">
          ({innsenderErGjenlevendeForelder && PersonStatus.GJENLEVENDE_FORELDER + ' ' + RelatertPersonsRolle.FORELDER})
        </span>
        <br />
        <ForelderWrap>Innsender av søknad</ForelderWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.foedselsnummer} />
        <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
