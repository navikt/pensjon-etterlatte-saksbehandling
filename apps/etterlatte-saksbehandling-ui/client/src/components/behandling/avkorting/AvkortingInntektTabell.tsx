import { BodyShort, HStack, Table, Tag, Tooltip } from '@navikt/ds-react'
import {
  ForventetInntektHeaderHjelpeTekst,
  InnvilgaMaanederHeaderHjelpeTekst,
} from '~components/behandling/avkorting/AvkortingHjelpeTekster'
import { NOK } from '~utils/formatering/formatering'
import {
  FaktiskInntektGrunnlag,
  ForventetInntektGrunnlag,
  IAvkortingGrunnlag,
  isForventetInntekt,
  SystemOverstyrtInnvilgaMaanederAarsak,
} from '~shared/types/IAvkorting'
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
          <Table.HeaderCell />
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Inntektstype
              <ForventetInntektHeaderHjelpeTekst />
            </HStack>
          </Table.HeaderCell>
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Inntekt totalt
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
          return (
            <Table.ExpandableRow key={index} content={<InntektDetaljer avkortingGrunnlag={avkortingGrunnlag} />}>
              <Table.DataCell key="InntektType">
                {isForventetInntekt(avkortingGrunnlag) ? (
                  <Tag variant="alt2">Forventet inntekt</Tag>
                ) : (
                  <Tag variant="alt1">Faktisk inntekt</Tag>
                )}
              </Table.DataCell>
              <Table.DataCell key="InntektTotalt">{NOK(avkortingGrunnlag.inntektInnvilgetPeriode)}</Table.DataCell>
              <Table.DataCell>
                <HStack gap="4" align="center">
                  <BodyShort>{avkortingGrunnlag.innvilgaMaaneder}</BodyShort>
                  {fyller67 &&
                    isForventetInntekt(avkortingGrunnlag) &&
                    (!avkortingGrunnlag.overstyrtInnvilgaMaaneder ||
                      avkortingGrunnlag.overstyrtInnvilgaMaaneder.aarsak ===
                        SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67) && (
                      <Tooltip content="Fyller 67 år">
                        <HeadCloudIcon aria-hidden fontSize="1.5rem" />
                      </Tooltip>
                    )}
                  {isForventetInntekt(avkortingGrunnlag) && !!avkortingGrunnlag.overstyrtInnvilgaMaaneder && (
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
            </Table.ExpandableRow>
          )
        })}
      </Table.Body>
    </Table>
  )
}

const ForventetInntektDetaljer = ({
  forventetInntektGrunnlag,
}: {
  forventetInntektGrunnlag: ForventetInntektGrunnlag
}) => {
  const aarsinntekt = forventetInntektGrunnlag.inntektTom ?? 0
  const fratrekkInnAar = forventetInntektGrunnlag.fratrekkInnAar ?? 0
  const forventetInntekt = aarsinntekt - fratrekkInnAar
  const inntektUtland = forventetInntektGrunnlag.inntektUtlandTom ?? 0
  const fratrekkUtland = forventetInntektGrunnlag.fratrekkInnAarUtland ?? 0
  const forventetInntektUtland = inntektUtland - fratrekkUtland

  return (
    <div>
      <BodyShort>Forventet inntekt Norge: {forventetInntekt}</BodyShort>
      <BodyShort>Fratrekk inn-år Norge: {fratrekkInnAar}</BodyShort>
      <BodyShort>Forventet inntekt utland: {forventetInntektUtland}</BodyShort>
      <BodyShort>Fratrekk inn-år Utland: {fratrekkUtland}</BodyShort>
    </div>
  )
}

const FaktiskInntektDetaljer = ({ faktiskInntektGrunnlag }: { faktiskInntektGrunnlag: FaktiskInntektGrunnlag }) => {
  return (
    <div>
      <BodyShort>Lønnsinntekt: {faktiskInntektGrunnlag.loennsinntekt}</BodyShort>
      <BodyShort>Næringsinntekt: {faktiskInntektGrunnlag.naeringsinntekt}</BodyShort>
      <BodyShort>AFP: {faktiskInntektGrunnlag.afp}</BodyShort>
      <BodyShort>Utlandsinntekt: {faktiskInntektGrunnlag.utlandsinntekt}</BodyShort>
    </div>
  )
}

const InntektDetaljer = ({ avkortingGrunnlag }: { avkortingGrunnlag: IAvkortingGrunnlag }) => {
  if (isForventetInntekt(avkortingGrunnlag))
    return <ForventetInntektDetaljer forventetInntektGrunnlag={avkortingGrunnlag} />
  else return <FaktiskInntektDetaljer faktiskInntektGrunnlag={avkortingGrunnlag} />
}
