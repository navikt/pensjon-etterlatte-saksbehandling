import React from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PersonIcon } from '@navikt/aksel-icons'
import { Heading, Table, Tag } from '@navikt/ds-react'
import { SpaceChildren } from '~shared/styled'
import { formaterStringDato } from '~utils/formattering'
import { AlderTag } from '~components/person/personopplysninger/components/AlderTag'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { IPdlPerson } from '~shared/types/Person'

export const Foreldre = ({
  avdoed,
  gjenlevende,
  foreldreansvar,
}: {
  avdoed?: IPdlPerson[]
  gjenlevende?: IPdlPerson[]
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
              {avdoed.map((doed: IPdlPerson, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      {`${doed.fornavn} ${doed.etternavn}`}
                      {!!doed.doedsdato && (
                        <Tag variant="error-filled" size="small">
                          Død {formaterStringDato(doed.doedsdato)}
                        </Tag>
                      )}
                    </SpaceChildren>
                  </Table.DataCell>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      <KopierbarVerdi value={doed.foedselsnummer} iconPosition="right" />
                      <AlderTag foedselsdato={doed.foedselsdato} />
                    </SpaceChildren>
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
              {gjenlevende.map((levende: IPdlPerson, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      {levende.fornavn} {levende.etternavn}
                    </SpaceChildren>
                  </Table.DataCell>
                  <Table.DataCell>
                    <SpaceChildren direction="row">
                      <KopierbarVerdi value={levende.foedselsnummer} iconPosition="right" />
                      <AlderTag foedselsdato={levende.foedselsdato} />
                    </SpaceChildren>
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
