import { Alert, Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { RadioGroupWrapper } from './KommerBarnetTilGodeVurdering'
import { lagreBegrunnelseKommerBarnetTilgode } from '~shared/api/behandling'
import { useState } from 'react'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { VurderingsTitle, Undertekst } from '../../styled'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import styled from 'styled-components'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { JaNei } from '~shared/types/ISvar'
import { oppdaterKommerBarnetTilgode } from '~store/reducers/BehandlingReducer'

export const EndreVurdering = ({
  setRedigeringsModusFalse,
  kommerBarnetTilgode,
}: {
  setRedigeringsModusFalse: () => void
  kommerBarnetTilgode: IKommerBarnetTilgode | null
}) => {
  const behandlingId = useAppSelector((state) => state.behandlingReducer.behandling.id)
  const dispatch = useAppDispatch()
  const [svar, setSvar] = useState<JaNei | undefined>(kommerBarnetTilgode?.svar)
  const [radioError, setRadioError] = useState<string>()
  const [begrunnelse, setBegrunnelse] = useState<string>(kommerBarnetTilgode?.begrunnelse || '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>()
  const [kommerBarnetTilGode, setKommerBarnetTilGode] = useApiCall(lagreBegrunnelseKommerBarnetTilgode)

  function lagreBegrunnelseKlikket() {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    begrunnelse.length < 11 ? setBegrunnelseError('Begrunnelsen må være minst 10 tegn') : setBegrunnelseError(undefined)

    if (radioError === undefined && begrunnelseError === undefined && svar !== undefined)
      setKommerBarnetTilGode({ behandlingId, begrunnelse, svar }, (response) => {
        dispatch(oppdaterKommerBarnetTilgode(response))
        setRedigeringsModusFalse()
      })
  }

  return (
    <div>
      <VurderingsTitle title={"Trenger avklaring"} />
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
      <Button
        loading={isPending(kommerBarnetTilGode)}
        style={{ marginTop: '10px' }}
        variant={'primary'}
        size={'small'}
        onClick={() => lagreBegrunnelseKlikket()}
      >
        Lagre
      </Button>
      <Button style={{ marginLeft: '10px' }} variant={'secondary'} size={'small'} onClick={setRedigeringsModusFalse}>
        Avbryt
      </Button>
      {isFailure(kommerBarnetTilGode) && <ApiErrorAlert variant="error">Kunne ikke lagre vurdering</ApiErrorAlert>}
    </div>
  )
}

const ApiErrorAlert = styled(Alert).attrs({ variant: 'error' })`
  margin-top: 8px;
`
