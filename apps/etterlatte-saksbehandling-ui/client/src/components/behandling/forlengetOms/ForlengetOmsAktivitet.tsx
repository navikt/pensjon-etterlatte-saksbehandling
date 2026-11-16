import { FormEvent, useState } from 'react'
import { Alert, BodyShort, Box, Button, Heading, TextField, VStack } from '@navikt/ds-react'
import { IBehandlingReducer, oppdaterRevurderingInfo, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { erBehandlingRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Resultat } from '~components/behandling/vilkaarsvurdering/Resultat'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ForlengetOmsInfo } from '~shared/types/RevurderingInfo'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { ForlengetOmsUnderUtvikling } from './ForlengetOmsUnderUtvikling'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'

export const ForlengetOmsAktivitet = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const dispatch = useAppDispatch()
  const saksbehandler = useInnloggetSaksbehandler()
  const redigerbar = erBehandlingRedigerbar(behandling.status, behandling.sakEnhetId, saksbehandler.skriveEnheter)
  const info = behandling.revurderinginfo?.revurderingInfo
  const lagretPeriode = info?.type === 'FORLENGET_OMS' ? info : undefined
  const fom = behandling.virkningstidspunkt?.dato.slice(0, 7)
  const [tom, setTom] = useState(lagretPeriode?.tom ?? '')
  const [periodeFeil, setPeriodeFeil] = useState<string>()
  const [redigerTotalvurdering, setRedigerTotalvurdering] = useState(false)
  const [lagrePeriodeStatus, lagrePeriode] = useApiCall(lagreRevurderingInfo)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const periodeRedigerbar =
    redigerbar && !vilkaarsvurdering?.resultat && behandling.status === IBehandlingStatus.OPPRETTET
  const periodeErLagret = !!fom && lagretPeriode?.fom === fom && lagretPeriode.tom === tom && lagretPeriode.tom >= fom

  const lagre = (event: FormEvent) => {
    event.preventDefault()
    if (!fom || !tom || tom < fom) {
      setPeriodeFeil('Velg en til og med-måned som er lik eller etter fra og med-måneden.')
      return
    }

    setPeriodeFeil(undefined)
    const revurderingInfo: ForlengetOmsInfo = { type: 'FORLENGET_OMS', fom, tom }
    lagrePeriode({ behandlingId: behandling.id, revurderingInfo }, () =>
      dispatch(oppdaterRevurderingInfo({ revurderingInfo, begrunnelse: undefined }))
    )
  }

  return (
    <>
      <Box paddingInline="space-64" paddingBlock="space-48 space-16">
        <Heading size="large" level="1">
          Aktivitet – forlenget OMS
        </Heading>
      </Box>
      <ForlengetOmsUnderUtvikling />
      <VStack paddingInline="space-64 space-32" paddingBlock="space-16" gap="space-24">
        <Heading size="medium" level="2">
          Periode for forlenget OMS
        </Heading>
        <BodyShort>Fra og med-måneden settes som virkningstidspunkt i revurderingsoversikten.</BodyShort>
        <form onSubmit={lagre}>
          <VStack gap="space-16" maxWidth="30rem">
            <TextField type="month" label="Fra og med" value={fom ?? ''} readOnly />
            <TextField
              type="month"
              label="Til og med"
              value={tom}
              min={fom}
              readOnly={!periodeRedigerbar}
              onChange={(event) => {
                setTom(event.target.value)
                setPeriodeFeil(undefined)
              }}
              error={periodeFeil}
            />
            {periodeRedigerbar && (
              <Button type="submit" loading={isPending(lagrePeriodeStatus)}>
                Lagre periode
              </Button>
            )}
            {mapFailure(lagrePeriodeStatus, (error) => (
              <ApiErrorAlert>Kunne ikke lagre perioden: {error.detail}</ApiErrorAlert>
            ))}
          </VStack>
        </form>
        {!periodeErLagret && (
          <Alert variant="info">Lagre en gyldig fra og med- og til og med-måned før utfallet kan vurderes.</Alert>
        )}
        {!vilkaarsvurdering && <Alert variant="info">Vilkårene må vurderes på vilkårsvurderingssiden først.</Alert>}
      </VStack>
      {(!periodeErLagret || !vilkaarsvurdering) && (
        <BehandlingHandlingKnapper>
          <Button disabled>Neste</Button>
        </BehandlingHandlingKnapper>
      )}
      {periodeErLagret && vilkaarsvurdering && (
        <Resultat
          setRedigerTotalvurdering={setRedigerTotalvurdering}
          redigerTotalvurdering={redigerTotalvurdering}
          virkningstidspunktDato={behandling.virkningstidspunkt?.dato}
          sakstype={behandling.sakType}
          vilkaarsvurdering={vilkaarsvurdering}
          oppdaterVilkaar={(vurdering) => dispatch(updateVilkaarsvurdering(vurdering))}
          behandlingId={behandling.id}
          redigerbar={redigerbar && !isPending(lagrePeriodeStatus)}
          behandlingstype={behandling.behandlingType}
          revurderingsaarsak={behandling.revurderingsaarsak}
        />
      )}
    </>
  )
}
