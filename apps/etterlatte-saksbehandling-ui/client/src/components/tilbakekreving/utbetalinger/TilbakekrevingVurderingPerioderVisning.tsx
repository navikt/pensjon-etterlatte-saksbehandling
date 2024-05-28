import {
  teksterTilbakekrevingResultat,
  teksterTilbakekrevingSkyld,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
} from '~shared/types/Tilbakekreving'
import React, { useState } from 'react'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { Box, Button, HStack, Table } from '@navikt/ds-react'
import { format } from 'date-fns'
import { useNavigate } from 'react-router'

export function TilbakekrevingVurderingPerioderVisning({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const navigate = useNavigate()
  const [perioder] = useState<TilbakekrevingPeriode[]>(behandling.tilbakekreving.perioder)

  return (
    <Box paddingBlock="8" paddingInline="16 8">
      <Table className="table" zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell style={{ minWidth: '6em' }}>Måned</Table.HeaderCell>
            <Table.HeaderCell>Brutto utbetaling</Table.HeaderCell>
            <Table.HeaderCell>Ny brutto utbetaling</Table.HeaderCell>
            <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
            <Table.HeaderCell>Skatteprosent</Table.HeaderCell>
            <Table.HeaderCell>Brutto tilbakekreving</Table.HeaderCell>
            <Table.HeaderCell>Netto tilbakekreving</Table.HeaderCell>
            <Table.HeaderCell>Skatt</Table.HeaderCell>
            <Table.HeaderCell>Skyld</Table.HeaderCell>
            <Table.HeaderCell>Resultat</Table.HeaderCell>
            <Table.HeaderCell>Tilbakekrevingsprosent</Table.HeaderCell>
            <Table.HeaderCell>Rentetillegg</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {perioder.map((periode, index) => {
            const beloeper = periode.ytelse
            return (
              <Table.Row key={'beloeperRad' + index}>
                <Table.DataCell key="maaned">{format(periode.maaned, 'MMMM yyyy')}</Table.DataCell>
                <Table.DataCell key="bruttoUtbetaling">{beloeper.bruttoUtbetaling} kr</Table.DataCell>
                <Table.DataCell key="nyBruttoUtbetaling">{beloeper.nyBruttoUtbetaling} kr</Table.DataCell>
                <Table.DataCell key="beregnetFeilutbetaling">{beloeper.beregnetFeilutbetaling} kr</Table.DataCell>
                <Table.DataCell key="skatteprosent">{beloeper.skatteprosent} %</Table.DataCell>
                <Table.DataCell key="bruttoTilbakekreving">{beloeper.bruttoTilbakekreving} kr</Table.DataCell>
                <Table.DataCell key="nettoTilbakekreving">{beloeper.nettoTilbakekreving} kr</Table.DataCell>
                <Table.DataCell key="skatt">{beloeper.skatt} kr</Table.DataCell>
                <Table.DataCell key="skyld">{teksterTilbakekrevingSkyld[beloeper.skyld!!]}</Table.DataCell>
                <Table.DataCell key="resultat">{teksterTilbakekrevingResultat[beloeper.resultat!!]}</Table.DataCell>
                <Table.DataCell key="tilbakekrevingsprosent">{beloeper.tilbakekrevingsprosent} %</Table.DataCell>
                <Table.DataCell key="rentetillegg">{beloeper.rentetillegg} kr</Table.DataCell>
              </Table.Row>
            )
          })}
        </Table.Body>
      </Table>
      <Border style={{ marginTop: '3em' }} />
      <HStack justify="center">
        <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/oppsummering`)}>
          Neste
        </Button>
      </HStack>
    </Box>
  )
}
