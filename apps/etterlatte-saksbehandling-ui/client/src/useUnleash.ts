import { createContext, useContext, useEffect } from 'react'
import { IFeature, Status } from '~shared/types/IFeature'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentFeatureToggles } from '~shared/api/unleash'
import { useDispatch } from 'react-redux'
import { endreToggle, useUnleashReducer, useUnleashReducerToggle } from '~store/reducers/UnleashReducer'
import { throttle } from 'lodash'

export const enum FeatureToggle {
  sanksjon = 'sanksjon',
  aktivitetsplikt_ny_vurdering = 'aktivitetsplikt.ny-vurdering',
  validere_aarsintnekt_neste_aar = 'validere_aarsintnekt_neste_aar',
  overstyr_beregning_knapp = 'overstyr-beregning-knapp',
  notater = 'notater',
  kun_en_registrert_juridisk_forelder = 'kun-en-registrert-juridisk-forelder',
  kopier_trygdetidsgrunnlag = 'kopier-trygdetidsgrunnlag',
  kan_redigere_journalpost_bruker = 'kan_redigere_journalpost_bruker',
  opprette_generell_oppgave = 'opprette-generell-oppgave',
  pensjon_etterlatte_klage_delvis_omgjoering = 'pensjon-etterlatte.klage-delvis-omgjoering',
  pensjon_etterlatte_kan_opprette_vedtak_avvist_klage = 'pensjon-etterlatte.kan-opprette-vedtak-avvist-klage',
  pensjon_etterlatte_oppdater_ident_paa_sak = 'pensjon-etterlatte.oppdater-ident-paa-sak',
}

export interface Toggle {
  togglename: FeatureToggle
  enabled: boolean
}

const sanksjon: Toggle = { togglename: FeatureToggle.sanksjon, enabled: false }
const aktivitetsplikt_ny_vurdering: Toggle = {
  togglename: FeatureToggle.aktivitetsplikt_ny_vurdering,
  enabled: false,
}
const validere_aarsintnekt_neste_aar: Toggle = {
  togglename: FeatureToggle.validere_aarsintnekt_neste_aar,
  enabled: false,
}
const overstyr_beregning_knapp: Toggle = {
  togglename: FeatureToggle.overstyr_beregning_knapp,
  enabled: false,
}
const notater: Toggle = { togglename: FeatureToggle.notater, enabled: false }
const kun_en_registrert_juridisk_forelder: Toggle = {
  togglename: FeatureToggle.kun_en_registrert_juridisk_forelder,
  enabled: false,
}
const kopier_trygdetidsgrunnlag: Toggle = {
  togglename: FeatureToggle.kopier_trygdetidsgrunnlag,
  enabled: false,
}
const kan_redigere_journalpost_bruker: Toggle = {
  togglename: FeatureToggle.kan_redigere_journalpost_bruker,
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
const pensjon_etterlatte_oppdater_ident_paa_sak: Toggle = {
  togglename: FeatureToggle.pensjon_etterlatte_oppdater_ident_paa_sak,
  enabled: false,
}

export const unleashStartState: Record<string, Toggle> = {
  sanksjon: sanksjon,
  'aktivitetsplikt.ny-vurdering': aktivitetsplikt_ny_vurdering,
  validere_aarsintnekt_neste_aar: validere_aarsintnekt_neste_aar,
  notater: notater,
  'kun-en-registrert-juridisk-forelder': kun_en_registrert_juridisk_forelder,
  kopier_trygdetidsgrunnlag: kopier_trygdetidsgrunnlag,
  kan_redigere_journalpost_bruker: kan_redigere_journalpost_bruker,
  'opprette-generell-oppgave': opprette_generell_oppgave,
  'pensjon-etterlatte.klage-delvis-omgjoering': pensjon_etterlatte_klage_delvis_omgjoering,
  'pensjon-etterlatte.kan-opprette-vedtak-avvist-klage': pensjon_etterlatte_kan_opprette_vedtak_avvist_klage,
  'overstyr-beregning-knapp': overstyr_beregning_knapp,
  [FeatureToggle.pensjon_etterlatte_oppdater_ident_paa_sak]: pensjon_etterlatte_oppdater_ident_paa_sak,
}

export const Unleashcontext = createContext<{
  updateToggle: () => void
}>({
  updateToggle: () => {},
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

export const useUnleash = () => {
  const dispatch = useDispatch()
  const [, fetchFeature] = useApiCall(hentFeatureToggles)
  const unleashState = useUnleashReducer()

  const updateToggle = () => {
    const toggles = Object.keys(unleashState)
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

  return { updateToggle: throttle(updateToggle, 1000) }
}

export const useFeaturetoggle = (featureToggle: FeatureToggle): boolean => {
  const { updateToggle } = useContext(Unleashcontext)
  const toggle = useUnleashReducerToggle(featureToggle)

  useEffect(() => {
    updateToggle()
  }, [])

  return toggle.enabled
}
