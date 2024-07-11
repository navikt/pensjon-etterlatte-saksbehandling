import { IBehandlingStatus, IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { Heading, Label, UNSAFE_Combobox } from '@navikt/ds-react'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandlingsstatus, oppdaterViderefoertOpphoer } from '~store/reducers/BehandlingReducer'
import { lagreViderefoertOpphoer } from '~shared/api/behandling'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { VilkaarType } from '~shared/api/vilkaarsvurdering'

const VilkaarTypeTittel: Record<VilkaarType, string> = {
  [VilkaarType.BP_FORMAAL_2024]: 'BP formål',
  [VilkaarType.BP_DOEDSFALL_FORELDER_2024]: 'BP dødsfall forelder',
} as const

export const ViderefoereOpphoerVurdering = ({
  behandling,
  viderefoertOpphoer,
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  behandling: IDetaljertBehandling
  viderefoertOpphoer: ViderefoertOpphoer | null
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [opphoerstidspunkt] = useState<Date | null>(behandling.opphoerFom ? new Date(behandling.opphoerFom.dato) : null)
  const [vilkaar, setVilkaar] = useState<VilkaarType | undefined>(viderefoertOpphoer?.vilkaar)
  const [vilkaarError, setVilkaarError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(viderefoertOpphoer?.begrunnelse || '')
  const [setViderefoertOpphoerStatus, setViderefoertOpphoer, resetToInitial] = useApiCall(lagreViderefoertOpphoer)
  const [kravdato] = useState<string | undefined>()

  const valider = () => {
    if (!vilkaar) {
      return 'Du må velge et vilkår som ikke lenger blir oppfylt'
    }
    if (!opphoerstidspunkt) {
      return 'Du må velge opphørstidspunkt'
    }
    return ''
  }

  const lagre = (onSuccess?: () => void) => {
    setVilkaarError(valider())

    if (vilkaar !== undefined)
      return setViderefoertOpphoer({ behandlingId, begrunnelse, vilkaar, kravdato }, (utlandstilknyningstype) => {
        dispatch(oppdaterViderefoertOpphoer(utlandstilknyningstype))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setVilkaar(viderefoertOpphoer?.vilkaar)
    setVilkaarError('')
    setBegrunnelse(viderefoertOpphoer?.begrunnelse || '')
    setVurdert(viderefoertOpphoer !== null)
    onSuccess?.()
  }

  const options = Object.keys(VilkaarType).map((k) => k)

  return (
    <VurderingsboksWrapper
      tittel="Hvilket vilkår?"
      subtittelKomponent={
        <>
          {viderefoertOpphoer?.vilkaar ? (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {VilkaarTypeTittel[viderefoertOpphoer.vilkaar]}
            </Label>
          ) : (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              Ikke vurdert
            </Label>
          )}
        </>
      }
      redigerbar={redigerbar}
      vurdering={
        viderefoertOpphoer?.kilde
          ? {
              saksbehandler: viderefoertOpphoer?.kilde.ident,
              tidspunkt: new Date(viderefoertOpphoer?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={viderefoertOpphoer?.begrunnelse}
      defaultRediger={viderefoertOpphoer === null}
    >
      <>
        <Heading level="3" size="small">
          Er dette et videreført opphold?
        </Heading>
        <UNSAFE_Combobox
          label="Velg vilkåret som gjør at saken opphører"
          options={options}
          onToggleSelected={(option) => {
            setVilkaar(VilkaarType[option as VilkaarType])
            setVilkaarError('')
          }}
          selectedOptions={!!vilkaar ? [vilkaar!] : []}
          isLoading={false}
          error={vilkaarError ? vilkaarError : false}
        />
        <SoeknadsoversiktTextArea
          label="Begrunnelse"
          placeholder="Valgfritt"
          value={begrunnelse}
          onChange={(e) => {
            const oppdatertBegrunnelse = e.target.value
            setBegrunnelse(oppdatertBegrunnelse)
          }}
        />
        {isFailureHandler({
          apiResult: setViderefoertOpphoerStatus,
          errorMessage: 'Kunne ikke lagre opphørsdato',
        })}
      </>
    </VurderingsboksWrapper>
  )
}
