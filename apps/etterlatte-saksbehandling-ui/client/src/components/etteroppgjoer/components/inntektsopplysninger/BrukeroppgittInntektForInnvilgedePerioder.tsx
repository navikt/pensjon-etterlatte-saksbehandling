import { BodyShort, Detail, Heading, HelpText, HStack, Label, Table, VStack } from '@navikt/ds-react'
import { ForventetInntektGrunnlag, IAvkortingGrunnlag, erForventetInntekt } from '~shared/types/IAvkorting'
import { NOK } from '~utils/formatering/formatering'
import { formaterDato, formaterDatoMedFallback, formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { lastDayOfMonth } from 'date-fns'

export const BrukeroppgittInntektForInnvilgedePerioder = ({
  avkortingGrunnlag,
}: {
  avkortingGrunnlag: IAvkortingGrunnlag[]
}) => {
  const inntektNorge = (avkortingGrunnlag: ForventetInntektGrunnlag) =>
    avkortingGrunnlag.inntektTom - avkortingGrunnlag.fratrekkInnAar
  const inntektUtland = (avkortingGrunnlag: ForventetInntektGrunnlag) =>
    avkortingGrunnlag.inntektUtlandTom - avkortingGrunnlag.fratrekkInnAarUtland

  return (
    <VStack gap="space-4">
      <VStack gap="space-1">
        <Heading size="small">Siste brukeroppgitte inntekt for innvilgede perioder</Heading>
        <BodyShort>Brukeroppgitt inntekt er det brukeren forventer å tjene før skatt</BodyShort>
      </VStack>

      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Forventet inntekt</Table.HeaderCell>
            <Table.HeaderCell scope="col">
              <HStack gap="space-1">
                Innvilgede måneder
                <HelpText>
                  Her vises antall måneder med innvilget stønad i gjeldende inntektsår. Antallet endres ikke selv om man
                  har hatt inntektsendring i løpet av året.
                </HelpText>
              </HStack>
            </Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til</Table.HeaderCell>
            <Table.HeaderCell scope="col">Spesifikasjon</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avkortingGrunnlag?.length ? (
            avkortingGrunnlag.map((grunnlag, i) => {
              if (erForventetInntekt(grunnlag)) {
                return (
                  <Table.ExpandableRow
                    key={i}
                    content={
                      <HStack gap="space-6">
                        <VStack gap="space-2">
                          <Label>Forventet inntekt Norge</Label>
                          <BodyShort>{NOK(inntektNorge(grunnlag))}</BodyShort>
                        </VStack>
                        <VStack gap="space-2">
                          <Label>Forventet inntekt utland</Label>
                          <BodyShort>{NOK(inntektUtland(grunnlag))}</BodyShort>
                        </VStack>
                        <VStack gap="space-2">
                          <Label>Kilde</Label>
                          <BodyShort>{grunnlag.kilde.ident}</BodyShort>
                          <Detail>Saksbehandler: {formaterDatoMedKlokkeslett(grunnlag.kilde.tidspunkt)}</Detail>
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
                )
              } else {
                return (
                  <Table.Row key={i}>
                    <Table.DataCell colSpan={6}>
                      <Heading size="xsmall">Har kun forventet inntekt, faktisk inntekt er ikke støttet</Heading>
                    </Table.DataCell>
                  </Table.Row>
                )
              }
            })
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
