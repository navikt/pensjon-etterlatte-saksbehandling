import { BodyShort, Heading, Table, VStack } from '@navikt/ds-react'
import { AInntekt, AInntektMaaned } from '~shared/types/EtteroppgjoerForbehandling'
import { NOK } from '~utils/formatering/formatering'

export const OpplysningerFraAInntekt = ({ ainntekt }: { ainntekt: AInntekt }) => {
  const sumAvAlleSummertBeloep = (inntektsmaaneder: AInntektMaaned[]) => {
    return inntektsmaaneder.reduce((a, b) => a + b.summertBeloep, 0)
  }

  return (
    <VStack gap="4">
      <Heading size="small">Opplysninger fra A-Inntekt</Heading>
      <BodyShort>Opplysnignene er oppgitt som brutto inntekt</BodyShort>
      {!!ainntekt.inntektsmaaneder?.length ? (
        <Table>
          <Table.Header>
            <Table.Row>
              {ainntekt.inntektsmaaneder.map((maaned, i) => (
                <Table.HeaderCell key={i} scope="col">
                  {maaned.maaned}
                </Table.HeaderCell>
              ))}
              <Table.HeaderCell scope="col">Sum</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            <Table.Row>
              {ainntekt.inntektsmaaneder.map((maaned, i) => (
                <Table.DataCell key={i}>{NOK(maaned.summertBeloep)}</Table.DataCell>
              ))}
              <Table.DataCell>{NOK(sumAvAlleSummertBeloep(ainntekt.inntektsmaaneder))}</Table.DataCell>
            </Table.Row>
          </Table.Body>
        </Table>
      ) : (
        <Heading size="small">Ingen opplysninger fra A-Inntekt</Heading>
      )}
    </VStack>
  )
}
