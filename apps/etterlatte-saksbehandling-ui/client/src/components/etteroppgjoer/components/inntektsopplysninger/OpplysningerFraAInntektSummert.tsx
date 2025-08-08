import { SummerteInntekterAOrdningen } from '~shared/types/EtteroppgjoerForbehandling'
import { BodyShort, Heading, Table, VStack } from '@navikt/ds-react'
import { NOK } from '~utils/formatering/formatering'

export function OpplysningerFraAInntektSummert({ inntekter }: { inntekter: SummerteInntekterAOrdningen }) {
  return (
    <VStack gap="4">
      <Heading size="small">Opplysninger fra A-Inntekt for relevante filter</Heading>
      <BodyShort>
        Opplysnignene er oppgitt som brutto inntekt og summert innenfor hver måned. Hvis man vil se hva som inngår i en
        måned må man manuelt sjekke opp i A-ordningen.
      </BodyShort>

      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell>Type inntekt</Table.HeaderCell>
            {inntekter.afp.inntekter.map((maaned) => (
              <Table.HeaderCell key={maaned.maaned} scope="col">
                {maaned.maaned}
              </Table.HeaderCell>
            ))}
          </Table.Row>
        </Table.Header>
        <Table.Body>
          <Table.Row>
            <Table.DataCell>AFP</Table.DataCell>
            {inntekter.afp.inntekter.map((maaned) => (
              <Table.DataCell key={maaned.maaned}>{NOK(maaned.beloep)}</Table.DataCell>
            ))}
          </Table.Row>
          <Table.Row>
            <Table.DataCell>Lønn</Table.DataCell>
            {inntekter.loenn.inntekter.map((maaned) => (
              <Table.DataCell key={maaned.maaned}>{NOK(maaned.beloep)}</Table.DataCell>
            ))}
          </Table.Row>

          <Table.Row>
            <Table.DataCell>OMS</Table.DataCell>
            {inntekter.oms.inntekter.map((maaned) => (
              <Table.DataCell key={maaned.maaned}>{NOK(maaned.beloep)}</Table.DataCell>
            ))}
          </Table.Row>
        </Table.Body>
      </Table>
    </VStack>
  )
}
