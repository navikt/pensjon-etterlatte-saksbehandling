import styled from 'styled-components'
import { useState } from 'react'
import { IBehandlingStatus, IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { Heading, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBegrunnelseKommerBarnetTilgode } from '~shared/api/behandling'
import { oppdaterBehandlingsstatus, oppdaterKommerBarnetTilgode } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/SoeknadsoversiktTextArea'

export const KommerBarnetTilGodeVurdering = ({
  kommerBarnetTilgode,
  redigerbar,
  behandlingId,
}: {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  redigerbar: boolean
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<JaNei | undefined>(kommerBarnetTilgode?.svar)
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(kommerBarnetTilgode?.begrunnelse || '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>('')
  const [, setKommerBarnetTilGode, resetToInitial] = useApiCall(lagreBegrunnelseKommerBarnetTilgode)

  const lagre = (onSuccess?: () => void) => {
    if (!svar) {
      setRadioError('Du må velge et svar')
    } else {
      setRadioError('')
    }
    const harBegrunnelse = begrunnelse.trim().length > 0
    if (harBegrunnelse) {
      setBegrunnelseError('')
    } else {
      setBegrunnelseError('Begrunnelsen må fylles ut')
    }

    if (svar !== undefined && harBegrunnelse)
      return setKommerBarnetTilGode({ behandlingId, begrunnelse, svar }, (response) => {
        dispatch(oppdaterKommerBarnetTilgode(response))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(kommerBarnetTilgode?.svar)
    setRadioError('')
    setBegrunnelseError('')
    setBegrunnelse(kommerBarnetTilgode?.begrunnelse || '')
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel="Vurderer du det som sannsynlig at pensjonen kommer barnet tilgode?"
      subtittelKomponent={
        <>
          {kommerBarnetTilgode?.svar && (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {JaNeiRec[kommerBarnetTilgode.svar]}
            </Label>
          )}
        </>
      }
      redigerbar={redigerbar}
      vurdering={
        kommerBarnetTilgode?.kilde
          ? {
              saksbehandler: kommerBarnetTilgode?.kilde.ident,
              tidspunkt: new Date(kommerBarnetTilgode?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={kommerBarnetTilgode?.begrunnelse}
      defaultRediger={kommerBarnetTilgode === null}
    >
      <div>
        <Heading level="3" size="small">
          Vurderer du det som sannsynlig at pensjonen kommer barnet tilgode?
        </Heading>
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              setSvar(JaNei[event as JaNei])
              setRadioError('')
            }}
            value={svar || ''}
            error={radioError ? radioError : false}
          >
            <div className="flex">
              <Radio value={JaNei.JA}>Ja</Radio>
              <Radio value={JaNei.NEI}>Nei</Radio>
            </div>
          </RadioGroup>
        </RadioGroupWrapper>
        <SoeknadsoversiktTextArea
          label="Begrunnelse"
          placeholder="Forklar begrunnelsen"
          value={begrunnelse}
          onChange={(e) => {
            const oppdatertBegrunnelse = e.target.value
            setBegrunnelse(oppdatertBegrunnelse)
            if (oppdatertBegrunnelse.trim().length > 0) setBegrunnelseError('')
          }}
          error={begrunnelseError ? begrunnelseError : false}
        />
      </div>
    </VurderingsboksWrapper>
  )
}

const RadioGroupWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 1em;

  .flex {
    display: flex;
    gap: 20px;
  }
`
