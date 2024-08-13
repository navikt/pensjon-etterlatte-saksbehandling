import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSimulertUtbetaling, simulerUtbetaling } from '~shared/api/utbetaling'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import React, { useEffect, useState } from 'react'
import { BodyShort, Box, ErrorMessage, Heading, Label, Table } from '@navikt/ds-react'
import { SimulertBeregning, SimulertBeregningsperiode } from '~shared/types/Utbetaling'
import { formaterDato, formaterKanskjeStringDato } from '~utils/formatering/dato'
import { NOK } from '~utils/formatering/formatering'
import styled from 'styled-components'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { useAppSelector } from '~store/Store'
import { compareDesc } from 'date-fns'

export const SimulerUtbetaling = (props: { behandling: IBehandlingReducer }) => {
  const { behandling } = props
  const [simuleringStatus, simulerUtbetalingRequest] = useApiCall(simulerUtbetaling)
  const [lagretSimuleringStatus, hentLagretSimulerUtbetaling] = useApiCall(hentSimulertUtbetaling)
  const [, setLagretSimulerUtbetaling] = useState<SimulertBeregning | null>()

  // For OMS, lytte etter oppdatert beregning/avkorting
  const avkorting = useAppSelector((state) => state.behandlingReducer.behandling?.avkorting)

  function behandlingStatusFerdigEllerVedtakFattet() {
    return erFerdigBehandlet(behandling.status) || behandling.status === IBehandlingStatus.FATTET_VEDTAK
  }

  useEffect(() => {
    if (behandlingStatusFerdigEllerVedtakFattet()) {
      hentLagretSimulerUtbetaling(behandling.id, (result, statusCode) => {
        if (statusCode === 200) {
          setLagretSimulerUtbetaling(result)
        }
      })
    } else {
      simuler()
    }
  }, [behandling.status, avkorting])

  const simuler = () => {
    if (behandling.status === IBehandlingStatus.BEREGNET || behandling.status === IBehandlingStatus.AVKORTET) {
      simulerUtbetalingRequest(behandling.id)
    }
  }

  return (
    <>
      <Box paddingBlock="12">
        <Heading spacing size="small" level="1">
          Simulere utbetaling
        </Heading>

        {behandlingStatusFerdigEllerVedtakFattet() &&
          mapResult(lagretSimuleringStatus, {
            pending: <Spinner label="Henter lagret simulering..." />,
            success: (lagretSimulering) =>
              lagretSimulering ? (
                <SimuleringBeregning data={lagretSimulering} />
              ) : (
                <BodyShort textColor="subtle">Fant ingen lagret simulering.</BodyShort>
              ),
            error: () => <ApiErrorAlert>Feil ved henting av lagret simulering</ApiErrorAlert>,
          })}

        {mapResult(simuleringStatus, {
          pending: <Spinner label="Simulerer..." />,
          success: (simuleringrespons) =>
            simuleringrespons ? (
              <SimuleringBeregning data={simuleringrespons} />
            ) : (
              <ErrorMessage size="small">Simuleringstjenesten ga ikke svar.</ErrorMessage>
            ),
          error: () => <ApiErrorAlert>Feil ved simulering</ApiErrorAlert>,
        })}
      </Box>
    </>
  )
}

const SimuleringBeregning = ({ data }: { data: SimulertBeregning }) => {
  return (
    <>
      <UtbetalingTable tittel="Kommende utbetaling(er)" perioder={data.kommendeUtbetalinger} />

      {data.etterbetaling.length > 0 && <UtbetalingTable tittel="Etterbetaling" perioder={data.etterbetaling} />}

      {data.tilbakekreving.length > 0 && (
        <UtbetalingTable tittel="Potensiell tilbakekreving" perioder={data.tilbakekreving} />
      )}

      <>
        Beregnet dato: {formaterDato(data.datoBeregnet)}
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

const UtbetalingTable = ({ tittel, perioder }: { tittel: string; perioder: SimulertBeregningsperiode[] }) => {
  const sortertePerioder = [...perioder].sort((a, b) => compareDesc(new Date(a.fom), new Date(b.fom)))

  return (
    <TableWrapper>
      <Heading size="xsmall">{tittel}</Heading>
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
          {sortertePerioder.map((periode, idx) => (
            <Table.Row key={idx}>
              <Table.DataCell>
                {formaterDato(periode.fom)} - {formaterKanskjeStringDato(periode.tom)}
              </Table.DataCell>
              <Table.DataCell>
                {periode.klassekodeBeskrivelse} {periode.tilbakefoering && '(tidligere utbetalt)'}
              </Table.DataCell>
              <Table.DataCell>{periode.konto}</Table.DataCell>
              <Table.DataCell>{formaterDato(periode.forfall)}</Table.DataCell>
              <Table.DataCell align="right">{NOK(periode.beloep)}</Table.DataCell>
            </Table.Row>
          ))}
          <Table.Row>
            <Table.DataCell colSpan={4}>
              <Label>Sum</Label>
            </Table.DataCell>
            <Table.DataCell align="right">
              {NOK(perioder.map((row) => row.beloep).reduce((sum, current) => sum + current, 0))}
            </Table.DataCell>
          </Table.Row>
        </Table.Body>
      </Table>
    </TableWrapper>
  )
}
