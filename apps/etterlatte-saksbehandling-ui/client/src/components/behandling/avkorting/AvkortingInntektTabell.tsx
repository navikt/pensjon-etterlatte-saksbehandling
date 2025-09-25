import { BodyShort, Box, HStack, Label, Table, Tag, Tooltip, VStack } from '@navikt/ds-react'
import {
  ForventetInntektHjelpeTekst,
  ForventetInntektUtlandHjelpeTekst,
  InnvilgaMaanederHeaderHjelpeTekst,
} from '~components/behandling/avkorting/AvkortingHjelpeTekster'
import { NOK } from '~utils/formatering/formatering'
import {
  erForventetInntekt,
  FaktiskInntektGrunnlag,
  ForventetInntektGrunnlag,
  IAvkortingGrunnlag,
  SystemOverstyrtInnvilgaMaanederAarsak,
} from '~shared/types/IAvkorting'
import { ArrowCirclepathIcon, HeadCloudIcon } from '@navikt/aksel-icons'
import { aarFraDatoString, formaterDato } from '~utils/formatering/dato'
import { lastDayOfMonth } from 'date-fns'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import React from 'react'
import { useBehandling } from '~components/behandling/useBehandling'
import { TekstMedMellomrom } from '~shared/TekstMedMellomrom'

interface Props {
  avkortingGrunnlagListe: IAvkortingGrunnlag[]
  fyller67: boolean
  erEtteroppgjoerRevurdering?: boolean
}

