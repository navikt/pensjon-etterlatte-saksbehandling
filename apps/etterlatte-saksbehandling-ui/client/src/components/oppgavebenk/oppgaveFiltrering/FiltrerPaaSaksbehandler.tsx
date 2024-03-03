import React from 'react'
import { UNSAFE_Combobox } from '@navikt/ds-react'
import { Filter, SAKSBEHANDLERFILTER } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { Saksbehandler } from '~shared/types/saksbehandler'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
  filter: Filter
  setFilter: (filer: Filter) => void
}

export const FiltrerPaaSaksbehandler = ({ filter, setFilter, saksbehandlereIEnhet }: Props) => {
  const saksbehandlerFilterValues = (): string[] => {
    return Object.entries(SAKSBEHANDLERFILTER).map(([, beskrivelse]) => beskrivelse)
  }

  const valgtSaksbehandler = (sb: string): string[] => {
    if (saksbehandlerFilterValues().find((val) => val === sb)) {
      return [sb]
    } else {
      const selectedSaksbehandler: Saksbehandler = saksbehandlereIEnhet.find((behandler) => behandler.ident === sb) || {
        navn: '',
        ident: '',
      }
      return [selectedSaksbehandler.navn]
    }
  }

  const onVelgSaksbehandler = (option: string, isSelected: boolean) => {
    if (isSelected) {
      if (saksbehandlerFilterValues().find((val) => val === option)) {
        setFilter({ ...filter, saksbehandlerFilter: option })
      } else {
        const selectedSaksbehandler: Saksbehandler = saksbehandlereIEnhet.find(
          (behandler) => behandler.navn === option
        ) || { navn: '', ident: '' }
        setFilter({ ...filter, saksbehandlerFilter: selectedSaksbehandler?.ident })
      }
    } else {
      setFilter({ ...filter, saksbehandlerFilter: '' })
    }
  }

  return (
    <UNSAFE_Combobox
      label="Saksbehandler"
      options={saksbehandlerFilterValues().concat(Array.from(saksbehandlereIEnhet.map((behandler) => behandler.navn)))}
      selectedOptions={valgtSaksbehandler(filter.saksbehandlerFilter)}
      onToggleSelected={onVelgSaksbehandler}
    />
  )
}
