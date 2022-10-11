import React, { useState } from 'react'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../handlinger/vilkaarsvurderingKnapper'
import {
  lagreTotalVurdering,
  slettTotalVurdering,
  Vilkaarsvurdering,
  VilkaarsvurderingResultat,
} from '../../../shared/api/vilkaarsvurdering'
import { VilkaarBorder } from './styled'
import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { ISvar } from '../../../store/reducers/BehandlingReducer'
import { svarTilTotalResultat } from './utils'
import { Delete } from '@navikt/ds-icons'

type Props = {
  dato: string
  vilkaarsvurdering: Vilkaarsvurdering
  oppdaterVilkaar: (vilkaarsvurdering?: Vilkaarsvurdering) => void
  behandlingId: string
}
const MIN_KOMMENTAR_LENGDE = 10

export const Resultat: React.FC<Props> = ({ dato, vilkaarsvurdering, oppdaterVilkaar, behandlingId }) => {
  const [svar, setSvar] = useState<ISvar>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const [kommentarError, setKommentarError] = useState<string>()

  const alleVilkaarErVurdert = !vilkaarsvurdering.vilkaar.some((vilkaar) => !vilkaar.vurdering)

  const slettVilkaarsvurderingResultat = () => {
    slettTotalVurdering(behandlingId).then((response) => {
      if (response.status == 'ok') {
        oppdaterVilkaar(response.data)
        reset()
      }
    })
  }

  const lagreVilkaarsvurderingResultat = () => {
    !(svar && [ISvar.JA, ISvar.NEI].includes(svar))
      ? setRadioError('Du må svare på om vilkårsvurderingen er oppfylt')
      : setRadioError(undefined)
    !(kommentar.length >= MIN_KOMMENTAR_LENGDE)
      ? setKommentarError('Begrunnelsen må være minst 10 tegn')
      : setKommentarError(undefined)

    if (
      radioError === undefined &&
      kommentarError === undefined &&
      svar !== undefined &&
      kommentar.length >= MIN_KOMMENTAR_LENGDE
    ) {
      lagreTotalVurdering(behandlingId, svarTilTotalResultat(svar), kommentar).then((response) => {
        if (response.status == 'ok') {
          oppdaterVilkaar(response.data)
        }
      })
    }
  }

  const resultatTekst = () =>
    vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT ? 'Innvilget' : 'Avslag'

  const reset = () => {
    setSvar(undefined)
    setRadioError(undefined)
    setKommentar('')
    setKommentarError(undefined)
  }

  return (
    <>
      <VilkaarsvurderingContent>
        {vilkaarsvurdering.resultat && (
          <>
            <TekstWrapper>
              <span>
                <b>Vilkårsresultat: </b>
                {`${resultatTekst()} fra ${format(new Date(dato), 'dd.MM.yyyy')}`}
              </span>
            </TekstWrapper>
            <p>
              Manuelt av {vilkaarsvurdering.resultat.saksbehandler} (
              <i>{format(new Date(vilkaarsvurdering.resultat.tidspunkt), 'dd.MM.yyyy HH:ss')})</i>
            </p>
            <SlettWrapper onClick={slettVilkaarsvurderingResultat}>
              <Delete />
              <span className={'text'}>Slett vurdering</span>
            </SlettWrapper>
          </>
        )}

        {!vilkaarsvurdering.resultat && !alleVilkaarErVurdert && (
          <TekstWrapper>Alle vilkår må vurderes før man kan gå videre</TekstWrapper>
        )}

        {!vilkaarsvurdering.resultat && alleVilkaarErVurdert && (
          <>
            <TekstWrapper>
              <b>Er vilkårsvurderingen oppfylt?</b>
            </TekstWrapper>
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
                  <Radio value={ISvar.JA}>Ja</Radio>
                  <Radio value={ISvar.NEI}>Nei</Radio>
                </div>
              </RadioGroup>
            </RadioGroupWrapper>
            <Textarea
              label="Begrunnelse"
              hideLabel={false}
              placeholder="Gi en begrunnelse for vurderingen"
              value={kommentar}
              onChange={(e) => {
                const kommentarLocal = e.target.value
                setKommentar(kommentarLocal)
                kommentarLocal.length >= MIN_KOMMENTAR_LENGDE && setKommentarError(undefined)
              }}
              minRows={3}
              size="medium"
              error={kommentarError ? kommentarError : false}
            />
            <Button variant={'primary'} size={'small'} onClick={lagreVilkaarsvurderingResultat}>
              Lagre
            </Button>
          </>
        )}
      </VilkaarsvurderingContent>

      <VilkaarBorder />
      <BehandlingHandlingKnapper>
        {vilkaarsvurdering.resultat && <VilkaarsVurderingKnapper />}
      </BehandlingHandlingKnapper>
    </>
  )
}

export const RadioGroupWrapper = styled.div`
  margin-top: 1em;
  margin-bottom: 1em;
  display: flex;

  .flex {
    display: flex;
    gap: 20px;
  }
`

const VilkaarsvurderingContent = styled.div`
  padding-left: 36px;
  padding-right: 36px;

  button {
    margin-top: 10px;
  }
`

const TekstWrapper = styled.div`
  margin-top: 30px;
  display: flex;
  font-size: 1.2em;
`

const SlettWrapper = styled.div`
  display: inline-flex;
  cursor: pointer;
  color: #0067c5;

  .text {
    margin-left: 0.3em;
    font-size: 0.9em;
    font-weight: normal;
  }

  &:hover {
    text-decoration-line: underline;
  }
`
