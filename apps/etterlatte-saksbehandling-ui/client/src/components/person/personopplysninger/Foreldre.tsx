import React from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PersonIcon } from '@navikt/aksel-icons'
import { Heading, HStack, Table, Tag } from '@navikt/ds-react'
import { formaterDato } from '~utils/formattering'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { Familiemedlem } from '~shared/types/familieOpplysninger'

export const Foreldre = ({
  avdoed,
  gjenlevende,
  foreldreansvar,
}: {
  avdoed?: Familiemedlem[]
  gjenlevende?: Familiemedlem[]
  foreldreansvar?: string[]
}) => {
  const harForeldreansvar = (fnr: string): boolean => {
    return !!foreldreansvar && foreldreansvar.includes(fnr)
  }

  return (
    <Personopplysning heading="Foreldre" icon={<PersonIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Navn</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Bostedsadresse</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Foreldreansvar</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avdoed?.length ? (
            <>
              {avdoed.map((doed: Familiemedlem, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    <HStack gap="4">
                      {`${doed.fornavn} ${doed.etternavn}`}
                      {!!doed.doedsdato && (
                        <Tag variant="error-filled" size="small">
                          Død {formaterDato(doed.doedsdato)}
                        </Tag>
                      )}
                    </HStack>
                  </Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="4">
                      <KopierbarVerdi value={doed.foedselsnummer} iconPosition="right" />
                      {!!doed.foedselsdato && <AlderTag foedselsdato={doed.foedselsdato} />}
                    </HStack>
                  </Table.DataCell>
                  <BostedsadresseDataCell bostedsadresse={doed.bostedsadresse} index={0} />
                  <Table.DataCell>-</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={4}>
                <Heading size="small">Ingen avdøde</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
          {!!gjenlevende?.length ? (
            <>
              {gjenlevende.map((levende: Familiemedlem, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    <HStack gap="4">
                      {levende.fornavn} {levende.etternavn}
                    </HStack>
                  </Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="4">
                      <KopierbarVerdi value={levende.foedselsnummer} iconPosition="right" />
                      {!!levende.foedselsdato && <AlderTag foedselsdato={levende.foedselsdato} />}
                    </HStack>
                  </Table.DataCell>
                  {!!levende.bostedsadresse ? (
                    <BostedsadresseDataCell bostedsadresse={levende.bostedsadresse} index={0} />
                  ) : (
                    <Table.DataCell>Ingen bostedsadresse tilgjengelig</Table.DataCell>
                  )}
                  <Table.DataCell>{harForeldreansvar(levende.foedselsnummer) ? 'Ja' : 'Nei'}</Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={4}>
                <Heading size="small">Ingen gjenlevende</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
