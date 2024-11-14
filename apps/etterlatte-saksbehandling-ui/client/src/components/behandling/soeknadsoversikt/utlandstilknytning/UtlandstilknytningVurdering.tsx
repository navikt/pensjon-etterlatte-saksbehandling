import { IBehandlingStatus, IUtlandstilknytning, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
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

const UtlandstilknytningTypeTittel: Record<UtlandstilknytningType, string> = {
  [UtlandstilknytningType.NASJONAL]: 'Nasjonal',
  [UtlandstilknytningType.UTLANDSTILSNITT]: 'Utlandstilsnitt - bosatt Norge',
  [UtlandstilknytningType.BOSATT_UTLAND]: 'Bosatt utland',
} as const

export const UtlandstilknytningVurdering = ({
  utlandstilknytning,
  redigerbar,
  behandlingId,
}: {
  utlandstilknytning: IUtlandstilknytning | null
  redigerbar: boolean
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<UtlandstilknytningType | undefined>(utlandstilknytning?.type)
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(utlandstilknytning?.begrunnelse || '')
  const [setUtlandstilknytningStatus, setUtlandstilknytning, resetToInitial] = useApiCall(lagreUtlandstilknytning)

  const lagre = (onSuccess?: () => void) => {
    if (!svar) {
      setRadioError('Du må velge et svar')
    } else {
      setRadioError('')
    }

    if (svar !== undefined)
      return setUtlandstilknytning({ behandlingId, begrunnelse, svar }, (utlandstilknyningstype) => {
        dispatch(oppdaterUtlandstilknytning(utlandstilknyningstype))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(utlandstilknytning?.type)
    setRadioError('')
    setBegrunnelse(utlandstilknytning?.begrunnelse || '')
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel="Hvilken type sak er dette?"
      subtittelKomponent={
        <>
          {utlandstilknytning?.type ? (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {UtlandstilknytningTypeTittel[utlandstilknytning.type]}
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
        utlandstilknytning?.kilde
          ? {
              saksbehandler: utlandstilknytning?.kilde.ident,
              tidspunkt: new Date(utlandstilknytning?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={utlandstilknytning?.begrunnelse}
      defaultRediger={utlandstilknytning === null}
      visAvbryt={!!utlandstilknytning?.kilde}
    >
      <>
        <Heading level="3" size="small">
          Hvilken type sak er dette?
        </Heading>
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              setSvar(UtlandstilknytningType[event as UtlandstilknytningType])
              setRadioError('')
            }}
            value={svar || ''}
            error={radioError ? radioError : false}
          >
            <Radio value={UtlandstilknytningType.NASJONAL}>Nasjonal</Radio>
            <Radio value={UtlandstilknytningType.UTLANDSTILSNITT}>Utlandstilsnitt - bosatt Norge</Radio>
            <Radio value={UtlandstilknytningType.BOSATT_UTLAND}>Bosatt Utland</Radio>
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
