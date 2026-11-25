import { ReactNode } from 'react'
import { Alert, HGrid, TextField, VStack } from '@navikt/ds-react'
import { useForm } from 'react-hook-form'
import { format } from 'date-fns'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { ForlengetOmsInfo } from '~shared/types/RevurderingInfo'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { formaterMaanedAar } from '~utils/formatering/dato'
import { ControlledMaanedVelger } from '~shared/components/maanedVelger/ControlledMaanedVelger'
import { ForlengetOmsAktivitet, ForlengetOmsVurdering } from '~shared/api/forlengetOms'
import { VilkaarsvurderingResultat } from '~shared/api/vilkaarsvurdering'

type Props = {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  aktivitet?: ForlengetOmsAktivitet
  lagrer: boolean
  onLagrerChange: (lagrer: boolean) => void
  onLagret: (vurdering: ForlengetOmsVurdering) => void
  resultatBegrunnelse: { tekst: string; onChange: (tekst: string) => void }
  validerAktivitet: () => boolean
  innvilgelseFeil?: string
  children: ReactNode
}

export const ForlengetOmsPeriodeOgResultat = ({
  behandling,
  redigerbar,
  vurdering,
  aktivitet,
  lagrer,
  onLagrerChange,
  onLagret,
  resultatBegrunnelse,
  validerAktivitet,
  innvilgelseFeil,
  children,
}: Props) => {
  const info = behandling.revurderinginfo?.revurderingInfo
  const lagretPeriode = info?.type === 'FORLENGET_OMS' ? info : undefined
  const fom = behandling.virkningstidspunkt?.dato.slice(0, 7)
  const { control, handleSubmit, watch, setError } = useForm<{ tom: string | null }>({
    defaultValues: { tom: lagretPeriode?.tom ? `${lagretPeriode.tom}-01` : null },
  })
  const tom = watch('tom')?.slice(0, 7) ?? ''
  const vilkaarsvurdering = behandling.vilkaarsvurdering
  const avslagErLagret = vilkaarsvurdering?.resultat?.utfall === VilkaarsvurderingResultat.IKKE_OPPFYLT
  const periodeRedigerbar =
    redigerbar && !lagrer && !vilkaarsvurdering?.resultat && behandling.status === IBehandlingStatus.OPPRETTET
  const periodeErLagret = !!fom && lagretPeriode?.fom === fom && lagretPeriode.tom === tom && lagretPeriode.tom >= fom

  const validerPeriode = (onSuccess: (periode: ForlengetOmsInfo) => void) => {
    if (!fom || !tom || tom < fom) {
      setError('tom', { message: 'Velg en til og med-måned som er lik eller etter fra og med-måneden.' })
      return
    }

    const revurderingInfo: ForlengetOmsInfo = { type: 'FORLENGET_OMS', fom, tom }
    onSuccess(revurderingInfo)
  }

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-32" gap="space-24">
        {!avslagErLagret && (
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
          </VStack>
        )}
        {children}
        {!periodeErLagret && !vilkaarsvurdering?.resultat && (
          <Alert variant="info">
            Innvilgelse krever en fra og med- og til og med-måned. Perioden lagres sammen med utfallet. Avslag krever
            ingen periode.
          </Alert>
        )}
        {!vilkaarsvurdering && <Alert variant="info">Vilkårene må vurderes på vilkårsvurderingssiden først.</Alert>}
      </VStack>
      <ResultatForlengetOms
        behandling={behandling}
        redigerbar={redigerbar}
        vurdering={vurdering}
        aktivitet={aktivitet}
        onLagrerChange={onLagrerChange}
        onLagret={onLagret}
        resultatBegrunnelse={resultatBegrunnelse}
        innvilgelseFeil={innvilgelseFeil}
        validerPeriode={(onSuccess) => {
          void handleSubmit(() => validerPeriode(onSuccess))()
        }}
        validerAktivitet={validerAktivitet}
      />
    </>
  )
}
