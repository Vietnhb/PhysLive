import { useEffect, useState } from "react";
import axios from "axios";
import NavBar from "../components/NavBar";
import { getToken } from "../utils/token";
import { getMe } from "../api/userApi";
import type { User } from "../types/user";
import { useNavigate } from "react-router-dom";
function Home() {
    const token = getToken();
    const [content, setContent] = useState<"home" | "support" | "care">("home");
    const [count, setCount] = useState(0);
    const [todos, setTodos] = useState<string[]>([]);
    const [input, setInput] = useState("");
    const [error, setError] = useState("");
    const [user, setUser] = useState<User | null>(null);
    const navigate = useNavigate();
    useEffect(() => {
        if (!token) return;
        void getMe()
            .then((data) => { setUser(data); })
            .catch((err: unknown) => {
                if (axios.isAxiosError<{ message?: string }>(err)) {
                    setError(err.response?.data?.message ?? "Không thể tải thông tin người dùng");
                    return;
                }
                setError("Không thể tải thông tin người dùng");
            });
    }, [token])

    const addTodo = () => {
        if (input.trim() === "") return;
        setTodos([...todos, input]);
        setInput("");
    }

    const removeTodo = (index: number) => {
        setTodos(todos.filter((_, i) => i !== index));
    }
    return (
        <div>
            <NavBar onChange={setContent} />
            {content === "home" &&
                <>
                    <h1>Trang Chu</h1>
                    {token && (
                        <div>
                            <p>Id: {user?.id}</p>
                            <p>Email: {user?.email}</p>
                            <p>FullName: {user?.fullName}</p>
                            <p>Role: {user?.role}</p>
                        </div>
                    )}
                    {user?.role == "ADMIN" && (<button onClick={() => navigate("/admin")}  >Trang Quan Ly</button>)}
                    {!token && (<h5>Chua dang nhap</h5>)}
                    {error && <p style={{ color: "red" }}>{error}</p>}
                    <div style={{
                        display: "flex",
                        gap: "20px",
                    }}>
                        <h1 onClick={() => setCount(count - 1)}>-</h1>
                        <h1>{count}</h1>
                        <h1 onClick={() => setCount(count + 1)}>+</h1>
                    </div>
                    <div>
                        <h1>Todos List</h1>
                        <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Nhap CV" ></input>
                        <button onClick={addTodo}>Save</button>
                        <ul>{todos.map((todo, index) => (
                            <li key={index} > {todo}
                                <button onClick={() => removeTodo(index)} >Remove</button>
                            </li>
                        ))}</ul>
                    </div>
                </>
            }
            {content === "support" && <h1>CSKH</h1>}
            {content === "care" && <h1>Ho Tro</h1>}
        </div >
    )
}
export default Home;
