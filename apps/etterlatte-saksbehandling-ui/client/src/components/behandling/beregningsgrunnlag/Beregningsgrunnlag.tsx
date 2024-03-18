import { SakType } from '~shared/types/sak'
import BeregningsgrunnlagBarnepensjon from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagBarnepensjon'
import BeregningsgrunnlagOmstillingsstoenad from '~components/behandling/beregningsgrunnlag/BeregningsgrunnlagOmstillingsstoenad'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { BodyLong, Button, Heading, TextField } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { Content, ContentHeader } from '~shared/styled'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { useVedtaksResultat } from '~components/behandling/useVedtaksResultat'
import { hentOverstyrBeregning, opprettOverstyrBeregning } from '~shared/api/beregning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { OverstyrBeregning } from '~shared/types/Beregning'
import React, { Dispatch, SetStateAction, useEffect, useState } from 'react'
import OverstyrBeregningGrunnlag from './OverstyrBeregningGrunnlag'
import { Vilkaarsresultat } from '~components/behandling/felles/Vilkaarsresultat'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import styled from 'styled-components'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const Beregningsgrunnlag = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props

  const [overstyrBeregning, getOverstyrBeregning] = useApiCall(hentOverstyrBeregning)
  const [overstyrt, setOverstyrt] = useState<OverstyrBeregning | undefined>(undefined)
  const vedtaksresultat = useVedtaksResultat()
  const visOverstyrKnapp = useFeatureEnabledMedDefault('overstyr-beregning-knapp', false)

  const virkningstidspunkt = behandling.virkningstidspunkt?.dato
    ? formaterStringDato(behandling.virkningstidspunkt.dato)
    : undefined

  useEffect(() => {
    getOverstyrBeregning(behandling.id, (result) => {
      if (result) {
        setOverstyrt(result || undefined)
      }
    })
  }, [])

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Beregningsgrunnlag
          </Heading>
          <Vilkaarsresultat vedtaksresultat={vedtaksresultat} virkningstidspunktFormatert={virkningstidspunkt} />
        </HeadingWrapper>
      </ContentHeader>
      <>
        {isSuccess(overstyrBeregning) && (
          <>
            {visOverstyrKnapp && !overstyrt && (
              <OverstyrBeregning behandlingId={behandling.id} setOverstyrt={setOverstyrt} />
            )}
            {overstyrt && <OverstyrBeregningGrunnlag behandling={behandling} overstyrBeregning={overstyrt} />}
            {!overstyrt &&
              {
                [SakType.BARNEPENSJON]: <BeregningsgrunnlagBarnepensjon behandling={behandling} />,
                [SakType.OMSTILLINGSSTOENAD]: <BeregningsgrunnlagOmstillingsstoenad behandling={behandling} />,
              }[behandling.sakType]}
          </>
        )}
      </>
    </Content>
  )
}

const OverstyrBeregning = (props: {
  behandlingId: string
  setOverstyrt: Dispatch<SetStateAction<OverstyrBeregning | undefined>>
}) => {
  const { behandlingId, setOverstyrt } = props
  const [overstyrBeregningStatus, opprettOverstyrtBeregningReq] = useApiCall(opprettOverstyrBeregning)
  const [begrunnelse, setBegrunnelse] = useState<string>('')
  const overstyrBeregning = () => {
    opprettOverstyrtBeregningReq(
      {
        behandlingId,
        beskrivelse: begrunnelse,
      },
      (result) => {
        if (result) {
          setOverstyrt(result || undefined)
        }
      }
    )
  }

  return (
    <FormWrapper>
      <Heading size="small">Overstyre beregning</Heading>
      <BodyLong>Er det ønskelig å overstyre beregning?</BodyLong>
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
