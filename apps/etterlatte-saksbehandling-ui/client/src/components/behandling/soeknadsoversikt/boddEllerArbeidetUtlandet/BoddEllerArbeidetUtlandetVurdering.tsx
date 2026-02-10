import { IBehandlingStatus, IBoddEllerArbeidetUtlandet } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import {
  BodyShort,
  Box,
  Checkbox,
  Heading,
  HelpText,
  HStack,
  Radio,
  RadioGroup,
  Textarea,
  VStack,
} from '@navikt/ds-react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreBoddEllerArbeidetUtlandet } from '~shared/api/behandling'
import { oppdaterBehandlingsstatus, oppdaterBoddEllerArbeidetUtlandet } from '~store/reducers/BehandlingReducer'
import { JaNei } from '~shared/types/ISvar'
import BoddEllerArbeidetIUtlandetVisning from '~components/behandling/soeknadsoversikt/boddEllerArbeidetUtlandet/BoddEllerArbeidetIUtlandetVisning'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

export const BoddEllerArbeidetUtlandetVurdering = ({
  redigerbar,
  behandlingId,
}: {
  redigerbar: boolean
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()
  const boddEllerArbeidetUtlandet =
    useAppSelector((state) => state.behandlingReducer.behandling?.boddEllerArbeidetUtlandet) ?? null

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
  const [vurdereAvdoedesTrygdeavtale, setVurdereAvdoedesTrygdeavtale] = useState<boolean>(
    boddEllerArbeidetUtlandet?.vurdereAvdoedesTrygdeavtale ?? false
  )
  const [skalSendeKravpakke, setSkalSendeKravpakke] = useState<boolean>(
    boddEllerArbeidetUtlandet?.skalSendeKravpakke ?? false
  )

  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(boddEllerArbeidetUtlandet?.begrunnelse || '')
  const [setBoddEllerArbeidetUtlandetStatus, setBoddEllerArbeidetUtlandet, resetToInitial] =
    useApiCall(lagreBoddEllerArbeidetUtlandet)

  const lagre = (onSuccess?: () => void) => {
    if (!svar) {
      setRadioError('Du må velge et svar')
      return
    }
    setRadioError('')

    return setBoddEllerArbeidetUtlandet(
      {
        behandlingId,
        begrunnelse,
        boddEllerArbeidetUtlandet: svar === JaNei.JA,
        boddArbeidetIkkeEosEllerAvtaleland,
        boddArbeidetEosNordiskKonvensjon,
        boddArbeidetAvtaleland,
        vurdereAvdoedesTrygdeavtale: vurdereAvdoedesTrygdeavtale,
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
    onSuccess?.()
  }

  const settNyttSvar = (svar: JaNei) => {
    setSvar(svar)
    setRadioError('')

    if (svar === JaNei.NEI) {
      setBoddArbeidetIkkeEosEllerAvtaleland(false)
      setBoddArbeidetEosNordiskKonvensjon(false)
      setBoddArbeidetAvtaleland(false)
      setVurdereAvdoedesTrygdeavtale(false)
      setSkalSendeKravpakke(false)
    }
  }

  return (
    <VurderingsboksWrapper
      tittel="Har avdøde bodd eller arbeidet i utlandet?"
      subtittelKomponent={<BoddEllerArbeidetIUtlandetVisning boddEllerArbeidetUtlandet={boddEllerArbeidetUtlandet} />}
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
      visAvbryt={!!boddEllerArbeidetUtlandet?.kilde}
    >
      <>
        <Heading level="3" size="small">
          Har avdøde bodd eller arbeidet i utlandet?
        </Heading>
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={settNyttSvar}
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
          <VStack gap="space-4">
            <div>
              <Heading level="3" size="xsmall">
                Vurdering av utlandsopphold
              </Heading>
              <Checkbox
                checked={boddArbeidetIkkeEosEllerAvtaleland}
                onChange={() => {
                  setBoddArbeidetIkkeEosEllerAvtaleland(!boddArbeidetIkkeEosEllerAvtaleland)
                }}
              >
                Avdøde har bodd/arbeidet i utlandet (Ikke EØS/avtaleland)
              </Checkbox>
              <Checkbox
                checked={boddArbeidetEosNordiskKonvensjon}
                onChange={() => {
                  setBoddArbeidetEosNordiskKonvensjon(!boddArbeidetEosNordiskKonvensjon)
                }}
              >
                Avdøde har bodd/arbeidet i utlandet (EØS/nordisk konvensjon)
              </Checkbox>
              <Checkbox
                checked={boddArbeidetAvtaleland}
                onChange={() => {
                  setBoddArbeidetAvtaleland(!boddArbeidetAvtaleland)
                }}
              >
                Avdøde har bodd/arbeidet i utlandet (Avtaleland)
              </Checkbox>
            </div>
            <div>
              <Heading level="3" size="xsmall">
                Huk av hvis aktuelt
              </Heading>
              <Checkbox
                checked={vurdereAvdoedesTrygdeavtale}
                onChange={() => {
                  setVurdereAvdoedesTrygdeavtale(!vurdereAvdoedesTrygdeavtale)
                }}
              >
                Vurdere avdødes trygdeavtale
              </Checkbox>
              <Checkbox
                checked={skalSendeKravpakke}
                onChange={() => {
                  setSkalSendeKravpakke(!skalSendeKravpakke)
                }}
              >
                <HStack gap="space-2">
                  <BodyShort>Det skal sendes kravpakke</BodyShort>
                  <HelpText strategy="fixed">
                    Hvis avdøde har hatt AP/UT og kravpakke er sendt med svar om ingen rett fra utland, skal det ikke
                    sendes kravpakke. Hvis du krysser av vil det automatisk bli opprettet “kravpakke til utland” etter
                    attestering.
                  </HelpText>
                </HStack>
              </Checkbox>
            </div>
          </VStack>
        )}
        <Box width="15rem">
          <Textarea
            label="Begrunnelse"
            placeholder="Valgfritt"
            minRows={3}
            autoComplete="off"
            value={begrunnelse}
            onChange={(e) => {
              const oppdatertBegrunnelse = e.target.value
              setBegrunnelse(oppdatertBegrunnelse)
            }}
          />
        </Box>
        {isFailureHandler({
          apiResult: setBoddEllerArbeidetUtlandetStatus,
          errorMessage: 'Kunne ikke lagre bodd eller arbeidet i utlandet',
        })}
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
