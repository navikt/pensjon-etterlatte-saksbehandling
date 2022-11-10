import { BodyShort, Button, Detail, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import {
  slettVurdering,
  Vilkaar,
  Vilkaarsvurdering,
  VurderingsResultat,
  vurderVilkaar,
} from '~shared/api/vilkaarsvurdering'
import styled from 'styled-components'
import { format } from 'date-fns'
import { Delete, Edit } from '@navikt/ds-icons'

const MIN_KOMMENTAR_LENGDE = 1

export const Vurdering = ({
  vilkaar,
  oppdaterVilkaar,
  behandlingId,
}: {
  vilkaar: Vilkaar
  oppdaterVilkaar: (vilkaarsvurdering: Vilkaarsvurdering) => void
  behandlingId: string
}) => {
  const [aktivVurdering, setAktivVurdering] = useState<boolean>(false)
  const [resultat, setResultat] = useState<VurderingsResultat>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const [kommentarError, setKommentarError] = useState<string>()
  const [vilkaarsUnntakType, setVilkaarsUnntakType] = useState<string>()

  const vilkaarVurdert = () => {
    !resultat ? setRadioError('Du må velge et svar') : setRadioError(undefined)
    !(kommentar.length >= MIN_KOMMENTAR_LENGDE)
      ? setKommentarError('Begrunnelse er påkrevet')
      : setKommentarError(undefined)

    if (
      radioError === undefined &&
      kommentarError === undefined &&
      resultat !== undefined &&
      kommentar.length >= MIN_KOMMENTAR_LENGDE
    ) {
      const inkluderUnntaksvilkaar =
        resultat == VurderingsResultat.IKKE_OPPFYLT && vilkaarsUnntakType && vilkaarsUnntakType != ''

      vurderVilkaar(behandlingId, {
        hovedvilkaar: {
          type: vilkaar.hovedvilkaar.type,
          resultat,
        },
        ...(inkluderUnntaksvilkaar && {
          unntaksvilkaar: {
            type: vilkaarsUnntakType,
            resultat: VurderingsResultat.OPPFYLT,
          },
        }),
        kommentar: kommentar,
      }).then((response) => {
        if (response.status == 'ok') {
          oppdaterVilkaar(response.data)
          reset()
        }
      })
    }
  }

  const slettVurderingAvVilkaar = () => {
    slettVurdering(behandlingId, vilkaar.hovedvilkaar.type).then((response) => {
      if (response.status == 'ok') {
        oppdaterVilkaar(response.data)
      }
    })
  }

  const redigerVilkaar = () => {
    setAktivVurdering(true)
    setResultat(vilkaar.hovedvilkaar?.resultat)

    const unntaksvilkaarOppfylt = vilkaar.unntaksvilkaar?.find(
      (unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat
    )

    if (unntaksvilkaarOppfylt) {
      setVilkaarsUnntakType(unntaksvilkaarOppfylt.type)
    } else {
      setVilkaarsUnntakType('')
    }

    setKommentar(vilkaar.vurdering?.kommentar || '')
  }

  const reset = () => {
    setAktivVurdering(false)
    setResultat(undefined)
    setRadioError(undefined)
    setKommentar('')
    setKommentarError(undefined)
  }

  const overskrift = () => {
    if (
      vilkaar.hovedvilkaar?.resultat == VurderingsResultat.OPPFYLT ||
      vilkaar.unntaksvilkaar?.some((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)
    ) {
      return 'Vilkår oppfylt'
    } else if (
      vilkaar.hovedvilkaar?.resultat == VurderingsResultat.IKKE_OPPFYLT &&
      !vilkaar.unntaksvilkaar?.some((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)
    ) {
      return 'Vilkår er ikke oppfylt'
    } else {
      return 'Vilkåret er ikke vurdert'
    }
  }

  const oppfyltUnntaksvilkaar = vilkaar.unntaksvilkaar?.find(
    (unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat
  )

  return (
    <div>
      {vilkaar.vurdering && !aktivVurdering && (
        <>
          <KildeVilkaar>
            <Heading size="small">{overskrift()}</Heading>
            <VilkaarVurdertInformasjon>
              <Detail size="medium">Manuelt av {vilkaar.vurdering?.saksbehandler}</Detail>
              <Detail size="medium">
                Sist endret {format(new Date(vilkaar.vurdering!!.tidspunkt), 'dd.MM.yyyy HH:mm')}
              </Detail>
            </VilkaarVurdertInformasjon>
            {oppfyltUnntaksvilkaar && (
              <VilkaarVurdertInformasjon>
                <Heading size="xsmall">Unntak er oppfylt</Heading>
                <BodyShort size="small">{oppfyltUnntaksvilkaar?.paragraf.tittel}</BodyShort>
              </VilkaarVurdertInformasjon>
            )}
            {vilkaar.vurdering?.kommentar && (
              <VilkaarVurdertInformasjon>
                <Heading size="xsmall">Begrunnelse</Heading>
                <BodyShort size="small">{vilkaar.vurdering?.kommentar}</BodyShort>
              </VilkaarVurdertInformasjon>
            )}
          </KildeVilkaar>

          <RedigerWrapper onClick={redigerVilkaar}>
            <Edit />
            <span className={'text'}> Rediger</span>
          </RedigerWrapper>
          <RedigerWrapper onClick={slettVurderingAvVilkaar}>
            <Delete />
            <span className={'text'}> Slett</span>
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
                setResultat(VurderingsResultat[event as VurderingsResultat])
                setRadioError(undefined)
              }}
              value={resultat || ''}
              error={radioError ? radioError : false}
            >
              <div className="flex">
                <Radio value={VurderingsResultat.OPPFYLT}>Oppfylt</Radio>
                <Radio value={VurderingsResultat.IKKE_OPPFYLT}>Ikke oppfylt</Radio>
                <Radio value={VurderingsResultat.IKKE_VURDERT}>Ikke vurdert</Radio>
              </div>
            </RadioGroup>
          </RadioGroupWrapper>

          {VurderingsResultat.IKKE_OPPFYLT === resultat && vilkaar.unntaksvilkaar && vilkaar.unntaksvilkaar.length > 0 && (
            <>
              <VurderingsTitle>Er unntak fra hovedregelen oppfylt?</VurderingsTitle>
              <Unntaksvilkaar>
                <RadioGroup
                  legend=""
                  size="small"
                  className="radioGroup"
                  onChange={(event) => setVilkaarsUnntakType(event)}
                  value={vilkaarsUnntakType || ''}
                >
                  <div className="flex">
                    {vilkaar.unntaksvilkaar.map((unntakvilkaar) => {
                      return (
                        <Radio key={unntakvilkaar.type} value={unntakvilkaar.type}>
                          {unntakvilkaar.paragraf.tittel}
                        </Radio>
                      )
                    })}
                    <Radio key="Nei" value="">
                      Nei, ingen av unntakene er oppfylt
                    </Radio>
                  </div>
                </RadioGroup>
              </Unntaksvilkaar>
            </>
          )}
          <Textarea
            label="Begrunnelse (obligatorisk)"
            hideLabel={false}
            placeholder="Gi en begrunnelse for vurderingen"
            value={kommentar}
            onChange={(e) => {
              const kommentarLocal = e.target.value
              setKommentar(kommentarLocal)
              kommentarLocal.length >= MIN_KOMMENTAR_LENGDE && setKommentarError(undefined)
            }}
            minRows={3}
            size="small"
            error={kommentarError ? kommentarError : false}
          />
          <VurderingKnapper>
            <Button variant={'primary'} size={'small'} onClick={vilkaarVurdert}>
              Lagre
            </Button>
            <Button variant={'secondary'} size={'small'} onClick={reset}>
              Avbryt
            </Button>
          </VurderingKnapper>
        </>
      )}
      {!vilkaar.vurdering && !aktivVurdering && (
        <IkkeVurdert>
          <Heading size="small">Vilkåret er ikke vurdert</Heading>
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
  margin-right: 10px;

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
  font-size: 0.7em;

  p {
    font-weight: normal;
  }
`

export const KildeOverskrift = styled.div`
  color: black;
  font-size: 1.2em;
`

export const VurderingsTitle = styled.div`
  display: flex;
  font-size: 0.8em;
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

export const Unntaksvilkaar = styled.div`
  margin-bottom: 1em;
`

export const VurderingKnapper = styled.div`
  button {
    margin-top: 10px;
    margin-right: 10px;
  }
`

export const VilkaarVurdertInformasjon = styled.div`
  margin-bottom: 1.5em;
  color: var(--navds-global-color-gray-700);
`
