import React from 'react'
import { UNSAFE_Combobox } from '@navikt/ds-react'
import { Filter, SAKSBEHANDLERFILTER } from '~components/oppgavebenk/filtreringAvOppgaver/oppgavelistafiltre'
import { Saksbehandler } from '~shared/types/saksbehandler'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  filter: Filter
  setFilter: (filer: Filter) => void
}

export const FiltrerPaaSaksbehandler = ({ filter, setFilter, saksbehandlereIEnhet }: Props) => {
  const saksbehandlerFilterValues: string[] = Object.entries(SAKSBEHANDLERFILTER).map(([, beskrivelse]) => beskrivelse)
  const options = [...saksbehandlerFilterValues, ...saksbehandlereIEnhet.map((behandler) => behandler.navn)]

  /*
   * Først sjekkke om valgt saksbehandler finnes i SAKSBEHANDLERFILTER.
   * Hvis ikke sjekk om den finnes i lista over saksbehandlere i enhetene til
   * den innloggede saksbehandleren
   */
  const valgtSaksbehandler = (sb: string): string[] => {
    if (saksbehandlerFilterValues.find((val) => val === sb)) {
      return [sb]
    } else {
      const selectedSaksbehandlerNavn = saksbehandlereIEnhet.find((behandler) => behandler.ident === sb)?.navn || ''
      return [selectedSaksbehandlerNavn]
    }
  }

  /*
   * Samme her. Først sjekkke om valgt saksbehandler finnes i SAKSBEHANDLERFILTER.
   * Hvis ikke sjekk om den finnes i lista over saksbehandlere i enhetene til
   * den innloggede saksbehandleren
   */
  const onVelgSaksbehandler = (option: string, isSelected: boolean) => {
    if (isSelected) {
      if (saksbehandlerFilterValues.find((val) => val === option)) {
        setFilter({ ...filter, saksbehandlerFilter: option })
      } else {
        const selectedSaksbehandlerIdent =
          saksbehandlereIEnhet.find((behandler) => behandler.navn === option)?.ident || ''
        setFilter({ ...filter, saksbehandlerFilter: selectedSaksbehandlerIdent })
      }
    } else {
      setFilter({ ...filter, saksbehandlerFilter: '' })
    }
  }

  return (
    <UNSAFE_Combobox
      label="Saksbehandler"
      options={options}
      selectedOptions={valgtSaksbehandler(filter.saksbehandlerFilter)}
      onToggleSelected={onVelgSaksbehandler}
    />
  )
}
