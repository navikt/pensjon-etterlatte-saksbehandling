import { IPdlPerson } from '~shared/types/Person'
import { People } from '@navikt/ds-icons'
import { format } from 'date-fns'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { Detail, Heading, Link } from '@navikt/ds-react'
import styled from 'styled-components'
import { KildePdl } from '~shared/types/kilde'
import { DatoFormat, formaterFnr } from '~utils/formattering'
import { IconSize } from '~shared/types/Icon'
import { CopyToClipboard } from '@navikt/ds-react-internal'

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
        <People fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size={'small'} level={'3'}>
          {getHeaderTekst()}
        </Heading>
        <div>
          {`${person.fornavn} ${person.etternavn}`}
          <FnrWrapper>
            <Link href={`/person/${person.foedselsnummer}`} target="_blank" rel="noreferrer noopener">
              ({formaterFnr(person.foedselsnummer)})
            </Link>
            <CopyToClipboard
              copyText={person.foedselsnummer}
              popoverText={`Kopierte ${person.foedselsnummer}`}
              size={'small'}
            />
          </FnrWrapper>
        </div>
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

const FnrWrapper = styled.div`
  display: flex;
`
