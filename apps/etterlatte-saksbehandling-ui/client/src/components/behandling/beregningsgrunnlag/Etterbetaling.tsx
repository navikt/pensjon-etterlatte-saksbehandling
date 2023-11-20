import { IEtterbetaling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Button, Checkbox, ErrorMessage, Heading } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreEtterbetaling, slettEtterbetaling } from '~shared/api/behandling'
import { formaterDato, formaterStringDato } from '~utils/formattering'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'
import { Controller, useForm } from 'react-hook-form'
import { ApiErrorAlert } from '~ErrorBoundary'
import { parseISO } from 'date-fns'

const MaanedSection = styled.section`
  display: grid;
  gap: 0.5em;
`

const EtterbetalingWrapper = styled.div`
  display: flex;
  gap: 1rem;
  padding-right: 1rem;
  margin-top: 1rem;
  padding-bottom: 2rem;
`

const Etterbetaling = (props: {
  behandlingId: string
  lagraEtterbetaling: IEtterbetaling | null
  redigerbar: boolean
}) => {
  const { lagraEtterbetaling, redigerbar, behandlingId } = props

  const [status, apiLagreEtterbetaling] = useApiCall(lagreEtterbetaling)
  const [slettStatus, apiSlettEtterbetaling] = useApiCall(slettEtterbetaling)

  const { control, handleSubmit } = useForm({
    defaultValues: lagraEtterbetaling
      ? {
          fraDato: parseISO(lagraEtterbetaling.fraDato),
          tilDato: parseISO(lagraEtterbetaling.tilDato),
        }
      : { fraDato: undefined, tilDato: undefined },
  })
  const [erEtterbetaling, setErEtterbetaling] = useState<boolean>(lagraEtterbetaling != null)

  const featureToggleNameEtterbetaling = 'registrer-etterbetaling'
  const vis = useFeatureEnabledMedDefault(featureToggleNameEtterbetaling, false)

  if (!vis) {
    return null
  }

  const submit = (fstate: { fraDato: Date | undefined; tilDato: Date | undefined }) => {
    const { fraDato, tilDato } = fstate
    apiLagreEtterbetaling({
      behandlingId: behandlingId,
      etterbetaling: {
        fraDato: fraDato ? formaterDato(fraDato) : undefined,
        tilDato: tilDato ? formaterDato(tilDato) : undefined,
      },
    })
  }

  const slett = () => {
    apiSlettEtterbetaling({ behandlingId }, () => {
      setErEtterbetaling(false)
    })
  }

  if (!redigerbar) {
    if (!lagraEtterbetaling) {
      return null
    }

    return (
      <>
        <Heading size="small" level="3">
          Innebærer etterbetaling:
        </Heading>
        <BodyShort>Fra og med måned: {formaterStringDato(lagraEtterbetaling.fraDato)}</BodyShort>
        <BodyShort>Til og med måned: {formaterStringDato(lagraEtterbetaling.tilDato)}</BodyShort>
      </>
    )
  }

  return (
    <form onSubmit={handleSubmit(submit)}>
      <Checkbox
        onChange={() => {
          setErEtterbetaling(!erEtterbetaling)
        }}
        checked={erEtterbetaling}
      >
        <Heading size="small" level="3">
          Innebærer etterbetaling?
        </Heading>
      </Checkbox>
      {erEtterbetaling ? (
        <>
          <EtterbetalingWrapper>
            <Controller
              control={control}
              render={({ field }) => {
                return (
                  <MaanedSection>
                    <MaanedVelger {...field} label="Fra og med måned" />
                  </MaanedSection>
                )
              }}
              name="fraDato"
            />
            <Controller
              control={control}
              render={({ field }) => {
                return (
                  <MaanedSection>
                    <MaanedVelger {...field} label="Til og med måned" />
                  </MaanedSection>
                )
              }}
              name="tilDato"
            />
          </EtterbetalingWrapper>
          <FlexRow justify="left">
            <Button
              variant="secondary"
              disabled={isPending(status)}
              loading={isPending(slettStatus)}
              type="button"
              onClick={slett}
            >
              Slett etterbetaling
            </Button>
            <Button variant="primary" loading={isPending(status)} disabled={isPending(slettStatus)} type="submit">
              Lagre etterbetaling
            </Button>
          </FlexRow>
          {isFailure(status) && <ErrorMessage>{status.error.detail}</ErrorMessage>}
          {isFailure(slettStatus) && <ApiErrorAlert>{slettStatus.error.detail}</ApiErrorAlert>}
        </>
      ) : null}
    </form>
  )
}

export default Etterbetaling
