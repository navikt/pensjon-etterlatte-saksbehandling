import styled from 'styled-components'
import { GenderIcon, GenderList } from '../icons/genderIcon'
import { IPersonResult } from '~components/person/typer'
import { BodyShort, Box, HStack, Label, Link, Skeleton } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { IPdlPerson, IPdlPersonNavn } from '~shared/types/Person'

import { mapApiResult, Result } from '~shared/api/apiUtils'

export const PdlPersonStatusBar = ({ person }: { person: IPdlPerson | IPdlPersonNavn }) => (
  <StatusBar
    result={{
      status: 'success',
      data: {
        foedselsnummer: person.foedselsnummer,
        fornavn: person.fornavn,
        mellomnavn: person.mellomnavn,
        etternavn: person.etternavn,
      },
    }}
  />
)

export const StatusBar = ({ result }: { result: Result<IPersonResult> }) => {
  const gender = (fnr: string): GenderList => {
    const genderNum = Number(fnr[8])
    if (genderNum % 2 === 0) {
      return GenderList.female
    }
    return GenderList.male
  }

  return mapApiResult(
    result,
    <PersonSkeleton />,
    () => null,
    (person) => (
      <StatusbarBox>
        <HStack gap="2" align="center" justify="start">
          <GenderIcon gender={gender(person.foedselsnummer)} />
          <Label>
            <Link href={`/person/${person.foedselsnummer}`}>{genererNavn(person)}</Link>
          </Label>
          <BodyShort>|</BodyShort>
          <KopierbarVerdi value={person.foedselsnummer} />
        </HStack>
      </StatusbarBox>
    )
  )
}

const PersonSkeleton = () => (
  <StatusbarBox>
    <HStack gap="4">
      <Skeleton variant="circle" width="30px" height="30px" />
      <Skeleton variant="rounded" width="10rem" height="1rem" />
      <BodyShort>|</BodyShort>
      <Skeleton variant="rounded" width="10rem" height="1rem" />
    </HStack>
  </StatusbarBox>
)

const genererNavn = (personInfo: IPersonResult) => {
  return [personInfo.fornavn, personInfo.mellomnavn, personInfo.etternavn].join(' ')
}

const StatusbarBox = styled(Box)`
  padding: var(--a-spacing-3) 0 var(--a-spacing-3) var(--a-spacing-5);
  border-bottom: 1px solid var(--a-border-subtle);
  background: #f8f8f8;
`
