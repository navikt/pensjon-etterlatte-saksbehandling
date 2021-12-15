import { renderHook } from "@testing-library/react-hooks";
import { useContext } from "react";
import { AppContext, reducer } from "../store/AppContext";

describe("test appcontext", () => {
    it("context should have default objects", () => {
        const { result } = renderHook(() => useContext(AppContext));
        expect(result.current).toHaveProperty("state");
        expect(result.current).toHaveProperty("dispatch");
    });

    it("context should have data-objects", () => {
        const { result } = renderHook(() => useContext(AppContext));
        expect(result.current.state).toHaveProperty("menuReducer");
        expect(result.current.state).toHaveProperty("userReducer");
    });

    it("should toggle menu", async () => {
        const { result } = renderHook(() => useContext(AppContext));
        expect(result.current.state.menuReducer.open).toBe(false);

        const newState = reducer(result.current.state, {type: "toggle"});
        expect(newState.menuReducer.open).toBe(true);
    });
});
