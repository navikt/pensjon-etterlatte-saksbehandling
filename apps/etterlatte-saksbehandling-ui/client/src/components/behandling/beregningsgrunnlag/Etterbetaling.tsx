import { IEtterbetaling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Button, Checkbox, ErrorMessage, Heading } from '@navikt/ds-react'
import React, { useState } from 'react'
import styled from 'styled-components'
import { FlexRow } from '~shared/styled'
import { isPending, useApiCall } from '~shared/hooks/useApiCall'
import { lagreEtterbetaling, slettEtterbetaling } from '~shared/api/behandling'
import { formaterKanskjeStringDato } from '~utils/formattering'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import MaanedVelger from '~components/behandling/beregningsgrunnlag/MaanedVelger'

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

const dato = (str: string | undefined) => {
  if (!str) {
    return undefined
  }
  const splitted = str.split('.')
  const dato = new Date()
  dato.setDate(Number(splitted[0]))
  dato.setMonth(Number(splitted[1]) - 1) // -1 for å justere for at Date-objektet bruker 0-indeksering
  dato.setFullYear(Number(splitted[2]))
  dato.setHours(12)
  dato.setMinutes(0)
  dato.setSeconds(0)
  dato.setMilliseconds(0)
  return dato
}

const Etterbetaling = (props: {
  behandlingId: string
  lagraEtterbetaling: IEtterbetaling | null
  redigerbar: boolean
  virkningstidspunkt: string | undefined
}) => {
  const { lagraEtterbetaling, redigerbar, behandlingId, virkningstidspunkt } = props

  const [status, apiLagreEtterbetaling] = useApiCall(lagreEtterbetaling)
  const [, apiSlettEtterbetaling] = useApiCall(slettEtterbetaling)
  const [etterbetaling, setEtterbetaling] = useState(lagraEtterbetaling)
  const [erEtterbetaling, setErEtterbetaling] = useState<boolean>(!!etterbetaling)

  const featureToggleNameEtterbetaling = 'registrer-etterbetaling'
  const vis = useFeatureEnabledMedDefault(featureToggleNameEtterbetaling, false)
  const [errorTekst, setErrorTekst] = useState('')

  const valider = () => {
    if (!erEtterbetaling) {
      return ''
    }
    const fraDato = etterbetaling?.fraDato
    const tilDato = etterbetaling?.tilDato
    if (!fraDato || !tilDato) {
      return 'Både fra- og til-måned for etterbetaling må fylles ut.'
    }
    const fra = new Date(fraDato!!)
    const til = new Date(tilDato!!)
    if (fra > til) {
      return 'Fra-måned kan ikke være etter til-måned.'
    }
    if (virkningstidspunkt && fra < dato(virkningstidspunkt)!!) {
      return 'Fra-måned kan ikke være før virkningstidspunkt.'
    }
    if (til > new Date()) {
      return 'Til-måned kan ikke være i framtida.'
    }
    return ''
  }

  const lagre = () => {
    if (erEtterbetaling) {
      const feil = valider()
      setErrorTekst(feil)
      if (feil !== '') {
        return
      }
      apiLagreEtterbetaling({ behandlingId, etterbetaling: etterbetaling!! }, () => {})
    } else {
      apiSlettEtterbetaling({ behandlingId }, () => {})
    }
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
          Innebærer etterbetaling?
        </Heading>
        <BodyShort>Fra og med måned: {formaterKanskjeStringDato(etterbetaling?.fraDato?.toString())}</BodyShort>
        <BodyShort>Til og med måned: {formaterKanskjeStringDato(etterbetaling?.tilDato?.toString())}</BodyShort>
      </>
    )
  }

  return (
    <>
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
        <EtterbetalingWrapper>
          <MaanedSection>
            <MaanedVelger
              value={etterbetaling?.fraDato ? new Date(etterbetaling?.fraDato) : undefined}
              onChange={(e) => setEtterbetaling({ ...etterbetaling, fraDato: e })}
              label="Fra og med måned"
            />
          </MaanedSection>
          <MaanedSection>
            <MaanedVelger
              value={etterbetaling?.tilDato ? new Date(etterbetaling?.tilDato) : undefined}
              onChange={(e) => setEtterbetaling({ ...etterbetaling, tilDato: e })}
              label="Til og med måned"
            />
          </MaanedSection>
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
      {errorTekst !== '' ? <ErrorMessage>{errorTekst}</ErrorMessage> : null}
    </>
  )
}

export default Etterbetaling
