import { IPdlPerson } from '~shared/types/Person'
import { PersonIcon } from '@navikt/aksel-icons'
import { format } from 'date-fns'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { CopyButton, Detail, Heading, Link } from '@navikt/ds-react'
import styled from 'styled-components'
import { DatoFormat, formaterFnr } from '~utils/formattering'
import { IconSize } from '~shared/types/Icon'
import { GrunnlagKilde } from '~shared/types/grunnlag'

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
  kilde: GrunnlagKilde
  avdoed?: boolean
  mottaker?: boolean
  gjenlevende?: boolean
}

export const Person = ({ person, kilde, avdoed = false, mottaker = false, gjenlevende = false }: Props) => {
  const getHeaderTekst = () => {
    if (avdoed) {
      return 'Avdød forelder'
    } else if (mottaker) {
      return 'Mottaker'
    } else if (gjenlevende) {
      return 'Gjenlevende forelder'
    }
  }
  return (
    <PersonBorder>
      <IconWrapper>
        <PersonIcon fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size="small" level="3">
          {getHeaderTekst()}
        </Heading>
        <div>
          {`${person.fornavn} ${person.etternavn}`}
          <FnrWrapper>
            <Link href={`/person/${person.foedselsnummer}`} target="_blank" rel="noreferrer noopener">
              ({formaterFnr(person.foedselsnummer)})
            </Link>
            <CopyButton copyText={person.foedselsnummer} size="small" />
          </FnrWrapper>
        </div>
        <div>
          <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={avdoed} />
          <Detail>{`${kilde.type.toUpperCase()} ${format(
            new Date(kilde.tidspunkt),
            DatoFormat.DAG_MAANED_AAR
          )}`}</Detail>
        </div>
      </PersonInfoWrapper>
    </PersonBorder>
  )
}

const FnrWrapper = styled.div`
  display: flex;
`
