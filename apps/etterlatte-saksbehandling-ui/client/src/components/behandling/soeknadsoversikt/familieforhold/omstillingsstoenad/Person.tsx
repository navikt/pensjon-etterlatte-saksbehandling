import { IPdlPerson } from '~shared/types/Person'
import { PersonIcon } from '@navikt/aksel-icons'
import { format } from 'date-fns'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { BodyShort, Detail, Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import { KildePdl } from '~shared/types/kilde'
import { DatoFormat, formaterFnr } from '~utils/formattering'
import { IconSize } from '~shared/types/Icon'

const PersonBorder = styled.div`
  padding: 1.2em 1em 1em 0em;
  display: flex;
`

const IconWrapper = styled.span`
  width: 2.5rem;
`

const PersonInfoWrapper = styled.div`
  display: grid;
  gap: 0.5rem;
`

type Props = {
  person: IPdlPerson
  kilde: KildePdl
  avdoed?: boolean
}

export const Person = ({ person, kilde, avdoed = false }: Props) => {
  return (
    <PersonBorder>
      <IconWrapper>
        <PersonIcon fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size={'small'} level={'3'}>
          {avdoed ? 'Avdød' : 'Gjenlevende'}
        </Heading>
        <BodyShort>
          {`${person.fornavn} ${person.etternavn}`} ({formaterFnr(person.foedselsnummer)})
        </BodyShort>
        <div>
          <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={avdoed} />
          <Detail>{`${kilde.type.toUpperCase()} ${format(
            new Date(kilde.tidspunktForInnhenting),
            DatoFormat.DAG_MAANED_AAR
          )}`}</Detail>
        </div>
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
