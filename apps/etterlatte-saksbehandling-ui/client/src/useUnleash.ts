import { createContext, useContext, useEffect } from 'react'
import { IFeature, Status } from '~shared/types/IFeature'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFeatureToggles } from '~shared/api/unleash'
import { useDispatch } from 'react-redux'
import { endreToggle, useUnleashReducer, useUnleashReducerToggle } from '~store/reducers/UnleashReducer'
import { throttle } from 'lodash'
import { logger } from '~utils/logger'
import { isPending } from '~shared/api/apiUtils'

export const enum FeatureToggle {
  overstyr_beregning_knapp = 'overstyr-beregning-knapp',
  opprette_generell_oppgave = 'opprette-generell-oppgave',
  pensjon_etterlatte_klage_delvis_omgjoering = 'pensjon-etterlatte.klage-delvis-omgjoering',
  pensjon_etterlatte_kan_opprette_vedtak_avvist_klage = 'pensjon-etterlatte.kan-opprette-vedtak-avvist-klage',
  aktivitetsplikt_oppgave_unntak_uten_frist = 'aktivitetsplikt-oppgave-unntak-uten-frist',
  aktivitetsplikt_oppgave_unntak_med_frist = 'aktivitetsplikt-oppgave-unntak-med-frist',
  oppgave_til_journalpost = 'oppgave-til-journalpost',
  etteroppgjoer_dev_opprett_forbehandling = 'etteroppgjoer-dev-opprett-forbehandling',
  vis_tilbakestill_etteroppgjoer = 'vis-tilbakestill-etteroppgjoer',
  omgjoer_tilbakekreving = 'omgjoer-tilbakekreving',
  overstyr_netto_brutto_tilbakekreving = 'overstyr-netto-brutto-tilbakekreving',
  avslutte_omgjoeringsoppgave = 'avslutte-omgjoeringsoppgave',
  beregning_bruk_nye_beregningsregler = 'beregning_bruk_nye_beregningsregler',
  oppdater_inntekt_forbehandling = 'oppdater-inntekt-forbehandling',
}

export interface Toggle {
  togglename: FeatureToggle
  enabled: boolean
}

const beregning_bruk_nye_beregningsregler: Toggle = {
  togglename: FeatureToggle.beregning_bruk_nye_beregningsregler,
  enabled: false,
}

const avslutte_omgjoeringsoppgave: Toggle = {
  togglename: FeatureToggle.avslutte_omgjoeringsoppgave,
  enabled: false,
}

const overstyr_netto_brutto_tilbakekreving: Toggle = {
  togglename: FeatureToggle.overstyr_netto_brutto_tilbakekreving,
  enabled: false,
}

const oppgave_til_journalpost: Toggle = {
  togglename: FeatureToggle.oppgave_til_journalpost,
  enabled: false,
}

const overstyr_beregning_knapp: Toggle = {
  togglename: FeatureToggle.overstyr_beregning_knapp,
  enabled: false,
}

const opprette_generell_oppgave: Toggle = {
  togglename: FeatureToggle.opprette_generell_oppgave,
  enabled: false,
}
const pensjon_etterlatte_klage_delvis_omgjoering: Toggle = {
  togglename: FeatureToggle.pensjon_etterlatte_klage_delvis_omgjoering,
  enabled: false,
}
const pensjon_etterlatte_kan_opprette_vedtak_avvist_klage: Toggle = {
  togglename: FeatureToggle.pensjon_etterlatte_kan_opprette_vedtak_avvist_klage,
  enabled: false,
}

const aktivitetsplikt_oppgave_unntak_uten_frist: Toggle = {
  togglename: FeatureToggle.aktivitetsplikt_oppgave_unntak_uten_frist,
  enabled: false,
}

const aktivitetsplikt_oppgave_unntak_med_frist: Toggle = {
  togglename: FeatureToggle.aktivitetsplikt_oppgave_unntak_med_frist,
  enabled: false,
}

const etteroppgjoer_dev_opprett_forbehandling: Toggle = {
  togglename: FeatureToggle.etteroppgjoer_dev_opprett_forbehandling,
  enabled: false,
}

const vis_tilbakestill_etteroppgjoer: Toggle = {
  togglename: FeatureToggle.vis_tilbakestill_etteroppgjoer,
  enabled: false,
}

