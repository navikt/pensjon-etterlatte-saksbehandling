import styled from 'styled-components'
import { GenderIcon, GenderList } from '../icons/genderIcon'
import { IPersonResult } from '~components/person/typer'
import { BodyShort, Box, HelpText, HStack, Label, Link, Skeleton } from '@navikt/ds-react'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { IPdlPersonNavnFoedsel } from '~shared/types/Person'
import { mapApiResult, Result } from '~shared/api/apiUtils'
import { useEffect } from 'react'
import { hentPersonNavnogFoedsel } from '~shared/api/pdltjenester'
import { useApiCall } from '~shared/hooks/useApiCall'
import { differenceInYears, parse } from 'date-fns'
import { DatoFormat } from '~utils/formattering'

export const PdlPersonStatusBar = ({ person }: { person: IPdlPersonNavnFoedsel }) => (
  <StatusBar
    result={{
      status: 'success',
      data: {
        foedselsnummer: person.foedselsnummer,
        fornavn: person.fornavn,
        mellomnavn: person.mellomnavn,
        etternavn: person.etternavn,
        foedselsaar: person.foedselsaar,
        foedselsdato: person.foedselsdato,
      },
    }}
  />
)

export const StatusBarPersonHenter = ({ ident }: { ident: string | null | undefined }) => {
  const [personStatus, hentPerson] = useApiCall(hentPersonNavnogFoedsel)
  useEffect(() => {
    ident && hentPerson(ident)
  }, [ident])

  return <StatusBar result={personStatus} />
}

export const StatusBar = ({ result }: { result: Result<IPdlPersonNavnFoedsel> }) => {
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
          <VisAlderForPerson foedselsdato={person.foedselsdato} foedselsaar={person.foedselsaar} />
          <BodyShort>|</BodyShort>
          <KopierbarVerdi value={person.foedselsnummer} />
        </HStack>
      </StatusbarBox>
    )
  )
}

const VisAlderForPerson = ({ foedselsdato, foedselsaar }: { foedselsdato: Date | undefined; foedselsaar: number }) => {
  if (!foedselsdato) {
    const alder = differenceInYears(new Date(), parse(String(foedselsdato), DatoFormat.AAR_MAANED_DAG, new Date()))
    return <BodyShort textColor="subtle">({alder} år)</BodyShort>
  } else {
    return (
      <>
        <BodyShort textColor="subtle">Fødselsår: {foedselsaar}</BodyShort>
        <HelpText title="Personen mangler fødselsdato">
          Vi har ingen fødselsdato på vedkommende og kan derfor ikke vise nøyaktig alder. Fødselsår: {foedselsaar}
        </HelpText>
      </>
    )
  }
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
