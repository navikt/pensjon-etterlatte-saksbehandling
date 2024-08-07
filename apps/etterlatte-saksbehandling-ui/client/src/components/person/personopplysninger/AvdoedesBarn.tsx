import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { Heading, HStack, Table } from '@navikt/ds-react'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { Familiemedlem } from '~shared/types/familieOpplysninger'

export const AvdoedesBarn = ({ avdoede }: { avdoede?: Familiemedlem[] }): ReactNode => {
  if (!avdoede?.length) {
    return (
      <Table.Row>
        <Table.DataCell colSpan={3}>
          <Heading size="small">Ingen avdøde</Heading>
        </Table.DataCell>
      </Table.Row>
    )
  }

  const barn = avdoede?.flatMap(({ barn }) => barn)?.filter((v) => !!v) || []
  const fnr = barn?.map(({ foedselsnummer }) => foedselsnummer) || []
  const unikeBarn = barn?.filter(({ foedselsnummer }, i) => !fnr.includes(foedselsnummer, i + 1))

  console.log('unikeFnr', fnr)
  console.log('unikeBarn', unikeBarn)

  // const alleBarn = avdoede?.map((a) => a.barn)

  return (
    <Personopplysning heading="Søsken (avdødes barn)" icon={<ChildEyesIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Bostedsadresse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!unikeBarn?.length ? (
            unikeBarn.map((barn, index) => (
              <Table.Row key={index}>
                <Table.DataCell>
                  {barn.fornavn} {barn.etternavn}
                </Table.DataCell>
                <Table.DataCell>
                  <HStack gap="4">
                    <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
                    {!!barn.foedselsdato && <AlderTag foedselsdato={barn.foedselsdato} />}
                  </HStack>
                </Table.DataCell>
                <BostedsadresseDataCell bostedsadresse={barn.bostedsadresse} index={0} />
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={3}>
                <Heading size="small">Ingen barn</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
