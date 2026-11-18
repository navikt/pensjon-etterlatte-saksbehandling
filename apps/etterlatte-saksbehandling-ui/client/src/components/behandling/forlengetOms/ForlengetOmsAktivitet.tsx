import { useState } from 'react'
import { Alert, BodyShort, Box, Button, Heading, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { format } from 'date-fns'
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
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { formaterMaanedAar } from '~utils/formatering/dato'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'

export const ForlengetOmsAktivitet = ({ behandling }: { behandling: IBehandlingReducer }) => {
  const dispatch = useAppDispatch()
  const saksbehandler = useInnloggetSaksbehandler()
  const redigerbar = erBehandlingRedigerbar(behandling.status, behandling.sakEnhetId, saksbehandler.skriveEnheter)
  const info = behandling.revurderinginfo?.revurderingInfo
  const lagretPeriode = info?.type === 'FORLENGET_OMS' ? info : undefined
  const fom = behandling.virkningstidspunkt?.dato.slice(0, 7)
  const { control, handleSubmit, watch, setError } = useForm<{ tom: string | null }>({
    defaultValues: { tom: lagretPeriode?.tom ? `${lagretPeriode.tom}-01` : null },
  })
  const tom = watch('tom')?.slice(0, 7) ?? ''
  const [redigerTotalvurdering, setRedigerTotalvurdering] = useState(false)
  const [lagrePeriodeStatus, lagrePeriode] = useApiCall(lagreRevurderingInfo)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const periodeRedigerbar =
    redigerbar && !vilkaarsvurdering?.resultat && behandling.status === IBehandlingStatus.OPPRETTET
  const periodeErLagret = !!fom && lagretPeriode?.fom === fom && lagretPeriode.tom === tom && lagretPeriode.tom >= fom

  const lagre = () => {
    if (!fom || !tom || tom < fom) {
      setError('tom', { message: 'Velg en til og med-måned som er lik eller etter fra og med-måneden.' })
      return
    }

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
      <VStack paddingInline="space-64 space-32" paddingBlock="space-16" gap="space-24">
        <Heading size="medium" level="2">
          Periode for forlenget OMS
        </Heading>
        <BodyShort>Fra og med-måneden settes som virkningstidspunkt i revurderingsoversikten.</BodyShort>
        <form onSubmit={handleSubmit(lagre)}>
          <VStack gap="space-16" maxWidth="30rem">
            <TextField label="Fra og med" value={fom ? formaterMaanedAar(fom) : ''} readOnly />
            {periodeRedigerbar ? (
              <ControlledMaanedVelger
                name="tom"
                label="Til og med"
                control={control}
                fromDate={fom ? new Date(fom) : undefined}
                required
                kunViseMaanedOgAar
                validate={(maaned) => {
                  if (!fom || !maaned || format(maaned, 'yyyy-MM') < fom) {
                    return 'Velg en til og med-måned som er lik eller etter fra og med-måneden.'
                  }
                }}
              />
            ) : (
              <TextField label="Til og med" value={tom ? formaterMaanedAar(tom) : ''} readOnly />
            )}
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
