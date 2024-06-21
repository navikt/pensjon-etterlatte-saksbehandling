import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { simulerUtbetaling } from '~shared/api/utbetaling'
import { isInitial, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import React from 'react'
import { BodyShort, Button, Heading, Table, HStack, Box } from '@navikt/ds-react'
import { SimulertBeregning, UtbetalingAggregert } from '~shared/types/Utbetaling'
import { formaterKanskjeStringDato, formaterStringDato, NOK } from '~utils/formattering'
import styled from 'styled-components'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'

export const SimulerUtbetaling = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const [simuleringStatus, simulerUtbetalingRequest] = useApiCall(simulerUtbetaling)

  const simuler = () => {
    if (isInitial(simuleringStatus)) {
      if (behandling.status === IBehandlingStatus.BEREGNET || behandling.status === IBehandlingStatus.AVKORTET) {
        simulerUtbetalingRequest(behandling.id)
      }
    }
  }

  return (
    <>
      <Box paddingBlock="12">
        <Heading spacing size="small" level="1">
          Simulere utbetaling
        </Heading>

        {!erFerdigBehandlet(behandling.status) && (
          <Button variant="secondary" size="small" onClick={simuler}>
            Simuler
          </Button>
        )}
        {mapResult(simuleringStatus, {
          pending: <Spinner visible={true} label="Simulerer..." />,
          success: (simuleringrespons) => <SimuleringBeregning data={simuleringrespons} />,
          error: () => <ApiErrorAlert>Feil ved simulering</ApiErrorAlert>,
        })}
      </Box>
    </>
  )
}

const SimuleringBeregning = ({ data }: { data: SimulertBeregning }) => {
  return (
    <>
      <TableWrapper>
        <b>Kommende utbetaling(er)</b>
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Periode</Table.HeaderCell>
              <Table.HeaderCell>Klasse</Table.HeaderCell>
              <Table.HeaderCell>Konto</Table.HeaderCell>
              <Table.HeaderCell>Forfall</Table.HeaderCell>
              <Table.HeaderCell align="right">Beløp</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {data.kommendeUtbetalinger.map((periode, idx) => (
              <Table.Row key={idx}>
                <Table.DataCell>
                  {formaterStringDato(periode.fom)} - {formaterKanskjeStringDato(periode.tom)}
                </Table.DataCell>
                <Table.DataCell>{periode.klassekodeBeskrivelse}</Table.DataCell>
                <Table.DataCell>{periode.konto}</Table.DataCell>
                <Table.DataCell>{formaterStringDato(periode.forfall)}</Table.DataCell>
                <Table.DataCell align="right">{NOK(periode.beloep)}</Table.DataCell>
              </Table.Row>
            ))}
            <Table.Row>
              <Table.DataCell colSpan={4}>
                <b>Sum utbetaling</b>
              </Table.DataCell>
              <Table.DataCell align="right">{NOK(data.beloep)}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      </TableWrapper>

      <HStack gap="12">
        {data.etterbetaling.length > 0 && <UtbetalingAggregertTable tittel="Etterbetaling" data={data.etterbetaling} />}

        {data.tilbakekreving.length > 0 && (
          <UtbetalingAggregertTable tittel="Potensiell tilbakekreving" data={data.tilbakekreving} />
        )}
      </HStack>

      <>
        Beregnet dato: {formaterStringDato(data.datoBeregnet)}
        {data.infomelding && <BodyShort textColor="subtle">{data.infomelding}</BodyShort>}
      </>
    </>
  )
}

const TableWrapper = styled.div`
  display: flex;
  flex-wrap: wrap;
  max-width: 1000px;
  margin-top: 1em;
  margin-bottom: 1em;
`

const UtbetalingAggregertTable = (props: { tittel: string; data: UtbetalingAggregert[] }) => {
  return (
    <TableWrapper>
      <b>{props.tittel}</b>
      <Table zebraStripes size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell align="right">Beløp</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {props.data.map((rad, idx) => (
            <Table.Row key={idx}>
              <Table.DataCell>{rad.klassekodeBeskrivelse}</Table.DataCell>
              <Table.DataCell align="right">{NOK(rad.beloep)}</Table.DataCell>
            </Table.Row>
          ))}
          <Table.Row>
            <Table.DataCell>
              <b>Sum</b>
            </Table.DataCell>
            <Table.DataCell align="right">
              {NOK(props.data.map((row) => row.beloep).reduce((sum, current) => sum + current, 0))}
            </Table.DataCell>
          </Table.Row>
        </Table.Body>
      </Table>
    </TableWrapper>
  )
}
