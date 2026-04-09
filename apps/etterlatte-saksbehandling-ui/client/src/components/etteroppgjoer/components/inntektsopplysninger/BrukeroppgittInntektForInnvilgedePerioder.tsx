import { BodyShort, Detail, Heading, HelpText, HStack, Label, Table, VStack } from '@navikt/ds-react'
import {
  ForventetInntektGrunnlag,
  FaktiskInntektGrunnlag,
  IAvkortingGrunnlag,
  erForventetInntekt,
} from '~shared/types/IAvkorting'
import { NOK } from '~utils/formatering/formatering'
import { formaterDato, formaterDatoMedFallback, formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { lastDayOfMonth } from 'date-fns'

export const BrukeroppgittInntektForInnvilgedePerioder = ({
  avkortingGrunnlag,
}: {
  avkortingGrunnlag: IAvkortingGrunnlag[]
}) => {
  const forventetInntektNorge = (avkortingGrunnlag: ForventetInntektGrunnlag) =>
    avkortingGrunnlag.inntektTom - avkortingGrunnlag.fratrekkInnAar
  const forventetInntektUtland = (avkortingGrunnlag: ForventetInntektGrunnlag) =>
    avkortingGrunnlag.inntektUtlandTom - avkortingGrunnlag.fratrekkInnAarUtland

  const faktiskInntektNorge = (avkortingGrunnlag: FaktiskInntektGrunnlag) =>
    avkortingGrunnlag.loennsinntekt + avkortingGrunnlag.naeringsinntekt + avkortingGrunnlag.afp
  const faktiskInntektUtland = (avkortingGrunnlag: FaktiskInntektGrunnlag) => avkortingGrunnlag.utlandsinntekt

  return (
    <VStack gap="space-16">
      <VStack gap="space-4">
        <Heading size="small">Siste brukeroppgitte inntekt for innvilgede perioder</Heading>
        <BodyShort>Brukeroppgitt inntekt er det brukeren forventer å tjene før skatt</BodyShort>
      </VStack>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Type</Table.HeaderCell>
            <Table.HeaderCell scope="col">Inntekt</Table.HeaderCell>
            <Table.HeaderCell scope="col">
              <HStack gap="space-4">
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
                      <HStack gap="space-24">
                        <VStack gap="space-8">
                          <Label>Forventet inntekt Norge</Label>
                          <BodyShort>{NOK(forventetInntektNorge(grunnlag))}</BodyShort>
                        </VStack>
                        <VStack gap="space-8">
                          <Label>Forventet inntekt utland</Label>
                          <BodyShort>{NOK(forventetInntektUtland(grunnlag))}</BodyShort>
                        </VStack>
                        <VStack gap="space-8">
                          <Label>Kilde</Label>
                          <BodyShort>{grunnlag.kilde.ident}</BodyShort>
                          <Detail>Saksbehandler: {formaterDatoMedKlokkeslett(grunnlag.kilde.tidspunkt)}</Detail>
                        </VStack>
                      </HStack>
                    }
                  >
                    <Table.DataCell>Forventet</Table.DataCell>
                    <Table.DataCell>
                      {NOK(forventetInntektNorge(grunnlag) + forventetInntektUtland(grunnlag))}
                    </Table.DataCell>
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
                  <Table.ExpandableRow
                    key={i}
                    content={
                      <HStack gap="space-24">
                        <VStack gap="space-8">
                          <Label>Faktisk inntekt Norge</Label>
                          <BodyShort>{NOK(faktiskInntektNorge(grunnlag))}</BodyShort>
                          <Detail>Lønnsinntekt: {NOK(grunnlag.loennsinntekt)}</Detail>
                          <Detail>Næringsinntekt: {NOK(grunnlag.naeringsinntekt)}</Detail>
                          <Detail>AFP: {NOK(grunnlag.afp)}</Detail>
                        </VStack>
                        <VStack gap="space-8">
                          <Label>Faktisk inntekt utland</Label>
                          <BodyShort>{NOK(faktiskInntektUtland(grunnlag))}</BodyShort>
                        </VStack>
                        <VStack gap="space-8">
                          <Label>Kilde</Label>
                          <BodyShort>{grunnlag.kilde.ident}</BodyShort>
                          <Detail>Saksbehandler: {formaterDatoMedKlokkeslett(grunnlag.kilde.tidspunkt)}</Detail>
                        </VStack>
                      </HStack>
                    }
                  >
                    <Table.DataCell>Faktisk</Table.DataCell>
                    <Table.DataCell>
                      {NOK(faktiskInntektNorge(grunnlag) + faktiskInntektUtland(grunnlag))}
                    </Table.DataCell>
                    <Table.DataCell>{grunnlag.innvilgaMaaneder}</Table.DataCell>
                    <Table.DataCell>{formaterDatoMedFallback(grunnlag.fom, '-')}</Table.DataCell>
                    <Table.DataCell>
                      {!!grunnlag.tom ? formaterDato(lastDayOfMonth(new Date(grunnlag.tom))) : '-'}
                    </Table.DataCell>
                    <Table.DataCell>{grunnlag.spesifikasjon}</Table.DataCell>
                  </Table.ExpandableRow>
                )
              }
            })
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={7}>
                <Heading size="xsmall">Ingen inntekt oppgitt fra bruker</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
