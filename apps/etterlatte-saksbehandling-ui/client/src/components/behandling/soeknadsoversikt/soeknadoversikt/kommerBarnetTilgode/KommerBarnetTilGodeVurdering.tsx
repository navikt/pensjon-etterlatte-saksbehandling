import { Undertekst, VurderingsTitle } from '../../styled'
import styled from 'styled-components'
import { useState } from 'react'
import { IBehandlingStatus, IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { BodyShort, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBegrunnelseKommerBarnetTilgode } from '~shared/api/behandling'
import { oppdaterBehandlingsstatus, oppdaterKommerBarnetTilgode } from '~store/reducers/BehandlingReducer'
import { useAppDispatch } from '~store/Store'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsoversiktTextArea'

export const KommerBarnetTilGodeVurdering = ({
  kommerBarnetTilgode,
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<JaNei | undefined>(kommerBarnetTilgode?.svar)
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(kommerBarnetTilgode?.begrunnelse || '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>('')
  const [, setKommerBarnetTilGode, resetToInitial] = useApiCall(lagreBegrunnelseKommerBarnetTilgode)

  const lagre = (onSuccess?: () => void) => {
    !svar ? setRadioError('Du må velge et svar') : setRadioError('')
    const harBegrunnelse = begrunnelse.trim().length > 0
    harBegrunnelse ? setBegrunnelseError('') : setBegrunnelseError('Begrunnelsen må fylles ut')

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
    setVurdert(kommerBarnetTilgode !== null)
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel={''}
      subtittelKomponent={
        <>
          <BodyShort spacing>Vurderer du det som sannsynlig at pensjonen kommer barnet tilgode?</BodyShort>
          {kommerBarnetTilgode?.svar && (
            <Label as={'p'} size="small" style={{ marginBottom: '32px' }}>
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
        <VurderingsTitle title={'Trenger avklaring'} />
        <Undertekst $gray={false}>
          Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
        </Undertekst>
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
          value={begrunnelse}
          onChange={(e) => {
            const oppdatertBegrunnelse = e.target.value
            setBegrunnelse(oppdatertBegrunnelse)
            oppdatertBegrunnelse.trim().length > 0 && setBegrunnelseError('')
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
