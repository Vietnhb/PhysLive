import axios from "axios";
import { API_URL } from "../config/api";
import { getToken } from "../utils/token";

const axiosClient = axios.create({
    baseURL: API_URL
});
axiosClient.interceptors.request.use((config) => {
    const token = getToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config;
});
export default axiosClient;