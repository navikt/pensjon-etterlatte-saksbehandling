import React, { useState } from 'react'
import styled from 'styled-components'
import { BehandlingHandlingKnapper } from '../handlinger/BehandlingHandlingKnapper'
import { VilkaarsVurderingKnapper } from '../handlinger/vilkaarsvurderingKnapper'
import {
  lagreTotalVurdering,
  slettTotalVurdering,
  IVilkaarsvurdering,
  VilkaarsvurderingResultat,
} from '~shared/api/vilkaarsvurdering'
import { VilkaarBorder } from './styled'
import { BodyShort, Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { svarTilTotalResultat } from './utils'
import { Delete } from '@navikt/ds-icons'
import { StatusIcon } from '~shared/icons/statusIcon'
import { formaterStringDato } from '~utils/formattering'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { ISvar } from '~shared/types/ISvar'

type Props = {
  virkningstidspunktDato: string
  vilkaarsvurdering: IVilkaarsvurdering
  oppdaterVilkaar: (vilkaarsvurdering: IVilkaarsvurdering) => void
  behandlingId: string
}
const MIN_KOMMENTAR_LENGDE = 1

export const Resultat: React.FC<Props> = ({
  virkningstidspunktDato,
  vilkaarsvurdering,
  oppdaterVilkaar,
  behandlingId,
}) => {
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
      ? setKommentarError('Begrunnelse er påkrevet')
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
    vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT
      ? 'Ja, vilkår er oppfylt'
      : 'Nei, vilkår er ikke oppfylt'

  const reset = () => {
    setSvar(undefined)
    setRadioError(undefined)
    setKommentar('')
    setKommentarError(undefined)
  }

  const status =
    vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT
      ? VurderingsResultat.OPPFYLT
      : VurderingsResultat.IKKE_OPPFYLT

  return (
    <>
      <VilkaarsvurderingContent>
        <HeadingWrapper>
          <Heading size="small">Er vilkårene for barnepensjon oppfylt?</Heading>
        </HeadingWrapper>
        {vilkaarsvurdering.resultat && (
          <ContentWrapper>
            <TekstWrapper>
              <StatusIcon status={status} noLeftPadding /> {`${resultatTekst()}`}
            </TekstWrapper>
            {vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT && (
              <BodyShort>Barnepensjon er innvilget f.o.m {formaterStringDato(virkningstidspunktDato)}</BodyShort>
            )}
            <Kommentar>
              <Heading size="xsmall">Begrunnelse</Heading>
              <BodyShort size="small">{vilkaarsvurdering.resultat.kommentar}</BodyShort>
            </Kommentar>
            <SlettWrapper onClick={slettVilkaarsvurderingResultat}>
              <Delete />
              <span className={'text'}>Slett vurdering</span>
            </SlettWrapper>
          </ContentWrapper>
        )}

        {!vilkaarsvurdering.resultat && !alleVilkaarErVurdert && (
          <TekstWrapper>Alle vilkår må vurderes før man kan gå videre</TekstWrapper>
        )}

        {!vilkaarsvurdering.resultat && alleVilkaarErVurdert && (
          <>
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
                <Radio value={ISvar.JA}>Ja, vilkår er oppfylt</Radio>
                <Radio value={ISvar.NEI}>Nei, vilkår er ikke oppfylt</Radio>
              </RadioGroup>
            </RadioGroupWrapper>
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
  margin-bottom: 2em;
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
  margin-top: 20px;
  margin-bottom: 10px;
  display: flex;
  font-weight: bold;
`

const Kommentar = styled.div`
  margin-top: 20px;
  margin-bottom: 20px;
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

const HeadingWrapper = styled.div`
  margin-top: 2em;
`

const ContentWrapper = styled.div`
  color: var(--navds-global-color-gray-700);
`
