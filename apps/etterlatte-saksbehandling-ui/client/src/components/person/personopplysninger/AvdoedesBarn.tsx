import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { Heading, Table } from '@navikt/ds-react'
import { Personopplysning as PdlPersonopplysning } from '~shared/types/grunnlag'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { SpaceChildren } from '~shared/styled'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { KopierFnr } from '~components/person/personopplysninger/components/KopierFnr'

export const AvdoedesBarn = ({ avdoede }: { avdoede?: PdlPersonopplysning[] }): ReactNode => {
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
            <>
              {avdoede.map((doed) => (
                <>
                  {!!doed.opplysning.avdoedesBarn?.length ? (
                    <>
                      {doed.opplysning.avdoedesBarn.map((barn, index) => (
                        <Table.Row key={index}>
                          <Table.DataCell>
                            {barn.fornavn} {barn.etternavn}
                          </Table.DataCell>
                          <Table.DataCell>
                            <SpaceChildren direction="row">
                              <KopierFnr fnr={barn.foedselsnummer} />
                              <AlderTag foedselsdato={barn.foedselsdato} />
                            </SpaceChildren>
                          </Table.DataCell>
                          <BostedsadresseDataCell bostedsadresse={doed.opplysning.bostedsadresse} index={0} />
                        </Table.Row>
                      ))}
                    </>
                  ) : (
                    <Table.Row>
                      <Table.DataCell>
                        <Heading size="small">
                          Ingen barn for avdoed: ${doed.opplysning.fornavn} ${doed.opplysning.etternavn}
                        </Heading>
                      </Table.DataCell>
                      <Table.DataCell>-</Table.DataCell>
                      <Table.DataCell>-</Table.DataCell>
                    </Table.Row>
                  )}
                </>
              ))}
            </>
          ) : (
            <Heading size="small">Ingen avdøde</Heading>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
