import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import { ISvar, VurderingsResultat as VurderingsresultatOld } from '../../../store/reducers/BehandlingReducer'
import { useParams } from 'react-router-dom'
import {
  slettVurdering,
  Vilkaar,
  Vilkaarsvurdering,
  VurderingsResultat,
  vurderVilkaar,
} from '../../../shared/api/vilkaarsvurdering'
import { StatusIcon } from '../../../shared/icons/statusIcon'
import styled from 'styled-components'
import { format } from 'date-fns'

export const Vurdering = ({
  vilkaar,
  oppdaterVilkaar,
}: {
  vilkaar: Vilkaar
  oppdaterVilkaar: (vilkaarsvurdering?: Vilkaarsvurdering) => void
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
          setAktivVurdering(false)
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

  const erVurdert = (): boolean => !!vilkaar.vurdering
  const erOppfyllt = (): boolean => vilkaar.vurdering?.resultat == VurderingsResultat.OPPFYLT

  const reset = () => {
    setAktivVurdering(false)
    setSvar(undefined)
    setRadioError(undefined)
    setKommentar('')
    setBegrunnelseError(undefined)
  }

  return (
    <div>
      {erVurdert() && (
        <>
          <KildeVilkaar>
            {erOppfyllt() ? (
              <KildeOverskrift>
                <StatusIcon status={VurderingsresultatOld.OPPFYLT} />
                Vilkår er oppfyllt
              </KildeOverskrift>
            ) : (
              <KildeOverskrift>
                <StatusIcon status={VurderingsresultatOld.IKKE_OPPFYLT} />
                Vilkår er ikke oppfyllt
              </KildeOverskrift>
            )}
            <p>Manuelt av {vilkaar.vurdering?.saksbehandler}</p>
            <p>
              Kommentar: <br />
              {vilkaar.vurdering?.kommentar}
            </p>
            <p>Sist endret {format(new Date(vilkaar.vurdering!!.tidspunkt), 'dd.MM.yyyy')}</p>
          </KildeVilkaar>
          <Button variant={'danger'} size={'small'} onClick={slettVurderingAvVilkaar}>
            Slett vurdering
          </Button>
        </>
      )}
      {!erVurdert() && aktivVurdering && (
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
      {!erVurdert() && !aktivVurdering && (
        <IkkeVurdert>
          <StatusIcon status={VurderingsresultatOld.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING} />
          Vilkåret er ikke vurdert
          <Button variant={'primary'} size={'small'} onClick={() => setAktivVurdering(true)}>
            Vurder vilkår
          </Button>
        </IkkeVurdert>
      )}
    </div>
  )
}

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
