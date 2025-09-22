import { SummerteInntekterAOrdningen } from '~shared/types/EtteroppgjoerForbehandling'
import { BodyShort, Heading, Label, VStack, Table } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'
import React from 'react'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { LenkeTilInntektOversikt } from '~components/etteroppgjoer/components/inntektsopplysninger/LenkeTilInntektOversikt'

export function OpplysningerFraAInntektSummert({ inntekter }: { inntekter: SummerteInntekterAOrdningen }) {
  return (
    <VStack gap="4">
      <Heading size="small">Opplysninger fra A-ordningen</Heading>
      <BodyShort>
        Opplysningene er angitt som brutto inntekt og er summert per måned. For detaljer per måned, sjekk A-ordningen.
      </BodyShort>

      <LenkeTilInntektOversikt />

      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Type inntekt</Table.HeaderCell>
            {inntekter.afp.inntekter.map((maaned) => (
              <Table.HeaderCell key={maaned.maaned} scope="col" align="right">
                {maaned.maaned}
              </Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          <Table.Row>
            <Table.DataCell>Lønn</Table.DataCell>
            {inntekter.loenn.inntekter.map((maaned) => (
              <Table.DataCell key={maaned.maaned} align="right">
                {NOK(maaned.beloep)}
              </Table.DataCell>
            ))}
          </Table.Row>
          <Table.Row>
            <Table.DataCell>AFP</Table.DataCell>
            {inntekter.afp.inntekter.map((maaned) => (
              <Table.DataCell key={maaned.maaned} align="right">
                {NOK(maaned.beloep)}
              </Table.DataCell>
            ))}
          </Table.Row>
          <Table.Row>
            <Table.DataCell>OMS</Table.DataCell>
            {inntekter.oms.inntekter.map((maaned) => (
              <Table.DataCell key={maaned.maaned} align="right">
                {NOK(maaned.beloep)}
              </Table.DataCell>
            ))}
          </Table.Row>
        </Table.Body>
      </Table>

      <div>
        <Label size="small">Kilde</Label>
        <BodyShort size="small">A-ordningen {formaterDatoMedKlokkeslett(inntekter.tidspunktBeregnet)}</BodyShort>
      </div>
    </VStack>
  )
}
