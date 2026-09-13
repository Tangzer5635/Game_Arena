import api from "./api";
import type { RegisterRequest } from "../types/auth";

export const register = async (
    data: RegisterRequest
): Promise<void> => {
    await api.post("/users/", data);
};