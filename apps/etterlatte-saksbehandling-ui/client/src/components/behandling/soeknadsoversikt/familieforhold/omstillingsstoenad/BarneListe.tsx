import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { Familieforhold, IPdlPerson } from '~shared/types/Person'
import { IAdresse } from '~shared/types/IAdresse'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { IconSize } from '~shared/types/Icon'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'

export const BarneListe = ({ familieforhold }: { familieforhold: Familieforhold }) => {
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
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
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
              <Table.DataCell colSpan={6} aria-colspan={6}>
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
  // Søker er alltid gjenlevende for OMS
  const erGjenlevendesBarn =
    familieforhold.soeker?.opplysning.familieRelasjon?.barn?.includes(barn.foedselsnummer) ?? false

  if (!!barn.doedsdato) {
    return (
      <Table.Row>
        <Table.DataCell>
          {barn.fornavn} {barn.etternavn} <DoedsdatoTag doedsdato={barn.doedsdato} />
        </Table.DataCell>
        <Table.DataCell>
          <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
        </Table.DataCell>
        <Table.DataCell>-</Table.DataCell>
        <Table.DataCell>-</Table.DataCell>
        <Table.DataCell>-</Table.DataCell>
        <Table.DataCell>{erGjenlevendesBarn ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
      </Table.Row>
    )
  }

  const aktivAdresse: IAdresse | undefined = barn.bostedsadresse?.find((adresse: IAdresse) => adresse.aktiv)
  const adresse = `${aktivAdresse?.adresseLinje1}, ${aktivAdresse?.postnr ?? ''} ${aktivAdresse?.poststed ?? ''}`

  return (
    <Table.Row>
      <Table.DataCell>
        {barn.fornavn} {barn.etternavn} ({hentAlderForDato(barn.foedselsdato)} år)
      </Table.DataCell>
      <Table.DataCell>
        <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
      </Table.DataCell>
      <Table.DataCell>{adresse}</Table.DataCell>
      <Table.DataCell>
        {!!aktivAdresse ? formaterDatoMedFallback(aktivAdresse.gyldigFraOgMed, '-') : 'Mangler adresse'}
      </Table.DataCell>
      <Table.DataCell>
        {!!aktivAdresse ? formaterDatoMedFallback(aktivAdresse.gyldigTilOgMed, '-') : 'Mangler adresse'}
      </Table.DataCell>
      <Table.DataCell>{erGjenlevendesBarn ? 'Gjenlevende og avdød' : 'Kun avdød'}</Table.DataCell>
    </Table.Row>
  )
}
