import React, { useState } from 'react'
import styled from 'styled-components'
import { format } from 'date-fns'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../handlinger/vilkaarsvurderingKnapper'
import { setTotalVurdering, slettTotalVurdering, Vilkaarsvurdering } from '../../../shared/api/vilkaarsvurdering'
import { VilkaarBorder } from './styled'
import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { ISvar } from '../../../store/reducers/BehandlingReducer'
import { useParams } from 'react-router-dom'
import { svarTilTotalResultat } from './utils'
import { DeleteIcon } from '../../../shared/icons/DeleteIcon'

type Props = {
  dato: string
  vilkaarsvurdering: Vilkaarsvurdering
  oppdaterVilkaar: (vilkaarsvurdering?: Vilkaarsvurdering) => void
}

export const Resultat: React.FC<Props> = ({ dato, vilkaarsvurdering, oppdaterVilkaar }) => {
  const { behandlingId } = useParams()
  const [svar, setSvar] = useState<ISvar>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const [kommentarError, setKommentarError] = useState<string>()

  const alleVilkaarErVurdert = !vilkaarsvurdering.vilkaar.some((vilkaar) => !vilkaar.vurdering)

  const slettVilkaarsvurderingResultat = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    slettTotalVurdering(behandlingId!!).then((response) => {
      if (response.status == 'ok') {
        oppdaterVilkaar()
      }
    })
  }

  const setVilkaarsvurderingResultat = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    kommentar.length < 11 ? setKommentarError('Begrunnelsen må være minst 10 tegn') : setKommentarError(undefined)

    if (radioError === undefined && kommentarError === undefined && svar !== undefined && kommentar.length > 10) {
      setTotalVurdering(behandlingId!!, svarTilTotalResultat(svar), kommentar).then((response) => {
        if (response.status == 'ok') {
          oppdaterVilkaar(response.data)
        }
      })
    }
  }

  return (
    <>
      <VilkaarsvurderingContent>
        {vilkaarsvurdering.resultat && (
          <>
            <TekstWrapper>
              <b>Vilkårsresultat: &nbsp;</b>
              Innvilget fra {format(new Date(dato), 'dd.MM.yyyy')}
            </TekstWrapper>
            <p>
              Manuelt av {vilkaarsvurdering.resultat.saksbehandler} (
              <i>{format(new Date(vilkaarsvurdering.resultat.tidspunkt), 'dd.MM.yyyy HH:ss')})</i>
            </p>
            <SlettWrapper onClick={slettVilkaarsvurderingResultat} style={{ marginLeft: '-22px' }}>
              <DeleteIcon />
              <span className={'text'}>Slett vurdering</span>
            </SlettWrapper>
          </>
        )}

        {!vilkaarsvurdering.resultat && !alleVilkaarErVurdert && (
          <TekstWrapper>Alle vilkår må vurderes før man kan gå videre</TekstWrapper>
        )}

        {!vilkaarsvurdering.resultat && alleVilkaarErVurdert && (
          <>
            <TekstWrapper style={{ fontWeight: 'bold' }}>Er vilkårsvurderingen oppfylt?</TekstWrapper>
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
              label="Begrunnelse"
              hideLabel={false}
              placeholder="Gi en begrunnelse for vurderingen"
              value={kommentar}
              onChange={(e) => {
                setKommentar(e.target.value)
                kommentar.length > 10 && setKommentarError(undefined)
              }}
              minRows={3}
              size="medium"
              error={kommentarError ? kommentarError : false}
            />
            <Button variant={'primary'} size={'small'} onClick={setVilkaarsvurderingResultat}>
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

  button {
    margin-top: 10px;
  }

  .navds-textarea__wrapper {
    width: 500px;
  }
`

const TekstWrapper = styled.div`
  margin-top: 30px;
  display: flex;
  font-size: 1.2em;
`

const SlettWrapper = styled.div`
  display: inline-flex;
  float: left;
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