const omgjoer_tilbakekreving: Toggle = {
  togglename: FeatureToggle.omgjoer_tilbakekreving,
  enabled: false,
}

const oppdater_inntekt_forbehandling: Toggle = {
  togglename: FeatureToggle.oppdater_inntekt_forbehandling,
  enabled: false,
}

export const unleashStartState: Record<string, Toggle> = {
  [FeatureToggle.opprette_generell_oppgave]: opprette_generell_oppgave,
  [FeatureToggle.pensjon_etterlatte_klage_delvis_omgjoering]: pensjon_etterlatte_klage_delvis_omgjoering,
  [FeatureToggle.pensjon_etterlatte_kan_opprette_vedtak_avvist_klage]:
    pensjon_etterlatte_kan_opprette_vedtak_avvist_klage,
  [FeatureToggle.overstyr_beregning_knapp]: overstyr_beregning_knapp,
  [FeatureToggle.aktivitetsplikt_oppgave_unntak_med_frist]: aktivitetsplikt_oppgave_unntak_med_frist,
  [FeatureToggle.aktivitetsplikt_oppgave_unntak_uten_frist]: aktivitetsplikt_oppgave_unntak_uten_frist,
  [FeatureToggle.oppgave_til_journalpost]: oppgave_til_journalpost,
  [FeatureToggle.etteroppgjoer_dev_opprett_forbehandling]: etteroppgjoer_dev_opprett_forbehandling,
  [FeatureToggle.vis_tilbakestill_etteroppgjoer]: vis_tilbakestill_etteroppgjoer,
  [FeatureToggle.omgjoer_tilbakekreving]: omgjoer_tilbakekreving,
  [FeatureToggle.overstyr_netto_brutto_tilbakekreving]: overstyr_netto_brutto_tilbakekreving,
  [FeatureToggle.avslutte_omgjoeringsoppgave]: avslutte_omgjoeringsoppgave,
  [FeatureToggle.beregning_bruk_nye_beregningsregler]: beregning_bruk_nye_beregningsregler,
  [FeatureToggle.oppdater_inntekt_forbehandling]: oppdater_inntekt_forbehandling,
}

export const Unleashcontext = createContext<{
  updateToggle: () => void
  logWithThrottle: (featureToggle: FeatureToggle) => void
}>({
  updateToggle: () => {},
  logWithThrottle: () => {},
})

const mapStatusForToggle = (featureToggle: IFeature, defaultValue: boolean): boolean => {
  switch (featureToggle.enabled) {
    case Status.HENTING_FEILA:
      return defaultValue
    case Status.UDEFINERT:
      return defaultValue
    case Status.AV:
      return false
    case Status.PAA:
      return true
  }
}

const logMissingFeatureToggle = (featureToggle: FeatureToggle) => {
  const msg = `Ugyldig toggle registrert: ${featureToggle}`
  console.error(msg)
  logger.generalError({ msg: msg })
}

export const useUnleash = () => {
  const dispatch = useDispatch()
  const [fetchResult, fetchFeature] = useApiCall(hentFeatureToggles)
  const unleashState = useUnleashReducer()

  const updateToggle = () => {
    const toggles = Object.keys(unleashState)
    if (isPending(fetchResult)) {
      return
    }
    fetchFeature(
      toggles,
      (hentedeToggles) => {
        hentedeToggles.map((feature) => {
          const enabled = mapStatusForToggle(feature, unleashStartState[feature.toggle].enabled)
          dispatch(endreToggle({ togglename: feature.toggle, enabled: enabled }))
        })
      },
      () => {
        console.warn(`Henting feilet for toggles`)
      }
    )
  }

  return { updateToggle: throttle(updateToggle, 1000), logWithThrottle: throttle(logMissingFeatureToggle, 1000) }
}

let lastUpdate = -1
const ET_MINUTT_MILLISEKUNDER = 60 * 1000

export const useFeaturetoggle = (featureToggle: FeatureToggle): boolean => {
  const { updateToggle, logWithThrottle } = useContext(Unleashcontext)
  const toggle = useUnleashReducerToggle(featureToggle)

  useEffect(() => {
    if (toggle) {
      if (lastUpdate + ET_MINUTT_MILLISEKUNDER < Date.now()) {
        updateToggle()
        lastUpdate = Date.now()
      }
    } else {
      logWithThrottle(featureToggle)
    }
  }, [])

  return toggle ? toggle.enabled : false
}
