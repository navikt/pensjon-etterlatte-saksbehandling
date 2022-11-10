import { Undertekst, VurderingsTitle } from '../../styled'
import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { RadioGroupWrapper } from './KommerBarnetTilGodeVurdering'
import { lagreBegrunnelseKommerBarnetTilgode } from '../../../../../shared/api/behandling'
import { useEffect, useState } from 'react'
import {
  IKommerBarnetTilgode,
  JaNei,
  oppdaterKommerBarnetTilgode,
} from '../../../../../store/reducers/BehandlingReducer'
import { useAppDispatch, useAppSelector } from '../../../../../store/Store'

export const EndreVurdering = ({
  setRedigeringsModusFalse,
  kommerBarnetTilgode,
}: {
  setRedigeringsModusFalse: () => void
  kommerBarnetTilgode: IKommerBarnetTilgode | null
}) => {
  const behandlingId = useAppSelector((state) => state.behandlingReducer.behandling.id)
  const dispatch = useAppDispatch()
  const [svar, setSvar] = useState<JaNei>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>(kommerBarnetTilgode?.begrunnelse || '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>()

  function lagreBegrunnelseKlikket() {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    kommentar.length < 11 ? setBegrunnelseError('Begrunnelsen må være minst 10 tegn') : setBegrunnelseError(undefined)

    if (radioError === undefined && begrunnelseError === undefined && svar !== undefined)
      lagreBegrunnelseKommerBarnetTilgode(behandlingId, kommentar, svar.toString()).then((response) => {
        if (response.status === 'ok') {
          dispatch(oppdaterKommerBarnetTilgode(response.data))
          reset()
        }
      })
  }

  useEffect(() => {
    if (kommerBarnetTilgode && kommerBarnetTilgode.svar) {
      setSvar(kommerBarnetTilgode.svar)
    }
  }, [])

  function reset() {
    setRedigeringsModusFalse()
  }

  return (
    <div>
      <VurderingsTitle>Trenger avklaring</VurderingsTitle>
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
        value={kommentar}
        onChange={(e) => {
          setKommentar(e.target.value)
          kommentar.length > 10 && setBegrunnelseError(undefined)
        }}
        minRows={3}
        size="small"
        error={begrunnelseError ? begrunnelseError : false}
      />
      <Button
        style={{ marginTop: '10px' }}
        variant={'primary'}
        size={'small'}
        onClick={() => lagreBegrunnelseKlikket()}
      >
        Lagre
      </Button>
      <Button
        style={{ marginTop: '10px', marginLeft: '10px' }}
        variant={'secondary'}
        size={'small'}
        onClick={() => reset()}
      >
        Avbryt
      </Button>
    </div>
  )
}
