import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { ChildEyesIcon } from '@navikt/aksel-icons'
import { CopyButton, Heading, Table } from '@navikt/ds-react'
import { Personopplysning as PdlPersonopplysning } from '~shared/types/grunnlag'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { SpaceChildren } from '~shared/styled'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'

export const AvdoedesBarn = ({ avdoede }: { avdoede?: PdlPersonopplysning[] }): ReactNode => {
  return (
    <Personopplysning heading="Søsken (avdødes barn)" icon={<ChildEyesIcon height="2rem" width="2rem" />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Bostedsadresse</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {avdoede && avdoede.length > 0 ? (
            <>
              {avdoede.map((doed) => (
                <>
                  {doed.opplysning.avdoedesBarn && doed.opplysning.avdoedesBarn.length > 0 ? (
                    <>
                      {doed.opplysning.avdoedesBarn.map((barn, index) => (
                        <Table.Row key={index}>
                          <Table.DataCell>
                            {barn.fornavn} {barn.etternavn}
                          </Table.DataCell>
                          <Table.DataCell>
                            <SpaceChildren direction="row">
                              <CopyButton
                                copyText={barn.foedselsnummer}
                                text={barn.foedselsnummer}
                                size="small"
                                iconPosition="right"
                              />
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
                    </Table.Row>
                  )}
                </>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell>
                <Heading size="small">Ingen avdøde</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
