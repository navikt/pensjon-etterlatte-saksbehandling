import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { Heading, Table } from '@navikt/ds-react'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { SpaceChildren } from '~shared/styled'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { PersonopplysningPerson } from '~shared/api/pdltjenester'

export const AvdoedesBarn = ({ avdoede }: { avdoede?: PersonopplysningPerson[] }): ReactNode => {
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
          {!!avdoede?.length ? (
            avdoede.map((doed, i) =>
              !!doed.avdoedesBarn?.length ? (
                doed.avdoedesBarn.map((barn, index) => (
                  <Table.Row key={index}>
                    <Table.DataCell>
                      {barn.fornavn} {barn.etternavn}
                    </Table.DataCell>
                    <Table.DataCell>
                      <SpaceChildren direction="row">
                        <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
                        {!!barn.foedselsdato && <AlderTag foedselsdato={barn.foedselsdato} />}
                      </SpaceChildren>
                    </Table.DataCell>
                    <BostedsadresseDataCell bostedsadresse={doed.bostedsadresse} index={0} />
                  </Table.Row>
                ))
              ) : (
                <Table.Row key={i}>
                  <Table.DataCell colSpan={3}>
                    <Heading size="small">
                      Ingen barn for avdoed: {doed.fornavn} {doed.etternavn}
                    </Heading>
                  </Table.DataCell>
                </Table.Row>
              )
            )
          ) : (
            <Heading size="small">Ingen avdøde</Heading>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
