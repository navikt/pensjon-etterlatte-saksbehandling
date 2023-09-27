import { IEtterbetaling } from '~shared/types/IDetaljertBehandling'
import { Button, Checkbox, Heading } from '@navikt/ds-react'
import React, { useState } from 'react'
import { DatoVelger } from '~shared/DatoVelger'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreEtterbetaling } from '~shared/api/behandling'

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
  etterbetalingInit: IEtterbetaling | null
  redigerbar: boolean
}) => {
  const { etterbetalingInit, redigerbar, behandlingId } = props
  const [status, apiLagreEtterbetaling] = useApiCall(lagreEtterbetaling)
  const [etterbetaling, setEtterbetaling] = useState(etterbetalingInit)
  const [erEtterbetaling, setErEtterbetaling] = useState<boolean>(!!etterbetaling)

  const lagre = () => {
    apiLagreEtterbetaling({ behandlingId, etterbetaling: etterbetaling!! }, () => {})
  }

  const avbryt = () => {}

  return (
    <>
      {redigerbar ? (
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
      ) : erEtterbetaling ? (
        <Heading size="small" level="3">
          Er etterbetaling
        </Heading>
      ) : null}
      {erEtterbetaling ? (
        <EtterbetalingWrapper>
          <DatoSection>
            {redigerbar ? (
              <DatoVelger
                value={etterbetaling?.fraDato == null ? null : new Date(etterbetaling?.fraDato)}
                onChange={(e) => setEtterbetaling({ ...etterbetaling, fraDato: e })}
                label="Fra dato"
              />
            ) : (
              <>Fra dato: {etterbetaling?.fraDato}</>
            )}
          </DatoSection>
          <DatoSection>
            {redigerbar ? (
              <DatoVelger
                value={etterbetaling?.tilDato == null ? null : new Date(etterbetaling?.tilDato)}
                onChange={(e) => setEtterbetaling({ ...etterbetaling, tilDato: e })}
                label="Til dato"
              />
            ) : (
              <>Til dato: {etterbetaling?.tilDato}</>
            )}
          </DatoSection>
        </EtterbetalingWrapper>
      ) : null}
      <FlexRow justify="left">
        <Button variant="secondary" disabled={isPending(status)} onClick={avbryt}>
          Avbryt
        </Button>
        <Button variant="primary" loading={isPending(status)} onClick={lagre}>
          Lagre
        </Button>
      </FlexRow>
    </>
  )
}

export default Etterbetaling
