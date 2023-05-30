import {
  GyldigFramsattType,
  IBehandlingStatus,
  IGyldighetproving,
  IGyldighetResultat,
  IManuellVurdering,
} from '~shared/types/IDetaljertBehandling'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'
import { useState } from 'react'
import { Undertekst, VurderingsTitle } from '~components/behandling/soeknadsoversikt/styled'
import styled from 'styled-components'
import { BodyShort, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { SoeknadsoversiktTextArea } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { oppdaterBehandlingsstatus, oppdaterGyldighetsproeving } from '~store/reducers/BehandlingReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreGyldighetsproeving } from '~shared/api/behandling'

export const GyldigFramsattVurdering = ({
  behandlingId,
  gyldigFramsatt,
  redigerbar,
}: {
  behandlingId: string
  gyldigFramsatt: IGyldighetResultat | undefined
  redigerbar: boolean
}) => {
  const tittel = finnTittel(gyldigFramsatt)
  const innsenderErGjenlevende = finnVurdering(gyldigFramsatt, GyldigFramsattType.INNSENDER_ER_GJENLEVENDE)
  const manuellVurdering: IManuellVurdering = innsenderErGjenlevende?.basertPaaOpplysninger

  const dispatch = useAppDispatch()
  const [vurdert, setVurdert] = useState(manuellVurdering != null)
  const [svar, setSvar] = useState<JaNei | undefined>(finnSvar(gyldigFramsatt))
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(manuellVurdering?.begrunnelse || '')
  const [begrunnelseError, setBegrunnelseError] = useState<string>('')
  const [, setGyldighetsproeving, resetToInitial] = useApiCall(lagreGyldighetsproeving)

  const lagre = (onSuccess?: () => void) => {
    !svar ? setRadioError('Du må velge et svar') : setRadioError('')
    const harBegrunnelse = begrunnelse.trim().length > 0
    harBegrunnelse ? setBegrunnelseError('') : setBegrunnelseError('Begrunnelsen må fylles ut')

    if (svar !== undefined && harBegrunnelse) {
      return setGyldighetsproeving(
        {
          behandlingId,
          svar,
          begrunnelse,
        },
        (response) => {
          dispatch(oppdaterGyldighetsproeving(response))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
          onSuccess?.()
        }
      )
    }
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(finnSvar(gyldigFramsatt))
    setRadioError('')
    setBegrunnelseError('')
    setBegrunnelse(manuellVurdering?.begrunnelse || '')
    setVurdert(manuellVurdering != null)
    onSuccess?.()
  }

  return !vurdert ? (
    <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
  ) : (
    <VurderingsboksWrapper
      tittel={tittel}
      subtittelKomponent={
        <>
          <BodyShort spacing>Er innsender den gjenlevende?</BodyShort>
          {gyldigFramsatt?.resultat && (
            <Label as={'p'} size="small" style={{ marginBottom: '32px' }}>
              {JaNeiRec[gyldigFramsatt.resultat === VurderingsResultat.OPPFYLT ? JaNei.JA : JaNei.NEI]}
            </Label>
          )}
        </>
      }
      redigerbar={redigerbar}
      vurdering={
        manuellVurdering?.kilde
          ? {
              saksbehandler: manuellVurdering?.kilde.ident,
              tidspunkt: new Date(manuellVurdering?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={manuellVurdering?.begrunnelse}
      defaultRediger={
        gyldigFramsatt?.resultat !== VurderingsResultat.OPPFYLT &&
        gyldigFramsatt?.resultat !== VurderingsResultat.IKKE_OPPFYLT
      }
    >
      <div>
        <VurderingsTitle title={'Trenger avklaring'} />
        <Undertekst $gray={false}>Innsender er den gjenlevende?</Undertekst>
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              setSvar(JaNei[event as JaNei])
              setRadioError('')
            }}
            value={svar || ''}
            error={radioError ? radioError : false}
          >
            <div className="flex">
              <Radio value={JaNei.JA}>Ja</Radio>
              <Radio value={JaNei.NEI}>Nei</Radio>
            </div>
          </RadioGroup>
        </RadioGroupWrapper>
        <SoeknadsoversiktTextArea
          value={begrunnelse}
          onChange={(e) => {
            const oppdatertBegrunnelse = e.target.value
            setBegrunnelse(oppdatertBegrunnelse)
            oppdatertBegrunnelse.trim().length > 0 && setBegrunnelseError('')
          }}
          error={begrunnelseError ? begrunnelseError : false}
        />
      </div>
    </VurderingsboksWrapper>
  )
}

function finnTittel(gyldigFamsatt: IGyldighetResultat | undefined): string {
  switch (gyldigFamsatt?.resultat) {
    case VurderingsResultat.OPPFYLT:
      return 'Innsender er gjenlevende forelder'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Innsender er ikke gjenlevende'
    default:
      return 'Trenger avklaring'
  }
}

function finnVurdering(gyldigFramsatt: IGyldighetResultat | undefined, type: GyldigFramsattType) {
  return gyldigFramsatt?.vurderinger.find((g: IGyldighetproving) => g.navn === type)
}

function finnSvar(gyldigFamsatt: IGyldighetResultat | undefined): JaNei | undefined {
  switch (gyldigFamsatt?.resultat) {
    case VurderingsResultat.OPPFYLT:
      return JaNei.JA
    case VurderingsResultat.IKKE_OPPFYLT:
      return JaNei.NEI
    default:
      return undefined
  }
}

const RadioGroupWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 1em;

  .flex {
    display: flex;
    gap: 20px;
  }
`
