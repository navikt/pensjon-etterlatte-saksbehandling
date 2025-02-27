import { BodyShort, HStack, Table, Tooltip } from '@navikt/ds-react'
import {
  ForventetInntektHeaderHjelpeTekst,
  ForventetInntektHjelpeTekst,
  ForventetInntektUtlandHjelpeTekst,
  InnvilgaMaanederHeaderHjelpeTekst,
} from '~components/behandling/avkorting/AvkortingHjelpeTekster'
import { NOK } from '~utils/formatering/formatering'
import { IAvkortingGrunnlag, SystemOverstyrtInnvilgaMaanederAarsak } from '~shared/types/IAvkorting'
import { ArrowCirclepathIcon, HeadCloudIcon } from '@navikt/aksel-icons'
import { formaterDato } from '~utils/formatering/dato'
import { lastDayOfMonth } from 'date-fns'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'

export const AvkortingInntektTabell = ({
  avkortingGrunnlagListe,
  fyller67,
}: {
  avkortingGrunnlagListe: IAvkortingGrunnlag[]
  fyller67: boolean
}) => {
  return (
    <Table className="table" zebraStripes>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell>Forventet inntekt Norge</Table.HeaderCell>
          <Table.HeaderCell>Forventet inntekt utland</Table.HeaderCell>
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Forventet inntekt totalt
              <ForventetInntektHeaderHjelpeTekst />
            </HStack>
          </Table.HeaderCell>
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Innvilgede måneder
              <InnvilgaMaanederHeaderHjelpeTekst />
            </HStack>
          </Table.HeaderCell>
          <Table.HeaderCell>Periode</Table.HeaderCell>
          <Table.HeaderCell>Spesifikasjon av inntekt</Table.HeaderCell>
          <Table.HeaderCell>Kilde</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {avkortingGrunnlagListe.map((avkortingGrunnlag, index) => {
          const aarsinntekt = avkortingGrunnlag.inntektTom ?? 0
          const fratrekkInnAar = avkortingGrunnlag.fratrekkInnAar ?? 0
          const forventetInntekt = aarsinntekt - fratrekkInnAar
          const inntektUtland = avkortingGrunnlag.inntektUtlandTom ?? 0
          const fratrekkUtland = avkortingGrunnlag.fratrekkInnAarUtland ?? 0
          const forventetInntektUtland = inntektUtland - fratrekkUtland
          return (
            <Table.Row key={index}>
              <Table.DataCell key="Inntekt">
                <HStack gap="2">
                  <BodyShort>{NOK(forventetInntekt)}</BodyShort>
                  <ForventetInntektHjelpeTekst
                    aarsinntekt={aarsinntekt}
                    fratrekkInnAar={fratrekkInnAar}
                    forventetInntekt={forventetInntekt}
                  />
                </HStack>
              </Table.DataCell>
              <Table.DataCell key="InntektUtland">
                <HStack gap="2">
                  <BodyShort>{NOK(forventetInntektUtland)}</BodyShort>
                  <ForventetInntektUtlandHjelpeTekst
                    inntektUtland={inntektUtland}
                    fratrekkUtland={fratrekkUtland}
                    forventetInntektUtland={forventetInntektUtland}
                  />
                </HStack>
              </Table.DataCell>
              <Table.DataCell key="InntektTotalt">{NOK(forventetInntekt + forventetInntektUtland)}</Table.DataCell>
              <Table.DataCell>
                <HStack gap="4" align="center">
                  <BodyShort>{avkortingGrunnlag.innvilgaMaaneder}</BodyShort>
                  {fyller67 &&
                    (!avkortingGrunnlag.overstyrtInnvilgaMaaneder ||
                      avkortingGrunnlag.overstyrtInnvilgaMaaneder.aarsak ===
                        SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67) && (
                      <Tooltip content="Fyller 67 år">
                        <HeadCloudIcon aria-hidden fontSize="1.5rem" />
                      </Tooltip>
                    )}
                  {!!avkortingGrunnlag.overstyrtInnvilgaMaaneder && (
                    <Tooltip content="Antall innvilga måneder er overstyrt">
                      <ArrowCirclepathIcon aria-hidden fontSize="1.5rem" />
                    </Tooltip>
                  )}
                </HStack>
              </Table.DataCell>
              <Table.DataCell key="Periode">
                {avkortingGrunnlag.fom && formaterDato(avkortingGrunnlag.fom)} -{' '}
                {avkortingGrunnlag.tom && formaterDato(lastDayOfMonth(new Date(avkortingGrunnlag.tom)))}
              </Table.DataCell>
              <Table.DataCell key="InntektSpesifikasjon">{avkortingGrunnlag.spesifikasjon}</Table.DataCell>
              <Table.DataCell key="InntektKilde">
                {avkortingGrunnlag.kilde && (
                  <Info
                    tekst={avkortingGrunnlag.kilde.ident}
                    label=""
                    undertekst={`saksbehandler: ${formaterDato(avkortingGrunnlag.kilde.tidspunkt)}`}
                  />
                )}
              </Table.DataCell>
            </Table.Row>
          )
        })}
      </Table.Body>
    </Table>
  )
}
