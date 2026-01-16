import {
  harPerioderMedBarnepensjonSkattetrekk,
  klasseKodeBpSkat,
  klasseTypeYtelse,
  TilbakekrevingBehandling,
  TilbakekrevingBeloep,
} from '~shared/types/Tilbakekreving'
import React from 'react'
import { Alert, BodyLong, BodyShort, Box, HelpText, HStack, Table, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import { JaNei } from '~shared/types/ISvar'
import { formaterMaanedAar } from '~utils/formatering/dato'

export function TilbakekrevingVurderingOppsummering({ behandling }: { behandling: TilbakekrevingBehandling }) {
  function sumKlasseTypeYtelse(callback: (beloep: TilbakekrevingBeloep) => number | null) {
    return tilbakekreving.perioder
      .flatMap((it) => it.tilbakekrevingsbeloep.filter(klasseTypeYtelse).map((beloep) => callback(beloep)))
      .map((beloep) => (beloep ? beloep : 0))
      .reduce((sum, current) => sum + current, 0)
  }

  // Identifisere trekk som brukes for å trekke 17% skatt på barnepensjon
  function fastSkattetrekkBarnepensjon(callback: (beloep: TilbakekrevingBeloep) => number | null) {
    return tilbakekreving.perioder
      .flatMap((it) => it.tilbakekrevingsbeloep.filter(klasseKodeBpSkat).map((beloep) => callback(beloep)))
      .map((beloep) => (beloep ? beloep : 0))
      .reduce((sum, current) => sum + Math.abs(current), 0)
  }

  const tilbakekreving = behandling.tilbakekreving
  const sumFeilutbetaling = sumKlasseTypeYtelse((beloep) => beloep.beregnetFeilutbetaling)
  const sumBruttoTilbakekreving = sumKlasseTypeYtelse((beloep) => beloep.bruttoTilbakekreving)
  const sumSkatt = sumKlasseTypeYtelse((beloep) => beloep.skatt)
  const sumNettoTilbakekreving = sumKlasseTypeYtelse((beloep) => beloep.nettoTilbakekreving)
  const sumRenter = sumKlasseTypeYtelse((beloep) => beloep.rentetillegg)
  const oppsummertInnkreving = sumNettoTilbakekreving + sumRenter

  // Beregninger inkludert skattetrekk
  const fastSkattetrekkBp = fastSkattetrekkBarnepensjon((beloep) => beloep.bruttoUtbetaling)
  const sumFeilutbetalingInkludertTrekk = sumFeilutbetaling + fastSkattetrekkBp
  const sumBruttoTilbakekrevingInkludertTrekk = sumBruttoTilbakekreving + fastSkattetrekkBp
  const sumSkattInkludertTrekk = sumSkatt + fastSkattetrekkBp

  const perioderMedOverstyringOgSkattetrekk = tilbakekreving.perioder.filter(
    (periode) =>
      periode.tilbakekrevingsbeloep.some((beloep) => beloep.klasseType == 'SKAT') &&
      periode.tilbakekrevingsbeloep.some((beloep) => beloep.overstyrBehandletNettoTilBrutto === JaNei.JA)
  )

  return (
    <VStack gap="8">
      <Table className="table" zebraStripes style={{ width: '50rem' }}>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell></Table.HeaderCell>
            <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
            <Table.HeaderCell></Table.HeaderCell>
            <Table.HeaderCell>Tilbakekreving</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          <Table.Row key="Beloep">
            <Table.DataCell></Table.DataCell>
            <Table.DataCell>{NOK(sumFeilutbetaling)}</Table.DataCell>
            <Table.DataCell></Table.DataCell>
            <Table.DataCell></Table.DataCell>
          </Table.Row>
          <Table.Row key="Brutto">
            <Table.DataCell>Brutto tilbakekreving</Table.DataCell>
            <Table.DataCell></Table.DataCell>
            <Table.DataCell></Table.DataCell>
            <Table.DataCell>{NOK(sumBruttoTilbakekreving)}</Table.DataCell>
          </Table.Row>
          <Table.Row key="Skatt">
            <Table.DataCell>Fradrag skatt</Table.DataCell>
            <Table.DataCell></Table.DataCell>
            <Table.DataCell>-</Table.DataCell>
            <Table.DataCell>{NOK(sumSkatt)}</Table.DataCell>
          </Table.Row>
          <Table.Row key="Netto">
            <Table.DataCell>Netto tilbakekreving</Table.DataCell>
            <Table.DataCell></Table.DataCell>
            <Table.DataCell>=</Table.DataCell>
            <Table.DataCell>{NOK(sumNettoTilbakekreving)}</Table.DataCell>
          </Table.Row>
          <Table.Row key="Renter">
            <Table.DataCell>Rentetillegg</Table.DataCell>
            <Table.DataCell></Table.DataCell>
            <Table.DataCell>+</Table.DataCell>
            <Table.DataCell>{NOK(sumRenter)}</Table.DataCell>
          </Table.Row>
          <Table.Row key="SumInnkreving">
            <Table.HeaderCell>Sum til innkreving</Table.HeaderCell>
            <Table.HeaderCell></Table.HeaderCell>
            <Table.HeaderCell>=</Table.HeaderCell>
            <Table.HeaderCell>{NOK(oppsummertInnkreving)}</Table.HeaderCell>
          </Table.Row>
        </Table.Body>
      </Table>

      {perioderMedOverstyringOgSkattetrekk.length > 0 && (
        <Box marginBlock="10 0" maxWidth="45rem">
          <Alert variant="error">
            <VStack gap="4">
              <BodyLong>
                Det er lagt inn overstyring av netto tilbakekreving til brutto tilbakekreving i samme periode. Dette vil
                sannsynligvis gi feil i beløpene i tilbakekrevingen.
              </BodyLong>
              <BodyShort>Perioder det gjelder:</BodyShort>
              <ul>
                {perioderMedOverstyringOgSkattetrekk.map((periode) => (
                  <li key={periode.maaned.toString()}>{formaterMaanedAar(periode.maaned)}</li>
                ))}
              </ul>
            </VStack>
          </Alert>
        </Box>
      )}

      {harPerioderMedBarnepensjonSkattetrekk(behandling) && (
        <>
          <Box marginBlock="10 0" maxWidth="45em">
            <Alert variant="info">
              <VStack gap="5">
                <BodyLong>
                  Oppsummeringen ovenfor gir en oversikt over beløpene som overføres til oppdrag og Nav Innkreving. Vi
                  sier brutto tilbakebetaling fordi skattebeløpet ikke har blitt sendt videre til Skatteetaten fra Nav.
                </BodyLong>
                <BodyLong>
                  Oppsummeringen under viser informasjonen som bruker får i vedtaket. Vi gir informasjon om brutto
                  utbetalt beløp minus skatt som er trukket. Bruker skal betale tilbake det som er reelt utbetalt. Det
                  opplyses derfor om netto tilbakekreving.
                </BodyLong>
              </VStack>
            </Alert>
          </Box>

          <Table className="table" zebraStripes style={{ width: '50rem' }}>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell></Table.HeaderCell>
                <Table.HeaderCell>Beregnet feilutbetaling</Table.HeaderCell>
                <Table.HeaderCell></Table.HeaderCell>
                <Table.HeaderCell>Tilbakekreving</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              <Table.Row key="Beloep">
                <Table.DataCell></Table.DataCell>
                <Table.DataCell>
                  <HStack gap="1">
                    {NOK(sumFeilutbetalingInkludertTrekk)}
                    <HelpText strategy="fixed">
                      {NOK(sumFeilutbetaling)} {' + '}
                      {NOK(fastSkattetrekkBp)} (Feilutbetaling + Skattetrekk barnepensjon)
                    </HelpText>
                  </HStack>
                </Table.DataCell>
                <Table.DataCell></Table.DataCell>
                <Table.DataCell></Table.DataCell>
              </Table.Row>
              <Table.Row key="Brutto">
                <Table.DataCell>Brutto tilbakekreving</Table.DataCell>
                <Table.DataCell></Table.DataCell>
                <Table.DataCell></Table.DataCell>
                <Table.DataCell>
                  <HStack gap="1">
                    {NOK(sumBruttoTilbakekrevingInkludertTrekk)}
                    <HelpText strategy="fixed">
                      {NOK(sumBruttoTilbakekreving)} {' + '}
                      {NOK(fastSkattetrekkBp)} (Brutto tilbakekreving + Skattetrekk barnepensjon)
                    </HelpText>
                  </HStack>
                </Table.DataCell>
              </Table.Row>
              <Table.Row key="Skatt">
                <Table.DataCell>Fradrag skatt</Table.DataCell>
                <Table.DataCell></Table.DataCell>
                <Table.DataCell>-</Table.DataCell>
                <Table.DataCell>
                  <HStack gap="1">
                    {NOK(sumSkattInkludertTrekk)}
                    <HelpText strategy="fixed">
                      Skattetrekket for barnepensjon som utgjør {NOK(fastSkattetrekkBp)}
                    </HelpText>
                  </HStack>
                </Table.DataCell>
              </Table.Row>
              <Table.Row key="Netto">
                <Table.DataCell>Netto tilbakekreving</Table.DataCell>
                <Table.DataCell></Table.DataCell>
                <Table.DataCell>=</Table.DataCell>
                <Table.DataCell>{NOK(sumNettoTilbakekreving)}</Table.DataCell>
              </Table.Row>
              <Table.Row key="Renter">
                <Table.DataCell>Rentetillegg</Table.DataCell>
                <Table.DataCell></Table.DataCell>
                <Table.DataCell>+</Table.DataCell>
                <Table.DataCell>{NOK(sumRenter)}</Table.DataCell>
              </Table.Row>
              <Table.Row key="SumInnkreving">
                <Table.HeaderCell>Sum til innkreving</Table.HeaderCell>
                <Table.HeaderCell></Table.HeaderCell>
                <Table.HeaderCell>=</Table.HeaderCell>
                <Table.HeaderCell>{NOK(oppsummertInnkreving)}</Table.HeaderCell>
              </Table.Row>
            </Table.Body>
          </Table>
        </>
      )}
    </VStack>
  )
}