export const AvkortingInntektTabell = ({
  avkortingGrunnlagListe,
  fyller67,
  erEtteroppgjoerRevurdering = false,
}: Props) => {
  const behandling = useBehandling()

  const erEtteroppgjoersAar = (avkortingGrunnlag: IAvkortingGrunnlag) =>
    erEtteroppgjoerRevurdering &&
    !!behandling?.virkningstidspunkt?.dato &&
    aarFraDatoString(avkortingGrunnlag.fom) === aarFraDatoString(behandling?.virkningstidspunkt?.dato)

  return (
    <Table className="table" zebraStripes>
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell />
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Inntektstype
            </HStack>
          </Table.HeaderCell>
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Inntekt totalt
            </HStack>
          </Table.HeaderCell>
          <Table.HeaderCell>
            <HStack gap="2" align="center" wrap={false}>
              Innvilgede måneder
              <InnvilgaMaanederHeaderHjelpeTekst />
            </HStack>
          </Table.HeaderCell>
          <Table.HeaderCell>År</Table.HeaderCell>
          <Table.HeaderCell>Periode</Table.HeaderCell>
          <Table.HeaderCell>Kilde</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {avkortingGrunnlagListe.map((avkortingGrunnlag, index) => {
          return (
            <Table.ExpandableRow key={index} content={<InntektDetaljer avkortingGrunnlag={avkortingGrunnlag} />}>
              <Table.DataCell key="InntektType">
                {erEtteroppgjoersAar(avkortingGrunnlag) ? (
                  <Tag variant="success">
                    {erForventetInntekt(avkortingGrunnlag) ? 'Forventet inntekt' : 'Faktisk inntekt'}
                  </Tag>
                ) : (
                  <Tag variant="alt3">
                    {erForventetInntekt(avkortingGrunnlag) ? 'Forventet inntekt' : 'Faktisk inntekt'}
                  </Tag>
                )}
              </Table.DataCell>
              <Table.DataCell key="InntektTotalt">{NOK(avkortingGrunnlag.inntektInnvilgetPeriode)}</Table.DataCell>
              <Table.DataCell>
                <HStack gap="4" align="center">
                  <BodyShort>{avkortingGrunnlag.innvilgaMaaneder}</BodyShort>
                  {fyller67 &&
                    erForventetInntekt(avkortingGrunnlag) &&
                    (!avkortingGrunnlag.overstyrtInnvilgaMaaneder ||
                      avkortingGrunnlag.overstyrtInnvilgaMaaneder.aarsak ===
                        SystemOverstyrtInnvilgaMaanederAarsak.BLIR_67) && (
                      <Tooltip content="Fyller 67 år">
                        <HeadCloudIcon aria-hidden fontSize="1.5rem" />
                      </Tooltip>
                    )}
                  {erForventetInntekt(avkortingGrunnlag) && !!avkortingGrunnlag.overstyrtInnvilgaMaaneder && (
                    <Tooltip content="Antall innvilga måneder er overstyrt">
                      <ArrowCirclepathIcon aria-hidden fontSize="1.5rem" />
                    </Tooltip>
                  )}
                </HStack>
              </Table.DataCell>
              <Table.DataCell key="Aar">
                {erEtteroppgjoersAar(avkortingGrunnlag) ? (
                  <Tag variant="success">{aarFraDatoString(avkortingGrunnlag.fom)}</Tag>
                ) : (
                  <Tag variant="alt3">{aarFraDatoString(avkortingGrunnlag.fom)}</Tag>
                )}
              </Table.DataCell>
              <Table.DataCell key="Periode">
                {avkortingGrunnlag.fom && formaterDato(avkortingGrunnlag.fom)} -{' '}
                {avkortingGrunnlag.tom && formaterDato(lastDayOfMonth(new Date(avkortingGrunnlag.tom)))}
              </Table.DataCell>
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
      <Box borderWidth="0 0 1 0" borderColor="border-subtle" marginBlock="0 4">
        <HStack gap="20">
          <VStack minWidth="15rem">
            <HStack gap="1">
              <Label>Forventet inntekt i Norge</Label>
              <ForventetInntektHjelpeTekst
                aarsinntekt={aarsinntekt}
                fratrekkInnAar={fratrekkInnAar}
                forventetInntekt={forventetInntekt}
              />
            </HStack>
            <BodyShort>{NOK(forventetInntekt)}</BodyShort>
          </VStack>

          <VStack>
            <HStack gap="2">
              <Label>Fratrekk inn-år Norge</Label>
            </HStack>
            <BodyShort>{NOK(fratrekkInnAar)}</BodyShort>
          </VStack>
        </HStack>
      </Box>

      <Box borderWidth="0 0 1 0" borderColor="border-subtle" marginBlock="0 4">
        <HStack gap="20">
          <VStack minWidth="15rem">
            <HStack gap="1">
              <Label>Forventet inntekt utland</Label>
              <ForventetInntektUtlandHjelpeTekst
                inntektUtland={inntektUtland}
                fratrekkUtland={fratrekkUtland}
                forventetInntektUtland={forventetInntektUtland}
              />
            </HStack>
            <BodyShort>{NOK(forventetInntektUtland)}</BodyShort>
          </VStack>

          <VStack>
            <HStack gap="2">
              <Label>Fratrekk inn-år utland</Label>
            </HStack>
            <BodyShort>{NOK(fratrekkUtland)}</BodyShort>
          </VStack>
        </HStack>
      </Box>

      <Box marginBlock="0 4">
        <VStack>
          <Label>Spesifikasjon av inntekt</Label>
          <TekstMedMellomrom>{forventetInntektGrunnlag.spesifikasjon}</TekstMedMellomrom>
        </VStack>
      </Box>
    </div>
  )
}

const FaktiskInntektDetaljer = ({ faktiskInntektGrunnlag }: { faktiskInntektGrunnlag: FaktiskInntektGrunnlag }) => {
  return (
    <div>
      <Box borderWidth="0 0 1 0" borderColor="border-subtle" marginBlock="0 4">
        <HStack gap="20">
          <VStack minWidth="15rem">
            <Label>Lønnsinntekt</Label>
            <BodyShort>{NOK(faktiskInntektGrunnlag.loennsinntekt)}</BodyShort>
          </VStack>

          <VStack>
            <HStack gap="2">
              <Label>Næringsinntekt</Label>
            </HStack>
            <BodyShort>{NOK(faktiskInntektGrunnlag.naeringsinntekt)}</BodyShort>
          </VStack>
        </HStack>
      </Box>
      <Box borderWidth="0 0 1 0" borderColor="border-subtle" marginBlock="0 4">
        <HStack gap="20">
          <VStack minWidth="15rem">
            <Label>AFP</Label>
            <BodyShort>{NOK(faktiskInntektGrunnlag.afp)}</BodyShort>
          </VStack>

          <VStack>
            <HStack gap="2">
              <Label>Utlandsinntekt</Label>
            </HStack>
            <BodyShort>{NOK(faktiskInntektGrunnlag.utlandsinntekt)}</BodyShort>
          </VStack>
        </HStack>
      </Box>

      <Box marginBlock="0 4">
        <VStack>
          <Label>Spesifikasjon av inntekt</Label>
          <TekstMedMellomrom>{faktiskInntektGrunnlag.spesifikasjon}</TekstMedMellomrom>
        </VStack>
      </Box>
    </div>
  )
}

const InntektDetaljer = ({ avkortingGrunnlag }: { avkortingGrunnlag: IAvkortingGrunnlag }) => {
  if (erForventetInntekt(avkortingGrunnlag))
    return <ForventetInntektDetaljer forventetInntektGrunnlag={avkortingGrunnlag} />
  else return <FaktiskInntektDetaljer faktiskInntektGrunnlag={avkortingGrunnlag} />
}
