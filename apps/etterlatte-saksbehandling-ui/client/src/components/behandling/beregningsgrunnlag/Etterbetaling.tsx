import { IEtterbetaling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Button, Checkbox, Heading } from '@navikt/ds-react'
import React, { useEffect, useState } from 'react'
import { DatoVelger } from '~shared/DatoVelger'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreEtterbetaling } from '~shared/api/behandling'
import { hentFunksjonsbrytere } from '~shared/api/feature'
import { formaterKanskjeStringDato } from '~utils/formattering'

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
  const [vis, setVis] = useState<boolean>(false)

  const [funksjonsbrytere, postHentFunksjonsbrytere] = useApiCall(hentFunksjonsbrytere)
  const featureToggleNameEtterbetaling = 'registrer-etterbetaling'

  useEffect(() => {
    postHentFunksjonsbrytere([featureToggleNameEtterbetaling], (brytere) => {
      const etterbetalingBryter = brytere.find((bryter) => bryter.toggle === featureToggleNameEtterbetaling)
      if (etterbetalingBryter) {
        setVis(etterbetalingBryter.enabled)
      }
    })
  }, [])

  const lagre = () => {
    apiLagreEtterbetaling({ behandlingId, etterbetaling: etterbetaling!! }, () => {})
  }

  const avbryt = () => {
    setEtterbetaling(etterbetalingInit)
  }

  if (!vis || !isSuccess(funksjonsbrytere)) {
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
              value={!etterbetaling?.fraDato ? null : new Date(etterbetaling?.fraDato)}
              onChange={(e) => setEtterbetaling({ ...etterbetaling, fraDato: e })}
              label="Fra dato"
            />
          </DatoSection>
          <DatoSection>
            <DatoVelger
              value={!etterbetaling?.tilDato ? null : new Date(etterbetaling?.tilDato)}
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
