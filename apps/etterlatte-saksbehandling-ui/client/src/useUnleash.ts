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
  sanksjon = 'sanksjon',
  overstyr_beregning_knapp = 'overstyr-beregning-knapp',
  notater = 'notater',
  kopier_trygdetidsgrunnlag = 'kopier-trygdetidsgrunnlag',
  opprette_generell_oppgave = 'opprette-generell-oppgave',
  pensjon_etterlatte_klage_delvis_omgjoering = 'pensjon-etterlatte.klage-delvis-omgjoering',
  pensjon_etterlatte_kan_opprette_vedtak_avvist_klage = 'pensjon-etterlatte.kan-opprette-vedtak-avvist-klage',
  trygdetid_fra_pesys = 'trygdetid-fra-pesys',
  opprette_oppfoelgingsoppgave = 'opprette-oppfoelgingsoppgave',
  aktivitetsplikt_oppgave_unntak_uten_frist = 'aktivitetsplikt-oppgave-unntak-uten-frist',
  aktivitetsplikt_oppgave_unntak_med_frist = 'aktivitetsplikt-oppgave-unntak-med-frist',
  bytt_til_annen_sak = 'bytt-til-annen-sak',
  oppgave_til_journalpost = 'oppgave-til-journalpost',
  etteroppgjoer_dev_opprett_forbehandling = 'etteroppgjoer-dev-opprett-forbehandling',
  vis_tilbakestill_etteroppgjoer = 'vis-tilbakestill-etteroppgjoer',
  omgjoer_tilbakekreving = 'omgjoer-tilbakekreving',
  overstyr_netto_brutto_tilbakekreving = 'overstyr-netto-brutto-tilbakekreving',
  avslutte_omgjoeringsoppgave = 'avslutte-omgjoeringsoppgave',
}

export interface Toggle {
  togglename: FeatureToggle
  enabled: boolean
}

const avslutte_omgjoeringsoppgave: Toggle = {
  togglename: FeatureToggle.avslutte_omgjoeringsoppgave,
  enabled: false,
}

const trygdetid_fra_pesys: Toggle = {
  togglename: FeatureToggle.trygdetid_fra_pesys,
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

const sanksjon: Toggle = { togglename: FeatureToggle.sanksjon, enabled: false }

const overstyr_beregning_knapp: Toggle = {
  togglename: FeatureToggle.overstyr_beregning_knapp,
  enabled: false,
}
const notater: Toggle = { togglename: FeatureToggle.notater, enabled: false }
const kopier_trygdetidsgrunnlag: Toggle = {
  togglename: FeatureToggle.kopier_trygdetidsgrunnlag,
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
const opprette_oppfoelgingsoppgave: Toggle = {
  togglename: FeatureToggle.opprette_oppfoelgingsoppgave,
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

const bytt_til_annen_sak: Toggle = {
  togglename: FeatureToggle.bytt_til_annen_sak,
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

export const unleashStartState: Record<string, Toggle> = {
  [FeatureToggle.trygdetid_fra_pesys]: trygdetid_fra_pesys,
  [FeatureToggle.sanksjon]: sanksjon,
  [FeatureToggle.notater]: notater,
  [FeatureToggle.kopier_trygdetidsgrunnlag]: kopier_trygdetidsgrunnlag,
  [FeatureToggle.opprette_generell_oppgave]: opprette_generell_oppgave,
  [FeatureToggle.pensjon_etterlatte_klage_delvis_omgjoering]: pensjon_etterlatte_klage_delvis_omgjoering,
  [FeatureToggle.pensjon_etterlatte_kan_opprette_vedtak_avvist_klage]:
    pensjon_etterlatte_kan_opprette_vedtak_avvist_klage,
  [FeatureToggle.overstyr_beregning_knapp]: overstyr_beregning_knapp,
  [FeatureToggle.opprette_oppfoelgingsoppgave]: opprette_oppfoelgingsoppgave,
  [FeatureToggle.aktivitetsplikt_oppgave_unntak_med_frist]: aktivitetsplikt_oppgave_unntak_med_frist,
  [FeatureToggle.aktivitetsplikt_oppgave_unntak_uten_frist]: aktivitetsplikt_oppgave_unntak_uten_frist,
  [FeatureToggle.bytt_til_annen_sak]: bytt_til_annen_sak,
  [FeatureToggle.oppgave_til_journalpost]: oppgave_til_journalpost,
  [FeatureToggle.etteroppgjoer_dev_opprett_forbehandling]: etteroppgjoer_dev_opprett_forbehandling,
  [FeatureToggle.vis_tilbakestill_etteroppgjoer]: vis_tilbakestill_etteroppgjoer,
  [FeatureToggle.omgjoer_tilbakekreving]: omgjoer_tilbakekreving,
  [FeatureToggle.overstyr_netto_brutto_tilbakekreving]: overstyr_netto_brutto_tilbakekreving,
  [FeatureToggle.avslutte_omgjoeringsoppgave]: avslutte_omgjoeringsoppgave,
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

export const useFeaturetoggle = (featureToggle: FeatureToggle): boolean => {
  const { updateToggle, logWithThrottle } = useContext(Unleashcontext)
  const toggle = useUnleashReducerToggle(featureToggle)

  useEffect(() => {
    if (toggle) {
      if (lastUpdate + 60_0000 < Date.now()) {
        updateToggle()
        lastUpdate = Date.now()
      }
    } else {
      logWithThrottle(featureToggle)
    }
  }, [])

  return toggle ? toggle.enabled : false
}
