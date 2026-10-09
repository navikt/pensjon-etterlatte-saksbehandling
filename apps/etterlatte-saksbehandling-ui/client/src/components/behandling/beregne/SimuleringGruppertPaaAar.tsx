import { Box, Heading, ReadMore, Table, VStack } from '@navikt/ds-react'
import { getYear } from 'date-fns'
import { CSSProperties, ReactNode } from 'react'
import {
  SimulertBeregning,
  SimulertBeregningsperiode,
  SimulertEtterbetalingOppsummering,
  SimulertFeilutbetalingOppsummering,
  SimulertKlasseType,
} from '~shared/types/Utbetaling'
import { NOK } from '~utils/formatering/formatering'
import { UtbetalingTable } from './UtbetalingTable'

const utenBunnlinje: CSSProperties = { borderBottom: 'none' }

const klasseTypeTekst: Record<SimulertKlasseType, string> = {
  YTEL: 'Ytelse',
  SKAT: 'Skattetrekk',
  FEIL: 'Feilutbetaling',
  MOTP: 'Motpostering',
  JUST: 'Justering',
  TREK: 'Trekk',
}

const SumTabell = ({ children }: { children: ReactNode }) => (
  <Table>
    <Table.Header>
      <Table.Row>
        <Table.HeaderCell scope="col">Type</Table.HeaderCell>
        <Table.HeaderCell scope="col" align="right">
          Sum
        </Table.HeaderCell>
      </Table.Row>
    </Table.Header>
    <Table.Body>{children}</Table.Body>
  </Table>
)

const EtterbetalingRader = ({
  etterbetaling,
  suffix = '',
}: {
  etterbetaling: SimulertEtterbetalingOppsummering
  suffix?: string
}) => (
  <>
    <Table.Row>
      <Table.DataCell>Brutto etterbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(etterbetaling.brutto)}</Table.DataCell>
    </Table.Row>
    {etterbetaling.beloepPerKlasseType
      .filter(({ klasseType }) => klasseType !== 'YTEL')
      .map(({ klasseType, beloep }) => (
        <Table.Row key={klasseType}>
          <Table.DataCell>{klasseTypeTekst[klasseType]}</Table.DataCell>
          <Table.DataCell align="right">{NOK(beloep)}</Table.DataCell>
        </Table.Row>
      ))}
    <Table.Row>
      <Table.DataCell style={utenBunnlinje}>Netto etterbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right" style={utenBunnlinje}>
        {NOK(etterbetaling.netto)}
      </Table.DataCell>
    </Table.Row>
  </>
)

const FeilutbetalingRader = ({
  feilutbetaling,
  suffix = '',
}: {
  feilutbetaling: SimulertFeilutbetalingOppsummering
  suffix?: string
}) => (
  <>
    <Table.Row>
      <Table.DataCell>Brutto feilutbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.brutto)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell>Beløp brukeren skulle hatt{suffix}</Table.DataCell>
      <Table.DataCell align="right">{NOK(feilutbetaling.beloepBrukerenSkulleHatt)}</Table.DataCell>
    </Table.Row>
    <Table.Row>
      <Table.DataCell style={utenBunnlinje}>Netto feilutbetaling{suffix}</Table.DataCell>
      <Table.DataCell align="right" style={utenBunnlinje}>
        {NOK(feilutbetaling.netto)}
      </Table.DataCell>
    </Table.Row>
  </>
)

export const SimuleringGruppertPaaAar = ({ data }: { data: SimulertBeregning }) => {
  return (
    <>
      {data.oppsummeringer.perAar.map(({ aarstall, oppsummering }) => {
        const etterbetaling = hentPerioderForAar(aarstall, data.etterbetaling)
        const tilbakekreving = hentPerioderForAar(aarstall, data.tilbakekreving)

        return (
          <Box key={aarstall} maxWidth="70rem" padding="space-20">
            <Heading level="3" size="small">
              Resultat av simulering i {aarstall}
            </Heading>
            <Box width="25rem" marginBlock="space-20">
              <VStack gap="space-20">
                <SumTabell>
                  <EtterbetalingRader etterbetaling={oppsummering.etterbetaling} />
                </SumTabell>
                {oppsummering.feilutbetaling && (
                  <SumTabell>
                    <FeilutbetalingRader feilutbetaling={oppsummering.feilutbetaling} />
                  </SumTabell>
                )}
              </VStack>
            </Box>
            <Box maxWidth="1000px">
              <ReadMore header={`Se detaljer om simulering i ${aarstall}`}>
                <VStack gap="space-20">
                  <UtbetalingTable tittel={`Etterbetaling ${aarstall}`} perioder={etterbetaling} />
                  <UtbetalingTable tittel={`Tilbakekreving ${aarstall}`} perioder={tilbakekreving} />
                </VStack>
              </ReadMore>
            </Box>
          </Box>
        )
      })}
      {data.oppsummeringer.perAar.length > 1 && (
        <Box maxWidth="70rem" padding="space-20">
          <Heading level="3" size="small">
            Etterbetaling for hele perioden
          </Heading>
          <Box width="25rem" marginBlock="space-20">
            <VStack gap="space-20">
              <SumTabell>
                <EtterbetalingRader
                  etterbetaling={data.oppsummeringer.forPerioden.etterbetaling}
                  suffix=" for perioden"
                />
              </SumTabell>
              {data.oppsummeringer.forPerioden.feilutbetaling && (
                <SumTabell>
                  <FeilutbetalingRader
                    feilutbetaling={data.oppsummeringer.forPerioden.feilutbetaling}
                    suffix=" for perioden"
                  />
                </SumTabell>
              )}
            </VStack>
          </Box>
        </Box>
      )}
    </>
  )
}

function hentPerioderForAar(aarstall: number, perioder: SimulertBeregningsperiode[]) {
  return perioder.filter((periode) => {
    const aar = getYear(periode.fom)
    return aar === aarstall
  })
}
