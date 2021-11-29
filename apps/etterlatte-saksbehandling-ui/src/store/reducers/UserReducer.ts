import { IAction } from "../AppContext";

export interface IUserReducer {
  loggedIn: boolean;
  fnr: string;
}

export const userReducerInitialState: IUserReducer = {
  loggedIn: false,
  fnr: ""
};

export const userReducer = (state: IUserReducer, action: IAction) => {
  console.log('her')
  switch (action.type) {
    case "login":
      console.log(action.data)
      return {
        ...state,
        loggedIn: action.data.loggedIn
      };
    default:
      return state;
  }
}