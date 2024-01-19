import { ReactElement } from 'react'
import { SakType } from '../types/sak'

interface MapSakTypeProps {
  saktype: SakType
  barnepensjon: ReactElement
  omstillingsstoenad: ReactElement
}

export const MapSakType = (props: MapSakTypeProps) => {
  switch (props.saktype) {
    case SakType.OMSTILLINGSSTOENAD:
      return props.omstillingsstoenad
    case SakType.BARNEPENSJON:
      return props.barnepensjon
  }
}
