import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { People } from '@navikt/ds-icons'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import { hentAdresserEtterDoedsdato } from '~components/behandling/felles/utils'
import { PersonStatus, RelatertPersonsRolle } from '~components/behandling/types'
import { IPdlPerson } from '~shared/types/Person'

type Props = {
  person: IPdlPerson
  innsenderErGjenlevendeForelder: boolean
  doedsdato: string
}

export const GjenlevendeForelder = ({ person, innsenderErGjenlevendeForelder, doedsdato }: Props) => {
  const adresserEtterDoedsdato = hentAdresserEtterDoedsdato(person.bostedsadresse ?? [], doedsdato)

  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <People />
        </span>
        {`${person.fornavn} ${person.etternavn}`}
        <span className="personRolle">
          ({innsenderErGjenlevendeForelder && PersonStatus.GJENLEVENDE_FORELDER + ' ' + RelatertPersonsRolle.FORELDER})
        </span>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.foedselsnummer} />
        <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} adresseDoedstidspunkt={false} />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
