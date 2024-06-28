import { CopyButton, Heading, HStack, Link, Table, VStack } from '@navikt/ds-react'
import { Familieforhold, IPdlPerson } from '~shared/types/Person'
import styled from 'styled-components'
import { IAdresse } from '~shared/types/IAdresse'
import { formaterDato } from '~utils/formatering/dato'
import { IconSize } from '~shared/types/Icon'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { formaterFnr } from '~utils/formatering/formatering'

const FnrWrapper = styled.div`
  display: flex;
`

type Props = {
  familieforhold: Familieforhold
}

export const BarneListe = ({ familieforhold }: Props) => {
  const barneListe = familieforhold.avdoede.flatMap((it) => it.opplysning.avdoedesBarn ?? [])

  return (
    <VStack gap="4">
      <HStack gap="2">
        <ChildEyesIcon fontSize={IconSize.DEFAULT} />
        <Heading size="small" level="3">
          Barn
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
          {barneListe.length ? (
            barneListe.map((barn, i) => (
              <BarnRow key={i + barn.foedselsnummer} barn={barn} familieforhold={familieforhold} />
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={5} aria-colspan={5} align="center">
                Avdøde har ingen barn
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}

const BarnRow = ({ barn, familieforhold }: { barn: IPdlPerson; familieforhold: Familieforhold }) => {
  const barnetsFnr = barn.foedselsnummer

  // Søker er alltid gjenlevende for OMS
  const erGjenlevendesBarn = familieforhold.soeker?.opplysning.familieRelasjon?.barn?.includes(barnetsFnr) ?? false

  if (!!barn.doedsdato) {
    return (
      <Table.Row>
        <Table.DataCell>
          {barn.fornavn} {barn.etternavn} <DoedsdatoTag doedsdato={barn.doedsdato} />
        </Table.DataCell>
        <Table.DataCell>
          <FnrWrapper>
            <Link href={`/person/${barn.foedselsnummer}`} target="_blank" rel="noreferrer noopener">
              {formaterFnr(barn.foedselsnummer)}
            </Link>
            <CopyButton copyText={barn.foedselsnummer} size="small" />
          </FnrWrapper>
        </Table.DataCell>
        <Table.DataCell>-</Table.DataCell>
        <Table.DataCell>-</Table.DataCell>
        <Table.DataCell>{erGjenlevendesBarn ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
      </Table.Row>
    )
  }

  const alder = hentAlderForDato(barn.foedselsdato)
  const aktivAdresse: IAdresse | undefined = barn.bostedsadresse?.find((adresse: IAdresse) => adresse.aktiv)
  const adresse = `${aktivAdresse?.adresseLinje1}, ${aktivAdresse?.postnr ?? ''} ${aktivAdresse?.poststed ?? ''}`
  const periode = aktivAdresse
    ? `${formaterDato(aktivAdresse!!.gyldigFraOgMed!!)} - ${
        aktivAdresse?.gyldigTilOgMed ? formaterDato(aktivAdresse!!.gyldigTilOgMed!!) : 'nå'
      }`
    : 'Mangler adresse'

  return (
    <Table.Row>
      <Table.DataCell>
        {barn.fornavn} {barn.etternavn} ({alder} år)
      </Table.DataCell>
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
