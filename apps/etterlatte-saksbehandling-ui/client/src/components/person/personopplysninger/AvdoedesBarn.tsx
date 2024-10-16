import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { Heading, HStack, Table } from '@navikt/ds-react'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { Familiemedlem } from '~shared/types/familieOpplysninger'
import { SakType } from '~shared/types/sak'

export const AvdoedesBarn = ({ sakType, avdoede }: { sakType: SakType; avdoede?: Familiemedlem[] }): ReactNode => {
  const unikeBarn = avdoede
    ?.flatMap((a) => a.barn)
    .filter((b) => !!b)
    // Fjerne duplikater – ved å sjekke på indeks vil vi få match på første tilfelle, slik at duplikater hoppes over
    .filter((b, index, arr) => index === arr.findIndex((t) => t?.foedselsnummer === b.foedselsnummer))

  const opprettHeading = (): string => {
    switch (sakType) {
      case SakType.BARNEPENSJON:
        return 'Søsken (avdødes barn)'
      case SakType.OMSTILLINGSSTOENAD:
        return 'Barn (avdødes barn)'
    }
  }

  return (
    <Personopplysning heading={opprettHeading()} icon={<ChildEyesIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Bostedsadresse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avdoede?.length ? (
            !!unikeBarn?.length ? (
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
            )
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={3}>
                <Heading size="small">Ingen avdøde</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
