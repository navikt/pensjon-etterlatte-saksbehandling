import { PersonInfoFnr } from './personinfo/PersonInfoFnr'
import { PersonBorder, PersonHeader, PersonInfoWrapper } from '../styled'
import { ChildIcon } from '~shared/icons/childIcon'
import { PersonDetailWrapper, TypeStatusWrap } from '../../styled'
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
          <ChildIcon />
        </span>
        {`${person.fornavn} ${person.etternavn}`}{' '}
        <span className={'personRolle'}>({differenceInYears(new Date(), new Date(person.foedselsdato))} år)</span>
        <br />
        <TypeStatusWrap type="barn">Mottaker av pensjon</TypeStatusWrap>
      </PersonHeader>
      <PersonInfoWrapper>
        <PersonInfoFnr fnr={person.foedselsnummer} />
        <PersonInfoAdresse adresser={adresserEtterDoedsdato} visHistorikk={true} />
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
