import React from "react";
import { combineReducers } from "./combineReducers";
import { IMenuReducer, menuReducer, menuReducerInitialState } from "./reducers/MenuReducer";
import { IUserReducer, userReducerInitialState, userReducer } from "./reducers/UserReducer";

export interface IAppState {
  menuReducer: IMenuReducer;
  userReducer: IUserReducer;
}

export interface IAction {
  type: string;
  data?: any
}

export interface IAppContext {
  state: IAppState;
  dispatch: (action: IAction) => void;
}

export const initialState: IAppState = {
  menuReducer: menuReducerInitialState,
  userReducer: userReducerInitialState
};

const reducer = combineReducers({ menuReducer, userReducer });

export const AppContext = React.createContext<any>({});

export const ContextProvider = (props: any) => {
  const [state, dispatch] = React.useReducer<any>(reducer, initialState);

  return (
    <AppContext.Provider value={{state, dispatch}}>
      {props.children}
    </AppContext.Provider>
  );
};
