import { BodyShort, Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import React, { useState } from 'react'
import {
  IVilkaarsvurdering,
  slettVurdering,
  Vilkaar,
  VurderingsResultat,
  vurderVilkaar,
} from '~shared/api/vilkaarsvurdering'
import styled from 'styled-components'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { useApiCall } from '~shared/hooks/useApiCall'
import { formaterVurderingsResultat } from '~components/behandling/vilkaarsvurdering/utils'
import { Vurdering as VurderingWrapper } from '~/components/behandling/soeknadsoversikt/styled'

const MIN_KOMMENTAR_LENGDE = 1
const INGEN_VILKAAR_OPPFYLT = 'ingen_vilkaar_oppfylt'

interface VilkaarForm {
  resultat: VurderingsResultat | null
  kommentar: string | undefined
  vilkaarsUnntakType: string | undefined
}

interface VilkaarFormValidert {
  resultat: VurderingsResultat
  kommentar: string
  vilkaarsUnntakType: string | undefined
}

const initiellForm = (vilkaar: Vilkaar): VilkaarForm => ({
  resultat: vilkaar.hovedvilkaar?.resultat ?? null,
  kommentar: vilkaar.vurdering?.kommentar ?? '',
  vilkaarsUnntakType:
    vilkaar.unntaksvilkaar?.find((unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat)?.type ??
    '',
})

export const Vurdering = ({
  vilkaar,
  oppdaterVilkaar,
  behandlingId,
  redigerbar,
}: {
  vilkaar: Vilkaar
  oppdaterVilkaar: (vilkaarsvurdering: IVilkaarsvurdering) => void
  behandlingId: string
  redigerbar: boolean
}) => {
  const [aktivVurdering, setAktivVurdering] = useState<boolean>(false)
  const [vilkaarutkast, setVilkaarutkast] = useState<VilkaarForm>(initiellForm(vilkaar))
  const [radioError, setRadioError] = useState<string>()
  const [unntakRadioError, setUnntakRadioError] = useState<string>()
  const [kommentarError, setKommentarError] = useState<string>()
  const [, postVurderVilkaar] = useApiCall(vurderVilkaar)
  const [, postSlettVurdering] = useApiCall(slettVurdering)
  const vilkaarSpoersmaal = vilkaar.hovedvilkaar.spoersmaal
    ? vilkaar.hovedvilkaar.spoersmaal
    : 'Er hovedvilkår oppfylt?' // TODO denne burde vi kunne bli kvitt når BP får spørsmål som en del av vilkår

  const valider = (vilkaarForm: VilkaarForm): vilkaarForm is VilkaarFormValidert => {
    const resultatIkkeValgt = vilkaarForm.resultat == undefined
    resultatIkkeValgt ? setRadioError('Du må velge et svar') : setRadioError(undefined)

    const unntakIkkeValgt =
      vilkaarForm.resultat == VurderingsResultat.IKKE_OPPFYLT &&
      !vilkaarForm.vilkaarsUnntakType &&
      vilkaar.unntaksvilkaar.length > 0
    unntakIkkeValgt ? setUnntakRadioError('Du må velge et unntak') : setUnntakRadioError(undefined)

    const manglerKommentar = MIN_KOMMENTAR_LENGDE > (vilkaarForm.kommentar?.length ?? 0)
    manglerKommentar ? setKommentarError('Du må oppgi en begrunnelse') : setKommentarError(undefined)

    return !resultatIkkeValgt && !unntakIkkeValgt && !manglerKommentar
  }

  const vilkaarVurdert = (values: VilkaarFormValidert, onSuccess?: () => void) => {
    const unntaksvilkaar =
      values.resultat == VurderingsResultat.IKKE_OPPFYLT &&
      values.vilkaarsUnntakType &&
      values.vilkaarsUnntakType !== INGEN_VILKAAR_OPPFYLT
        ? {
            unntaksvilkaar: {
              type: values.vilkaarsUnntakType,
              resultat: VurderingsResultat.OPPFYLT,
            },
          }
        : {}

    return postVurderVilkaar(
      {
        behandlingId,
        request: {
          vilkaarId: vilkaar.id,
          hovedvilkaar: {
            type: vilkaar.hovedvilkaar.type,
            resultat: values.resultat,
          },
          ...unntaksvilkaar,
          kommentar: values.kommentar,
        },
      },
      (response) => {
        oppdaterVilkaar(response)
        setAktivVurdering(false)
        onSuccess?.()
      }
    )
  }

  const slettVurderingAvVilkaar = (onSuccess?: () => void) =>
    postSlettVurdering({ behandlingId: behandlingId, type: vilkaar.id }, (data) => {
      oppdaterVilkaar(data)
      onSuccess?.()
      setVilkaarutkast({ resultat: null, kommentar: '', vilkaarsUnntakType: '' })
    })

  const reset = (onSuccess?: () => void) => {
    setAktivVurdering(false)
    setRadioError(undefined)
    setKommentarError(undefined)
    setVilkaarutkast(initiellForm(vilkaar))
    onSuccess?.()
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
      return 'Vilkåret er ikke aktuelt'
    }
  }

  const oppfyltUnntaksvilkaar = vilkaar.unntaksvilkaar?.find(
    (unntaksvilkaar) => VurderingsResultat.OPPFYLT === unntaksvilkaar.resultat
  )

  return (
    <VurderingWrapper>
      {!vilkaar.vurdering && !aktivVurdering ? (
        <IkkeVurdert>
          <Heading size="small">Vilkåret er ikke vurdert</Heading>
          <Button disabled={!redigerbar} variant="secondary" size="small" onClick={() => setAktivVurdering(true)}>
            Vurder vilkår
          </Button>
        </IkkeVurdert>
      ) : (
        <VurderingsboksWrapper
          tittel={overskrift()}
          subtittelKomponent={
            <>
              {!!oppfyltUnntaksvilkaar ? (
                <VilkaarVurdertInformasjon>
                  <Heading size="xsmall" level="3">
                    Er unntak fra hovedvilkåret oppfylt?
                  </Heading>
                  <BodyShort size="small">{oppfyltUnntaksvilkaar?.tittel}</BodyShort>
                </VilkaarVurdertInformasjon>
              ) : (
                <VilkaarVurdertInformasjon>
                  <Heading size="xsmall" level="3">
                    {vilkaarSpoersmaal}
                  </Heading>
                  <BodyShort size="small">{formaterVurderingsResultat(vilkaar.hovedvilkaar.resultat)}</BodyShort>
                </VilkaarVurdertInformasjon>
              )}
            </>
          }
          vurdering={
            vilkaar.vurdering
              ? { saksbehandler: vilkaar.vurdering.saksbehandler, tidspunkt: vilkaar.vurdering.tidspunkt }
              : undefined
          }
          key={`${redigerbar}+${vilkaar.id}`}
          kommentar={vilkaarutkast?.kommentar}
          defaultRediger={aktivVurdering}
          redigerbar={redigerbar}
          slett={(callback) => slettVurderingAvVilkaar(callback)}
          lagreklikk={(callback) => (valider(vilkaarutkast) ? vilkaarVurdert(vilkaarutkast, callback) : {})}
          avbrytklikk={reset}
        >
          <>
            <RadioGroupWrapper>
              <RadioGroup
                legend={vilkaarSpoersmaal}
                size="small"
                className="radioGroup"
                onChange={(event) => {
                  setVilkaarutkast({
                    ...vilkaarutkast,
                    resultat: VurderingsResultat[event as VurderingsResultat],
                  })
                  setRadioError(undefined)
                }}
                value={vilkaarutkast.resultat}
                error={radioError}
              >
                <div className="flex">
                  <Radio value={VurderingsResultat.OPPFYLT}>Ja</Radio>
                  <Radio value={VurderingsResultat.IKKE_OPPFYLT}>Nei</Radio>
                  <Radio value={VurderingsResultat.IKKE_VURDERT}>Ikke aktuelt</Radio>
                </div>
              </RadioGroup>
            </RadioGroupWrapper>

            {vilkaarutkast.resultat === VurderingsResultat.IKKE_OPPFYLT &&
              vilkaar.unntaksvilkaar &&
              vilkaar.unntaksvilkaar.length > 0 && (
                <>
                  <Unntaksvilkaar>
                    <RadioGroup
                      legend="Er unntak fra hovedvilkåret oppfylt?"
                      size="small"
                      className="radioGroup"
                      onChange={(type) => {
                        setVilkaarutkast({
                          ...vilkaarutkast,
                          vilkaarsUnntakType: type,
                        })
                        setUnntakRadioError(undefined)
                      }}
                      value={vilkaarutkast.vilkaarsUnntakType}
                      error={unntakRadioError}
                    >
                      <div className="flex">
                        {vilkaar.unntaksvilkaar.map((unntakvilkaar) => (
                          <Radio key={unntakvilkaar.type} value={unntakvilkaar.type}>
                            {unntakvilkaar.tittel}
                          </Radio>
                        ))}
                        <Radio key="Nei" value={INGEN_VILKAAR_OPPFYLT}>
                          Nei, ingen av unntakene er oppfylt
                        </Radio>
                      </div>
                    </RadioGroup>
                  </Unntaksvilkaar>
                </>
              )}
            <VurderingsLabel htmlFor={vilkaar.hovedvilkaar.tittel}>Begrunnelse (obligatorisk)</VurderingsLabel>
            <Textarea
              label=""
              hideLabel={false}
              placeholder="Gi en begrunnelse for vurderingen"
              defaultValue={vilkaarutkast.kommentar}
              onBlur={(e) => setVilkaarutkast({ ...vilkaarutkast, kommentar: e.target.value })}
              minRows={3}
              size="small"
              error={kommentarError ? kommentarError : false}
              autoComplete="off"
              name={vilkaar.hovedvilkaar.tittel}
            />
          </>
        </VurderingsboksWrapper>
      )}
    </VurderingWrapper>
  )
}

export const IkkeVurdert = styled.div`
  font-size: 0.8em;

  button {
    margin-top: 1em;
  }
`

export const VurderingsLabel = styled.label`
  font-size: 1rem;
`

export const RadioGroupWrapper = styled.div`
  margin-bottom: 1em;

  .flex {
    display: flex;
    gap: 20px;
  }

  legend {
    display: flex;
    font-size: 0.8em;
    font-weight: bold;
  }
`

export const Unntaksvilkaar = styled.div`
  margin-bottom: 1em;
`

export const VilkaarVurdertInformasjon = styled.div`
  margin-bottom: 1.5em;
  color: var(--a-gray-700);
`
