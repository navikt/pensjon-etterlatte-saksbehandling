import React, { useState } from 'react'
import styled from 'styled-components'
import { VilkaarsvurderingKnapper } from './VilkaarsvurderingKnapper'
import {
  IVilkaarsvurdering,
  oppdaterTotalVurdering,
  slettTotalVurdering,
  VilkaarsvurderingResultat,
} from '~shared/api/vilkaarsvurdering'
import { VilkaarWrapper } from './styled'
import { BodyShort, Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { svarTilTotalResultat } from './utils'
import { PencilWritingIcon, TrashIcon } from '@navikt/aksel-icons'
import { StatusIcon } from '~shared/icons/statusIcon'
import { formaterSakstype, formaterStringDato } from '~utils/formattering'
import { ISvar } from '~shared/types/ISvar'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useAppDispatch } from '~store/Store'
import { oppdaterBehandlingsstatus, updateVilkaarsvurdering } from '~store/reducers/BehandlingReducer'
import { IBehandlingStatus, IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { SakType } from '~shared/types/sak'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'

import { isPending } from '~shared/api/apiUtils'
import { OppdatertGrunnlagAlert } from '~components/behandling/trygdetid/Grunnlagopplysninger'

type Props = {
  virkningstidspunktDato: string | undefined
  sakstype: SakType
  vilkaarsvurdering: IVilkaarsvurdering
  oppdaterVilkaar: (vilkaarsvurdering: IVilkaarsvurdering) => void
  behandlingId: string
  redigerbar: boolean
  behandlingstype: IBehandlingsType
}

export const Resultat = (props: Props) => {
  const {
    virkningstidspunktDato,
    sakstype,
    vilkaarsvurdering,
    oppdaterVilkaar,
    behandlingId,
    redigerbar,
    behandlingstype,
  } = props
  const [svar, setSvar] = useState<ISvar>()
  const [radioError, setRadioError] = useState<string>()
  const [kommentar, setKommentar] = useState<string>('')
  const dispatch = useAppDispatch()
  const [totalVurderingStatus, oppdaterTotalVurderingCall] = useApiCall(oppdaterTotalVurdering)
  const [slettVurderingStatus, slettTotalVurderingCall] = useApiCall(slettTotalVurdering)

  const alleVilkaarErVurdert = !vilkaarsvurdering.vilkaar.some((vilkaar) => !vilkaar.vurdering)

  const slettVilkaarsvurderingResultat = () =>
    slettTotalVurderingCall(behandlingId, (res) => {
      oppdaterVilkaar(res)
      dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
      reset()
    })

  const lagreVilkaarsvurderingResultat = () => {
    !(svar && [ISvar.JA, ISvar.NEI].includes(svar))
      ? setRadioError('Du må svare på om vilkårsvurderingen er oppfylt')
      : setRadioError(undefined)

    if (radioError === undefined && svar !== undefined) {
      oppdaterTotalVurderingCall({ behandlingId, resultat: svarTilTotalResultat(svar), kommentar }, (res) => {
        oppdaterVilkaar(res)
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.VILKAARSVURDERT))
      })
    }
  }

  const resultatTekst = () => {
    if (erRevurdering) {
      return vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT
        ? 'Fortsatt oppfylt'
        : 'Opphør av ytelse'
    }

    return vilkaarsvurdering.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT
      ? 'Ja, vilkår er oppfylt'
      : 'Nei, vilkår er ikke oppfylt'
  }

  const reset = () => {
    setSvar(undefined)
    setRadioError(undefined)
    setKommentar('')
  }

  const status = vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT ? 'success' : 'error'
  const virkningstidspunktSamsvarer = virkningstidspunktDato === vilkaarsvurdering.virkningstidspunkt
  const erRevurdering = behandlingstype === IBehandlingsType.REVURDERING

  return (
    <>
      <VilkaarWrapper>
        <VilkaarsvurderingContent>
          <HeadingWrapper>
            <Heading size="small" level="2">
              {erRevurdering
                ? 'Utfall etter revurdering'
                : `Er vilkårene for ${formaterSakstype(sakstype).toLowerCase()} oppfylt?`}
            </Heading>
          </HeadingWrapper>
          {vilkaarsvurdering.resultat && (
            <ContentWrapper>
              <TekstWrapper>
                <StatusIcon status={status} /> {`${resultatTekst()}`}
              </TekstWrapper>
              {vilkaarsvurdering?.resultat?.utfall == VilkaarsvurderingResultat.OPPFYLT && (
                <BodyShort>
                  {erRevurdering
                    ? null
                    : `${formaterSakstype(sakstype)} er innvilget f.o.m ${formaterStringDato(
                        vilkaarsvurdering.virkningstidspunkt
                      )}`}
                </BodyShort>
              )}
              {vilkaarsvurdering?.resultat?.kommentar && (
                <Kommentar>
                  <Heading size="xsmall" level="3">
                    Begrunnelse
                  </Heading>
                  <ResultatKommentar>{vilkaarsvurdering.resultat.kommentar}</ResultatKommentar>
                </Kommentar>
              )}
              {redigerbar && (
                <Button
                  loading={isPending(slettVurderingStatus)}
                  onClick={slettVilkaarsvurderingResultat}
                  variant="tertiary"
                  icon={<TrashIcon />}
                >
                  Slett vurdering
                </Button>
              )}
              {redigerbar && (
                <Button
                  style={{ marginLeft: '1rem' }}
                  onClick={() => {
                    setKommentar(vilkaarsvurdering?.resultat?.kommentar || '')
                    const vilkaarsvurderingUtenResultat = { ...vilkaarsvurdering, resultat: undefined }
                    dispatch(updateVilkaarsvurdering(vilkaarsvurderingUtenResultat))
                  }}
                  variant="tertiary"
                  icon={<PencilWritingIcon />}
                  loading={isPending(totalVurderingStatus)}
                >
                  Rediger vurdering
                </Button>
              )}
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
                  <Radio value={ISvar.JA}>{erRevurdering ? 'Fortsatt oppfylt' : 'Ja, vilkår er oppfylt'}</Radio>
                  <Radio value={ISvar.NEI}>{erRevurdering ? 'Opphør av ytelse' : 'Nei, vilkår er ikke oppfylt'}</Radio>
                </RadioGroup>
              </RadioGroupWrapper>
              <Textarea
                label="Begrunnelse"
                hideLabel={false}
                placeholder="Valgfritt"
                value={kommentar}
                onChange={(e) => setKommentar(e.target.value)}
                minRows={3}
                size="medium"
                autoComplete="off"
              />
              <Button
                variant="primary"
                size="small"
                onClick={lagreVilkaarsvurderingResultat}
                loading={isPending(totalVurderingStatus)}
              >
                Lagre
              </Button>
            </>
          )}
        </VilkaarsvurderingContent>
      </VilkaarWrapper>

      {vilkaarsvurdering.resultat && !virkningstidspunktSamsvarer && (
        <OppdatertGrunnlagAlert variant="warning">
          Virkningstidspunktet er endret og vilkårene må da vurderes på nytt. For å starte ny vurdering må du slette
          nåværende vilkårsvurdering
        </OppdatertGrunnlagAlert>
      )}

      <Border />
      {redigerbar ? (
        <>
          {vilkaarsvurdering.isGrunnlagUtdatert && (
            <OppdatertGrunnlagAlert variant="warning">
              OBS! Grunnlaget for vilkårsvurderingen har blitt oppdatert siden sist. <br />
              Du må se over vurderingene og sjekke at de fortsatt er riktige.
            </OppdatertGrunnlagAlert>
          )}
          {vilkaarsvurdering.resultat && virkningstidspunktSamsvarer && (
            <VilkaarsvurderingKnapper behandlingId={behandlingId} />
          )}
        </>
      ) : (
        <NesteOgTilbake></NesteOgTilbake>
      )}
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
`

const ResultatKommentar = styled(BodyShort)`
  white-space: pre-wrap;
`

const HeadingWrapper = styled.div`
  margin-top: 2em;
`

const ContentWrapper = styled.div`
  color: var(--a-gray-700);
`
