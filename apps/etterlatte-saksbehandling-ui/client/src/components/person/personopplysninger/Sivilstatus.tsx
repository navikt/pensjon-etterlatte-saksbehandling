import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HeartIcon } from '@navikt/aksel-icons'
import { Heading, HStack, Table } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { lowerCase, startCase } from 'lodash'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { Familiemedlem, Sivilstand } from '~shared/types/familieOpplysninger'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'

export const Sivilstatus = ({
  sivilstand,
  avdoede,
}: {
  sivilstand?: Sivilstand[]
  avdoede?: Familiemedlem[]
}): ReactNode => {
  const relatertAvdoed = (relatertVedSiviltilstand: string, avdoede: Familiemedlem[]): Familiemedlem | undefined =>
    avdoede.find((val) => val.foedselsnummer === relatertVedSiviltilstand)

  return (
    <Personopplysning heading="Sivilstatus" icon={<HeartIcon />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Status</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Dato</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fødselsnummer</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!sivilstand?.length ? (
            <>
              {sivilstand.map((stand: Sivilstand, index: number) => (
                <Table.Row key={index}>
                  <Table.DataCell>{startCase(lowerCase(stand.sivilstatus))}</Table.DataCell>
                  <Table.DataCell>{!!stand.gyldigFraOgMed && formaterDato(stand.gyldigFraOgMed)}</Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="4">
                      {!!stand.relatertVedSivilstand && (
                        <>
                          <KopierbarVerdi value={stand.relatertVedSivilstand} iconPosition="right" />
                          {avdoede && avdoede.length >= 0 && (
                            <DoedsdatoTag doedsdato={relatertAvdoed(stand.relatertVedSivilstand, avdoede)?.doedsdato} />
                          )}
                        </>
                      )}
                    </HStack>
                  </Table.DataCell>
                </Table.Row>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={3}>
                <Heading size="small">Ingen sivilstatuser</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
