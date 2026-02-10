import { HStack, UNSAFE_Combobox } from '@navikt/ds-react'
import { Journalposttype, Journalstatus, Tema } from '~shared/types/Journalpost'
import { Dispatch, SetStateAction } from 'react'

const TEMA_FILTER: Record<Tema, string> = {
  EYB: 'Barnepensjon',
  EYO: 'Omstillingsstønad',
  PEN: 'Pensjon',
} as const

const TYPE_FILTER: Record<Journalposttype, string> = {
  I: 'Inngående',
  U: 'Utgående',
  N: 'Notat',
} as const

const STATUS_FILTER: Record<Journalstatus, string> = {
  MOTTATT: 'Mottatt',
  JOURNALFOERT: 'Journalført',
  FERDIGSTILT: 'Ferdigstilt',
  EKSPEDERT: 'Ekspedert',
  UNDER_ARBEID: 'Under arbeid',
  FEILREGISTRERT: 'Feilregistrert',
  UTGAAR: 'Utgår',
  AVBRUTT: 'Avbrutt',
  UKJENT_BRUKER: 'Ukjent bruker',
  RESERVERT: 'Reservert',
  OPPLASTING_DOKUMENT: 'Opplasting dokument',
  UKJENT: 'Ukjent',
} as const

export interface DokumentFilter {
  tema: Tema[]
  type: Journalposttype[]
  status: Journalstatus[]
  foerste?: number
  etter?: string
}

export const DokumentFilter = ({
  filter,
  setFilter,
}: {
  filter: DokumentFilter
  setFilter: Dispatch<SetStateAction<DokumentFilter>>
}) => (
  <HStack gap="space-4">
    <UNSAFE_Combobox
      label="Tema"
      options={Object.entries(TEMA_FILTER).map(([, tema]) => tema)}
      selectedOptions={filter.tema.map((key) => TEMA_FILTER[key])}
      onToggleSelected={(option, isSelected) => {
        const tema = Object.keys(TEMA_FILTER).find((key) => TEMA_FILTER[key as Tema] === option)
        if (isSelected) {
          setFilter({ ...filter, tema: [...filter.tema, tema as Tema] })
        } else {
          setFilter({ ...filter, tema: filter.tema.filter((t) => t !== tema) })
        }
      }}
      isMultiSelect
    />

    <UNSAFE_Combobox
      label="Type"
      options={Object.entries(TYPE_FILTER).map(([, value]) => value)}
      selectedOptions={filter.type.map((key) => TYPE_FILTER[key])}
      onToggleSelected={(option, isSelected) => {
        const type = Object.keys(TYPE_FILTER).find((key) => TYPE_FILTER[key as Journalposttype] === option)
        if (isSelected) {
          setFilter({ ...filter, type: [...filter.type, type as Journalposttype] })
        } else {
          setFilter({ ...filter, type: filter.type.filter((t) => t !== type) })
        }
      }}
      isMultiSelect
    />

    <UNSAFE_Combobox
      label="Status"
      options={Object.entries(STATUS_FILTER).map(([, value]) => value)}
      selectedOptions={filter.status.map((key) => STATUS_FILTER[key])}
      onToggleSelected={(option, isSelected) => {
        const status = Object.keys(STATUS_FILTER).find((key) => STATUS_FILTER[key as Journalstatus] === option)
        if (isSelected) {
          setFilter({ ...filter, status: [...filter.status, status as Journalstatus] })
        } else {
          setFilter({ ...filter, status: filter.status.filter((t) => t !== status) })
        }
      }}
      isMultiSelect
    />
  </HStack>
)
