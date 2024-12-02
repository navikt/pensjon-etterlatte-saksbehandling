import { Alert, BodyShort, Button, Heading, HStack, Table, Tooltip, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { NOK } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { lastDayOfMonth } from 'date-fns'
import { AvkortingInntektForm } from '~components/behandling/avkorting/AvkortingInntektForm'
import { IAvkortingGrunnlagFrontend, SystemOverstyrtInnvilgaMaanederAarsak } from '~shared/types/IAvkorting'
import { ArrowCirclepathIcon, HeadCloudIcon, PencilIcon } from '@navikt/aksel-icons'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import {
  ForventetInntektHeaderHjelpeTekst,
  ForventetInntektHjelpeTekst,
  ForventetInntektUtlandHjelpeTekst,
  InnvilgaMaanederHeaderHjelpeTekst,
} from '~components/behandling/avkorting/AvkortingHjelpeTekster'

export const AvkortingInntekt = ({
  behandling,
  avkortingGrunnlagFrontend,
  erInnevaerendeAar,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  avkortingGrunnlagFrontend: IAvkortingGrunnlagFrontend | undefined
  erInnevaerendeAar: boolean
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  const erRedigerbar = redigerbar && enhetErSkrivbar(behandling.sakEnhetId, useInnloggetSaksbehandler().skriveEnheter)
  const [visForm, setVisForm] = useState(false)
  const [visHistorikk, setVisHistorikk] = useState(false)

  const personopplysning = usePersonopplysninger()
  const fyller67 =
    personopplysning &&
    personopplysning.soeker &&
    avkortingGrunnlagFrontend &&
    avkortingGrunnlagFrontend.aar - personopplysning.soeker.opplysning.foedselsaar === 67

  const listeVisningAvkortingGrunnlag = () => {
    if (avkortingGrunnlagFrontend === undefined) {
      return []
    }
    if (visHistorikk) {
      return avkortingGrunnlagFrontend.fraVirk
        ? [avkortingGrunnlagFrontend.fraVirk].concat(avkortingGrunnlagFrontend.historikk)
        : avkortingGrunnlagFrontend.historikk
    } else {
      return [avkortingGrunnlagFrontend.fraVirk ?? avkortingGrunnlagFrontend.historikk[0]]
    }
  }

  const knappTekst = () => {
    if (erInnevaerendeAar) {
      if (avkortingGrunnlagFrontend?.fraVirk != null) {
        return 'Rediger'
      }
      return 'Legg til'
    } else {
      if (!!avkortingGrunnlagFrontend?.historikk?.length) {
        return 'Rediger'
      }
      return 'Legg til for neste år'
    }
  }

  return (
    <VStack maxWidth="70rem">
      {avkortingGrunnlagFrontend &&
        (avkortingGrunnlagFrontend.fraVirk || avkortingGrunnlagFrontend.historikk.length > 0) && (
          <VStack marginBlock="4">
            <Heading size="small">{avkortingGrunnlagFrontend.aar}</Heading>
            {fyller67 && (
              <Alert variant="warning">
                Bruker fyller 67 år i inntektsåret og antall innvilga måneder vil bli tilpasset deretter.
              </Alert>
            )}
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
                {listeVisningAvkortingGrunnlag().map((avkortingGrunnlag, index) => {
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
                      <Table.DataCell key="InntektTotalt">
                        {NOK(forventetInntekt + forventetInntektUtland)}
                      </Table.DataCell>
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
          </VStack>
        )}
      {erInnevaerendeAar &&
        avkortingGrunnlagFrontend &&
        ((avkortingGrunnlagFrontend.fraVirk == null && avkortingGrunnlagFrontend.historikk.length > 1) ||
          (avkortingGrunnlagFrontend.fraVirk != null && avkortingGrunnlagFrontend.historikk.length > 0)) && (
          <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />
        )}
      {erRedigerbar && visForm && (
        <AvkortingInntektForm
          behandling={behandling}
          avkortingGrunnlagFrontend={avkortingGrunnlagFrontend}
          erInnevaerendeAar={erInnevaerendeAar}
          setVisForm={setVisForm}
        />
      )}
      {erRedigerbar && !visForm && (
        <HStack marginBlock="4 0">
          <Button
            size="small"
            variant="secondary"
            icon={<PencilIcon title="a11y-title" fontSize="1.5rem" />}
            onClick={(e) => {
              e.preventDefault()
              setVisForm(true)
              resetInntektsavkortingValidering()
            }}
          >
            {knappTekst()}
          </Button>
        </HStack>
      )}
    </VStack>
  )
}
