import { IAction } from "../AppContext"


export interface IDetaljertBehandling {
  id: number;
  sak: number;
  grunnlag: [],
  vilkaarsproving: any;
  beregning: any;
  fastsatt: boolean;
}


export const detaljertBehandlingInitialState: IDetaljertBehandling = {
  id: 0,
  sak: 0,
  grunnlag: [],
  vilkaarsproving: undefined,
  beregning: undefined,
  fastsatt: false
}


export const behandlingReducer = (state = detaljertBehandlingInitialState, action: IAction): any => {

  switch(action.type) {
    case "add_behandling": 
      return {
        ...state,
        ...action.data
      }
      default: state
  }

}