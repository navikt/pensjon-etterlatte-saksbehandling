import { IBehandlingStatus, IBoddEllerArbeidetUtlandet } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { BodyShort, Checkbox, CheckboxGroup, HelpText, Label, Radio, RadioGroup } from '@navikt/ds-react'
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
  boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<JaNei | null>(finnSvar(boddEllerArbeidetUtlandet))
  const [boddArbeidetIkkeEosEllerAvtaleland, setBoddArbeidetIkkeEosEllerAvtaleland] = useState<boolean>(
    boddEllerArbeidetUtlandet?.boddArbeidetIkkeEosEllerAvtaleland ?? false
  )
  const [boddArbeidetEosNordiskKonvensjon, setBoddArbeidetEosNordiskKonvensjon] = useState<boolean>(
    boddEllerArbeidetUtlandet?.boddArbeidetEosNordiskKonvensjon ?? false
  )
  const [boddArbeidetAvtaleland, setBoddArbeidetAvtaleland] = useState<boolean>(
    boddEllerArbeidetUtlandet?.boddArbeidetAvtaleland ?? false
  )
  const [vurdereAvoededsTrygdeavtale, setVurdereAvoededsTrygdeavtale] = useState<boolean>(
    boddEllerArbeidetUtlandet?.vurdereAvoededsTrygdeavtale ?? false
  )
  const [norgeErBehandlendeland, setNorgeErBehandlendeland] = useState<boolean>(
    boddEllerArbeidetUtlandet?.norgeErBehandlendeland ?? false
  )
  const [skalSendeKravpakke, setSkalSendeKravpakke] = useState<boolean>(
    boddEllerArbeidetUtlandet?.skalSendeKravpakke ?? false
  )

  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(boddEllerArbeidetUtlandet?.begrunnelse || '')
  const [setBoddEllerArbeidetUtlandetStatus, setBoddEllerArbeidetUtlandet, resetToInitial] =
    useApiCall(lagreBoddEllerArbeidetUtlandet)

  const lagre = (onSuccess?: () => void) => {
    svar ? setRadioError('') : setRadioError('Du må velge et svar')

    if (svar)
      return setBoddEllerArbeidetUtlandet(
        {
          behandlingId,
          begrunnelse,
          svar: svar === JaNei.JA,
          boddArbeidetIkkeEosEllerAvtaleland,
          boddArbeidetEosNordiskKonvensjon,
          boddArbeidetAvtaleland,
          vurdereAvoededsTrygdeavtale,
          norgeErBehandlendeland,
          skalSendeKravpakke,
        },
        (response) => {
          dispatch(oppdaterBoddEllerArbeidetUtlandet(response))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
          onSuccess?.()
        }
      )
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
      tittel=""
      subtittelKomponent={
        <>
          <BodyShort spacing>Har avdøde bodd eller arbeidet i utlandet?</BodyShort>
          {boddEllerArbeidetUtlandet ? (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {JaNeiRec[boddEllerArbeidetUtlandet.boddEllerArbeidetUtlandet ? JaNei.JA : JaNei.NEI]}
            </Label>
          ) : (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              Ikke vurdert
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
        <VurderingsTitle title="Har avdøde bodd eller arbeidet i utlandet?" />
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
        {svar === JaNei.JA && (
          <>
            <CheckboxGroup legend="Vurdering av utlandsopphold">
              <Checkbox
                value={boddArbeidetIkkeEosEllerAvtaleland}
                onChange={() => {
                  setBoddArbeidetIkkeEosEllerAvtaleland(!boddArbeidetIkkeEosEllerAvtaleland)
                }}
              >
                Avdøde har bodd/arbeidet i utlandet (Ikke EØS/avtaleland)
              </Checkbox>
              <Checkbox
                value={boddArbeidetEosNordiskKonvensjon}
                onChange={() => {
                  setBoddArbeidetEosNordiskKonvensjon(!boddArbeidetEosNordiskKonvensjon)
                }}
              >
                Avdøde har bodd/arbeidet i utlandet (EØS/nordisk konvensjon)
              </Checkbox>
              <Checkbox
                value={boddArbeidetAvtaleland}
                onChange={() => {
                  setBoddArbeidetAvtaleland(!boddArbeidetAvtaleland)
                }}
              >
                Avdøde har bodd/arbeidet i utlandet (Avtaleland)
              </Checkbox>
            </CheckboxGroup>
            <CheckboxGroup legend="Vurdering av utlandsopphold m.m.">
              <Checkbox
                value={vurdereAvoededsTrygdeavtale}
                onChange={() => {
                  setVurdereAvoededsTrygdeavtale(!vurdereAvoededsTrygdeavtale)
                }}
              >
                Vurdere avdødes trygdeavtale
              </Checkbox>
              <Checkbox
                value={norgeErBehandlendeland}
                onChange={() => {
                  setNorgeErBehandlendeland(!norgeErBehandlendeland)
                }}
              >
                Norge er behandlende land
              </Checkbox>
              <Checkbox
                value={skalSendeKravpakke}
                onChange={() => {
                  setSkalSendeKravpakke(!skalSendeKravpakke)
                }}
              >
                Det skal sendes kravpakke
                <HelpText>
                  Hvis avdøde har hatt AP/UT og kravpakke er sendt med svar om ingen rett fra utland, skal det ikke
                  sendes kravpakke. Hvis du krysser av vil det automatisk bli opprettet “kravpakke til utland” etter
                  attestering.
                </HelpText>
              </Checkbox>
            </CheckboxGroup>
          </>
        )}
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

function finnSvar(boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null): JaNei | null {
  switch (boddEllerArbeidetUtlandet?.boddEllerArbeidetUtlandet) {
    case true:
      return JaNei.JA
    case false:
      return JaNei.NEI
    default:
      return null
  }
}
