import { ReactNode } from 'react'
import { Alert, Button, HGrid, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { format } from 'date-fns'
import { IBehandlingReducer, oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { isPending, mapFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ForlengetOmsInfo } from '~shared/types/RevurderingInfo'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { formaterMaanedAar } from '~utils/formatering/dato'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'

type Props = {
  behandling: IBehandlingReducer
  redigerbar: boolean
  resultatBegrunnelse: { tekst: string; onChange: (tekst: string) => void }
  validerAktivitet: () => boolean
  innvilgelseFeil?: string
  children: ReactNode
}

export const ForlengetOmsPeriodeOgResultat = ({
  behandling,
  redigerbar,
  resultatBegrunnelse,
  validerAktivitet,
  innvilgelseFeil,
  children,
}: Props) => {
  const dispatch = useAppDispatch()
  const info = behandling.revurderinginfo?.revurderingInfo
  const lagretPeriode = info?.type === 'FORLENGET_OMS' ? info : undefined
  const fom = behandling.virkningstidspunkt?.dato.slice(0, 7)
  const { control, handleSubmit, watch, setError } = useForm<{ tom: string | null }>({
    defaultValues: { tom: lagretPeriode?.tom ? `${lagretPeriode.tom}-01` : null },
  })
  const tom = watch('tom')?.slice(0, 7) ?? ''
  const [lagrePeriodeStatus, lagrePeriode] = useApiCall(lagreRevurderingInfo)
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const periodeRedigerbar =
    redigerbar && !vilkaarsvurdering?.resultat && behandling.status === IBehandlingStatus.OPPRETTET
  const periodeErLagret = !!fom && lagretPeriode?.fom === fom && lagretPeriode.tom === tom && lagretPeriode.tom >= fom

  const lagre = (onSuccess: () => void) => {
    if (periodeErLagret) {
      onSuccess()
      return
    }

    if (!fom || !tom || tom < fom) {
      setError('tom', { message: 'Velg en til og med-måned som er lik eller etter fra og med-måneden.' })
      return
    }

    const revurderingInfo: ForlengetOmsInfo = { type: 'FORLENGET_OMS', fom, tom }
    lagrePeriode({ behandlingId: behandling.id, revurderingInfo }, () => {
      dispatch(oppdaterRevurderingInfo({ revurderingInfo, begrunnelse: undefined }))
      onSuccess()
    })
  }

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-32" gap="space-24">
        <VStack gap="space-16" maxWidth="30rem">
          <HGrid columns={2} gap="space-16">
            <TextField label="Fra dato" value={fom ? formaterMaanedAar(fom) : ''} readOnly />
            {periodeRedigerbar ? (
              <ControlledMaanedVelger
                name="tom"
                label="Til dato"
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
              <TextField label="Til dato" value={tom ? formaterMaanedAar(tom) : ''} readOnly />
            )}
          </HGrid>
          {mapFailure(lagrePeriodeStatus, (error) => (
            <ApiErrorAlert>Kunne ikke lagre perioden: {error.detail}</ApiErrorAlert>
          ))}
        </VStack>
        {children}
        {!periodeErLagret && (
          <Alert variant="info">
            Innvilgelse krever en fra og med- og til og med-måned. Perioden lagres sammen med utfallet. Avslag krever
            ingen periode.
          </Alert>
        )}
        {!vilkaarsvurdering && <Alert variant="info">Vilkårene må vurderes på vilkårsvurderingssiden først.</Alert>}
      </VStack>
      {!vilkaarsvurdering && (
        <BehandlingHandlingKnapper>
          <Button disabled>Neste</Button>
        </BehandlingHandlingKnapper>
      )}
      {vilkaarsvurdering && (
        <ResultatForlengetOms
          behandling={behandling}
          redigerbar={redigerbar}
          resultatBegrunnelse={resultatBegrunnelse}
          innvilgelseFeil={innvilgelseFeil}
          lagrePeriode={(onSuccess) => {
            void handleSubmit(() => lagre(onSuccess))()
          }}
          lagrerPeriode={isPending(lagrePeriodeStatus)}
          validerAktivitet={validerAktivitet}
        />
      )}
    </>
  )
}
