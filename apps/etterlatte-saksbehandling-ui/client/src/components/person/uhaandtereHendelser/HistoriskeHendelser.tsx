import { Button, Heading } from '@navikt/ds-react'
import styled from 'styled-components'
import React, { useState } from 'react'
import { ChevronDownIcon, ChevronUpIcon } from '@navikt/aksel-icons'
import { Grunnlagsendringshendelse } from '~components/person/typer'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'

type Props = {
  hendelser: Array<Grunnlagsendringshendelse>
}
const HistoriskeHendelser = (props: Props) => {
  const [aapenhistorikk, setLastetBehandlingliste] = useState<boolean>(false)
  return (
    <HistoriskeHendelserWrapper>
      <Heading spacing size="medium" level="2">
        Tidligere hendelser
      </Heading>
      <div>
        <Button variant="tertiary" onClick={() => setLastetBehandlingliste(!aapenhistorikk)}>
          {aapenhistorikk ? (
            <MarginRightChevron>
              <ChevronUpIcon fontSize="1.5rem" />
            </MarginRightChevron>
          ) : (
            <MarginRightChevron>
              <ChevronDownIcon fontSize="1.5rem" />
            </MarginRightChevron>
          )}
          Vis historikk
        </Button>
        {aapenhistorikk && (
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell scope="col">Sakid</Table.HeaderCell>
                <Table.HeaderCell scope="col">Type</Table.HeaderCell>
                <Table.HeaderCell scope="col">GjelderPerson</Table.HeaderCell>
                <Table.HeaderCell scope="col">Opprettet</Table.HeaderCell>
                <Table.HeaderCell scope="col">Kommentar</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {props.hendelser.map(({ sakId, type, gjelderPerson, id, opprettet, kommentar }) => {
                return (
                  <Table.Row key={id}>
                    <Table.HeaderCell>{sakId}</Table.HeaderCell>
                    <Table.DataCell>{type}</Table.DataCell>
                    <Table.DataCell>{gjelderPerson}</Table.DataCell>
                    <Table.DataCell>{formaterStringDato(opprettet)}</Table.DataCell>
                    <Table.DataCell>{kommentar ? <p>{kommentar}</p> : <p>Ingen kommentar</p>}</Table.DataCell>
                  </Table.Row>
                )
              })}
            </Table.Body>
          </Table>
        )}
      </div>
    </HistoriskeHendelserWrapper>
  )
}

const HistoriskeHendelserWrapper = styled.div`
  margin-top: 5rem;
`

const MarginRightChevron = styled.span`
  margin-right: 5px;
`

export default HistoriskeHendelser
