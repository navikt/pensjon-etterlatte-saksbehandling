import { Undertekst, VurderingsTitle } from '../../styled'
import styled from 'styled-components'
import { useState } from 'react'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { BodyShort, Label, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBegrunnelseKommerBarnetTilgode } from '~shared/api/behandling'
import { oppdaterKommerBarnetTilgode } from '~store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '~store/Store'

export const KommerBarnetTilGodeVurdering = ({
  kommerBarnetTilgode,
  redigerbar,
  setVurder,
}: {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  redigerbar: boolean
  setVurder: (visVurderingKnapp: boolean) => void
}) => {
  const behandlingId = useAppSelector((state) => state.behandlingReducer.behandling.id)
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<JaNei | undefined>(kommerBarnetTilgode?.svar)
  const [radioError, setRadioError] = useState<string>()
  const [begrunnelse, setBegrunnelse] = useState<string>(kommerBarnetTilgode?.begrunnelse || '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>()
  const [, setKommerBarnetTilGode, resetToInitial] = useApiCall(lagreBegrunnelseKommerBarnetTilgode)

  const lagre = (onSuccess?: () => void) => {
    !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    begrunnelse.length < 10 ? setBegrunnelseError('Begrunnelsen må være minst 10 tegn') : setBegrunnelseError(undefined)

    if (radioError === undefined && begrunnelseError === undefined && svar !== undefined)
      setKommerBarnetTilGode({ behandlingId, begrunnelse, svar }, (response) => {
        dispatch(oppdaterKommerBarnetTilgode(response))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(kommerBarnetTilgode?.svar)
    setRadioError('')
    setBegrunnelseError('')
    setBegrunnelse(kommerBarnetTilgode?.begrunnelse || '')
    setVurder(kommerBarnetTilgode !== null)
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel={''}
      subtittelKomponent={
        <>
          <BodyShort spacing>Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?</BodyShort>
          {kommerBarnetTilgode?.svar && (
            <Label as={'p'} spacing size="small">
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
          : null
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={kommerBarnetTilgode?.begrunnelse}
      defaultRediger={kommerBarnetTilgode === null}
    >
      <div>
        <VurderingsTitle title={'Trenger avklaring'} />
        <Undertekst gray={false}>
          Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
        </Undertekst>
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              setSvar(JaNei[event as JaNei])
              setRadioError(undefined)
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
        <Textarea
          style={{ padding: '10px', marginBottom: '10px' }}
          label="Begrunnelse"
          hideLabel={false}
          placeholder="Forklar begrunnelsen"
          value={begrunnelse}
          onChange={(e) => {
            setBegrunnelse(e.target.value)
            begrunnelse.length > 10 && setBegrunnelseError(undefined)
          }}
          minRows={3}
          size="small"
          error={begrunnelseError ? begrunnelseError : false}
          autoComplete="off"
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
