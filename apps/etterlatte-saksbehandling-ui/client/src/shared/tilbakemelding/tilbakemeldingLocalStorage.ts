import { ClickEvent } from '~utils/amplitude'
import { logger } from '~utils/logger'

const BEHANDLINGER_HVOR_TILBAKEMELDING_ER_GITT_KEY = 'behandlingerHvorTilbakemeldingErGitt'

interface TilbakemeldingGittIBehandling {
  behandlingId: string
  clickEvent: ClickEvent
}

export const leggTilbakemeldingGittILocalStorage = (tilbakemeldingGitt: TilbakemeldingGittIBehandling) => {
  const tilbakemeldingerGitt: Array<TilbakemeldingGittIBehandling> = [...hentTilbakemeldingerGittFraLocalStorage()]
  tilbakemeldingerGitt.push(tilbakemeldingGitt)
  localStorage.setItem(BEHANDLINGER_HVOR_TILBAKEMELDING_ER_GITT_KEY, JSON.stringify(tilbakemeldingerGitt))
}

export const tilbakemeldingForBehandlingEksisterer = (tilbakemeldingGitt: TilbakemeldingGittIBehandling): boolean => {
  return !!hentTilbakemeldingerGittFraLocalStorage().find(
    (value) => JSON.stringify(value) === JSON.stringify(tilbakemeldingGitt)
  )
}

const hentTilbakemeldingerGittFraLocalStorage = (): Array<TilbakemeldingGittIBehandling> => {
  try {
    const tilbakemeldingerGitt = localStorage[BEHANDLINGER_HVOR_TILBAKEMELDING_ER_GITT_KEY]
    if (!!tilbakemeldingerGitt) return JSON.parse(tilbakemeldingerGitt)
    else return []
  } catch {
    logger.generalError({ msg: 'Feil i hentinger av tilbakemeldinger gitt i behandlinger fra localstorage' })

    return []
  }
}
