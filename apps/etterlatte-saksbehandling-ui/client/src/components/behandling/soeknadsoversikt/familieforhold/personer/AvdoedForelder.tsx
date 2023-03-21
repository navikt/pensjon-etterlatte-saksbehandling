import { IPdlPerson } from '~shared/types/Person'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { People } from '@navikt/ds-icons'
import { format } from 'date-fns'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import { PersonStatus, RelatertPersonsRolle } from '~components/behandling/types'
import { Detail } from '@navikt/ds-react'

type Props = {
  person: IPdlPerson
}

export const AvdoedForelder: React.FC<Props> = ({ person }) => {
  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <People />
        </span>
        {`${person.fornavn} ${person.etternavn}`}
        <span className="personRolle">
          ({PersonStatus.AVDOED} {RelatertPersonsRolle.FORELDER})
        </span>
        <Detail>Død {format(new Date(person.doedsdato), 'dd.MM.yyyy')}</Detail>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.foedselsnummer} />
        <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={true} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
