import { Table } from '@navikt/ds-react'
import React from 'react'
import { tekstKlasseKode, TilbakekrevingBeloep, TilbakekrevingPeriode } from '~shared/types/Tilbakekreving'
import { formaterMaanednavnAar } from '~utils/formatering/dato'

export function TilbakekrevingVurderingPerioderRadAndreKlassetyper({
  periode,
  beloep,
}: {
  periode: TilbakekrevingPeriode
  beloep: TilbakekrevingBeloep
}) {
  return (
    <>
      <Table.Row>
        <Table.DataCell key="maaned">{formaterMaanednavnAar(periode.maaned)}</Table.DataCell>
        <Table.DataCell key="klasse">{tekstKlasseKode[beloep.klasseKode] ?? beloep.klasseKode}</Table.DataCell>
        <Table.DataCell key="bruttoUtbetaling">{beloep.bruttoUtbetaling} kr</Table.DataCell>
        <Table.DataCell key="nyBruttoUtbetaling">{beloep.nyBruttoUtbetaling} kr</Table.DataCell>
        <Table.DataCell key="beregnetFeilutbetaling"></Table.DataCell>
        <Table.DataCell key="skatteprosent"></Table.DataCell>
        <Table.DataCell key="bruttoTilbakekreving"></Table.DataCell>
        <Table.DataCell key="nettoTilbakekreving"></Table.DataCell>
        <Table.DataCell key="skatt"></Table.DataCell>
        <Table.DataCell key="skyld"></Table.DataCell>
        <Table.DataCell key="resultat"></Table.DataCell>
        <Table.DataCell key="tilbakekrevingsprosent"></Table.DataCell>
        <Table.DataCell key="rentetillegg"></Table.DataCell>
      </Table.Row>
    </>
  )
}
