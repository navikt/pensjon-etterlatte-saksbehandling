import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { ISvar } from '../../../store/reducers/BehandlingReducer'
import { useParams } from 'react-router-dom'
import {
  slettVurdering,
  Vilkaar,
  Vilkaarsvurdering,
  VurderingsResultat,
  vurderVilkaar,
} from '../../../shared/api/vilkaarsvurdering'
import styled from 'styled-components'
import { format } from 'date-fns'
import { DeleteIcon } from '../../../shared/icons/DeleteIcon'
import { EditIcon } from '../../../shared/icons/EditIcon'

export const Vurdering = ({
  vilkaar,
  oppdaterVilkaar,
  erVurdert,
  erOppfylt,
}: {
  vilkaar: Vilkaar
  oppdaterVilkaar: (vilkaarsvurdering?: Vilkaarsvurdering) => void
  erVurdert: boolean
  erOppfylt: boolean
}) => {
  const { behandlingId } = useParams()
  const [aktivVurdering, setAktivVurdering] = useState<boolean>(false)
  const [svar, setSvar] = useState<ISvar>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const [begrunnelseError, setBegrunnelseError] = useState<string>()

  const vilkaarVurdert = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')
    !svar ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    kommentar.length < 11 ? setBegrunnelseError('Begrunnelsen må være minst 10 tegn') : setBegrunnelseError(undefined)

    if (radioError === undefined && begrunnelseError === undefined && svar !== undefined && kommentar.length > 10) {
      vurderVilkaar(behandlingId!!, {
        type: vilkaar.type,
        kommentar: kommentar,
        resultat: svar === ISvar.JA ? VurderingsResultat.OPPFYLT : VurderingsResultat.IKKE_OPPFYLT,
      }).then((response) => {
        if (response.status == 'ok') {
          oppdaterVilkaar(response.data)
          reset()
        }
      })
    }
  }

  const slettVurderingAvVilkaar = () => {
    if (!behandlingId) throw new Error('Mangler behandlingsid')

    slettVurdering(behandlingId!!, vilkaar.type).then((response) => {
      if (response.status == 'ok') {
        oppdaterVilkaar()
      }
    })
  }

  const redigerVilkaar = () => {
    setAktivVurdering(true)
    setKommentar(vilkaar.vurdering?.kommentar || '')
  }

  const reset = () => {
    setAktivVurdering(false)
    setSvar(undefined)
    setRadioError(undefined)
    setKommentar('')
    setBegrunnelseError(undefined)
  }

  const overskrift = () => (erOppfylt ? 'Vilkår oppfylt' : 'Vilkår er ikke oppfylt')

  return (
    <div>
      {erVurdert && !aktivVurdering && (
        <>
          <KildeVilkaar>
            <KildeOverskrift>{overskrift()}</KildeOverskrift>
            <p>Manuelt av {vilkaar.vurdering?.saksbehandler}</p>
            <p>
              Kommentar: <br />
              {vilkaar.vurdering?.kommentar}
            </p>
            <p>Sist endret {format(new Date(vilkaar.vurdering!!.tidspunkt), 'dd.MM.yyyy HH:mm')}</p>
          </KildeVilkaar>

          <RedigerWrapper onClick={slettVurderingAvVilkaar} style={{ marginLeft: '-22px' }}>
            <DeleteIcon />
            <span className={'text'}> Slett</span>
          </RedigerWrapper>
          <RedigerWrapper onClick={redigerVilkaar}>
            <EditIcon />
            <span className={'text'}> Rediger</span>
          </RedigerWrapper>
        </>
      )}
      {aktivVurdering && (
        <>
          <VurderingsTitle>Er vilkåret oppfylt?</VurderingsTitle>
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
            placeholder="Gi en begrunnelse for vurderingen"
            value={kommentar}
            onChange={(e) => {
              setKommentar(e.target.value)
              kommentar.length > 10 && setBegrunnelseError(undefined)
            }}
            minRows={3}
            size="small"
            error={begrunnelseError ? begrunnelseError : false}
          />
          <Button style={{ marginTop: '10px' }} variant={'primary'} size={'small'} onClick={vilkaarVurdert}>
            Lagre
          </Button>
          <Button
            style={{ marginTop: '10px', marginLeft: '10px' }}
            variant={'secondary'}
            size={'small'}
            onClick={reset}
          >
            Avbryt
          </Button>
        </>
      )}
      {!erVurdert && !aktivVurdering && (
        <IkkeVurdert>
          <p>Vilkåret er ikke vurdert</p>
          <Button variant={'secondary'} size={'small'} onClick={() => setAktivVurdering(true)}>
            Vurder vilkår
          </Button>
        </IkkeVurdert>
      )}
    </div>
  )
}

const RedigerWrapper = styled.div`
  display: inline-flex;
  float: left;
  cursor: pointer;
  color: #0067c5;
  margin-left: 10px;

  .text {
    margin-left: 0.3em;
    font-size: 0.7em;
    font-weight: normal;
  }

  &:hover {
    text-decoration-line: underline;
  }
`

export const IkkeVurdert = styled.div`
  font-size: 0.8em;

  button {
    margin-top: 1em;
  }
`

export const KildeVilkaar = styled.div`
  color: grey;
  font-size: 0.7em;
`

export const KildeOverskrift = styled.div`
  color: black;
  font-size: 1.2em;
`

export const VurderingsTitle = styled.div`
  display: flex;
  font-size: 1em;
  font-weight: bold;
`

export const RadioGroupWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 1em;

  .flex {
    display: flex;
    gap: 20px;
  }
`
