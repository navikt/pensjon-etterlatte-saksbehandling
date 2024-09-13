import { Heading, HelpText, Table } from '@navikt/ds-react'
import styled from 'styled-components'
import React, { useEffect, useState } from 'react'
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
import { virkningstidspunkt } from '~shared/types/IDetaljertBehandling'
import { IAvkortingGrunnlagForm } from '~shared/types/IAvkorting'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAvkortingGrunnlag } from '~shared/api/avkorting'

export const AvkortingInntekt = ({
  behandling,
  innevaerendeAar,
  redigerbar,
  resetInntektsavkortingValidering,
}: {
  behandling: IBehandlingReducer
  innevaerendeAar: boolean
  redigerbar: boolean
  resetInntektsavkortingValidering: () => void
}) => {
  const [, avkortingGrunnlagFormRequest] = useApiCall(hentAvkortingGrunnlag)
  const [avkortingGrunnlagForm, setAvkortingGrunnlagForm] = useState<IAvkortingGrunnlagForm>(null)

  const erRedigerbar = redigerbar && enhetErSkrivbar(behandling.sakEnhetId, useInnloggetSaksbehandler().skriveEnheter)
  const [visHistorikk, setVisHistorikk] = useState(false)

  const virkningstidspunktDate = new Date(virkningstidspunkt(behandling).dato)

  useEffect(() => {
    avkortingGrunnlagFormRequest(
      {
        behandlingId: behandling.id,
        aar: innevaerendeAar ? virkningstidspunktDate.getFullYear() : virkningstidspunktDate.getFullYear() + 1,
      },
      (data) => {
        setAvkortingGrunnlagForm(data)
      }
    )
  }, [])

  const listeVisningAvkortingGrunnlag = () => {
    if (visHistorikk) {
      return avkortingGrunnlagForm.fraVirk
        ? [avkortingGrunnlagForm.fraVirk].concat(avkortingGrunnlagForm.historikk)
        : avkortingGrunnlagForm.historikk
    } else {
      return [avkortingGrunnlagForm.fraVirk ?? avkortingGrunnlagForm.historikk[0]]
    }
  }

  return (
    <AvkortingInntektWrapper>
      {avkortingGrunnlagForm && (avkortingGrunnlagForm?.fraVirk || avkortingGrunnlagForm?.historikk.length > 0) && (
        <InntektAvkortingTabell>
          <Heading size="small">{avkortingGrunnlagForm.aar}</Heading>
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
                    Her vises antall måneder med innvilget stønad i gjeldende inntektsår. Registrert forventet inntekt,
                    med eventuelt fratrekk for inntekt opptjent før/etter innvilgelse, blir fordelt på de innvilgede
                    månedene. Antallet vil ikke endres selv om man tar en inntektsendring i løpet av året.
                  </HelpText>
                </Table.HeaderCell>
                <Table.HeaderCell>Periode</Table.HeaderCell>
                <Table.HeaderCell>Spesifikasjon av inntekt</Table.HeaderCell>
                <Table.HeaderCell>Kilde</Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {listeVisningAvkortingGrunnlag().map((avkortingGrunnlag, index) => {
                const aarsinntekt = avkortingGrunnlag.aarsinntekt ?? 0
                const fratrekkInnAar = avkortingGrunnlag.fratrekkInnAar ?? 0
                const forventetInntekt = aarsinntekt - fratrekkInnAar
                const inntektutland = avkortingGrunnlag.inntektUtland ?? 0
                const fratrekkUtland = avkortingGrunnlag.fratrekkInnAarUtland ?? 0
                const forventetInntektUtland = inntektutland - fratrekkUtland
                return (
                  <Table.Row key={index}>
                    <Table.DataCell key="Inntekt">
                      {NOK(forventetInntekt)}
                      <ToolTip title="Se hva forventet inntekt består av">
                        Forventet inntekt beregnes utfra forventet årsinntekt med fratrekk for måneder før innvilgelse.
                        <br />
                        Forventet inntekt Norge = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
                        {` ${NOK(aarsinntekt)} - ${NOK(fratrekkInnAar)} = ${NOK(forventetInntekt)}`}).
                      </ToolTip>
                    </Table.DataCell>
                    <Table.DataCell key="InntektUtland">
                      {NOK(forventetInntektUtland)}
                      <ToolTip title="Se hva forventet inntekt består av">
                        Forventet inntekt utland beregnes utfra inntekt utland med fratrekk for måneder før innvilgelse.
                        <br />
                        Forventet inntekt utland = forventet årsinntekt - inntekt i måneder før innvilgelse måneder (
                        {` ${NOK(inntektutland)} - ${NOK(fratrekkUtland)} = ${NOK(forventetInntektUtland)}`}).
                      </ToolTip>
                    </Table.DataCell>
                    <Table.DataCell key="InntektTotalt">
                      {NOK(forventetInntekt + forventetInntektUtland)}
                    </Table.DataCell>
                    <Table.DataCell>{avkortingGrunnlag.relevanteMaanederInnAar}</Table.DataCell>
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
        </InntektAvkortingTabell>
      )}
      {avkortingGrunnlagForm?.historikk.length > 0 && <TextButton isOpen={visHistorikk} setIsOpen={setVisHistorikk} />}

      {erRedigerbar && avkortingGrunnlagForm && (
        <InntektAvkortingForm>
          <AvkortingInntektForm
            behandling={behandling}
            avkortingGrunnlagForm={avkortingGrunnlagForm}
            innevaerendeAar={innevaerendeAar}
            resetInntektsavkortingValidering={resetInntektsavkortingValidering}
          />
        </InntektAvkortingForm>
      )}
    </AvkortingInntektWrapper>
  )
}

const AvkortingInntektWrapper = styled.div`
  max-width: 70rem;
`

const InntektAvkortingTabell = styled.div`
  margin: 1em 0 1em 0;
`

const InntektAvkortingForm = styled.form`
  display: flex;
  margin: 1em 0 0 0;
`
