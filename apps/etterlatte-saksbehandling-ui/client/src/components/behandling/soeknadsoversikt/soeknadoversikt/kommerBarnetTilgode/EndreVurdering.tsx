import { Undertekst, VurderingsTitle } from '../../styled'
import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { RadioGroupWrapper } from './KommerBarnetTilGodeVurdering'
import { hentBehandling, lagreBegrunnelseKommerBarnetTilgode } from '../../../../../shared/api/behandling'
import { useState } from 'react'
import { ISvar } from '../../../../../store/reducers/BehandlingReducer'
import { useAppSelector } from '../../../../../store/Store'

export const EndreVurdering = ({ setRedigeringsModusFalse }: { setRedigeringsModusFalse: () => void }) => {
  const behandlingId = useAppSelector((state) => state.behandlingReducer.behandling.id)
  const [svar, setSvar] = useState<ISvar>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const [begrunnelseError, setBegrunnelseError] = useState<string>()

  function lagreBegrunnelseKlikket() {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    kommentar.length < 11 ? setBegrunnelseError('Begrunnelsen må være minst 10 tegn') : setBegrunnelseError(undefined)

    if (radioError === undefined && begrunnelseError === undefined && svar !== undefined)
      lagreBegrunnelseKommerBarnetTilgode(behandlingId, kommentar, svar.toString()).then((response) => {
        console.log(response)
        if (response.status === 'ok') {
          hentBehandling(behandlingId).then((response) => {
            if (response.status === 'ok') {
              window.location.reload()
            }
          })
        }
      })
  }

  function reset() {
    setRedigeringsModusFalse()
    setSvar(undefined)
    setRadioError(undefined)
    setKommentar('')
    setBegrunnelseError(undefined)
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
            setSvar(ISvar[event as ISvar])
            setRadioError(undefined)
          }}
          error={radioError ? radioError : false}
        >
          <div className="flex">
            <Radio value={ISvar.JA.toString()}>Ja</Radio>
            <Radio value={ISvar.NEI.toString()}>Nei</Radio>
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
