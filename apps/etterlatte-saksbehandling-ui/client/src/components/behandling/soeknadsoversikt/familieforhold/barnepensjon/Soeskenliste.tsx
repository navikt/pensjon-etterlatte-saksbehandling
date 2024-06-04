import { ChildEyesIcon } from '@navikt/aksel-icons'
import { CopyButton, Heading, HStack, Link, Table, VStack } from '@navikt/ds-react'
import { Familieforhold, hentLevendeSoeskenFraAvdoedeForSoekerGrunnlag, IPdlPerson } from '~shared/types/Person'
import styled from 'styled-components'
import { IAdresse } from '~shared/types/IAdresse'
import { differenceInYears, format, parse } from 'date-fns'
import { DatoFormat, formaterFnr } from '~utils/formattering'
import { IconSize } from '~shared/types/Icon'

const FnrWrapper = styled.div`
  display: flex;
`

type Props = {
  familieforhold: Familieforhold
  soekerFnr: string
}

export const Soeskenliste = ({ familieforhold, soekerFnr }: Props) => {
  const barneListeIngenDoedeSoesken = hentLevendeSoeskenFraAvdoedeForSoekerGrunnlag(familieforhold.avdoede, soekerFnr)
  return (
    <VStack gap="4">
      <HStack gap="2">
        <ChildEyesIcon fontSize={IconSize.DEFAULT} />
        <Heading size="small" level="3">
          Avdødes barn (søsken)
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
            <Table.HeaderCell scope="col">Foreldre</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {barneListeIngenDoedeSoesken.length ? (
            barneListeIngenDoedeSoesken.map((barn, i) => {
              return <BarnRow key={i + barn.foedselsnummer} barn={barn} familieforhold={familieforhold} />
            })
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={5} aria-colspan={5} align="center">
                Avdøde har ingen andre barn enn søker
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}

const BarnRow = ({ barn, familieforhold }: { barn: IPdlPerson; familieforhold: Familieforhold }) => {
  const foedselsdato = parse(String(barn.foedselsdato), DatoFormat.AAR_MAANED_DAG, new Date())
  const alder = differenceInYears(new Date(), foedselsdato)

  const navnMedAlder = `${barn.fornavn} ${barn.etternavn} (${alder} år)`

  const aktivAdresse: IAdresse | undefined = barn.bostedsadresse?.find((adresse: IAdresse) => adresse.aktiv)
  const adresse = `${aktivAdresse?.adresseLinje1}, ${aktivAdresse?.postnr ?? ''} ${aktivAdresse?.poststed ?? ''}`
  const periode = aktivAdresse
    ? `${format(new Date(aktivAdresse!!.gyldigFraOgMed!!), DatoFormat.DAG_MAANED_AAR)} - ${
        aktivAdresse?.gyldigTilOgMed
          ? format(new Date(aktivAdresse!!.gyldigTilOgMed!!), DatoFormat.DAG_MAANED_AAR)
          : 'nå'
      }`
    : 'Mangler adresse'

  const barnetsFnr = barn.foedselsnummer
  const erGjenlevendesBarn =
    familieforhold.gjenlevende.flatMap((it) => it.opplysning.familieRelasjon?.barn).includes(barnetsFnr) ?? false

  return (
    <Table.Row>
      <Table.DataCell>{navnMedAlder}</Table.DataCell>
      <Table.DataCell>
        <FnrWrapper>
          <Link href={`/person/${barn.foedselsnummer}`} target="_blank" rel="noreferrer noopener">
            {formaterFnr(barn.foedselsnummer)}
          </Link>
          <CopyButton copyText={barn.foedselsnummer} size="small" />
        </FnrWrapper>
      </Table.DataCell>
      <Table.DataCell>{adresse}</Table.DataCell>
      <Table.DataCell>{periode}</Table.DataCell>
      <Table.DataCell>{erGjenlevendesBarn ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
    </Table.Row>
  )
}
