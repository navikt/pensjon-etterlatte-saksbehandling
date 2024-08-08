import { ChildEyesIcon } from '@navikt/aksel-icons'
import { CopyButton, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { Familieforhold, hentLevendeSoeskenFraAvdoedeForSoekerGrunnlag, IPdlPerson } from '~shared/types/Person'
import styled from 'styled-components'
import { IAdresse } from '~shared/types/IAdresse'
import { formaterDato, formaterKanskjeStringDato } from '~utils/formatering/dato'
import { formaterFnr } from '~utils/formatering/formatering'
import { IconSize } from '~shared/types/Icon'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { PersonLink } from '~components/person/PersonLink'

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
  const alder = hentAlderForDato(barn.foedselsdato)

  const aktivAdresse: IAdresse | undefined = barn.bostedsadresse?.find((adresse: IAdresse) => adresse.aktiv)
  const adresse = `${aktivAdresse?.adresseLinje1}, ${aktivAdresse?.postnr ?? ''} ${aktivAdresse?.poststed ?? ''}`
  const periode = aktivAdresse
    ? `${formaterKanskjeStringDato(aktivAdresse.gyldigFraOgMed)} - ${
        aktivAdresse?.gyldigTilOgMed ? formaterDato(aktivAdresse!!.gyldigTilOgMed!!) : 'nå'
      }`
    : 'Mangler adresse'

  const barnetsFnr = barn.foedselsnummer
  const erGjenlevendesBarn =
    familieforhold.gjenlevende.flatMap((it) => it.opplysning.familieRelasjon?.barn).includes(barnetsFnr) ?? false

  return (
    <Table.Row>
      <Table.DataCell>
        {barn.fornavn} {barn.etternavn} ({alder} år)
      </Table.DataCell>
      <Table.DataCell>
        <FnrWrapper>
          <PersonLink fnr={barn.foedselsnummer} target="_blank" rel="noreferrer noopener">
            {formaterFnr(barn.foedselsnummer)}
          </PersonLink>
          <CopyButton copyText={barn.foedselsnummer} size="small" />
        </FnrWrapper>
      </Table.DataCell>
      <Table.DataCell>{adresse}</Table.DataCell>
      <Table.DataCell>{periode}</Table.DataCell>
      <Table.DataCell>{erGjenlevendesBarn ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
    </Table.Row>
  )
}
