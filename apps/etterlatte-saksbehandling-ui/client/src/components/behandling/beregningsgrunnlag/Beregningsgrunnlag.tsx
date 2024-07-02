import { SakType } from '~shared/types/sak'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { BodyLong, Box, Button, Heading, Select, TextField } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { hentOverstyrBeregning, hentOverstyrBeregningGrunnlag, opprettOverstyrBeregning } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OverstyrBeregning, OverstyrBeregningGrunnlagPostDTO } from '~shared/types/Beregning'
import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import OverstyrBeregningGrunnlag from './OverstyrBeregningGrunnlag'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'
import { OverstyrtBeregningKategori } from '~shared/types/OverstyrtBeregning'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import styled from 'styled-components'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { statusErRedigerbar } from '~components/behandling/felles/utils'

const Beregningsgrunnlag = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props

  const [overstyrtBeregningGrunnlagRest, getOverstyrBeregningGrunnlag] = useApiCall(hentOverstyrBeregningGrunnlag)
  const [overstyrtBeregningRest, getOverstyrtBeregning] = useApiCall(hentOverstyrBeregning)

  const [overstyrtBeregning, setOverstyrtBeregning] = useState<OverstyrBeregning | undefined>(undefined)
  const [, setOverstyrtBeregningGrunnlag] = useState<OverstyrBeregningGrunnlagPostDTO | undefined>(undefined)

  const vedtaksresultat = useVedtaksResultat()
  const visOverstyrKnapp = useFeatureEnabledMedDefault('overstyr-beregning-knapp', false)

  const [visOverstyrtBeregningGrunnlag, setVisOverstyrtBeregningGrunnlag] = useState<Boolean>(false)
  const [erBehandlingFerdigstilt] = useState<Boolean>(!statusErRedigerbar(behandling.status))

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterDato(behandling.virkningstidspunkt.dato)
    : undefined

  useEffect(() => {
    getOverstyrtBeregning(behandling.id, (result) => {
      if (result) {
        setOverstyrtBeregning(result)

        if (!erBehandlingFerdigstilt) {
          setVisOverstyrtBeregningGrunnlag(true)
        }
      }
    })

    getOverstyrBeregningGrunnlag(behandling.id, (result) => {
      if (result) {
        setOverstyrtBeregningGrunnlag(result)

        if (erBehandlingFerdigstilt) {
          setVisOverstyrtBeregningGrunnlag(result.perioder.length > 0)
        }
      }
    })
  }, [])

  useEffect(() => {
    if (!erBehandlingFerdigstilt && overstyrtBeregning) {
      setVisOverstyrtBeregningGrunnlag(true)
    }
  }, [overstyrtBeregning])

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Beregningsgrunnlag
        </Heading>
        <Vilkaarsresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
      </Box>
      <>
        {isSuccess(overstyrtBeregningRest && overstyrtBeregningGrunnlagRest) && (
          <>
            {visOverstyrKnapp && !erBehandlingFerdigstilt && !overstyrtBeregning && (
              <OverstyrBeregningForGrunnlag behandlingId={behandling.id} setOverstyrt={setOverstyrtBeregning} />
            )}

            {visOverstyrtBeregningGrunnlag && (
              <OverstyrBeregningGrunnlag behandling={behandling} setOverstyrt={setOverstyrtBeregning} />
            )}

            {!visOverstyrtBeregningGrunnlag &&
              {
                [SakType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon behandling={behandling} />,
                [SakType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad behandling={behandling} />,
              }[behandling.sakType]}
          </>
        )}{' '}
        :
        {isFailureHandler({
          apiResult: overstyrtBeregningRest,
          errorMessage: 'Det oppsto en feil.',
        })}
      </>
    </>
  )
}

const OverstyrBeregningForGrunnlag = (props: {
  behandlingId: string
  setOverstyrt: Dispatch<SetStateAction<OverstyrBeregning | undefined>>
}) => {
  const { behandlingId, setOverstyrt } = props
  const [overstyrBeregningStatus, opprettOverstyrtBeregningReq] = useApiCall(opprettOverstyrBeregning)
  const [begrunnelse, setBegrunnelse] = useState<string>('')
  const [kategori, setKategori] = useState<OverstyrtBeregningKategori>()
  const [kategoriError, setKategoriError] = useState<string>('')

  const overstyrBeregning = () => {
    if (!!kategori) {
      setKategoriError('')
      opprettOverstyrtBeregningReq(
        {
          behandlingId,
          beskrivelse: begrunnelse,
          kategori: kategori,
        },
        (result) => {
          if (result) {
            setOverstyrt(result)
          }
        }
      )
    } else {
      setKategoriError('Vennligst velg en kategori for å overstyre beregningen.')
    }
  }

  return (
    <FormWrapper>
      <Heading size="small">Overstyre beregning</Heading>
      <BodyLong>Er det ønskelig å overstyre beregning?</BodyLong>
      <Select
        label="Velg årsak:"
        onChange={(e) => {
          setKategori(e.target.value as OverstyrtBeregningKategori)
        }}
        description="Hvis du ikke finner riktig kategori for å overstyre saken, må du ta kontakt med team Etterlatte"
        error={kategoriError}
      >
        <option value="">Velg kategori</option>
        {Object.entries(OverstyrtBeregningKategori).map(([key, value]) => (
          <option key={key} value={key}>
            {value}
          </option>
        ))}
      </Select>

      <TextField
        onChange={(e) => {
          setBegrunnelse(e.target.value)
          setKategoriError('')
        }}
        label=""
        placeholder="Begrunnelse"
      />

      <Button onClick={overstyrBeregning} loading={isPending(overstyrBeregningStatus)}>
        Overstyr
      </Button>
      {isFailureHandler({
        apiResult: overstyrBeregningStatus,
        errorMessage: 'Det oppsto en feil ved overstyring av behandlingen.',
      })}
    </FormWrapper>
  )
}

const FormWrapper = styled.div`
  padding: 1em 4em;
  width: 25em;
  display: grid;
  gap: var(--a-spacing-4);
`

export default Beregningsgrunnlag
