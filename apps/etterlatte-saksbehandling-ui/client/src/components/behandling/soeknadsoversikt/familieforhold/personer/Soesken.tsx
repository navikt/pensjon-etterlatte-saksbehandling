import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { PersonInfoAdresse } from './personinfo/PersonInfoAdresse'
import React from 'react'
import differenceInYears from 'date-fns/differenceInYears'
import { hentAdresserEtterDoedsdato } from '~components/behandling/felles/utils'
import { IFamilieforhold, IPdlPerson } from '~shared/types/Person'

export const Soesken = ({ person, familieforhold }: { person: IPdlPerson; familieforhold: IFamilieforhold }) => {
  const erHelsoesken = (fnr: string) => familieforhold.gjenlevende?.opplysning.familieRelasjon?.barn?.includes(fnr)

  return (
    <PersonBorder key={person.foedselsnummer}>
      <PersonHeader>
        <div>
          <span className="icon">
            <ChildEyesIcon />
          </span>
          {`${person.fornavn} ${person.etternavn} `}
          <span className={'personRolle'}>({differenceInYears(new Date(), new Date(person.foedselsdato))} år)</span>
        </div>
        <span className={'personInfo'}>{erHelsoesken(person.foedselsnummer) ? 'Helsøsken' : 'Halvsøsken'}</span>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.foedselsnummer} />
        <PersonInfoAdresse
          adresser={hentAdresserEtterDoedsdato(
            person.bostedsadresse!!,
            familieforhold.avdoede.opplysning.doedsdato.toString()
          )}
          visHistorikk={true}
          adresseDoedstidspunkt={false}
        />
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
