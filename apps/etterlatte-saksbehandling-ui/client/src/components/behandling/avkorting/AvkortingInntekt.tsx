import { Alert, BodyShort, Button, Heading, HelpText, HStack, Table, Tooltip, VStack } from '@navikt/ds-react'
import React, { useState } from 'react'
import { NOK } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { TextButton } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/TextButton'
import { ToolTip } from '~components/behandling/felles/ToolTip'
import { enhetErSkrivbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { lastDayOfMonth } from 'date-fns'
import { AvkortingInntektForm } from '~components/behandling/avkorting/AvkortingInntektForm'
import { IAvkortingGrunnlagFrontend } from '~shared/types/IAvkorting'
import { ArrowCirclepathIcon, PencilIcon } from '@navikt/aksel-icons'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

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
                    Forventet inntekt totalt
                    <HelpText title="Hva innebærer forventet inntekt totalt">
                      Forventet inntekt totalt er registrert inntekt Norge pluss inntekt utland minus eventuelt fratrekk
                      for inn-år. Beløpet vil automatisk avrundes ned til nærmeste tusen når avkorting beregnes.
                    </HelpText>
                  </Table.HeaderCell>
                  <Table.HeaderCell>
                    Innvilgede måneder
                    <HelpText title="Hva betyr innvilgede måneder">
                      Her vises antall måneder med innvilget stønad i gjeldende inntektsår. Registrert forventet
                      inntekt, med eventuelt fratrekk for inntekt opptjent før/etter innvilgelse, blir fordelt på de
                      innvilgede månedene. Antallet vil ikke endres selv om man tar en inntektsendring i løpet av året.
                    </HelpText>
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
                  const inntektutland = avkortingGrunnlag.inntektUtlandTom ?? 0
                  const fratrekkUtland = avkortingGrunnlag.fratrekkInnAarUtland ?? 0
                  const forventetInntektUtland = inntektutland - fratrekkUtland
                  return (
                    <Table.Row key={index}>
                      <Table.DataCell key="Inntekt">
                        {NOK(forventetInntekt)}
                        <ToolTip title="Se hva forventet inntekt består av">
                          Forventet inntekt beregnes utfra forventet årsinntekt med fratrekk for måneder før
                          innvilgelse.
                          <br />
                          Forventet inntekt Norge = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
                          {` ${NOK(aarsinntekt)} - ${NOK(fratrekkInnAar)} = ${NOK(forventetInntekt)}`}).
                        </ToolTip>
                      </Table.DataCell>
                      <Table.DataCell key="InntektUtland">
                        {NOK(forventetInntektUtland)}
                        <ToolTip title="Se hva forventet inntekt består av">
                          Forventet inntekt utland beregnes utfra inntekt utland med fratrekk for måneder før
                          innvilgelse.
                          <br />
                          Forventet inntekt utland = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
                          {` ${NOK(inntektutland)} - ${NOK(fratrekkUtland)} = ${NOK(forventetInntektUtland)}`}).
                        </ToolTip>
                      </Table.DataCell>
                      <Table.DataCell key="InntektTotalt">
                        {NOK(forventetInntekt + forventetInntektUtland)}
                      </Table.DataCell>
                      <Table.DataCell>
                        <HStack gap="4" align="center">
                          <BodyShort>{avkortingGrunnlag.innvilgaMaaneder}</BodyShort>
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
