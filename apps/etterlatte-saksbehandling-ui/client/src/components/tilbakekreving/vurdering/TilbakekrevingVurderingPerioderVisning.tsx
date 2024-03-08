import {
  teksterTilbakekrevingResultat,
  teksterTilbakekrevingSkyld,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
} from '~shared/types/Tilbakekreving'
import React, { useState } from 'react'
import { HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { Heading, Table } from '@navikt/ds-react'

export function TilbakekrevingVurderingPerioderVisning({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const [perioder] = useState<TilbakekrevingPeriode[]>(behandling.tilbakekreving.perioder)

  return (
    <InnholdPadding>
      <HeadingWrapper>
        <Heading level="2" size="medium" spacing>
          Utbetalinger
        </Heading>
      </HeadingWrapper>
      <Table className="table" zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Måned</Table.HeaderCell>
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
                <Table.DataCell key="maaned">{periode.maaned.toString()}</Table.DataCell>
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
    </InnholdPadding>
  )
}
