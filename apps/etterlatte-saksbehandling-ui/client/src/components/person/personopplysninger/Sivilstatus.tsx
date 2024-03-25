import React, { ReactNode } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { HeartIcon } from '@navikt/aksel-icons'
import { Heading, Table, Tag } from '@navikt/ds-react'
import { formaterDato } from '~utils/formattering'
import { SpaceChildren } from '~shared/styled'
import styled from 'styled-components'
import { lowerCase, startCase } from 'lodash'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { PersonopplysningPerson, Sivilstand } from '~shared/api/pdltjenester'
export const Sivilstatus = ({
  sivilstand,
  avdoede,
}: {
  sivilstand?: Sivilstand[]
  avdoede?: PersonopplysningPerson[]
}): ReactNode => {
  const relatertVedSivilstandDoedsdato = (
    relatertVedSiviltilstand: string,
    avdoede: PersonopplysningPerson[]
  ): string | undefined => {
    const relaterteAvdoed = avdoede.find((val) => val.foedselsnummer === relatertVedSiviltilstand)

    if (!!relaterteAvdoed?.doedsdato) return formaterDato(relaterteAvdoed.doedsdato)
    else return undefined
  }

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
                    <SpaceChildren direction="row">
                      {!!stand.relatertVedSivilstand && (
                        <>
                          <KopierbarVerdi value={stand.relatertVedSivilstand} iconPosition="right" />
                          {avdoede && avdoede.length >= 0 && (
                            <>
                              {!!relatertVedSivilstandDoedsdato(stand.relatertVedSivilstand, avdoede) && (
                                <DoedsDatoWrapper>
                                  <Tag variant="error-filled" size="small">
                                    Død {relatertVedSivilstandDoedsdato(stand.relatertVedSivilstand, avdoede)}
                                  </Tag>
                                </DoedsDatoWrapper>
                              )}
                            </>
                          )}
                        </>
                      )}
                    </SpaceChildren>
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

const DoedsDatoWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
`
