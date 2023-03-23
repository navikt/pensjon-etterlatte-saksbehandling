import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { Child } from '@navikt/ds-icons'
import { PersonDetailWrapper } from '../../styled'
import { IPdlPerson } from '~shared/types/Person'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import differenceInYears from 'date-fns/differenceInYears'
import { hentAdresserEtterDoedsdato } from '~components/behandling/felles/utils'

type Props = {
  person: IPdlPerson
  doedsdato: string
}

export const Barn: React.FC<Props> = ({ person, doedsdato }) => {
  const bostedsadresse = person.bostedsadresse ?? []
  const adresserEtterDoedsdato = hentAdresserEtterDoedsdato(bostedsadresse, doedsdato)
  const aktivAdresse = bostedsadresse.find((adresse) => adresse.aktiv)

  return (
    <PersonBorder>
      <PersonHeader>
        <span className="icon">
          <Child />
        </span>
        {`${person.fornavn} ${person.etternavn}`}{' '}
        <span className={'personRolle'}>({differenceInYears(new Date(), new Date(person.foedselsdato))} år)</span>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.foedselsnummer} />
        <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} adresseDoedstidspunkt={false} />
        {aktivAdresse && (
          <PersonDetailWrapper adresse={true}>
            <div>
              <strong>Aktiv adresse</strong>
            </div>
            <div>{aktivAdresse.adresseLinje1}</div>
            <div>{aktivAdresse.land}</div>
          </PersonDetailWrapper>
        )}
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
