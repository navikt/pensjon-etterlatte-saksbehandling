import { IAction } from "../AppContext";

export interface IMenuReducer {
  open: boolean;
}

export const menuReducerInitialState: IMenuReducer = {
  open: false,
};

export const menuReducer = (state: IMenuReducer, action: IAction) => {
  switch (action.type) {
   
    case "toggle":
      return {
        ...state,
        open: !state.open
      };
    default:
      return state;
  }
};