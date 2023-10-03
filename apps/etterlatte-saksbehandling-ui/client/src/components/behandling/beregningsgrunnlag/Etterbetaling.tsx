import { IEtterbetaling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Button, Checkbox, Heading } from '@navikt/ds-react'
import React, { useState } from 'react'
import { DatoVelger } from '~shared/DatoVelger'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreEtterbetaling } from '~shared/api/behandling'
import { formaterKanskjeStringDato } from '~utils/formattering'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'

const DatoSection = styled.section`
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
  const [etterbetaling, setEtterbetaling] = useState(lagraEtterbetaling)
  const [erEtterbetaling, setErEtterbetaling] = useState<boolean>(!!etterbetaling)

  const featureToggleNameEtterbetaling = 'registrer-etterbetaling'
  const vis = useFeatureEnabledMedDefault(featureToggleNameEtterbetaling, false)

  const lagre = () => {
    apiLagreEtterbetaling({ behandlingId, etterbetaling: etterbetaling!! }, () => {})
  }

  const avbryt = () => {
    setEtterbetaling(lagraEtterbetaling)
  }

  if (!vis) {
    return null
  }
  if (!redigerbar) {
    if (!erEtterbetaling) {
      return null
    }

    return (
      <>
        <Heading size="small" level="3">
          Er etterbetaling
        </Heading>
        <BodyShort>Fra dato: {formaterKanskjeStringDato(etterbetaling?.fraDato?.toString())}</BodyShort>
        <BodyShort>Til dato: {formaterKanskjeStringDato(etterbetaling?.tilDato?.toString())}</BodyShort>
      </>
    )
  }

  return (
    <>
      <Checkbox
        value={erEtterbetaling}
        onChange={() => {
          setErEtterbetaling(!erEtterbetaling)
        }}
      >
        <Heading size="small" level="3">
          Innebærer etterbetaling?
        </Heading>
      </Checkbox>
      {erEtterbetaling ? (
        <EtterbetalingWrapper>
          <DatoSection>
            <DatoVelger
              value={etterbetaling?.fraDato ? new Date(etterbetaling?.fraDato) : undefined}
              onChange={(e) => setEtterbetaling({ ...etterbetaling, fraDato: e })}
              label="Fra dato"
            />
          </DatoSection>
          <DatoSection>
            <DatoVelger
              value={etterbetaling?.tilDato ? new Date(etterbetaling?.tilDato) : undefined}
              onChange={(e) => setEtterbetaling({ ...etterbetaling, tilDato: e })}
              label="Til dato"
            />
          </DatoSection>
        </EtterbetalingWrapper>
      ) : null}
      <FlexRow justify="left">
        <Button variant="secondary" disabled={isPending(status)} onClick={avbryt}>
          Avbryt
        </Button>
        <Button variant="primary" loading={isPending(status)} onClick={lagre}>
          Lagre etterbetaling
        </Button>
      </FlexRow>
    </>
  )
}

export default Etterbetaling
