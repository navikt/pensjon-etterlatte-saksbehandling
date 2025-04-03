import { BodyShort, Detail, Heading, HStack, Label, Table, VStack } from '@navikt/ds-react'
import { IAvkortingGrunnlag } from '~shared/types/IAvkorting'
import { NOK } from '~utils/formatering/formatering'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import { lastDayOfMonth } from 'date-fns'

export const BrukeroppgittInntektForInnvilgedePerioder = ({
  avkortingGrunnlag,
}: {
  avkortingGrunnlag: IAvkortingGrunnlag[]
}) => {
  const inntektNorge = (avkortingGrunnlag: IAvkortingGrunnlag) =>
    avkortingGrunnlag.inntektTom - avkortingGrunnlag.fratrekkInnAar
  const inntektUtland = (avkortingGrunnlag: IAvkortingGrunnlag) =>
    avkortingGrunnlag.inntektUtlandTom - avkortingGrunnlag.fratrekkInnAarUtland

  return (
    <VStack gap="4">
      <VStack gap="1">
        <Heading size="small">Siste brukeroppgitte inntekt for innvilgede perioder</Heading>
        <BodyShort>Brukeroppgitt inntekt er det brukeren forventer å tjene før skatt</BodyShort>
      </VStack>

      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Forventet inntekt</Table.HeaderCell>
            <Table.HeaderCell scope="col">Innvilgede måneder</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til</Table.HeaderCell>
            <Table.HeaderCell scope="col">Spesifikasjon</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avkortingGrunnlag?.length ? (
            avkortingGrunnlag.map((grunnlag, i) => (
              <Table.ExpandableRow
                key={i}
                content={
                  <HStack gap="6">
                    <VStack gap="2">
                      <Label>Forventet inntekt Norge</Label>
                      <BodyShort>{NOK(inntektNorge(grunnlag))}</BodyShort>
                    </VStack>
                    <VStack gap="2">
                      <Label>Forventet inntekt utland</Label>
                      <BodyShort>{NOK(inntektUtland(grunnlag))}</BodyShort>
                    </VStack>
                    <VStack gap="2">
                      <Label>Kilde</Label>
                      <BodyShort>{grunnlag.kilde.ident}</BodyShort>
                      <Detail>Saksbehandler: {grunnlag.kilde.tidspunkt}</Detail>
                    </VStack>
                  </HStack>
                }
              >
                <Table.DataCell>{NOK(inntektNorge(grunnlag) + inntektUtland(grunnlag))}</Table.DataCell>
                <Table.DataCell>{grunnlag.innvilgaMaaneder}</Table.DataCell>
                <Table.DataCell>{formaterDatoMedFallback(grunnlag.fom, '-')}</Table.DataCell>
                <Table.DataCell>
                  {!!grunnlag.tom ? formaterDato(lastDayOfMonth(new Date(grunnlag.tom))) : '-'}
                </Table.DataCell>
                <Table.DataCell>{grunnlag.spesifikasjon}</Table.DataCell>
              </Table.ExpandableRow>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={6}>
                <Heading size="xsmall">Ingen inntekt oppgitt fra bruker</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
