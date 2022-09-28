import { VurderingsTitle } from '../soeknadsoversikt/styled'
import { Button, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { RadioGroupWrapper } from '../soeknadsoversikt/soeknadoversikt/kommerBarnetTilgode/KommerBarnetTilGodeVurdering'
import React, { useState } from 'react'
import { ISvar, VurderingsResultat as VurderingsresultatOld } from '../../../store/reducers/BehandlingReducer'
import { useParams } from 'react-router-dom'
import { slettVurdering, Vilkaar, VurderingsResultat, vurderVilkaar } from '../../../shared/api/vilkaarsvurdering'
import { StatusIcon } from '../../../shared/icons/statusIcon'
import styled from 'styled-components'

export const Vurdering = ({ vilkaar, oppdaterVilkaar }: { vilkaar: Vilkaar; oppdaterVilkaar: () => void }) => {
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

    if (radioError === undefined && begrunnelseError === undefined && svar !== undefined) {
      vurderVilkaar(behandlingId!!, {
        type: vilkaar.type,
        kommentar: kommentar,
        resultat: svar === ISvar.JA ? VurderingsResultat.OPPFYLT : VurderingsResultat.IKKE_OPPFYLT,
      }).then((response) => {
        if (response.status == 'ok') {
          setAktivVurdering(false)
          oppdaterVilkaar()
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

  const erVurdert = (): boolean => vilkaar.vurdering !== undefined
  const erOppfyllt = (): boolean => vilkaar.vurdering?.resultat == VurderingsResultat.OPPFYLT
  const avslag = (): boolean => !erOppfyllt()

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
            {erOppfyllt() && (
              <div className="svart">
                <StatusIcon status={VurderingsresultatOld.OPPFYLT} />
                Vilkår er oppfyllt
              </div>
            )}
            {avslag() && (
              <>
                <StatusIcon status={VurderingsresultatOld.IKKE_OPPFYLT} />
                Vilkår er ikke oppfyllt
              </>
            )}
            <p>Manuelt av {vilkaar.vurdering?.saksbehandler}</p>
            <p>
              Kommentar: <br />
              {vilkaar.vurdering?.kommentar}
            </p>
            <p>Sist endret {vilkaar.vurdering?.tidspunkt}</p>
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
          <br />
          <br />
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
`

export const KildeVilkaar = styled.div`
  color: grey;
  font-size: 0.7em;

  .svart {
    color: black;
    font-size: 1.2em;
  }
`
