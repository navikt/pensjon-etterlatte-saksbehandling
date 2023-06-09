import { IBehandlingStatus, IBoddEllerArbeidetUtlandet } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { BodyShort, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { VurderingsTitle } from '../../styled'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import { useState } from 'react'
import { useApiCall, isFailure } from '~shared/hooks/useApiCall'
import { lagreBoddEllerArbeidetUtlandet } from '~shared/api/behandling'
import { oppdaterBehandlingsstatus, oppdaterBoddEllerArbeidetUtlandet } from '~store/reducers/BehandlingReducer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'

export const BoddEllerArbeidetUtlandetVurdering = ({
  boddEllerArbeidetUtlandet,
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | undefined
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<JaNei | undefined>(finnSvar(boddEllerArbeidetUtlandet))
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(boddEllerArbeidetUtlandet?.begrunnelse || '')
  const [setBoddEllerArbeidetUtlandetStatus, setBoddEllerArbeidetUtlandet, resetToInitial] =
    useApiCall(lagreBoddEllerArbeidetUtlandet)

  const lagre = (onSuccess?: () => void) => {
    !svar ? setRadioError('Du må velge et svar') : setRadioError('')

    if (svar !== undefined)
      return setBoddEllerArbeidetUtlandet({ behandlingId, begrunnelse, svar: svar === JaNei.JA }, (response) => {
        dispatch(oppdaterBoddEllerArbeidetUtlandet(response))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(finnSvar(boddEllerArbeidetUtlandet))
    setRadioError('')
    setBegrunnelse(boddEllerArbeidetUtlandet?.begrunnelse || '')
    setVurdert(boddEllerArbeidetUtlandet !== null)
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel={''}
      subtittelKomponent={
        <>
          <BodyShort spacing>Har avdøde bodd eller arbeidet i utlandet?</BodyShort>
          {boddEllerArbeidetUtlandet?.boddEllerArbeidetUtlandet && (
            <Label as={'p'} size="small" style={{ marginBottom: '32px' }}>
              {JaNeiRec[boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? JaNei.JA : JaNei.NEI]}
            </Label>
          )}
        </>
      }
      redigerbar={redigerbar}
      vurdering={
        boddEllerArbeidetUtlandet?.kilde
          ? {
              saksbehandler: boddEllerArbeidetUtlandet?.kilde.ident,
              tidspunkt: new Date(boddEllerArbeidetUtlandet?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={boddEllerArbeidetUtlandet?.begrunnelse}
      defaultRediger={boddEllerArbeidetUtlandet === null}
    >
      <>
        <VurderingsTitle title={'Har avdøde bodd eller arbeidet i utlandet?'} />
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
          }}
          placeholder="Valgfritt"
        />
        {isFailure(setBoddEllerArbeidetUtlandetStatus) && (
          <ApiErrorAlert>Kunne ikke lagre bodd eller arbeidet i utlandet</ApiErrorAlert>
        )}
      </>
    </VurderingsboksWrapper>
  )
}

function finnSvar(boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | undefined): JaNei | undefined {
  switch (boddEllerArbeidetUtlandet?.boddEllerArbeidetUtlandet) {
    case true:
      return JaNei.JA
    case false:
      return JaNei.NEI
    default:
      return undefined
  }
}
