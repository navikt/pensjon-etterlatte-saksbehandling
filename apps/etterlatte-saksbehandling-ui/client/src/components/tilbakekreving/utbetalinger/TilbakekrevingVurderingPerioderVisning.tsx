import {
  klasseTypeYtelse,
  leggPaaOrginalIndex,
  teksterTilbakekrevingResultat,
  teksterTilbakekrevingSkyld,
  tekstKlasseKode,
  TilbakekrevingBehandling,
  TilbakekrevingPeriode,
} from '~shared/types/Tilbakekreving'
import React, { useState } from 'react'
import { Box, Button, HStack, Table } from '@navikt/ds-react'
import { useNavigate } from 'react-router'
import { formaterMaanednavnAar } from '~utils/formatering/dato'
import { TilbakekrevingVurderingPerioderRadAndreKlassetyper } from '~components/tilbakekreving/utbetalinger/TilbakekrevingVurderingPerioderRadAndreKlassetyper'
import { JaNei } from '~shared/types/ISvar'

const tekstOverstyring: Record<JaNei | 'ikke_vurdert', string> = {
  JA: 'Ja, netto overstyres',
  NEI: 'Nei, sendes som behandlet',
  ikke_vurdert: 'Ikke vurdert',
}

export function TilbakekrevingVurderingPerioderVisning({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const navigate = useNavigate()
  const [perioder] = useState<TilbakekrevingPeriode[]>(behandling.tilbakekreving.perioder)
  const harPerioderMedVurdertOverstyring = behandling.tilbakekreving.perioder.some((periode) =>
    periode.tilbakekrevingsbeloep.some((beloep) => !!beloep.overstyrBehandletNettoTilBrutto)
  )

  return (
    <Box paddingBlock="space-8" paddingInline="space-16 space-8">
      <Table className="table" zebraStripes>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell style={{ minWidth: '6em' }}>Måned</Table.HeaderCell>
            <Table.HeaderCell>Klasse</Table.HeaderCell>
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
            {harPerioderMedVurdertOverstyring && <Table.HeaderCell>Overstyr netto til brutto</Table.HeaderCell>}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {perioder.map((periode, indexPeriode) => {
            return periode.tilbakekrevingsbeloep.map(leggPaaOrginalIndex).map((beloep) => {
              if (klasseTypeYtelse(beloep)) {
                return (
                  <Table.Row key={`beloepRad-${indexPeriode}-${beloep.originalIndex}`}>
                    <Table.DataCell key="maaned">{formaterMaanednavnAar(periode.maaned)}</Table.DataCell>
                    <Table.DataCell key="klasse">
                      {tekstKlasseKode[beloep.klasseKode] ?? beloep.klasseKode}
                    </Table.DataCell>
                    <Table.DataCell key="bruttoUtbetaling">{beloep.bruttoUtbetaling} kr</Table.DataCell>
                    <Table.DataCell key="nyBruttoUtbetaling">{beloep.nyBruttoUtbetaling} kr</Table.DataCell>
                    <Table.DataCell key="beregnetFeilutbetaling">{beloep.beregnetFeilutbetaling} kr</Table.DataCell>
                    <Table.DataCell key="skatteprosent">{beloep.skatteprosent} %</Table.DataCell>
                    <Table.DataCell key="bruttoTilbakekreving">{beloep.bruttoTilbakekreving} kr</Table.DataCell>
                    <Table.DataCell key="nettoTilbakekreving">{beloep.nettoTilbakekreving} kr</Table.DataCell>
                    <Table.DataCell key="skatt">{beloep.skatt} kr</Table.DataCell>
                    <Table.DataCell key="skyld">{teksterTilbakekrevingSkyld[beloep.skyld!!]}</Table.DataCell>
                    <Table.DataCell key="resultat">{teksterTilbakekrevingResultat[beloep.resultat!!]}</Table.DataCell>
                    <Table.DataCell key="tilbakekrevingsprosent">{beloep.tilbakekrevingsprosent} %</Table.DataCell>
                    <Table.DataCell key="rentetillegg">{beloep.rentetillegg} kr</Table.DataCell>
                    {harPerioderMedVurdertOverstyring && (
                      <Table.DataCell>
                        {tekstOverstyring[beloep.overstyrBehandletNettoTilBrutto ?? 'ikke_vurdert']}
                      </Table.DataCell>
                    )}
                  </Table.Row>
                )
              } else {
                // Kun visning av andre klassetyper enn YTEL - Disse har ikke vurderingsfelter
                return (
                  <TilbakekrevingVurderingPerioderRadAndreKlassetyper
                    key={`beloepRad-${indexPeriode}-${beloep.originalIndex}`}
                    periode={periode}
                    beloep={beloep}
                    ekstraKolonne={harPerioderMedVurdertOverstyring}
                  />
                )
              }
            })
          })}
        </Table.Body>
      </Table>

      <Box paddingBlock="space-12 space-0" borderWidth="1 0 0 0">
        <HStack justify="center">
          <Button variant="primary" onClick={() => navigate(`/tilbakekreving/${behandling?.id}/oppsummering`)}>
            Neste
          </Button>
        </HStack>
      </Box>
    </Box>
  )
}
