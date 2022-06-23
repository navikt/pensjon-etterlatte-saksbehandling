import { IVilkaarsproving } from '../../../store/reducers/BehandlingReducer'

export enum Status {
  DONE = 'done',
  NOT_DONE = 'not done',
}

export interface VilkaarProps {
  id: string
  vilkaar: IVilkaarsproving | undefined
}
