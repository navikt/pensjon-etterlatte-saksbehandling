import { SakType } from '~shared/types/sak'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { BodyLong, Box, Button, Heading, Select, TextField } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { hentOverstyrBeregning, opprettOverstyrBeregning } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OverstyrBeregning } from '~shared/types/Beregning'
import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import OverstyrBeregningGrunnlag from './OverstyrBeregningGrunnlag'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'
import { KATEGORI } from '~shared/types/OverstyrtBeregning'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import styled from 'styled-components'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const Beregningsgrunnlag = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props

  const [overstyrtBeregning, getOverstyrtBeregning] = useApiCall(hentOverstyrBeregning)
  const [overstyrt, setOverstyrt] = useState<OverstyrBeregning | undefined>(undefined)
  const vedtaksresultat = useVedtaksResultat()
  const visOverstyrKnapp = useFeatureEnabledMedDefault('overstyr-beregning-knapp', false)

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined

  useEffect(() => {
    getOverstyrtBeregning(behandling.id, (result) => {
      if (result) {
        setOverstyrt(result)
      }
    })
  }, [])

  return (
    <>
      <Box paddingInline="16" paddingBlock="16 4">
        <Heading spacing size="large" level="1">
          Beregningsgrunnlag
        </Heading>
        <Vilkaarsresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
      </Box>
      <>
        {isSuccess(overstyrtBeregning) && (
          <>
            {visOverstyrKnapp && !overstyrt && (
              <OverstyrBeregningForGrunnlag behandlingId={behandling.id} setOverstyrt={setOverstyrt} />
            )}
            {overstyrt && (
              <OverstyrBeregningGrunnlag
                behandling={behandling}
                overstyrBeregning={overstyrt}
                setOverstyrt={setOverstyrt}
              />
            )}
            {!overstyrt &&
              {
                [SakType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon behandling={behandling} />,
                [SakType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad behandling={behandling} />,
              }[behandling.sakType]}
          </>
        )}
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
  const [kategori, setKategori] = useState<KATEGORI>()

  const overstyrBeregning = () => {
    opprettOverstyrtBeregningReq(
      {
        behandlingId,
        beskrivelse: begrunnelse,

        // @ts-expect-error ignorere undefined
        kategori: kategori,
      },
      (result) => {
        if (result) {
          setOverstyrt(result)
        }
      }
    )
  }

  return (
    <FormWrapper>
      <Heading size="small">Overstyre beregning</Heading>
      <BodyLong>Er det ønskelig å overstyre beregning?</BodyLong>
      <Select
        label="Velg årsak til overstyring:"
        onChange={(e) => {
          setKategori(e.target.value as KATEGORI)
        }}
      >
        <option value="">Velg kategori</option>
        {Object.entries(KATEGORI).map(([key, value]) => (
          <option key={key} value={key}>
            {value}
          </option>
        ))}
      </Select>
      <TextField
        onChange={(e) => {
          setBegrunnelse(e.target.value)
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
