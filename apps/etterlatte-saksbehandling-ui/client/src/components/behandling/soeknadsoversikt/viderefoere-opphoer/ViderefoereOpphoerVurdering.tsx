import { IBehandlingStatus, IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { Heading, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandlingsstatus, oppdaterUtlandstilknytning } from '~store/reducers/BehandlingReducer'
import { lagreUtlandstilknytning } from '~shared/api/behandling'
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
  const [vilkaar, setVilkaar] = useState<string | undefined>(viderefoertOpphoer?.vilkaar)
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(viderefoertOpphoer?.begrunnelse || '')
  const [setUtlandstilknytningStatus, setUtlandstilknytning, resetToInitial] = useApiCall(lagreUtlandstilknytning)

  const lagre = (onSuccess?: () => void) => {
    !opphoerstidspunkt ? setRadioError('Du må velge et svar') : setRadioError('')

    if (vilkaar !== undefined)
      return setUtlandstilknytning({ behandlingId, begrunnelse, svar: vilkaar }, (utlandstilknyningstype) => {
        dispatch(oppdaterUtlandstilknytning(utlandstilknyningstype))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setVilkaar(viderefoertOpphoer?.vilkaar)
    setRadioError('')
    setBegrunnelse(viderefoertOpphoer?.begrunnelse || '')
    setVurdert(viderefoertOpphoer !== null)
    onSuccess?.()
  }

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
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              setVilkaar(VilkaarType[event as VilkaarType])
              setRadioError('')
            }}
            value={vilkaar || ''}
            error={radioError ? radioError : false}
          >
            <Radio value={VilkaarType.BP_FORMAAL_2024}>BP formål 24</Radio>
            <Radio value={VilkaarType.BP_DOEDSFALL_FORELDER_2024}>BP dødsfall forelder 24</Radio>
          </RadioGroup>
        </RadioGroupWrapper>
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
          apiResult: setUtlandstilknytningStatus,
          errorMessage: 'Kunne ikke lagre utlandstilknytning',
        })}
      </>
    </VurderingsboksWrapper>
  )
}
